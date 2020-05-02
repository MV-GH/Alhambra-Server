package be.howest.ti.alhambra.logic;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class Building {
    private final BuildingType type;
    private final int cost;
    private final Map<String, Boolean> walls;


    @JsonCreator
    public Building(
        @JsonProperty("type") BuildingType type,
        @JsonProperty("cost") int cost,
        @JsonProperty("walls")  Map<String, Boolean> walls
        )
        {
        this.type = type;
        this.cost = cost;
        this.walls = walls;
    }

    public BuildingType getType() {
        return type;
    }

    public int getCost() {
        return cost;
    }

    public Map<String, Boolean> getWalls() {
        return walls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Building building = (Building) o;
        return cost == building.cost &&
                Objects.equals(type, building.type) &&
                Objects.equals(walls, building.walls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, cost, walls);
    }
}