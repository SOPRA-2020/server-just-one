package ch.uzh.ifi.seal.soprafs20.integration;


import ch.uzh.ifi.seal.soprafs20.constant.CardStatus;
import ch.uzh.ifi.seal.soprafs20.entity.Game;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.exceptions.ServiceException;
import ch.uzh.ifi.seal.soprafs20.rest.dto.GamePutDTO;
import ch.uzh.ifi.seal.soprafs20.service.GameService;
import ch.uzh.ifi.seal.soprafs20.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GameServiceIntegrationTest {

    @Autowired
    GameService gameService;
    @Autowired
    UserService userService;

    private Game testGame;
    static long gameId=-1;
    static boolean setUpIsDone = false;


    @BeforeEach
    void createGame() {

        if (setUpIsDone) {
            return;
        }

        User testUser = new User();
        testUser.setUsername("gamer");
        testUser.setPassword("123");
        testUser = userService.createUser(testUser);

        User gameUser2 = new User();
        gameUser2.setUsername("gamer2");
        gameUser2.setPassword("2123");
        gameUser2 = userService.createUser(gameUser2);

        User gameUser3 = new User();
        gameUser3.setUsername("gameUser3");
        gameUser3.setPassword("123");
        gameUser3 = userService.createUser(gameUser3);

        List<Long> playerIdList = new ArrayList<>();
        playerIdList.add(testUser.getId());
        playerIdList.add(gameUser2.getId());
        playerIdList.add(gameUser3.getId());

        gameId = gameService.createGame(playerIdList);
        testGame = gameService.getExistingGame(gameId);
        setUpIsDone = true;
    }


    @Test
    public void createGameExceptionForLessPlayer() {

        User testUser1 = new User();
        testUser1.setUsername("gameUW1");
        testUser1.setPassword("123");
        testUser1 = userService.createUser(testUser1);

        User gameUser4 = new User();
        gameUser4.setUsername("gameUW2");
        gameUser4.setPassword("2123");
        gameUser4 = userService.createUser(gameUser4);


        List<Long> playerIdList = new ArrayList<>();
        playerIdList.add(testUser1.getId());
        playerIdList.add(gameUser4.getId());

        assertThrows(ServiceException.class,()->gameService.createGame(playerIdList));

    }

    @Test
    public void chooseWord(){

        int chooseIndex = 1;
        gameService.chooseWord(gameId,chooseIndex);
        testGame = gameService.getExistingGame(gameId);
        assertEquals(testGame.getWordIndex(),0);

    }

    @Test
    public void chooseIncorrectIndex(){
        int chooseIndex = 10;
        assertThrows(ServiceException.class,()->gameService.chooseWord(gameId,chooseIndex));
    }

    @Test
    public void clueWord() throws InterruptedException {

        int chooseIndex = 5;
        gameService.chooseWord(gameId,chooseIndex);

        //Add time delay of 3 seconds
        TimeUnit.SECONDS.sleep(1);

        String word = "Name";
        String word2 ="Word";
        gameService.submitWord(gameId,word);
        gameService.submitWord(gameId,word2);
        testGame = gameService.getExistingGame(gameId);
        assertEquals(testGame.getCardStatus(), CardStatus.ALL_CLUES_RECEIVED);
    }


    @Test
    public void noValidClueEntered() throws InterruptedException {

        int chooseIndex = 2;
        gameService.chooseWord(gameId,chooseIndex);

        //Add time delay of 3 seconds
        TimeUnit.SECONDS.sleep(1);

        String word = "123";
        String word2 ="123";
        gameService.submitWord(gameId,word);
        gameService.submitWord(gameId,word2);
        testGame = gameService.getExistingGame(gameId);
        assertEquals(testGame.getCardStatus(), CardStatus.NO_VALID_CLUE_ENTERED);
    }

    @Test
    public void checkIncorrectGuess() throws InterruptedException {

        int chooseIndex = 3;
        gameService.chooseWord(gameId,chooseIndex);

        TimeUnit.SECONDS.sleep(3);

        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setGuess("asdsf");
        gamePutDTO.setWordIndex(1);

        gameService.checkGuess(gamePutDTO,gameId);
        testGame = gameService.getExistingGame(gameId);
        assertEquals(testGame.getWordsGuessedWrong(),1);

    }


}

