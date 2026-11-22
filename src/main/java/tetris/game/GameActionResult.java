package tetris.game;

import java.util.List;
import java.util.Objects;

/**
 * 한 행동의 처리 결과. 점수 계산과 상태 전환 후처리는 앱 제어에서 담당한다.
 * attackLines는 대전 모드에서 상대에게 보낼 공격 줄이며, 공격이 없으면 빈 목록이다.
 */
public record GameActionResult(boolean changed, int droppedRows, int clearedRows,
        boolean pieceLocked, boolean pieceSpawned, GamePhase previousPhase, GamePhase currentPhase,
        int lineScoreMultiplier, List<AttackLine> attackLines) {
    /** 기존 호출은 기본 배율 1을 사용한다. */
    public GameActionResult(boolean changed, int droppedRows, int clearedRows,
            boolean pieceLocked, boolean pieceSpawned, GamePhase previousPhase, GamePhase currentPhase) {
        this(changed, droppedRows, clearedRows, pieceLocked, pieceSpawned, previousPhase, currentPhase, 1);
    }

    /** 공격 줄이 없는 결과. */
    public GameActionResult(boolean changed, int droppedRows, int clearedRows,
            boolean pieceLocked, boolean pieceSpawned, GamePhase previousPhase, GamePhase currentPhase,
            int lineScoreMultiplier) {
        this(changed, droppedRows, clearedRows, pieceLocked, pieceSpawned, previousPhase, currentPhase,
                lineScoreMultiplier, List.of());
    }

    public GameActionResult {
        Objects.requireNonNull(previousPhase, "처리 전 진행 상태가 필요합니다.");
        Objects.requireNonNull(currentPhase, "처리 후 진행 상태가 필요합니다.");
        Objects.requireNonNull(attackLines, "공격 줄 목록이 필요합니다.");
        if (droppedRows < 0 || clearedRows < 0) {
            throw new IllegalArgumentException("하강 칸 수와 삭제된 줄 수는 음수일 수 없습니다.");
        }
        if (lineScoreMultiplier < 1) {
            throw new IllegalArgumentException("줄 삭제 점수 배율은 1 이상이어야 합니다.");
        }
        attackLines = List.copyOf(attackLines);
    }

    /** 게임오버 후처리를 한 번만 실행할 때 사용하는 전환 여부. */
    public boolean becameGameOver() {
        return previousPhase != GamePhase.GAME_OVER && currentPhase == GamePhase.GAME_OVER;
    }
}
