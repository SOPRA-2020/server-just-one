package ch.uzh.ifi.seal.soprafs20.service;

import ch.uzh.ifi.seal.soprafs20.constant.CardStatus;
import ch.uzh.ifi.seal.soprafs20.constant.GameStatus;
import ch.uzh.ifi.seal.soprafs20.constant.UserStatus;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.entity.Game;
import ch.uzh.ifi.seal.soprafs20.exceptions.ServiceException;
import ch.uzh.ifi.seal.soprafs20.repository.UserRepository;
import ch.uzh.ifi.seal.soprafs20.repository.GameRepository;
import ch.uzh.ifi.seal.soprafs20.rest.dto.GamePutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;

public class GameServiceTest {

    //@Mock
    //private UserRepository userRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private User testUser;
    private Game testGame;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        testUser = new User();
        testUser.setId(1L);

        testGame = new Game();
        testGame.setId(1L);
        ArrayList<Long> playerIds = new ArrayList<Long>();
        playerIds.add(0L);
        playerIds.add(1L);
        playerIds.add(2L);
        testGame.setPlayerIds(playerIds);

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
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        //Mockito.when(gameRepository.getOne(anyLong())).thenReturn(testGame);
        int wordIndexChoosedByPlayerForRound2 = 2;
        gameService.chooseWord(testGame.getId(), wordIndexChoosedByPlayerForRound2);

        //In second round, word selected for 2nd position will be at index 2
        assertEquals(6, testGame.getWordIndex());
        assertEquals("TW6", sampleWordList.get(testGame.getWordIndex()));
    }

    @Test
    public void chooseWordExceptionForInvalidNo(){
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        assertThrows(ServiceException.class,()->gameService.chooseWord(testGame.getId(), 7));
    }

    @Test
    public void chooseWordExceptionForSameChoiceInCaseOfRejection(){
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.getLastWordIndex().add(1);
        assertThrows(ServiceException.class,()->gameService.chooseWord(testGame.getId(), 1));
    }

    @Test
    public void rejectWordTestTimeFailure(){

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setTimestamp(java.time.LocalTime.now().minus(35, ChronoUnit.SECONDS));
        assertThrows(ServiceException.class, ()->gameService.rejectWord(1L));
    }

    @Test
    public void rejectWordTestMoreThanThreeTimes(){

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.getLastWordIndex().add(1);
        testGame.getLastWordIndex().add(2);
        testGame.getLastWordIndex().add(3);
        testGame.setTimestamp(java.time.LocalTime.now().minus(35, ChronoUnit.SECONDS));
        assertThrows(ServiceException.class, ()->gameService.rejectWord(1L));
    }

    @Test
    public void rejectWordSuccess(){

        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setGameStatus(GameStatus.AWAITING_CLUES);
        testGame.setTimestamp(java.time.LocalTime.now().minus(15, ChronoUnit.SECONDS));
        gameService.rejectWord(1L);

        assertEquals(testGame.getGameStatus(), GameStatus.AWAITING_INDEX);
        assertEquals(testGame.getCardStatus(), CardStatus.USER_REJECTED_WORD);

    }

    @Test
    public void getExistingGame_success() {
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        Game game = gameService.getExistingGame(1L);

        assertEquals(game, testGame);
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
        testGame.setTimestamp(java.time.LocalTime.now().minus(30, ChronoUnit.SECONDS));
        testGame.setWordIndex(1);
        gamePutDTO.setGuess(testGame.getWords().get(testGame.getWordIndex()));
        gamePutDTO.setWordIndex(testGame.getWordIndex());
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));

        GamePutDTO gamePutDTOTest = gameService.checkGuess(gamePutDTO, testGame.getId());

        assertEquals(gamePutDTOTest.getGuessCorrect(), "correct");
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
    public void timeForSubmitClueException(){
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setTimestamp(java.time.LocalTime.now().minus(35, ChronoUnit.SECONDS));
        assertThrows(ServiceException.class, ()->gameService.checkTimeForClue(testGame));
    }

    @Test
    public void allClueRejected(){
        Mockito.when(gameRepository.findById(Mockito.any())).thenReturn(Optional.of(testGame));
        testGame.setTimestamp(java.time.LocalTime.now().minus(15, ChronoUnit.SECONDS));
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

        gameService.submitWord(1L,"asfdj");
        assertEquals(testGame.getCardStatus(),CardStatus.NO_VALID_CLUE_ENTERED);

    }


    /* These are some tests for the private methods. They are commented sinc private methods cannot be
     * tested under normal circumstances and cannot be tested under normal circumstances. They are kept
     * here to still allow some quick implementation testing.
     */

    //@Test
    //public void getAllWordsFromList_Full() {
    //    ArrayList<String> words = gameService.getAllWordsFromWordList();
    //    for (String word: words) {
    //        assert (!word.equals(""));
    //    }
    //    assert(words.get(0).equals("Alcatraz"));
    //    assert(words.get(274).equals("Book"));
    //}

    //@Test
    //public void selectGameWords() {
    //    ArrayList<String> words = gameService.selectGameWords();
    //    assert(words.size() == 5*13);
    //    for (String word: words) {
    //        assert (!word.equals(""));
    //    }
    //}

}
