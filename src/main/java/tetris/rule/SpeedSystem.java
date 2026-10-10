package tetris.rule;

/** 생성 수 또는 삭제 줄 수 중 더 많이 진행된 기준에 따라 가속한다. */
public class SpeedSystem {
    public static final long INITIAL_INTERVAL_MILLIS = 1_000;
    public static final long MIN_INTERVAL_MILLIS = 100;
    public static final int PIECES_PER_LEVEL = 10;
    public static final int ROWS_PER_LEVEL = 10;
    public static final long INTERVAL_DECREASE_MILLIS = 100;

    /** 첫 블록도 생성 수에 포함한다. 두 기준을 동시에 넘겨도 레벨을 중복 가산하지 않는다. */
    public int level(int spawnedPieces, int clearedRows) {
        if (spawnedPieces < 0 || clearedRows < 0) throw new IllegalArgumentException("negative counter");
        return 1 + Math.max(spawnedPieces / PIECES_PER_LEVEL, clearedRows / ROWS_PER_LEVEL);
    }

    /** 레벨마다 100ms씩 간격을 줄이되 최소 100ms를 유지한다. */
    public long gravityIntervalMillis(int spawnedPieces, int clearedRows) {
        return Math.max(MIN_INTERVAL_MILLIS,
                INITIAL_INTERVAL_MILLIS - (level(spawnedPieces, clearedRows) - 1L) * INTERVAL_DECREASE_MILLIS);
    }
}
