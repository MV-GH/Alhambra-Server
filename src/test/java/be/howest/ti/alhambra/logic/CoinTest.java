package be.howest.ti.alhambra.logic;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CoinTest {

    @Test
    void coin() {
        Coin coin1 = new Coin(Currency.GREEN, 3);
        Coin coin2 = new Coin(Currency.BLUE, 9);
        Coin coin3 = new Coin(Currency.ORANGE, 11);
        Coin coin4 = new Coin(Currency.BLUE, 6);

        Coin[] coins = new Coin[]{coin1, coin2, coin3};

        assertEquals(23, Coin.getSumCoins(coins) );


        coins = new Coin[]{coin1};
        assertTrue(Coin.coinsSameCurrency(coins));

        coins = new Coin[]{coin1, coin2};
        assertFalse(Coin.coinsSameCurrency(coins));

        coins = new Coin[]{coin2, coin4};
        assertTrue(Coin.coinsSameCurrency(coins));
    }
}
