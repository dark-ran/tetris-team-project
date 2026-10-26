package tetris.rule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tetris.game.GameActionResult;
import tetris.game.GamePhase;

class GoldenScoreTest {
    @ParameterizedTest
    @CsvSource({"0,5,0", "1,5,500", "2,5,1500", "3,5,2500", "4,5,4000", "2,1,300"})
    void lineBonusUsesOneMultiplierAndDropScoreStaysIndependent(int rows, int multiplier, int expected) {
        ScoreSystem scores = new ScoreSystem();
        assertEquals(expected, scores.onLineClear(rows, multiplier));
        assertEquals(19, scores.onDrop(19, false));
        assertEquals(38, scores.onDrop(19, true));
    }

    @Test void invalidMultipliersAndOverflowAreRejectedAndOldResultsDefaultToOne() {
        ScoreSystem scores = new ScoreSystem();
        for (int factor : new int[] {-1, 0}) {
            assertThrows(IllegalArgumentException.class, () -> scores.onLineClear(1, factor));
            assertThrows(IllegalArgumentException.class, () -> new GameActionResult(true, 0, 1,
                    true, true, GamePhase.RUNNING, GamePhase.RUNNING, factor));
        }
        assertThrows(ArithmeticException.class, () -> scores.onLineClear(4, Integer.MAX_VALUE));
        assertEquals(1, new GameActionResult(false, 0, 0,
                false, false, GamePhase.READY, GamePhase.READY).lineScoreMultiplier());
    }
}
