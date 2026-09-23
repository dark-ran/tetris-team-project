package tetris.rule;

/** 낙하 및 줄 삭제 점수를 담당한다. 가속 보너스와 추가 점수 규칙은 미정이다. */
public class ScoreSystem {
    public int onDrop(int movedCells, boolean accelerated) {
        // TODO(Req1): 낙하 점수 계산
        throw new UnsupportedOperationException("TODO: 낙하 점수 계산");
    }

    public int onLineClear(int clearedRows) {
        // TODO(Req1): 줄 삭제 점수 계산
        throw new UnsupportedOperationException("TODO: 줄 삭제 점수 계산");
    }
}
