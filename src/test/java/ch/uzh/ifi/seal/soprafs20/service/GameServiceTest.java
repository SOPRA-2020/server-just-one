package ch.uzh.ifi.seal.soprafs20.service;

import ch.uzh.ifi.seal.soprafs20.constant.CardStatus;
import ch.uzh.ifi.seal.soprafs20.constant.GameStatus;
import ch.uzh.ifi.seal.soprafs20.constant.UserStatus;
import ch.uzh.ifi.seal.soprafs20.entity.Lobby;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.entity.Game;
import ch.uzh.ifi.seal.soprafs20.exceptions.ServiceException;
import ch.uzh.ifi.seal.soprafs20.exceptions.NotFoundException;
import ch.uzh.ifi.seal.soprafs20.repository.LobbyRepository;
import ch.uzh.ifi.seal.soprafs20.repository.UserRepository;
import ch.uzh.ifi.seal.soprafs20.repository.GameRepository;
import ch.uzh.ifi.seal.soprafs20.rest.dto.GameDeleteDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.GamePutDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.GameStat;
import ch.uzh.ifi.seal.soprafs20.wordcheck.Stemmer;
import ch.uzh.ifi.seal.soprafs20.wordcheck.WordCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.TransactionScoped;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;

public class GameServiceTest {

    //@Mock
    //private UserRepository userRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private WordCheck wordChecker;
    @Mock
    private Stemmer stemmer;

    @InjectMocks
    private GameService gameService;

    @InjectMocks
    private LobbyService lobbyService;

    private User testUser;
    private Game testGame;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setStatus(UserStatus.ONLINE);

