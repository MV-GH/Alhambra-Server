package be.howest.ti.alhambra.logic;

import org.junit.jupiter.api.Test;

import javax.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReserveTest {

    @Test
    void reserve(){
        Building building1 = new Building(BuildingType.valueOf("PAVILION"), 5, null);
        Building building2 = new Building(BuildingType.valueOf("CHAMBERS"), 9, null);
        Building building3 = new Building(BuildingType.valueOf("GARDEN"), 11, null);
        List<Building> buildings = new ArrayList<>();

        Reserve reserve = new Reserve(buildings);
        Reserve reserve2 = new Reserve(buildings);

        reserve.addBuilding(building1);
        reserve.addBuilding(building2);
        reserve.addBuilding(building3);

        assertEquals(building1, reserve.getBuildings().get(0));
        assertEquals(building2, reserve.getBuildings().get(1));
        assertEquals(building3, reserve.getBuildings().get(2));

        assertEquals(reserve, reserve2);
    }
    @Test
    void testEqualsAndHashcode(){
        Building building1 = new Building(BuildingType.valueOf("PAVILION"), 5, null);
        List<Building> buildings = new ArrayList<>();
        buildings.add(building1);

        Reserve reserve = new Reserve(buildings);
        Reserve reserve2 = new Reserve(buildings);

        assertTrue(reserve.equals(reserve2) && reserve2.equals(reserve));
        assertEquals(reserve.hashCode(), reserve2.hashCode());
    }
    @Test
    void testremovebuilding(){
        Building building1 = new Building(BuildingType.valueOf("PAVILION"), 5, null);
        Building building2 = new Building(BuildingType.valueOf("CHAMBERS"), 9, null);
        List<Building> buildings = new ArrayList<>();
        buildings.add(building1);

        Reserve reserve = new Reserve(buildings);
        assertThrows(IllegalArgumentException.class,()->reserve.removeBuilding(building2));
    }

}
