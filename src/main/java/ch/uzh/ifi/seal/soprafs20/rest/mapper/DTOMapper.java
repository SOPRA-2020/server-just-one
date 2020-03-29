package ch.uzh.ifi.seal.soprafs20.rest.mapper;

import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.entity.Game;
import ch.uzh.ifi.seal.soprafs20.rest.dto.*;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g., UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for creating information (POST).
 */
@Mapper
public interface DTOMapper {

    DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "rank", target ="rank")
    @Mapping(source = "score", target ="score")
    @Mapping(source = "gender", target = "gender")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "birthDay", target = "birthDay")
    @Mapping(source = "token", target = "token")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "gameId", target = "gameId")
    @Mapping(source = "lobbyId", target = "lobbyId")
    @Mapping(source = "invitations", target = "invitations")
    UserGetDTO convertEntityToUserGetDTO(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "playerIds", target = "playerIds")
    @Mapping(source = "round", target = "round")
    @Mapping(source = "gameStatus", target = "gameStatus")
    @Mapping(source = "score", target = "score")
    @Mapping(source = "activePlayer", target = "activePlayer")
    @Mapping(source = "timestamp", target = "timestamp")
    GameGetDTO convertEntityToGameGetDTO(Game game);

    @Mapping(source = "playerIds", target = "playerIds")
    Game convertGamePostDTOtoEntity(GamePostDTO gamePostDTO);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "gender", target = "gender")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "birthDay", target = "birthDay")
    UserUpdateDTO convertEntityToUserUpdateDTO(User user);

}
