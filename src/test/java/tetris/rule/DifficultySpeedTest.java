package tetris.rule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tetris.game.Difficulty;

class DifficultySpeedTest {
    @ParameterizedTest @CsvSource({"EASY,920,840", "NORMAL,900,800", "HARD,880,760"})
    void bothLineAndPieceProgressUseTheDifficultyReduction(Difficulty difficulty, long first, long second) {
        SpeedSystem speed = new SpeedSystem();
        assertEquals(1000, speed.gravityIntervalMillis(1, 0, difficulty));
        assertEquals(first, speed.gravityIntervalMillis(1, 10, difficulty));
        assertEquals(first, speed.gravityIntervalMillis(10, 0, difficulty));
        assertEquals(first, speed.gravityIntervalMillis(10, 10, difficulty));
        assertEquals(second, speed.gravityIntervalMillis(1, 20, difficulty));
        assertEquals(100, speed.gravityIntervalMillis(Integer.MAX_VALUE, Integer.MAX_VALUE, difficulty));
    }

    @Test void rejectsMissingDifficultyAndNegativeCounters() {
        SpeedSystem speed = new SpeedSystem();
        assertThrows(NullPointerException.class, () -> speed.gravityIntervalMillis(1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> speed.gravityIntervalMillis(-1, 0, Difficulty.HARD));
    }
}
