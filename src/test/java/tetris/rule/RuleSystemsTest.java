package tetris.rule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    @ParameterizedTest
    @CsvSource({"0,0,1,1000", "1,0,1,1000", "9,9,1,1000", "10,0,2,900", "0,10,2,900",
            "10,10,2,900", "19,19,2,900", "20,10,3,800", "10,20,3,800", "20,20,3,800",
            "89,89,9,200", "90,0,10,100", "0,90,10,100", "100,100,11,100",
            "2147483647,2147483647,214748365,100"})
    void eitherCounterIncreasesSpeedWithoutDoubleCounting(int pieces, int rows, int level, long interval) {
        SpeedSystem speed = new SpeedSystem();
        assertEquals(level, speed.level(pieces, rows));
        assertEquals(interval, speed.gravityIntervalMillis(pieces, rows));
    }
    @Test void negativeCountersAreRejected() {
        SpeedSystem speed = new SpeedSystem();
        assertThrows(IllegalArgumentException.class, () -> speed.level(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> speed.gravityIntervalMillis(0, -1));
    }
}