        testGame = new Game();
        testGame.setId(1L);
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        playerIds.add(2L);
        testGame.setPlayerIds(playerIds);
        testGame.setActivePlayerId(0L);

        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
        Mockito.when(gameRepository.save(Mockito.any())).thenReturn(testGame);
    }

    @Test
    public void createGame_validInputs_success() {
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        playerIds.add(2L);

        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));
        long id = gameService.createGame(playerIds);

        assertEquals(id, testGame.getId());
    }

    @Test
    public void createGame_playerNotFound() {
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        playerIds.add(2L);

        assertThrows(NotFoundException.class,()->gameService.createGame(playerIds));
    }


    @Test
    public void createGame_NotEnoughPlayers_throwsException() {
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));
        assertThrows(ServiceException.class, () -> gameService.createGame(playerIds));
    }

    @Test
    public void createGame_TooManyPlayers_throwsException() {
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        playerIds.add(1L);
        playerIds.add(1L);
        playerIds.add(1L);
        playerIds.add(1L);
        playerIds.add(1L);
        playerIds.add(1L);
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));
        assertThrows(ServiceException.class, () -> gameService.createGame(playerIds));
    }

    @Test
    public void chooseWordTest() {

        ArrayList<String> sampleWordList = new ArrayList<>(Arrays.asList("TW0", "TW1", "TW2", "TW3", "TW4", "TW5", "TW6", "TW7",
                "TW8", "TW9"));
        testGame.setRound(2);
        testGame.setGameStatus(GameStatus.AWAITING_INDEX);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        //Mockito.when(gameRepository.getOne(anyLong())).thenReturn(testGame);
        int wordIndexChoosedByPlayerForRound2 = 2;
        gameService.chooseWord(testGame.getId(), wordIndexChoosedByPlayerForRound2);

        //In second round, word selected for 2nd position will be at index 2
        assertEquals(6, testGame.getWordIndex());
        assertEquals("TW6", sampleWordList.get(testGame.getWordIndex()));
    }

    @Test
    public void chooseWordExceptionForInvalidNo() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setGameStatus(GameStatus.AWAITING_INDEX);
        assertThrows(ServiceException.class,()->gameService.chooseWord(testGame.getId(), 7));
    }

    @Test
    public void chooseWordExceptionForSameChoiceInCaseOfRejection() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.getLastWordIndex().add(1);
        testGame.setGameStatus(GameStatus.AWAITING_INDEX);
        assertThrows(ServiceException.class,()->gameService.chooseWord(testGame.getId(), 1));
    }

    @Test
    public void rejectWordTestTimeFailure() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setTimestamp(Instant.now().getEpochSecond()-35);
        testGame.setGameStatus(GameStatus.ACCEPT_REJECT);
        assertThrows(ServiceException.class, ()->gameService.rejectWord(1L));
    }

    @Test
    public void rejectWordTestMoreThanThreeTimes() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.getLastWordIndex().add(1);
        testGame.getLastWordIndex().add(2);
        testGame.getLastWordIndex().add(3);
        testGame.getLastWordIndex().add(4);
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.ACCEPT_REJECT);
        assertThrows(ServiceException.class, ()->gameService.rejectWord(1L));
    }

    @Test
    public void rejectWord_wrongStateThrowsException() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.getLastWordIndex().add(1);
        testGame.getLastWordIndex().add(2);
        testGame.getLastWordIndex().add(3);
        testGame.setGameStatus(GameStatus.AWAITING_GUESS);
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        assertThrows(ServiceException.class, ()->gameService.rejectWord(1L));
    }

    @Test
    public void rejectWordSuccess() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        List<Long> userAccepted =  new ArrayList<>();
        userAccepted.add(1L);
        userAccepted.add(2L);
        userAccepted.add(3L);
        testGame.setCountAccept(userAccepted);
        testGame.setGameStatus(GameStatus.ACCEPT_REJECT);
        gameService.rejectWord(1L);

        assertEquals(GameStatus.AWAITING_INDEX, testGame.getGameStatus());
        assertEquals(CardStatus.USER_REJECTED_WORD, testGame.getCardStatus());
        assertEquals(0,testGame.getCountAccept().size());
    }

    @Test
    public void getExistingGame_success() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Game game = gameService.getExistingGame(1L);

        assertEquals(game, testGame);
    }

    @Test
    public void getExistingGame_notFoundException() {
        assertThrows(NotFoundException.class, ()->gameService.getExistingGame(1L));
    }

    @Test
    public void getExstingUser_sucess() {
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));
        User user = gameService.getExistingUser(1L);
        assertEquals(user, testUser);
    }

    @Test
    public void getExstingUser_notFoundException() {
        assertThrows(NotFoundException.class, ()->gameService.getExistingUser(1L));
    }

    @Test
    public void checkGuess_successfulGuess() {
        GamePutDTO gamePutDTO = new GamePutDTO();
        ArrayList<String> words = new ArrayList<>();
        words.add("Alcatraz");
        words.add("Smoke");
        words.add("Hazelnut");
        words.add("Diamond");
        words.add("Rose");
        testGame.setWords(words);
        testGame.setTimestamp(Instant.now().getEpochSecond()-30);
        testGame.setWordIndex(1);
        testGame.setCardStackCount(2);
        testGame.setWordsGuessedCorrect(3);
        testGame.setCardGuessedCount(4);
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.AWAITING_GUESS);
        gamePutDTO.setGuess(testGame.getWords().get(testGame.getWordIndex()));
        gamePutDTO.setWordIndex(testGame.getWordIndex());

        int startWordsGuessedCorrect = testGame.getWordsGuessedCorrect();
        int startCardGuessedCount = testGame.getCardGuessedCount();
        int startCardStackCount = testGame.getCardStackCount();

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));

        GamePutDTO gamePutDTOTest = gameService.checkGuess(gamePutDTO, testGame.getId());

        assertEquals(startWordsGuessedCorrect + 1, testGame.getWordsGuessedCorrect());
        assertEquals(startCardGuessedCount + 1, testGame.getCardGuessedCount());
        assertEquals(startCardStackCount - 1, testGame.getCardStackCount());
        assertEquals("correct", gamePutDTOTest.getGuessCorrect());
    }

    @Test
    public void checkGuess_wrongGuess() {
        GamePutDTO gamePutDTO = new GamePutDTO();
        ArrayList<String> words = new ArrayList<>();
        words.add("Alcatraz");
        words.add("Smoke");
        words.add("Hazelnut");
        words.add("Diamond");
        words.add("Rose");
        testGame.setWords(words);
        testGame.setWordIndex(1);
        testGame.setCardStackCount(2);
        testGame.setWordsGuessedWrong(3);
        testGame.setCardGuessedCount(4);
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.AWAITING_GUESS);

        //Put a wrong guess
        gamePutDTO.setGuess(testGame.getWords().get(testGame.getWordIndex() + 1));
        gamePutDTO.setWordIndex(testGame.getWordIndex());

        int startWordsGuessedWrong = testGame.getWordsGuessedWrong();
        int startCardGuessedCount = testGame.getCardGuessedCount();
        int startCardStackCount = testGame.getCardStackCount();

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        GamePutDTO gamePutDTOTest = gameService.checkGuess(gamePutDTO, testGame.getId());

        assertEquals(startWordsGuessedWrong + 1, testGame.getWordsGuessedWrong());
        assertEquals(startCardGuessedCount + 1, testGame.getCardGuessedCount());
        assertEquals(startCardStackCount - 2, testGame.getCardStackCount());
        assertEquals("wrong", gamePutDTOTest.getGuessCorrect());
    }

    @Test
    public void checkGuess_skipGuess() {
        GamePutDTO gamePutDTO = new GamePutDTO();
        ArrayList<String> words = new ArrayList<>();
        words.add("Alcatraz");
        words.add("Smoke");
        words.add("Hazelnut");
        words.add("Diamond");
        words.add("Rose");
        testGame.setWords(words);
        testGame.setWordIndex(1);
        testGame.setCardStackCount(2);
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.AWAITING_GUESS);
        //Put a skip guess
        gamePutDTO.setGuess("SKIP");
        gamePutDTO.setWordIndex(testGame.getWordIndex());

        int startCardStackCount = testGame.getCardStackCount();

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));

        GamePutDTO gamePutDTOTest = gameService.checkGuess(gamePutDTO, testGame.getId());

        assertEquals(startCardStackCount - 1, testGame.getCardStackCount());
        assertEquals("skip", gamePutDTOTest.getGuessCorrect());
    }

    @Test
    public void checkGuess_timeoutGuess() {
        ArrayList<String> words = new ArrayList<>();
        words.add("Alcatraz");
        words.add("Smoke");
        words.add("Hazelnut");
        words.add("Diamond");
        words.add("Rose");
        testGame.setWords(words);
        testGame.setWordIndex(1);
        testGame.setCardStackCount(2);
        testGame.setWordsGuessedWrong(3);
        testGame.setCardGuessedCount(4);
        testGame.setTimestamp(Instant.now().getEpochSecond()-31);
        testGame.setGameStatus(GameStatus.AWAITING_GUESS);
        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setGuess("test");
        gamePutDTO.setWordIndex(testGame.getWordIndex());

        int startWordsGuessedWrong = testGame.getWordsGuessedWrong();
        int startCardGuessedCount = testGame.getCardGuessedCount();
        int startCardStackCount = testGame.getCardStackCount();

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        GamePutDTO gamePutDTOTest = gameService.checkGuess(gamePutDTO, testGame.getId());

        assertEquals(startWordsGuessedWrong + 1, testGame.getWordsGuessedWrong());
        assertEquals(startCardGuessedCount + 1, testGame.getCardGuessedCount());
        assertEquals(startCardStackCount - 2, testGame.getCardStackCount());
        assertEquals("timeout", gamePutDTOTest.getGuessCorrect());
    }

    @Test
    public void wrapup_playerLeavesGame () {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn((Optional.of(testGame)));
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
        long playerIdLeft = testGame.getPlayerIds().get(1);
        gameService.wrapup(testGame.getId(), playerIdLeft);

        assertNotSame(playerIdLeft, testGame.getPlayerIds().get(1));
    }

    @Test
    public void wrapup_lastPlayerLeavesGame () {
        Game game = new Game();
        game.setId(2L);
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(1L);
        game.setPlayerIds(playerIds);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn((Optional.of(game)));
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
        long playerIdLeft = game.getPlayerIds().get(0);
        gameService.wrapup(game.getId(), playerIdLeft);

        assertFalse(gameRepository.existsById(game.getId()));
    }

    @Test
    public void allClueRejected_CheckTrue(){
        List<String> tempList = new ArrayList<>();
        tempList.add("REJECTED");
        tempList.add("REJECTED");
        tempList.add("REJECTED");
        tempList.add("REJECTED");

        int compareSize = 4;
        assertTrue(gameService.allCluesRejected(tempList,compareSize));
    }

    @Test
    public void allClueRejected_CheckFalse(){
        List<String> tempList = new ArrayList<>();
        tempList.add("REJECTED");
        tempList.add("REJECTED");
        tempList.add("REJECTED");
        tempList.add("123");

        int compareSize = 4;
        assertFalse(gameService.allCluesRejected(tempList,compareSize));
    }

    @Test
    public void updateUserScoreTest(){
        User newUser = new User();
        newUser.setUsername("tester");
        newUser.setPassword("tester");
        newUser.setId(10L);
        newUser.setScore(20);
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(newUser));
        gameService.updateUserScore(10L,20);
        assertEquals(40,newUser.getScore());
    }

    @Test
    public void checkIsNumericTrue(){
        assertTrue(gameService.isNumeric("1234"));
    }

    @Test
    public void checkIsNumericFalse(){
        assertFalse(gameService.isNumeric("teststring"));
    }

    @Test
    public void removeDuplicateClueTest(){
        List<String> checkList = new ArrayList<>();
        checkList.add("Town");
        checkList.add("City");
        checkList.add("City");
        checkList.add("Place");

        gameService.checkDuplicateClue(checkList);
        assertEquals("REJECTED",checkList.get(1));
    }

    @Test
    public void acceptWordSuccess() {
        testGame.setGameStatus(GameStatus.ACCEPT_REJECT);
        ArrayList<Long> testAccept = new ArrayList<>();
        assertEquals(testAccept, testGame.getCountAccept());

        // first player accepts, nothing should be changing
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn((Optional.of(testGame)));
        gameService.acceptWord(0l ,0l);
        testAccept.add(0l);
        assertEquals(testAccept, testGame.getCountAccept());
        assertEquals(testGame.getGameStatus(), GameStatus.ACCEPT_REJECT);

        // second player accept, status changes
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn((Optional.of(testGame)));
        gameService.acceptWord(0l, 1l);
        assertEquals(new ArrayList<Long>(), testGame.getCountAccept());
        assertEquals(testGame.getGameStatus(), GameStatus.AWAITING_CLUES);
    }

    @Test
    public void clueAccepted() {
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        Mockito.when(stemmer.checkStemMatch(Mockito.any(),Mockito.any())).thenReturn(true);
        testGame.setWordIndex(0);
        testGame.setWords(Arrays.asList("break","making","split","test","word"));
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        gameService.submitWord(1L,"word");
        assert(testGame.getClues().size() >= 1);
        for (String clue: testGame.getClues()) {
            assertEquals("WORD", clue);
        }
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        gameService.submitWord(1L,"sword");
        assert(testGame.getClues().size() >= 2);

        assertEquals("SWORD", testGame.getClues().get(1));
    }

    @Test
    public void numericClueAccepted(){
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(false);
        Mockito.when(stemmer.checkStemMatch(Mockito.any(),Mockito.any())).thenReturn(false);
        testGame.setWordIndex(0);
        testGame.setWords(Arrays.asList("bond","making","split","test","word"));
        gameService.submitWord(1L,"007");
        assert(testGame.getClues().size() >= 1);
        for (String clue: testGame.getClues()) {
            assertEquals("007", clue);
        }
    }

    @Test
    public void rejectDuplicateClue(){
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        Mockito.when(stemmer.checkStemMatch(Mockito.any(),Mockito.any())).thenReturn(true);
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        playerIds.add(2L);
        playerIds.add(3L);
        testGame.setPlayerIds(playerIds);
        testGame.setWordIndex(0);
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        testGame.setWords(Arrays.asList("break","making","split","test","word"));
        gameService.submitWord(1L,"word");
        assert(testGame.getClues().size() >= 1);
        for (String clue: testGame.getClues()) {
            assertEquals("WORD", clue);
        }
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        gameService.submitWord(1L,"word");
        assert(testGame.getClues().size() >= 2);

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        gameService.submitWord(1L,"sword");
        assert(testGame.getClues().size() >= 3);

        assertEquals("REJECTED", testGame.getClues().get(0));
        assertEquals("SWORD",testGame.getClues().get(2));
    }

    @Test
    public void clueSameAsMysteryWord() {
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        Mockito.when(stemmer.checkStemMatch(Mockito.any(),Mockito.any())).thenReturn(true);
        testGame.setWordIndex(0);
        testGame.setWords(Arrays.asList("break","making","split","test","word"));
        gameService.submitWord(1L,"break");
        assert(testGame.getClues().size() >= 1);
        assertEquals("REJECTED", testGame.getClues().get(0));
    }

    @Test
    public void acceptAllUniqueClues(){
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        Mockito.when(stemmer.checkStemMatch(Mockito.any(),Mockito.any())).thenReturn(true);
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        playerIds.add(2L);
        playerIds.add(3L);
        testGame.setPlayerIds(playerIds);
        testGame.setWordIndex(0);
        testGame.setWords(Arrays.asList("break","making","split","test","word"));
        gameService.submitWord(1L,"word");
        assert(testGame.getClues().size() >= 1);
        for (String clue: testGame.getClues()) {
            assertEquals("WORD", clue);
        }
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        gameService.submitWord(1L,"words");
        assert(testGame.getClues().size() >= 2);

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(wordChecker.checkEnglishWord(Mockito.any())).thenReturn(true);
        gameService.submitWord(1L,"sword");
        assert(testGame.getClues().size() >= 3);

        assertEquals("WORD", testGame.getClues().get(0));
        assertEquals("WORDS",testGame.getClues().get(1));
        assertEquals("SWORD",testGame.getClues().get(2));
    }


    @Test
    public void timeForSubmitClueException(){
        testGame.setTimestamp(Instant.now().getEpochSecond()-40);
        ArrayList<String> clues =  new ArrayList<>();
        clues.add("REJECTED");
        clues.add("REJECTED");
        clues.add("REJECTED");
        testGame.setClues(clues);
        testGame.setWords(Arrays.asList("GameWord","GameWord2","TestGame"));
        testGame.setWordIndex(0);
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);

        ArrayList<Long> playerIdList = new ArrayList<>();
        playerIdList.add(1L);
        playerIdList.add(2L);
        playerIdList.add(3L);
        playerIdList.add(4L);
        playerIdList.add(5L);
        testGame.setPlayerIds(playerIdList);

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        gameService.submitWord(1L,"word");
        assertEquals(CardStatus.NO_VALID_CLUE_ENTERED, testGame.getCardStatus());

    }

    @Test
    public void englishWordCheckInvalid(){
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        ArrayList<String> clues =  new ArrayList<>();
        testGame.setWords(Arrays.asList("break","making","split","test","word"));
        testGame.setWordIndex(0);
        clues.add("REJECTED");
        clues.add("REJECTED");
        clues.add("REJECTED");
        testGame.setClues(clues);
        ArrayList<Long> playerIdList = new ArrayList<>();
        playerIdList.add(1L);
        playerIdList.add(2L);
        playerIdList.add(3L);
        playerIdList.add(4L);
        playerIdList.add(5L);
        testGame.setPlayerIds(playerIdList);

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        gameService.submitWord(1L,"asfdj");
        assertEquals(CardStatus.NO_VALID_CLUE_ENTERED, testGame.getCardStatus());
    }

    @Test
    public void stemCheckInvalid(){
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setTimestamp(Instant.now().getEpochSecond()-15);
        testGame.setWords(Arrays.asList("break","making","split","test","word"));
        testGame.setWordIndex(0);
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        ArrayList<String> clues =  new ArrayList<>();
        clues.add("REJECTED");
        clues.add("REJECTED");
        clues.add("REJECTED");
        testGame.setClues(clues);
        ArrayList<Long> playerIdList = new ArrayList<>();
        playerIdList.add(1L);
        playerIdList.add(2L);
        playerIdList.add(3L);
        playerIdList.add(4L);
        playerIdList.add(5L);
        testGame.setPlayerIds(playerIdList);

        gameService.submitWord(1L,"breaking");
        assertEquals(CardStatus.NO_VALID_CLUE_ENTERED, testGame.getCardStatus());
    }

    @Test
    public void getStatAfterGameTest(){
        Map<Long,Integer> scoreMap = new HashMap<>();

        scoreMap.put(1L,30);
        scoreMap.put(2L,50);
        scoreMap.put(3L,10);
        scoreMap.put(4L,00);

        testGame.setScore(scoreMap);
        testGame.setWordsGuessedCorrect(4);

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        GameStat gameStat = gameService.getFinalStats(1L);

        assertEquals(130, gameStat.getScore());

    }

    @Test
    public void getAllWordsFromList_Full() {
        ArrayList<String> words = gameService.getAllWordsFromWordList();
        for (String word: words) {
            assertNotEquals("", word);
        }
        assertEquals("Alcatraz", words.get(0));
        assertEquals("Book", words.get(274));
    }


    @Test
    public void selectGameWords() {
        ArrayList<String> words = gameService.selectGameWords();
        assert(words.size() == 5*13);
        for (String word: words) {
            assertNotEquals("", word);
        }
    }

    @Test
    public void RejectWordInvalidState_Exception(){
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        assertThrows(ServiceException.class,()->gameService.rejectWord(1));
    }

    @Test
    public void AcceptWordInvalidState_Exception(){
        testGame.setGameStatus(GameStatus.AWAITING_INDEX);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        assertThrows(ServiceException.class,()->gameService.acceptWord(1l, 1l));
    }

    @Test
    public void CheckGuessInvalidState_Exception(){
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        assertThrows(ServiceException.class,()->gameService.checkGuess(new GamePutDTO(), 1l));
    }

    @Test
    public void SubmitWordInvalidState_Exception(){
        testGame.setGameStatus(GameStatus.AWAITING_INDEX);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        assertThrows(ServiceException.class,()->gameService.submitWord(1l, "dummy"));
    }

    @Test
    public void ChooseWordInvalidState_Exception(){
        testGame.setGameStatus(GameStatus.AWAITING_GUESS);
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        assertThrows(ServiceException.class,()->gameService.chooseWord(1l, 1));
    }




