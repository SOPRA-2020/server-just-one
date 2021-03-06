package ch.uzh.ifi.seal.soprafs20.service;

import ch.uzh.ifi.seal.soprafs20.constant.CardStatus;
import ch.uzh.ifi.seal.soprafs20.constant.GameStatus;
import ch.uzh.ifi.seal.soprafs20.constant.UserStatus;
import ch.uzh.ifi.seal.soprafs20.entity.Game;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.exceptions.NotFoundException;
import ch.uzh.ifi.seal.soprafs20.exceptions.ServiceException;
import ch.uzh.ifi.seal.soprafs20.repository.GameRepository;
import ch.uzh.ifi.seal.soprafs20.repository.UserRepository;
import ch.uzh.ifi.seal.soprafs20.rest.dto.*;
import ch.uzh.ifi.seal.soprafs20.wordcheck.Stemmer;
import ch.uzh.ifi.seal.soprafs20.wordcheck.WordCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Game Service
 * This class is the "worker" and responsible for all functionality related to the game
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class GameService {

    private Logger log = LoggerFactory.getLogger(GameService.class);

    private GameRepository gameRepository;
    private UserRepository userRepository;
    private LobbyService lobbyService;

    private Random rand = new Random();
    private WordCheck wordChecker = new WordCheck();
    Stemmer stemCheck = new Stemmer();


    @Autowired
    public GameService(@Qualifier("gameRepository") GameRepository gameRepository, @Qualifier("userRepository") UserRepository userRepository, LobbyService lobbyService) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.lobbyService = lobbyService;
    }

    public Long createGame(List<Long> players) {
        if (players.size() < 3) {
            throw new ServiceException("Need more players to start a game");
        }
        if (players.size() > 7) {
            throw new ServiceException("Can only have a maximum of 7 players to start this game");
        }
        // Create the game with the provided player id's, populate the game word list, initialize score and round
        Game newGame = new Game();
        newGame.setPlayerIds(players);
        newGame.setGameStatus(GameStatus.AWAITING_INDEX);
        newGame.setCardStatus(CardStatus.AWAITING_INDEX);
        newGame.setRound(1);

        newGame.setRoundScore(0);
        newGame.setActivePlayerId(newGame.getPlayerIds().get(0));

        newGame.setWords(selectGameWords());
        newGame.setCardStackCount(13);
        newGame.setCardGuessedCount(0);
        newGame.setWordIndex(-1);

        // save the game to the database
        newGame = gameRepository.save(newGame);
        Long gameId = newGame.getId();
        gameRepository.flush();

        // add the game id to the players and remove them from the lobby
        List<Long> playerIds = newGame.getPlayerIds();
        for (Long playerId: playerIds) {
            User user = getExistingUser(playerId);
            user.setGameId(gameId);
            userRepository.save(user);
            userRepository.flush();
        }

        log.debug("Created Information for Game: {}", newGame);
        return gameId;
    }

    public ArrayList<String> selectGameWords() {
        // select 5 * 13 random unique words from the english word list
        // 5 * 13 from 5 words every round for 13 rounds
        ArrayList<String> gameWords = new ArrayList<>();
        ArrayList<String> allWords = getAllWordsFromWordList();
        for (int i =0; i < 5*13; i++) {
            int randomIndex = rand.nextInt(allWords.size());
            gameWords.add(allWords.get(randomIndex));
            allWords.remove(randomIndex);
        }
        return gameWords;
    }

    public ArrayList<String> getAllWordsFromWordList() {
        // Read in the entire word list and return it as a ArrayList of strings.
        String filename = "cards-EN.txt";
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream resource = classLoader.getResourceAsStream(filename);
        if (resource == null) {
            throw new IllegalArgumentException("The cards word list file was not found!");
        }
        ArrayList<String> words = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(resource));) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.equals("")) {
                    words.add(line);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new ServiceException("Error while reading word list");
        }
        return words;
    }

    public void chooseWord(long gameId, int wordIndex){
        Game game = getExistingGame(gameId);

        if (!game.getGameStatus().equals(GameStatus.AWAITING_INDEX)) {
            throw new ServiceException("The game cannot choose words in this state");
        }

        if(wordIndex>5){
            throw new ServiceException("Please enter the valid choices");
        }
        if(game.getLastWordIndex().contains(wordIndex)){
            throw new ServiceException("User not allowed to enter the same guess word");
        }

        /*
        65 words in game wordlist will be divided into 13 cards each containing 5 words.

        set of index no for each card should be (0,1...4),(5,6...9),......(60,61...64)
        */
        int generateNewIndex = 5*(game.getRound()-1)-1+wordIndex;
        /*
        e.g.
        input - gameRound 2, wordIndex - 3
        generateIndex = 5*(2-1)-1+3 = 7
         */
        game.setWordIndex(generateNewIndex);
        game.setTimestamp(Instant.now().getEpochSecond());
        /*
        We need to empty the previous clues in case user is choosing the new word after rejection.
         */
        game.getClues().clear();
        game.getLastWordIndex().add(wordIndex);
        game.setGameStatus(GameStatus.ACCEPT_REJECT);
        //game.setCardStatus(CardStatus.AWAITING_CLUES); //Card status should stay on AWAITING_INDEX
        gameRepository.save(game);
        gameRepository.flush();

    }

    /*
        this method will set the card status of the game to USER_REJECTED_WORD
        and will save the lastWordIndex in order to stop the user from entering the same word
        as of last time.
     */
    public void rejectWord(long id){
        Game game = getExistingGame(id);

        if (!game.getGameStatus().equals(GameStatus.ACCEPT_REJECT)) {
            throw new ServiceException("The game cannot reject words in this state");
        }

        long clueTime = game.getTimestamp();
        long nowTime = Instant.now().getEpochSecond();
        long elapsedSeconds = nowTime-clueTime;

        if(elapsedSeconds>30){
            throw new ServiceException("Cannot reject word after 30 seconds");
        }

        if(game.getLastWordIndex().size()>3){
            throw new ServiceException("Cannot reject more than 3 words");
        }
        if(game.getGameStatus().equals(GameStatus.AWAITING_GUESS)){
            throw new ServiceException("Cannot reject word when all the clues have been submitted");
        }
        game.setWordIndex(-1);
        game.setCardStatus(CardStatus.USER_REJECTED_WORD);
        game.setGameStatus(GameStatus.AWAITING_INDEX);
        game.getCountAccept().clear();
        gameRepository.save(game);
        gameRepository.flush();
    }

    /*
    This Method should set the gameStatus and cardStatus according to an accepted word
     */
    public void acceptWord(long gameId, long userId) {
        Game game = getExistingGame(gameId);

        if (!game.getGameStatus().equals(GameStatus.ACCEPT_REJECT)) {
            throw new ServiceException("The game cannot accept words in this state");
        }

        List<Long> userAccepted = game.getCountAccept();
        userAccepted.add(userId);
        game.setCountAccept(userAccepted);

        if(game.getCountAccept().size() == game.getPlayerIds().size() - 1) {
            game.setCardStatus(CardStatus.AWAITING_CLUES);
            game.setGameStatus(GameStatus.AWAITING_CLUES);
            List<Long> empty = new ArrayList<>();
            game.setCountAccept(empty);

            //Time when user accept the word and from this time active player will have 30 seconds to guess the word
            game.setTimestamp(Instant.now().getEpochSecond());
        }

        gameRepository.save(game);
        gameRepository.flush();
    }

    public Game getExistingGame(long id) {
        Optional<Game> optionalGame = gameRepository.findById(id);
        if (!optionalGame.isPresent()) {
            throw new NotFoundException(String.format("Could not find game with id %d.", id));
        }
        return optionalGame.get();
    }

    public User getExistingUser(long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent()) {
            throw new NotFoundException(String.format("Could not find user with id %d.", id));
        }
        return optionalUser.get();
    }

    // checks if the mysteryWord matches with the guess
    public GamePutDTO checkGuess(GamePutDTO gamePutDTO, long id) {
        Game game = getExistingGame(id);
        if(!game.getGameStatus().equals(GameStatus.AWAITING_GUESS)){
            throw new ServiceException("The game cannot check guesses in this state");
        }

        int index = gamePutDTO.getWordIndex();
        String guess = gamePutDTO.getGuess();
        String mysteryWord = game.getWords().get(index);

        // check if guess took too long
        GamePutDTO returnedDTO = new GamePutDTO();
        long guessTime = game.getTimestamp();
        long nowTime = Instant.now().getEpochSecond();
        long elapsedSeconds = nowTime-guessTime;

        if(elapsedSeconds>30){
            game.setRoundScore(game.getRoundScore()+(100/(int)elapsedSeconds)-50);

            returnedDTO.setGuessCorrect("timeout");
            //Handle according to a wrong guess -> this card and the next card is put away
            game.setCardStackCount(game.getCardStackCount() - 2);
            game.setCardGuessedCount(game.getCardGuessedCount() + 1);
            game.setWordsGuessedWrong(game.getWordsGuessedWrong() + 1);
        }
        //Skipped Guess
        else if (guess.equals("SKIP")) {
            returnedDTO.setGuessCorrect("skip");
            //handle according to a skipped guess -> the card is put away
            game.setCardStackCount(game.getCardStackCount() - 1);
        }
        //Successful Guess
        else if (mysteryWord.equalsIgnoreCase(guess)) {
            returnedDTO.setGuessCorrect("correct");
            //set the guesses and card numbers according to a correct guess
            game.setWordsGuessedCorrect(game.getWordsGuessedCorrect() + 1);
            game.setCardGuessedCount(game.getCardGuessedCount() + 1);

            game.setCardStackCount(game.getCardStackCount() - 1);

            game.setRoundScore(game.getRoundScore()+(100/(int)elapsedSeconds)+100);

        }
        // Wrong Guess
        else {
            game.setRoundScore(game.getRoundScore()+(100/(int)elapsedSeconds)-50);
            returnedDTO.setGuessCorrect("wrong");
            //Handle according to a wrong guess -> this card and the next card is put away
            game.setCardStackCount(game.getCardStackCount() - 2);
            game.setCardGuessedCount(game.getCardGuessedCount() + 1);
            game.setWordsGuessedWrong(game.getWordsGuessedWrong() + 1);
        }

        // call the function "roundEnd" to set all the information needed for a new round
        //  or wrap up the game if no cards are left on the stack
        game.setRound(game.getRound() + 1);
        roundEnd(game);

        gameRepository.save(game);
        gameRepository.flush();
        return returnedDTO;
    }

    private void roundEnd (Game game) {
        // check how many cards are left on the stack
        if (game.getCardStackCount() <= 0) {
            game.setGameStatus(GameStatus.GAME_OVER);
            return;
        }
        game.setGameStatus(GameStatus.AWAITING_INDEX);
        game.setCardStatus(CardStatus.AWAITING_INDEX);

        //Updating score of the player and also storing the score of previous rounds
        int scoreForRound = game.getRoundScore()/10;

        updateUserScore(game.getActivePlayerId(), scoreForRound);

        //Adding the score of this round to scoreboard
        if(game.getScore().containsKey(game.getActivePlayerId())) {
            int scoreOfPlayer = game.getScore().get(game.getActivePlayerId());
            scoreForRound += scoreOfPlayer;
        }
        game.getScore().put(game.getActivePlayerId(),scoreForRound);

        // select the next player
        List<Long> activePlayerId = game.getPlayerIds();
        int activePlayerIndex = (activePlayerId.indexOf(game.getActivePlayerId()) + 1 ) % activePlayerId.size();
        game.setActivePlayerId(game.getPlayerIds().get(activePlayerIndex));

        // reset the clues
        List<String> clues = new ArrayList<>();
        game.setClues(clues);

        //reset the card Index
        game.setWordIndex(-1);

        //reset lastWordList
        game.getLastWordIndex().clear();

        //reset roundend score
        game.setRoundScore(0);
    }

    public void wrapup(long id, long playerId) {
        Game game = getExistingGame(id);

        //remove the player from the playerId list of the game
        game.getPlayerIds().remove(playerId);
        gameRepository.save(game);
        gameRepository.flush();

        // add the game id to the players and remove them from the lobby
        //

        User user = getExistingUser(playerId);
        user.setGameId(0);
        userRepository.save(user);
        userRepository.flush();

        //if the game is empty because the last player left the game, the game is deleted
        if (game.getPlayerIds().isEmpty()) {
            gameRepository.delete(game);
            gameRepository.flush();
        }
    }

    static boolean allCluesRejected(List<?> templist, int compareSize) {
        return Collections.frequency(templist, "REJECTED") == compareSize;
    }

    /*
    Chekcing time taken to get the clue from the user
    If the user took more time than 30seconds, answer will be rejected and
    "REJECTED" will be populated into the list.
     */
    public long checkTimeForClue(Game game){
        long clueTime = game.getTimestamp();
        long nowTime = Instant.now().getEpochSecond();
        long elapsedSeconds = nowTime-clueTime;
        return elapsedSeconds;
    }

    public void updateUserScore(long playerId, int score){
        User user = getExistingUser(playerId);
        user.setScore(user.getScore()+score);
        userRepository.save(user);
        userRepository.flush();
    }

    public boolean isNumeric(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    public void checkDuplicateClue(List<String> clueList){
        List<String> duplicateList = new ArrayList<>();

        List<String> emptyList = new ArrayList<>();
        for(String tempString: clueList){
            if(emptyList.contains(tempString)){
                duplicateList.add(tempString);
            }
            else{
                emptyList.add(tempString);
            }
        }

        for(String duplicateString: duplicateList){
            Collections.replaceAll(clueList,duplicateString,"REJECTED");
        }
    }

    public void submitWord(long id, String word) {

        Game game = getExistingGame(id);
        if (!game.getGameStatus().equals(GameStatus.AWAITING_CLUES)) {
            throw new ServiceException("The game cannot check clues in this state");
        }

        long elapsedSeconds = checkTimeForClue(game);
        List<String> clues = game.getClues();

        if(elapsedSeconds>35){
            clues.add("REJECTED");
        }
        else if(word.equalsIgnoreCase(game.getWords().get(game.getWordIndex()))){
            clues.add("REJECTED");
        }
        else if (!wordChecker.checkEnglishWord(word) && !isNumeric(word) ) {
                //Need to add REJECTED to the list in order to check if all the clues have been received or not.
                //So removing the exception statement.
                clues.add("REJECTED");
        }
        else if(stemCheck.checkStemMatch(word,game.getWords().get(game.getWordIndex())) && !isNumeric(word)) {
            clues.add("REJECTED");
        }
        else {
                clues.add(word.toUpperCase());
                game.setRoundScore(game.getRoundScore()+(100/(int)elapsedSeconds));
        }
        game.setClues(clues);

        if (game.getClues().size() <= game.getPlayerIds().size() - 1) {
            game.setGameStatus(GameStatus.AWAITING_CLUES);
            game.setCardStatus(CardStatus.AWAITING_CLUES);
        }

        // create a special case rules for 3 players
        int maxNumClues = game.getPlayerIds().size() - 1;
        if (game.getPlayerIds().size() == 3) {
            // two clues for two challenging and one guessing player
            maxNumClues = 4;
        }

        if (clues.size() > maxNumClues) {
            throw new ServiceException("Too many clues submitted already");
        }

        if (game.getClues().size() >= maxNumClues) {

            //Checking if duplicate clues were entered.
            checkDuplicateClue(game.getClues());

            /*
            Checking if all the entered clue were invalid.
            If found true, the card will be removed and the game status will
            change to Awaiting Index in order to get new word.
             */
            if(allCluesRejected(clues, maxNumClues)) {
                game.setCardStatus(CardStatus.NO_VALID_CLUE_ENTERED);
                game.setWordIndex(-1);
                game.setRoundScore(0);
                game.setGameStatus(GameStatus.AWAITING_INDEX);
                game.getClues().clear();
            }
            else {
                game.setGameStatus(GameStatus.AWAITING_GUESS);
                //Setting the score as per user story
                game.setRoundScore(game.getRoundScore() - Collections.frequency(clues, "REJECTED"));
                //Setting the time stamp to current time stamp when all the clues have been received.
                //User will have than 30 seconds to guess the word.
                game.setTimestamp(Instant.now().getEpochSecond());
                game.setCardStatus(CardStatus.ALL_CLUES_RECEIVED);
            }
        }

        gameRepository.save(game);
        gameRepository.flush();
    }

    public GameStat getFinalStats(long id){

        Game game = getExistingGame(id);
        int cumulativeScore = new ArrayList<>(game.getScore().values()).stream()
                .mapToInt(Integer::intValue)
                .sum();

        cumulativeScore += game.getWordsGuessedCorrect()*10;
        GameStat gameStat = new GameStat();

        gameStat.setId(game.getId());
        gameStat.setScore(cumulativeScore);
        gameStat.setWordsGuessedCorrect(game.getWordsGuessedCorrect());
        gameStat.setWordsGuessedWrong(game.getWordsGuessedWrong());
        gameStat.setScorePlayerWise(game.getScore());

        return gameStat;
    }

    public Game removePlayerFromGame(long gameId, GameDeleteDTO gameDeleteDTO){

        Game game = getExistingGame(gameId);

        game.getPlayerIds().remove(gameDeleteDTO.getUserId());

        if (game.getPlayerIds().size()<3){
            game.setGameStatus(GameStatus.GAME_OVER);
        }
        else{
            roundEndPlayerLeaves(game,gameDeleteDTO.getUserId());
        }
        gameRepository.save(game);
        gameRepository.flush();

        User user = getExistingUser(gameDeleteDTO.getUserId());
        user.setGameId(0);

        if(gameDeleteDTO.isBrowserClose()) {
            //logging off user
            user.setStatus(UserStatus.OFFLINE);
            //removing user from lobby
            lobbyService.removePlayerFromLobby(gameDeleteDTO.getLobbyId(),gameDeleteDTO.getUserId(),gameDeleteDTO.isBrowserClose());
        }
        userRepository.save(user);
        userRepository.flush();

        return game;

    }


    private void roundEndPlayerLeaves(Game game, long userId) {
        // check how many cards are left on the stack
        if (game.getCardStackCount() <= 0) {
            game.setGameStatus(GameStatus.GAME_OVER);
            return;
        }
        game.setGameStatus(GameStatus.AWAITING_INDEX);
        game.setCardStatus(CardStatus.AWAITING_INDEX);

        //Changing the active player id only when active player leaves the game.
        if(userId == game.getActivePlayerId()) {
            // select the next player
            List<Long> activePlayerId = game.getPlayerIds();
            int activePlayerIndex = (activePlayerId.indexOf(game.getActivePlayerId()) + 1) % activePlayerId.size();
            game.setActivePlayerId(game.getPlayerIds().get(activePlayerIndex));
        }

        // reset the clues
        List<String> clues = new ArrayList<>();
        game.setClues(clues);

        game.setCardStackCount(game.getCardStackCount() - 1);

        game.setRound(game.getRound() + 1);

        //reset the card Index
        game.setWordIndex(-1);

        //reset lastWordList
        game.getLastWordIndex().clear();

        //reset roundend score
        game.setRoundScore(0);
    }
}
