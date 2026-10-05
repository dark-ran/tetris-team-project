package tetris.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("게임 행동 처리 결과의 전달 규칙")
class GameActionResultTest {
    @ParameterizedTest(name = "하강 {0}, 삭제 {1}은 음수 수치를 포함해 거부")
    @CsvSource({"-1, 0", "0, -1", "-1, -1"})
    void rejectsNegativeCounts(int droppedRows, int clearedRows) {
        assertThrows(IllegalArgumentException.class, () -> new GameActionResult(true,
                droppedRows, clearedRows, true, false, GamePhase.RUNNING, GamePhase.GAME_OVER));
    }

    @Test
    @DisplayName("진행 상태가 없는 결과는 거부")
    void rejectsMissingPhases() {
        assertThrows(NullPointerException.class, () -> new GameActionResult(false, 0, 0,
                false, false, null, GamePhase.READY));
        assertThrows(NullPointerException.class, () -> new GameActionResult(false, 0, 0,
                false, false, GamePhase.READY, null));
    }

    @Test
    @DisplayName("게임오버 전환에서도 마지막 하강과 줄 삭제 결과를 유지")
    void retainsTheFinalScoringDataAndDetectsTheTransition() {
        GameActionResult result = new GameActionResult(true, 18, 4, true, false,
                GamePhase.RUNNING, GamePhase.GAME_OVER);
        assertTrue(result.changed());
        assertEquals(18, result.droppedRows());
        assertEquals(4, result.clearedRows());
        assertTrue(result.pieceLocked());
        assertFalse(result.pieceSpawned());
        assertTrue(result.becameGameOver());
        assertFalse(new GameActionResult(false, 0, 0, false, false,
                GamePhase.GAME_OVER, GamePhase.GAME_OVER).becameGameOver());
    }
}
