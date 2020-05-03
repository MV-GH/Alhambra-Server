package be.howest.ti.alhambra.logic;

import com.fasterxml.jackson.annotation.*;

import java.util.Arrays;
import java.util.Objects;

public class City {

    private static final Building[][] DEFAULT_CITY = {{null, null, null}, {null, new Building(null, 0), null}, {null, null, null}};
    private Building[][] buildings;
    private int mapSize;

    public City() {
        this(City.DEFAULT_CITY);
    }

    @JsonCreator
    public City(@JsonProperty("city") Building[][] buildings) {
        System.out.println(Arrays.deepToString(buildings));
        this.buildings = buildings;
        this.mapSize = buildings.length;
    }

    public Building[][] getDefaultCity() {
        return DEFAULT_CITY;
    }

    @JsonGetter("city")
    public Building[][] getBuildings() {
        return buildings;
    }

    public void placeBuilding(Building building, Location location) { // #todo add validation for building allowed to be placed ivm walls,
        location = Location.convertLocationToStaticLocation(location, mapSize);

        if (buildings[location.getRow()][location.getCol()] != null) { // atm i only check if  the location is already used
            throw new IllegalArgumentException("Location is already used by another building");
        } else {
            buildings[location.getRow()][location.getCol()] = building;
        }
        checkMapSize();
    }

    private void checkMapSize() { //checks if the city needs to be expanded
        for (int row = 0; row < buildings.length; row++) {
            for (int col = 0; col < buildings.length; col++) {
                if (buildings[col][row] != null && ((row == 0 || row == mapSize - 1) || (col == 0 || col == mapSize - 1))) {
                    updateMapSize();
                    return;// stop/exit
                }
            }
        }
    }

    private void updateMapSize() { //expands the city
        mapSize += 2;

        Building[][] newBuildings = new Building[mapSize][mapSize];

        for (int row = 0; row < buildings.length; row++) {
            for (int col = 0; col < buildings.length; col++) {
                newBuildings[col + 1][row + 1] = buildings[col][row];
            }
        }
        buildings = newBuildings;
    }

    public void removeBuilding(Location location) {
        location = Location.convertLocationToStaticLocation(location, mapSize);

        if (buildings[location.getRow()][location.getCol()] == null) {
            throw new IllegalArgumentException("Location is already empty");
        } else {
            buildings[location.getRow()][location.getCol()] = null;
        }
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mapSize);
        result = 31 * result + Arrays.hashCode(buildings);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return mapSize == city.mapSize &&
                Arrays.deepEquals(buildings, city.buildings); //fk i just wasted two hours of my time on this. why does Arrays.equals not properly work with 2 dim arrays
    }

    @Override
    public String toString() {
        return "City{" +
                "buildings=" + Arrays.deepToString(buildings) +
                ", mapSize=" + mapSize +
                '}';
    }
}
