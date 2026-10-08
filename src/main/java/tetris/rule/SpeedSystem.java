package tetris.rule;

/** 10줄 삭제마다 가속한다. */
public class SpeedSystem {
    public static final long INITIAL_INTERVAL_MILLIS = 1_000;
    public static final long MIN_INTERVAL_MILLIS = 100;
    /** 10줄 삭제마다 레벨이 올라간다. */
    public int level(int spawnedPieces, int clearedRows) {
        if (spawnedPieces < 0 || clearedRows < 0) throw new IllegalArgumentException("negative counter");
        return 1 + Math.max(0, clearedRows / 10);
    }

    /** 레벨마다 100ms씩 간격을 줄이되 최소 100ms를 유지한다. */
    public long gravityIntervalMillis(int spawnedPieces, int clearedRows) {
        return Math.max(MIN_INTERVAL_MILLIS,
                INITIAL_INTERVAL_MILLIS - (level(spawnedPieces, clearedRows) - 1L) * 100);
    }
}
