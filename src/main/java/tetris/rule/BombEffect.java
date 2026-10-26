package tetris.rule;

import java.util.Set;
import tetris.board.Board;
import tetris.piece.Tetromino;

/** 폭탄 중심의 5×5 영역을 비운다. 개별 칸 삭제는 줄 삭제 수에 포함하지 않는다. */
public final class BombEffect implements LockEffect {
    @Override public void onLock(Board board, Tetromino piece, int row, int column) {
        board.removeArea(row, column, 2);
    }

    @Override public Set<Integer> rowsToClear(Board board, Tetromino piece, int row, int column) {
        return Set.of();
    }
}
