package tetris.rule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** 점수 보너스와 속도 규칙의 경계값·잘못된 입력을 검증한다. */
class RuleSystemsTest {
    @Test void scoresActualDistanceAndAddsLineBonuses() {
        ScoreSystem scores = new ScoreSystem();
        assertEquals(0, scores.onDrop(0, true));
        assertEquals(19, scores.onDrop(19, false));
        assertEquals(38, scores.onDrop(19, true));
        int[] expected = {0, 100, 300, 500, 800};
        for (int rows = 0; rows <= 4; rows++) assertEquals(expected[rows], scores.onLineClear(rows));
        assertThrows(IllegalArgumentException.class, () -> scores.onDrop(-1, false));
        assertThrows(IllegalArgumentException.class, () -> scores.onLineClear(5));
        assertThrows(ArithmeticException.class, () -> scores.onDrop(Integer.MAX_VALUE, true));
    }
    @Test void thresholdsUseFirstPieceAndEitherCounterAndClampAt100Millis() {
        SpeedSystem speed = new SpeedSystem();
        assertEquals(1, speed.level(0, 0));
        assertEquals(1000, speed.gravityIntervalMillis(1, 0));
        assertEquals(1000, speed.gravityIntervalMillis(10, 9));
        assertEquals(1, speed.level(11, 0));
        assertEquals(900, speed.gravityIntervalMillis(1, 10));
        assertEquals(2, speed.level(11, 10));
        assertEquals(100, speed.gravityIntervalMillis(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> speed.level(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> speed.gravityIntervalMillis(0, -1));
    }
}
