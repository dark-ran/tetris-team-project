package tetris.rule;

/** 기본 낙하 1점, 가속 낙하 2점, 복수 줄 삭제 보너스. */
public class ScoreSystem {
    /** 엔진이 반환한 실제 이동 거리로 계산한다. */
    public int onDrop(int movedCells, boolean accelerated) {
        if (movedCells < 0) throw new IllegalArgumentException("movedCells must be nonnegative");
        return Math.multiplyExact(movedCells, accelerated ? 2 : 1);
    }

    public int onLineClear(int clearedRows) {
        return switch (clearedRows) {
            case 0 -> 0;
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 500;
            case 4 -> 800;
            default -> throw new IllegalArgumentException("clearedRows must be between 0 and 4");
        };
    }
}
