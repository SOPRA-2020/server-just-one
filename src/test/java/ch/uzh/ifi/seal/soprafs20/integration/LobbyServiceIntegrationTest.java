package ch.uzh.ifi.seal.soprafs20.integration;

import ch.uzh.ifi.seal.soprafs20.entity.Lobby;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.exceptions.LobbyException;
import ch.uzh.ifi.seal.soprafs20.repository.LobbyRepository;
import ch.uzh.ifi.seal.soprafs20.repository.UserRepository;
import ch.uzh.ifi.seal.soprafs20.service.LobbyService;
import ch.uzh.ifi.seal.soprafs20.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;

@SpringBootTest
@ContextConfiguration
public class LobbyServiceIntegrationTest {

    @Autowired
    LobbyService lobbyService;
    @Autowired
    UserService userService;

    static User testUser;
    private Lobby lobbyTest;
    static Lobby createdLobbyTest;
    static Long lobbyId;
    static boolean setUpIsDone = false;

    @BeforeEach
    public void setupLobby(){

        if (setUpIsDone) {
            return;
        }

        testUser = new User();
        testUser.setPassword("testName");
        testUser.setUsername("testUsername");

        testUser = userService.createUser(testUser);

        lobbyTest = new Lobby();
        lobbyTest.setName("testLobby");
        lobbyTest.setHostPlayerId(testUser.getId());

        lobbyId = lobbyService.createLobby(lobbyTest);
        createdLobbyTest = lobbyService.getLobby(lobbyId);

        setUpIsDone = true;
    }

    @Test
    public void createLobbySuccess(){

        User newUser = new User();
        newUser.setUsername("user3");
        newUser.setPassword("password");

        newUser = userService.createUser(newUser);
        Lobby newLobby = new Lobby();
        newLobby.setName("newLobby");
        newLobby.setHostPlayerId(newUser.getId());
        Long newId = lobbyService.createLobby(newLobby);

        assertNotEquals(0,newId);
    }

    @Test
    public void createdLobbyExist_Exception(){

        Lobby newLobby = new Lobby();
        newLobby.setName("testLobby");
        newLobby.setHostPlayerId(2L);

        assertThrows(LobbyException.class, ()->lobbyService.createLobby(newLobby));
    }

    @Test
    public void addExistingUserToLobbyException(){
        assertThrows(LobbyException.class,()->lobbyService.addPlayerToLobby(createdLobbyTest.getId(),testUser.getId()));

    }

    @Test
    public void addMoreThanSevenPlayersToLobbyException(){

        User user1 = new User();
        user1.setUsername("uw1");
        user1.setPassword("user1");
        user1 = userService.createUser(user1);

        User user2 = new User();
        user2.setUsername("uw2");
        user2.setPassword("user2");
        user2 = userService.createUser(user2);

        User user3 = new User();
        user3.setUsername("uw3");
        user3.setPassword("user3");
        user3 = userService.createUser(user3);

        User user4 = new User();
        user4.setUsername("uw4");
        user4.setPassword("user4");
        user4 = userService.createUser(user4);

        User user5 = new User();
        user5.setUsername("uw5");
        user5.setPassword("user5");
        user5 = userService.createUser(user5);

        User user6 = new User();
        user6.setUsername("uw6");
        user6.setPassword("user6");
        user6 = userService.createUser(user6);

        lobbyService.addPlayerToLobby(createdLobbyTest.getId(),user1.getId());
        lobbyService.addPlayerToLobby(createdLobbyTest.getId(),user2.getId());
        lobbyService.addPlayerToLobby(createdLobbyTest.getId(),user3.getId());
        lobbyService.addPlayerToLobby(createdLobbyTest.getId(),user4.getId());
        lobbyService.addPlayerToLobby(createdLobbyTest.getId(),user5.getId());
        lobbyService.addPlayerToLobby(createdLobbyTest.getId(),user6.getId());



        final User returnedUser;
        User newUser = new User();
        newUser.setUsername("eighthUser");
        newUser.setPassword("password");
        returnedUser = userService.createUser(newUser);

        assertThrows(LobbyException.class,()->lobbyService.addPlayerToLobby(createdLobbyTest.getId(),returnedUser.getId()));
    }


/*    @Test
    public void addUserToLobby(){

        System.out.println("tostring->"+createdLobbyTest.toString());
        System.out.println("before ->"+createdLobbyTest.getPlayerIds().size());
        User newUser = new User();
        newUser.setUsername("user2");
        newUser.setPassword("password");

        newUser = userService.createUser(newUser);
        System.out.println("new user ->"+newUser.getId());

        lobbyService.addPlayerToLobby(createdLobbyTest.getId(),newUser.getId());
        System.out.println("after->"+createdLobbyTest.getPlayerIds().size());
        createdLobbyTest = lobbyService.getLobby(lobbyId);
        System.out.println("Here I am"+createdLobbyTest.toString());
        assertEquals(createdLobbyTest.getPlayerIds().get(1),newUser.getId());
    }*/

 /*   @Test
    public void removePlayerFromLobby(){


        createdLobbyTest.getPlayerIds();
        int size = createdLobbyTest.getPlayerIds().size();
        System.out.println("lobby size in remove"+size);

        lobbyService.removePlayerFromLobby(createdLobbyTest.getId(),2);
        assertEquals(createdLobbyTest.getPlayerIds().size(),size-1);

    }*/



}
