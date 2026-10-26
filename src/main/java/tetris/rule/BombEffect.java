package tetris.rule;

import java.util.Set;
import java.util.Optional;
import tetris.board.Board;
import tetris.piece.Tetromino;

/** 고정 시 아래쪽 접촉 위치에 따라 중심을 선택하여 5×5 영역을 비운다. */
public final class BombEffect implements LockEffect {
    @Override public void onLock(Board board, Tetromino piece, int row, int column) {
        TargetCell center = blastCenter(board, row, column);
        board.removeArea(center.row(), center.column(), 2);
    }

    @Override public Optional<TargetCell> targetCell(Board board, Tetromino piece, int row, int column) {
        return Optional.of(blastCenter(board, row, column));
    }

    private static TargetCell blastCenter(Board board, int row, int column) {
        int centerColumn = column + 1;
        if (row + 1 < Board.ROWS) {
            int[][] fixed = board.snapshot();
            if (fixed[row + 1][centerColumn] == Board.EMPTY_CELL) {
                boolean left = fixed[row + 1][column] != Board.EMPTY_CELL;
                boolean right = fixed[row + 1][column + 2] != Board.EMPTY_CELL;
                if (left != right) centerColumn = left ? column : column + 2;
            }
        }
        return new TargetCell(row, centerColumn);
    }

    @Override public Set<Integer> rowsToClear(Board board, Tetromino piece, int row, int column) {
        return Set.of();
    }
}
