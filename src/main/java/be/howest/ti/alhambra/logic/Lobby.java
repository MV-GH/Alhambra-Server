package be.howest.ti.alhambra.logic;


import be.howest.ti.alhambra.logic.exceptions.AlhambraEntityNotFoundException;
import be.howest.ti.alhambra.logic.exceptions.AlhambraGameRuleException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Lobby {
    @JsonIgnore
    private static final int MAX_PLAYER_COUNT = 6;
    @JsonIgnore
    private static final int MIN_PLAYER_COUNT = 2;
    private final String id;
    private boolean started;
    private Map<String, Boolean> playersReady;
    private int playerCount = 0;

    public Lobby(String gameId) {
        this(gameId, false, new HashMap<>());
    }

    @JsonCreator
    public Lobby(@JsonProperty("id") String id, @JsonProperty("started") boolean started, @JsonProperty("players") Map<String, Boolean> playersReady) {
        this.started = started;
        this.id = id;
        this.playersReady = playersReady;
    }

    public boolean isStarted() {
        return started;
    }

    public String getId() {
        return id;
    }


    public int getPlayerCount() {
        return playerCount;
    }

    @JsonGetter("players")
    public Map<String, Boolean> getPlayersReady() {
        return playersReady;
    }

    public void addPlayer(String name) {
        if (playerCount() < MAX_PLAYER_COUNT) {
            if (playersReady.containsKey(name)) {
                throw new AlhambraGameRuleException("Name already used");
            } else {
                playersReady.put(name, false);
            }
        } else {
            throw new AlhambraGameRuleException("The lobby is full");
        }
        playerCount = playerCount();
    }

    public int playerCount() {
        return playersReady.size();
    }

    public void removePlayer(String name) {
        if (!playersReady.containsKey(name)) throw new AlhambraEntityNotFoundException("Couldn't find that player: " + name);
        playersReady.remove(name);
        playerCount = playerCount();
    }

    public boolean readyUpPlayer(String name) {
        if (playersReady.replace(name, true) == null) throw new AlhambraEntityNotFoundException("Couldn't find that player: " + name);
        return true;
    }

    public boolean unreadyPlayer(String name) {
        if (playersReady.replace(name, false) == null) throw new AlhambraEntityNotFoundException("Couldn't find that player: " + name);
        return true;
    }

    public void startGame() {
        if (playerCount() > MIN_PLAYER_COUNT) {
            if (amountReady() == playerCount()) {
                this.started = true;
            } else {
                throw new AlhambraGameRuleException("All players need to be ready to start game");
            }
        } else {
            throw new AlhambraGameRuleException("You must be with 2 players to start a game");
        }
    }

    public int amountReady() {
        return (int) playersReady.values().stream().filter(status -> status).count();
    }

    @Override
    public int hashCode() {
        return Objects.hash(started, id, playersReady);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lobby lobby = (Lobby) o;
        return started == lobby.started &&
                Objects.equals(id, lobby.id) &&
                Objects.equals(playersReady, lobby.playersReady);
    }

    @Override
    public String toString() {
        return "Lobby{" +
                "started=" + started +
                ", gameId='" + id + '\'' +
                ", playersReady=" + playersReady +
                '}';
    }
}
