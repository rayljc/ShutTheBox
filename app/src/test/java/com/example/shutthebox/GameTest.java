package com.example.shutthebox;


import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class GameTest {

    private final Game game = new Game();

    @Test
    public void testCheckResultSucceed1() {
        final boolean result = game.checkResult(1, 2, Arrays.asList(1, 2));
        assertTrue(result);
    }

    @Test
    public void testCheckResultSucceed2() {
        final boolean result = game.checkResult(3, 5, Arrays.asList(1, 3, 4));
        assertTrue(result);
    }

    @Test
    public void testCheckResultFailed1() {
        final boolean result = game.checkResult(1, 2, Arrays.asList(2, 3));
        assertFalse(result);
    }

    @Test
    public void testCheckResultFailed2() {
        final boolean result = game.checkResult(3, 5, Arrays.asList(1, 2, 6));
        assertFalse(result);
    }

    @Test
    public void testRollDicePointBetweenOneAndSix() {
        final int point = game.rollDice();
        assert(point >= 1);
        assert(point <= 6);
    }

    @Test
    public void testRollDicePointTrulyRandom() {
        boolean random = false;
        final int point = game.rollDice();

        // It's almost impossible to get the same result for 6 times
        for (int i = 0; i < 5; i++) {
            if (point != game.rollDice()) {
                random = true;
            }
        }

        assertTrue(random);
    }
}