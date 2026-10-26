package tetris.rule;

import java.util.HashSet;
import java.util.Set;
import tetris.board.Board;
import tetris.piece.Tetromino;

/** 꽉 찬 줄과 L이 있는 줄을 함께 삭제한다. 겹치는 줄은 한 번만 센다. */
public final class LineClearEffect implements LockEffect {
    @Override public Set<Integer> rowsToClear(Board board, Tetromino piece, int row, int column) {
        Set<Integer> rows = new HashSet<>(board.fullRows());
        rows.add(row + piece.itemRow());
        return rows;
    }
}
