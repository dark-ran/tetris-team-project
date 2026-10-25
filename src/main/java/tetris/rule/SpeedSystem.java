
package tetris.rule;

import java.util.Objects;
import tetris.game.Difficulty;

/**
 * 게임 레벨과 난이도별 자동 낙하 간격을 계산한다.
 *
 * 기존 레벨 산정 규칙은 유지한다.
 */
public class SpeedSystem {
    public static final long INITIAL_INTERVAL_MILLIS = 1_000;
    public static final long MIN_INTERVAL_MILLIS = 100;

    public static final int PIECES_PER_LEVEL = 10;
    public static final int ROWS_PER_LEVEL = 10;

    public static final long INTERVAL_DECREASE_MILLIS = 100;

    /**
     * 생성 수와 삭제 줄 수 중 더 많이 진행된 기준으로
     * 레벨을 결정한다.
     */
    public int level(int spawnedPieces, int clearedRows) {
        if (spawnedPieces < 0 || clearedRows < 0) {
            throw new IllegalArgumentException("negative counter");
        }

        return 1 + Math.max(
                spawnedPieces / PIECES_PER_LEVEL,
                clearedRows / ROWS_PER_LEVEL);
    }

    /**
     * 기존 API는 NORMAL 난이도로 처리한다.
     */
    public long gravityIntervalMillis(
            int spawnedPieces,
            int clearedRows) {
        return gravityIntervalMillis(
                spawnedPieces, clearedRows, Difficulty.NORMAL);
    }

    /**
     * 난이도별 레벨당 낙하 간격 감소량을 적용한다.
     */
    public long gravityIntervalMillis(
            int spawnedPieces,
            int clearedRows,
            Difficulty difficulty) {

        Objects.requireNonNull(
                difficulty, "난이도가 필요합니다.");

        int currentLevel = level(spawnedPieces, clearedRows);

        long decrease = difficulty.intervalDecreaseMillis();

        return Math.max(
                MIN_INTERVAL_MILLIS,
                INITIAL_INTERVAL_MILLIS
                        - (currentLevel - 1L) * decrease);
    }
}
