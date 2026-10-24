package tetris.rule;

import java.util.Set;
import tetris.board.Board;
import tetris.piece.Tetromino;

/** 고정 후 삭제할 행을 결정한다. 실제 행 삭제와 누적 수치는 엔진이 처리한다. */
@FunctionalInterface
public interface LockEffect {
    LockEffect FULL_ROWS = (board, piece, row, column) -> board.fullRows();

    Set<Integer> rowsToClear(Board board, Tetromino piece, int row, int column);
}
