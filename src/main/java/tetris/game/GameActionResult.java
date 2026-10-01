package tetris.game;

import java.util.Objects;

/** 한 행동의 처리 결과. 점수 계산과 상태 전환 후처리는 앱 제어에서 담당한다. */
public record GameActionResult(boolean changed, int droppedRows, int clearedRows,
        boolean pieceLocked, boolean pieceSpawned, GamePhase previousPhase, GamePhase currentPhase) {
    public GameActionResult {
        Objects.requireNonNull(previousPhase, "처리 전 진행 상태가 필요합니다.");
        Objects.requireNonNull(currentPhase, "처리 후 진행 상태가 필요합니다.");
        if (droppedRows < 0 || clearedRows < 0) {
            throw new IllegalArgumentException("하강 칸 수와 삭제된 줄 수는 음수일 수 없습니다.");
        }
    }

    /** 게임오버 후처리를 한 번만 실행할 때 사용하는 전환 여부. */
    public boolean becameGameOver() {
        return previousPhase != GamePhase.GAME_OVER && currentPhase == GamePhase.GAME_OVER;
    }
}
