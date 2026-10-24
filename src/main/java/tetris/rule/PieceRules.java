package tetris.rule;

import java.util.Objects;
import tetris.piece.Tetromino;

/** 이동 규칙과 고정 효과를 조합한다. 엔진은 개별 효과의 구현을 알지 않는다. */
public record PieceRules(PieceMovement movement, LockEffect lockEffect) {
    private static final PieceRules STANDARD = new PieceRules(PieceMovement.STANDARD, LockEffect.FULL_ROWS);

    public PieceRules {
        Objects.requireNonNull(movement);
        Objects.requireNonNull(lockEffect);
    }

    public static PieceRules forPiece(Tetromino piece) {
        Objects.requireNonNull(piece);
        return STANDARD;
    }
}
