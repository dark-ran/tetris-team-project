package tetris.rule;

import java.util.Set;
import tetris.board.Board;
import tetris.piece.Tetromino;

/** 고정 시 보드에 효과를 적용하고 삭제할 행을 결정한다. 행 삭제와 누적 수치는 엔진이 처리한다. */
@FunctionalInterface
public interface LockEffect {
    LockEffect FULL_ROWS = (board, piece, row, column) -> board.fullRows();

    /** 행 전체를 내리지 않는 효과. 실제 고정 시에만 한 번 호출한다. */
    default void onLock(Board board, Tetromino piece, int row, int column) { }

    Set<Integer> rowsToClear(Board board, Tetromino piece, int row, int column);
}
