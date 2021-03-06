package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.CardStatus;
import ch.uzh.ifi.seal.soprafs20.constant.GameStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal Game Representation
 * This class composes the internal representation of the game and defines how the game is stored in the database.
 * Every variable will be mapped into a database field with the @Column annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes the primary key
 */
@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column()
    @ElementCollection
    private List<Long> playerIds = new ArrayList<>();

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private GameStatus gameStatus;

    @Column()
    @ElementCollection
    private List<String> words = new ArrayList<>();

    @Column(nullable = false)
    private int wordIndex;

    @Column
    @ElementCollection
    private List<Integer> lastWordIndex = new ArrayList<>();

    @Column(nullable = false)
    private int roundScore;

    @Column(nullable = false)
    private Long activePlayerId;

    @Column()
    @ElementCollection
    private List<String> clues = new ArrayList<>();

    @Column
    @ElementCollection
    private Map<Long,Integer> score = new HashMap<>();

    @Column
    private long timestamp;

    @Column
    private int wordsGuessedCorrect;

    @Column
    private int wordsGuessedWrong;

    @Column
    private int cardStackCount;

    @Column
    private int cardGuessedCount;

    @Column()
    private CardStatus cardStatus;

    @Column
    @ElementCollection
    private List<Long> countAccept = new ArrayList<>();

    @Override
    public String toString() {
        return "Game{" +
                "id=" + this.getId() +
                ", playerIds='" + this.getPlayerIds() +
                ", round='" + this.getRound() +
                ", gameStatus='" + this.getGameStatus() +
                ", words=" + this.getWords() +
                ", wordIndex='" + this.getWordIndex() +
                ", lastWordIndex=" + this.getLastWordIndex() +
                ", roundScore=" + this.getRoundScore() +
                ", activePlayerId=" + this.getActivePlayerId()+
                ", clues=" + this.getClues() +
                ", score=" + this.getScore() +
                ", timestamp='" + this.getTimestamp() +
                ", wordsGuessedWrong=" + this.getWordsGuessedWrong() +
                ", cardStackCount=" + this.getCardStackCount() +
                ", cardGuessedCount=" + this.getCardGuessedCount() +
                ", cardStatus=" + this.getCardStatus() +
                ", countAccept=" + this.getCountAccept() +
                '}';
    }

    public List<Long> getCountAccept() {return countAccept;}

    public void setCountAccept(List<Long> accept) {this.countAccept = accept;}

    public Long getId() {
        return id;
    }


    public List<Integer> getLastWordIndex() {
        return lastWordIndex;
    }

    public void setLastWordIndex(List<Integer> lastWordIndex) {
        this.lastWordIndex = lastWordIndex;
    }

    public CardStatus getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(CardStatus cardStatus) {
        this.cardStatus = cardStatus;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<Long> playerIds) {
        this.playerIds = playerIds;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public int getWordIndex() {
        return wordIndex;
    }

    public void setWordIndex(int wordIndex) {
        this.wordIndex = wordIndex;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public void setRoundScore(int score) {
        this.roundScore = score;
    }

    public Long getActivePlayerId() {
        return activePlayerId;
    }

    public Map<Long, Integer> getScore() {
        return score;
    }

    public void setScore(Map<Long, Integer> score) {
        this.score = score;
    }

    public void setActivePlayerId(Long activePlayerId) {
        this.activePlayerId = activePlayerId;
    }

    public List<String> getClues() {
        return clues;
    }

    public void setClues(List<String> clues) {
        this.clues = clues;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getWordsGuessedCorrect() {return  wordsGuessedCorrect;}

    public void setWordsGuessedCorrect(int wordsGuessedCorrect) {this.wordsGuessedCorrect = wordsGuessedCorrect;}

    public int getWordsGuessedWrong() {return  wordsGuessedWrong;}

    public void setWordsGuessedWrong(int wordsGuessedWrong) {this.wordsGuessedWrong = wordsGuessedWrong;}

    public int getCardStackCount() {return  cardStackCount;}

    public void setCardStackCount(int cardStackCount) {this.cardStackCount = cardStackCount;}

    public int getCardGuessedCount() {return  cardGuessedCount;}

    public void setCardGuessedCount(int cardGuessedCount) {this.cardGuessedCount = cardGuessedCount;}




}
