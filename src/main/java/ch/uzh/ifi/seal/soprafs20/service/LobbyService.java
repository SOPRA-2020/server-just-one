package ch.uzh.ifi.seal.soprafs20.service;


import ch.uzh.ifi.seal.soprafs20.entity.Lobby;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.exceptions.LobbyException;
import ch.uzh.ifi.seal.soprafs20.exceptions.NotFoundException;
import ch.uzh.ifi.seal.soprafs20.repository.LobbyRepository;
import ch.uzh.ifi.seal.soprafs20.repository.UserRepository;
import ch.uzh.ifi.seal.soprafs20.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.seal.soprafs20.rest.mapper.DTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.DeferredResult;

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
    private final UserRepository userRepository;
    private final LobbyPollService lobbyPollService;

    @Autowired
    public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository, @Qualifier("userRepository") UserRepository userRepository) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.lobbyPollService = new LobbyPollService(lobbyRepository);
    }

    public Long createLobby(Lobby newLobby){

        checkIfLobbyExist(newLobby);
        newLobby.getPlayerIds().add(newLobby.getHostPlayerId());

        newLobby = lobbyRepository.save(newLobby);

        User user = userRepository.getOne(newLobby.getHostPlayerId());
        user.setLobbyId(newLobby.getId());
        userRepository.save(user);

        return newLobby.getId();

    }

    public void updateStatusOfLobby(long id, int status){
        Lobby lobby = getLobby(id);
        lobby.setStatus(status);
        saveOrUpdate(lobby);
    }

    public void saveOrUpdate(Lobby updateLobby){
        lobbyRepository.save(updateLobby);
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

    // subscription method for a certain lobby id
    public void subscribe(Long id) {
        try {
            lobbyRepository.findById(id).get();
        } catch (Exception e) {
            throw new NotFoundException("Cannot subscribe to a non-existing lobby");
        }
        lobbyPollService.subscribe(id);
    }

    // unsubscription method for a certain lobby id
    public void unsubscribe(Long id) {
        lobbyPollService.unsubscribe(id);
    }

    // async, returns once there is a change for the lobby id
    public void pollGetUpdate(DeferredResult<LobbyGetDTO> result, Long id) {
        try {
            lobbyRepository.findById(id).get();
        } catch (Exception e) {
            throw new NotFoundException("Cannot poll for a non-existing lobby");
        }
        lobbyPollService.pollGetUpdate(result, id);
    }

    public void removePlayerFromLobby(long id, long lobbyId){
        Lobby lobby = getLobby(id);
        String baseErrorMessage = "This player id is invalid. Please provide proper id";
        if(lobby.getPlayerIds().contains(lobbyId)){
            lobby.getPlayerIds().remove(lobbyId);
        }
        else{
            throw new LobbyException(baseErrorMessage);
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
        if(lobby.getPlayerIds().size()>7){
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
