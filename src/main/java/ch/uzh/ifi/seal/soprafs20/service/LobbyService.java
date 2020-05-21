package ch.uzh.ifi.seal.soprafs20.service;


import ch.uzh.ifi.seal.soprafs20.constant.UserStatus;
import ch.uzh.ifi.seal.soprafs20.entity.Lobby;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.entity.Chat;
import ch.uzh.ifi.seal.soprafs20.exceptions.LobbyException;
import ch.uzh.ifi.seal.soprafs20.exceptions.NotFoundException;
import ch.uzh.ifi.seal.soprafs20.repository.LobbyRepository;
import ch.uzh.ifi.seal.soprafs20.repository.UserRepository;
import ch.uzh.ifi.seal.soprafs20.repository.ChatRepository;
import ch.uzh.ifi.seal.soprafs20.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.seal.soprafs20.rest.mapper.DTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lobby Service
 * This class is the "worker" and responsible for all functionality related to the lobby
 * (e.g., it creates, modifies). The result will be passed back to the caller.
 */

@Service
@Transactional
public class LobbyService {


    private final LobbyRepository lobbyRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Autowired
    public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository, @Qualifier("userRepository") UserRepository userRepository, @Qualifier("chatRepository") ChatRepository chatRepository) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    public Long createLobby(Lobby newLobby){

        checkIfLobbyExist(newLobby);
        try {
            userRepository.findById(newLobby.getHostPlayerId());
        } catch (Exception e) {
            throw new LobbyException(String.format("User with id: %d doesn't exist", newLobby.getHostPlayerId()));
        }
        newLobby.getPlayerIds().add(newLobby.getHostPlayerId());

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();

        User user = userRepository.getOne(newLobby.getHostPlayerId());
        user.setLobbyId(newLobby.getId());
        userRepository.save(user);

        Chat chat = new Chat();
        chat.setId(newLobby.getId());
        chatRepository.save(chat);
        chatRepository.flush();

        return newLobby.getId();
    }

    public void updateStatusOfLobby(long id, int status){
        Lobby lobby = getLobby(id);
        lobby.setStatus(status);
        saveOrUpdate(lobby);
    }

    public void saveOrUpdate(Lobby updateLobby){
        lobbyRepository.save(updateLobby);
        lobbyRepository.flush();
    }

    public List<LobbyGetDTO> getAllLobbies(){
        List<Lobby> lobbyList = this.lobbyRepository.findAll();
        List<LobbyGetDTO> lobbyGetDTOList = new ArrayList<>();
        for(Lobby tempLobby:lobbyList){
            LobbyGetDTO lobbyGetDTO = DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(tempLobby);
            lobbyGetDTOList.add(lobbyGetDTO);
        }
        return lobbyGetDTOList;
    }

    public User getExistingUser(long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent()) {
            throw new NotFoundException(String.format("Could not find user with id %d.", id));
        }
        return optionalUser.get();
    }

    public void removePlayerFromLobby(long id, long userId, boolean browserClose){
        Lobby lobby = getLobby(id);
        String baseErrorMessage = "This player id is invalid. Please provide proper id";
        if(lobby.getPlayerIds().contains(userId)){
            lobby.getPlayerIds().remove(userId);
        }
        else{
            throw new LobbyException(baseErrorMessage);
        }

        //Changing host when host player leaves the lobby
        if(lobby.getHostPlayerId() == userId && !lobby.getPlayerIds().isEmpty()){
            lobby.setHostPlayerId(lobby.getPlayerIds().get(0));
            User newLobbyHost = getExistingUser(lobby.getHostPlayerId());
            newLobbyHost.setLobbyId(id);
            userRepository.save(newLobbyHost);
            userRepository.flush();
        }
        User user = getExistingUser(userId);
        if(user.getLobbyId()==id){
            user.setLobbyId(-1);
        }

        if(browserClose) {
            //log off the user
            user.setStatus(UserStatus.OFFLINE);
        }

        userRepository.save(user);
        userRepository.flush();

        //Deleting the lobby if all player have left the lobby
        if(lobby.getPlayerIds().isEmpty()){
            lobbyRepository.delete(lobby);
            return;
        }
        saveOrUpdate(lobby);
    }

    public void addPlayerToLobby(long id, long userId){
        Lobby lobby = getLobby(id);

        if(lobby.getStatus()==1){
            throw new LobbyException("Game is in progress. You can't join lobby in the middle of the game. Please try later");
        }

        //Checking if the user exists before adding the user to lobby
        try {
            userRepository.findById(userId);
        } catch (Exception e) {
            throw new LobbyException(String.format("User with id: %d doesn't exist", userId));
        }
        String baseErrorMessage = "The lobby cannot have more than 7 player. Please join different lobby";

        //Size of lobby is limited to maximum of 7 players.
        if(lobby.getPlayerIds().size()>=7){
            throw new LobbyException(baseErrorMessage);
        }

        //Player should be unique in the lobby
        if(lobby.getPlayerIds().contains(userId)){
            baseErrorMessage = "Player already exists in the lobby";
            throw new LobbyException(baseErrorMessage);
        }
        lobby.getPlayerIds().add(userId);
        saveOrUpdate(lobby);

    }

    public Lobby getLobby(Long id) {
        Optional<Lobby> optionalLobby = lobbyRepository.findById(id);
        if (!optionalLobby.isPresent()) {
            throw new LobbyException(String.format("Could not find lobby with id %d.", id));
        }
        return optionalLobby.get();
    }

    public void checkIfLobbyExist(Lobby lobbyToBeCreated) {
        /*
        This method checks the uniqueness of the lobby by lobby name. If the lobby with the same name
        exists then it should not be created.
         */
        Lobby newLobby = lobbyRepository.findByName(lobbyToBeCreated.getName());

        String baseErrorMessage = "The provided %s is not unique. Therefore, the lobby could not be created!";
        if (null != newLobby) {
            throw new LobbyException(String.format(baseErrorMessage, "lobby name"));
        }
    }

}
