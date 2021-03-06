package be.howest.ti.alhambra.logic;

import be.howest.ti.alhambra.logic.exceptions.AlhambraEntityNotFoundException;
import be.howest.ti.alhambra.logic.exceptions.AlhambraGameRuleException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Game {
    private final List<Player> players;
    private final Bank bank;
    private final Market market;
    @JsonIgnore
    private final List<Building> buildings;
    @JsonIgnore
    private final List<Coin> coins;
    private final Random rand;
    @JsonProperty
    private Player dirk;
    private boolean ended;
    private String currentPlayer;
    @JsonIgnore
    private int index;
    @JsonIgnore
    private int round;

    public Game(List<PlayerInLobby> names) {
        this(false, "", convertNamesIntoPlayers(names), new Coin[4], new HashMap<>(), names.size() == 2 ? new Player("dirk\u2122") : null);
    }

    @JsonCreator
    public Game(@JsonProperty("ended") boolean ended, @JsonProperty("currentPlayer") String currentPlayer, @JsonProperty("players") List<Player> players, @JsonProperty("bank") Coin[] bank, @JsonProperty("market") Map<Currency, Building> market, @JsonProperty("twoPlayerSystem") Player dirk) {
        rand = new Random();
        this.ended = ended;
        this.currentPlayer = currentPlayer;
        this.players = players;
        this.bank = new Bank(bank);
        this.market = new Market(market);
        this.dirk = dirk;
        index = 0;
        round = 1;
        buildings = new ArrayList<>(loadFromFile()); //loadFromFile returns a fixed size list
        coins = dirk == null ? Coin.allCoins() : Coin.allCoinsTwoPlayers(); // two playerSystem has only 72 coins
        Collections.shuffle(buildings);
        Collections.shuffle(coins);
        addScoreRounds();//must before all other methods that might remove Coins
        givePlayersStarterCoins();
        nextPlayer();
        giveBuildingsToDirk(6);
    }


    public static List<Player> convertNamesIntoPlayers(List<PlayerInLobby> allPlayers) {
        List<Player> newPlayers = new ArrayList<>();
        allPlayers.forEach(player -> newPlayers.add(new Player(player.getName()).setToken(player.getToken())));
        return newPlayers;
    }

    public boolean giveBuildingToDirk(Building building, String playerName) {  // gives a building to dirk, check if two player system is on, if that players has that building
        if (dirk == null) throw new AlhambraGameRuleException("Dirk can solely be used when there is only two players!");
        if (!findPlayer(playerName).getBuildingsInHand().remove(building)) throw new AlhambraEntityNotFoundException("Couldn't find that building (" + building + ") in the hand of " + playerName);
        dirk.getReserve().addBuilding(building);
        return true;
    }

    public Player findPlayer(String name) {
        return players.stream().filter(player -> player.getName().equals(name)).findFirst().orElseThrow(() -> new AlhambraEntityNotFoundException("Couldn't find that player: " + name));
    }

    private void giveBuildingsToDirk(int amount) {
        if (dirk != null) { // dirk can be added at any time so it should only do it when dirk is added
            for (int i = 0; i < amount; i++) {
                dirk.getReserve().addBuilding(this.removeBuilding());
            }
        }
    }

    public void removePlayer(String name) {
        index = players.size() - 1 == index ? 0 : index; // reset index to prevent IOB when nextPlayer is called
        players.remove(findPlayer(name));
        if (players.size() > 1 && currentPlayer.equals(name)) nextPlayer(); // cant have a person that left as current Player
        if (players.size() == 2) dirk = new Player("dirk\u2122");
        else if (players.size() == 1) endGame();
    }

    private void endGame() { //end the game
        scoreRound(); // last score round
        givePlayersTitles();
        ended = true;
    }

    public void scoreRound() { // this function gets called when there is a score round
        ScoringTable.calcScoreBuildings(players, round++, dirk).forEach((player, score) -> {
            player.setScore(player.getScore() + score + player.getCity().calcScoreWall()); //adds the new score of buildings and wall score to the old score
            player.setVirtualScore(0); // set the virtual score to zero so that the market Counter may sense that there is a new round
        });
        if (round == 1) {
            giveBuildingsToDirk(6);
        } else if (round == 2) {
            giveBuildingsToDirk(buildings.size() / 3);
        }
    }

    private List<Building> loadFromFile() {
        try (InputStream in = Game.class.getResourceAsStream("/buildings.json")) {
            return Arrays.asList(
                    Json.decodeValue(Buffer.buffer(in.readAllBytes()),
                            Building[].class)
            );
        } catch (IOException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to load buildings", ex);
            return Collections.emptyList();
        }
    }

    private void addScoreRounds() {
        int firstScore = new Random().nextInt(coins.size() / 5) + coins.size() / 5; // so between 20 and 40
        int secondScore = new Random().nextInt(coins.size() / 5) + coins.size() / 5 * 3; // so between 60 and 80
        coins.add(firstScore, new Coin(null, 0));
        coins.add(secondScore, new Coin(null, 0));
    }


    private void givePlayersTitles() {
        Map<PlayerTitle, Player> titles = new HashMap<>();
        Map<Player, List<PlayerTitle>> playerWithTitle = new HashMap<>();

        players.forEach(player -> playerWithTitle.put(player, new ArrayList<>())); // init values
        PlayerTitle.getAllPlayerTitles().forEach(title -> { //for each title it calculates that players his score for that title and if its higher than previous then replace it else if equal then null
            players.forEach(player -> {
                int playerValue = calcPlayerTitleValue(player, title);
                if (title.getValue() < playerValue) {
                    title.setValue(playerValue);
                    titles.put(title, player);
                } else if (title.getValue() == playerValue) { //only keep the highest title for each player, duplicate highest title and titles with values zero gets disregarded
                    titles.put(title, null);
                }
            });
            if (titles.get(title) != null) playerWithTitle.get(titles.get(title)).add(title); //adds that title to the list of title that person has

        });

        playerWithTitle.forEach((player, titleList) -> { // gives each player a random title out of list of titles they have
            if (titleList.isEmpty()) { //makes sure each player has at least one title
                titleList.add(PlayerTitle.getDefault());
            }
            player.setTitle(titleList.get(rand.nextInt(titleList.size())));
        });
    }

    private int calcPlayerTitleValue(Player player, PlayerTitle title) { // sets the value for each player Title
        switch (title.getRole()) {
            case "The hoarder":
                return Coin.getSumCoins(player.getCoins().getCoinsBag().toArray(Coin[]::new));
            case "The Great Wall of China":
                return player.getCity().calcScoreWall();
            case "The Collector":
                return player.getReserve().getBuildings().size();
            case "Bob the builder":
                return (int) Arrays.stream(player.getCity().getBuildings())
                        .flatMap(Arrays::stream)
                        .filter(building -> building != null && building.getType() != null)
                        .count();
            case "Richie Rich":
                return Coin.getSumCoins(player.getCoins().getSpentCoins().toArray(Coin[]::new));
            case "The stalker":
                return player.getViewTown();
            case "Mr. Perfect":
                return player.getRedesigns();
            case "Nothing special":
                return 0;
            default:
                throw new IllegalArgumentException("Given title is not supported: " + title);
        }
    }

    public boolean isEnded() {
        return ended;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Bank getBank() {
        return bank;
    }

    public Market getMarket() {
        return market;
    }

    public Coin removeCoin() { //removes an coin and returns it
        try {
            Coin coin = coins.remove(0);
            if (coin.getAmount() == 0) { //calls an scoreRound
                scoreRound();
                return removeCoin();
            }
            return coin;
        } catch (IndexOutOfBoundsException e) {
            // game ends when no buildings are left
            // this shouldn't throw an error since bank.fillBank works with nulls
            return null;
        }
    }

    @JsonProperty("market") //this method is solely used for JSON conversion
    public Map<Currency, Building> marketConvert() {
        return market.getMarket();
    }

    @JsonProperty("bank") //this method is solely used for JSON conversion
    public Coin[] bankConvert() {
        return bank.getBankCoins();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ended, currentPlayer, players, bank, market);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return ended == game.ended &&
                Objects.equals(currentPlayer, game.currentPlayer) &&
                Objects.equals(players, game.players) &&
                Objects.equals(bank, game.bank) &&
                Objects.equals(market, game.market);
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public Building removeBuilding() {
        try {
            return buildings.remove(0);
        } catch (IndexOutOfBoundsException e) {
            // end game bc game is over, used up all the buildings
            endGame();
            return null;
        }
    }

    private void givePlayersStarterCoins() {
        players.forEach(player -> {
            int sum = 0;
            List<Coin> bag = new ArrayList<>();
            while (sum < 20) {
                Coin coin = removeCoin();
                sum += coin.getAmount();
                bag.add(coin);
            }
            player.getCoins().addCoins(bag.toArray(Coin[]::new));
        });
    }

    public Game takeCoins(String playerName, Coin[] coins) {
        checkTurn(playerName);
        try {
            bank.removeCoins(coins, true);
            bank.removeCoins(coins); //now i actually remove them
            findPlayer(playerName).getCoins().addCoins(coins); // now add them to the player
            nextPlayer();
        } catch (IllegalArgumentException exception) {
            throw new AlhambraEntityNotFoundException("Couldn't find those coins: " + Arrays.toString(coins));
        }

        return this;
    }

    private void checkTurn(String playerName) {
        if (!currentPlayer.equals(playerName)) throw new AlhambraGameRuleException("It's not your turn");
    }

    private void nextPlayer() { // when called it sets the next current Player
        ScoringTable.calcScoreBuildings(players, round).forEach(Player::setVirtualScore); // set the virtual score
        bank.fillBank(this);
        market.fillMarkets(this);
        currentPlayer = players.get(index).getName(); // gets the name of the currentPlayer
        if (++index >= players.size()) index = 0; // add one to the index and set it to zero when max is reached
    }

    /* Checks if the turn of this person, all coins are same currency,the sum of coins is enough
     * and then either changes the turn or not depending on same cost as sum
     * Then moves that building from market to buildingsInHand and removes the coins from the player */
    public Game buyBuilding(String playerName, Currency currency, Coin[] coins) {
        checkTurn(playerName);
        int sum = Coin.getSumCoins(coins);
        int cost = market.getBuilding(currency).getCost();
        Player player = findPlayer(playerName);

        if (!player.getCoins().containsCoins(coins)) throw new AlhambraGameRuleException("Player doesn't own those coins");
        else if (!Coin.coinsSameCurrency(coins)) throw new AlhambraGameRuleException("Coins must have the same currency");
        else if (sum < cost) throw new AlhambraGameRuleException("Not enough coins were given");
        else {
            player.getBuildingsInHand().add(market.removeBuilding(currency)); //remove and add it to the hand
            player.getCoins().removeCoins(coins);
            if (sum != cost) nextPlayer();
        }
        return this;
    }

    public Game build(String playerName, Building building, Location location) {
        Player player = findPlayer(playerName);

        if (!player.getBuildingsInHand().remove(building)) throw new AlhambraEntityNotFoundException("Couldn't find that building in the hand");
        if (location == null) {
            player.getReserve().addBuilding(building);
        } else {
            player.getCity().placeBuilding(building, location);
        }
        return this;
    }

    /* check if its the player turn and check if the player has that building in reserve
     *
     * */
    public Game redesign(String playerName, Building building, Location location) {
        checkTurn(playerName);
        Player player = findPlayer(playerName);
        if (building != null && !player.getReserve().contains(building)) throw new AlhambraEntityNotFoundException("Couldn't that find building in reserve");
        else if (building == null && location != null) cityToReserve(player, location);//city to reserve
        else if (location != null) { //reserve to city or swap if there is a building on the location
            Building oldBuilding = player.getCity().getBuilding(location);

            if (oldBuilding != null) cityToReserve(player, location);
            try { //try and swap the building
                player.getCity().placeBuilding(building, location);
            } catch (AlhambraGameRuleException e) { //put the building back in the city that was removed
                player.getReserve().removeBuilding(oldBuilding);
                player.getCity().placeBuilding(oldBuilding, location);
                throw e;
            }
            player.getReserve().removeBuilding(building);
        } else {
            throw new AlhambraGameRuleException("Incorrect usage of redesign api");
        }
        nextPlayer();
        player.incrRedesign(); // stats for playerTitle
        return this;
    }

    private void cityToReserve(Player player, Location location) {
        Building relocateBuilding = player.getCity().getBuilding(location);

        if (relocateBuilding != null && relocateBuilding.getType() != null) { //check if not fountain or empty
            player.getReserve().addBuilding(player.getCity().removeBuilding(location));
        } else {
            throw new AlhambraEntityNotFoundException("Wrong location given for city to reserve " + location);
        }
    }

}