/*
    @Test
    public void removePlayerFromGame(){
        ArrayList<Long> playerIdList = new ArrayList<>();
        playerIdList.add(1L);
        playerIdList.add(2L);
        playerIdList.add(3L);
        playerIdList.add(4L);
        playerIdList.add(5L);
        testGame.setPlayerIds(playerIdList);
        testGame.setRound(1);
        testGame.setActivePlayerId(3L);

        User testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setStatus(UserStatus.ONLINE);

        Lobby lobbyTest = new Lobby();
        lobbyTest.setId(1l);
        lobbyTest.setName("testLobby");
        lobbyTest.setHostPlayerId(1L);
        Mockito.when(lobbyRepository.save(Mockito.any())).thenReturn(lobbyTest);

        List<Long> playerList  = new ArrayList<>();
        Long[] longList = new Long[]{2L,3L,4L,5L,6L,7L};
        Collections.addAll(playerList,longList);
        lobbyTest.setPlayerIds(playerList);

        GameDeleteDTO gameDeleteDTO = new GameDeleteDTO();
        gameDeleteDTO.setUserId(2L);
        gameDeleteDTO.setLobbyId(1L);
        gameDeleteDTO.setBrowserClose(true);

        Mockito.when(userRepository.getOne(Mockito.any())).thenReturn(testUser);
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(lobbyTest));
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(testUser2));
        gameService.removePlayerFromGame(testGame.getId(),gameDeleteDTO);
        assertEquals(false,testGame.getPlayerIds().contains(2L));
        assertEquals(UserStatus.OFFLINE,testUser2.getStatus());

    }*/
}
