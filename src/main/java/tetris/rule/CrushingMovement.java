package tetris.rule;

import tetris.board.Board;
import tetris.piece.Tetromino;

/** 무게추는 장애물을 통과하며 지운다. 최초 접촉 후 좌우 이동은 계속 제한된다. */
public final class CrushingMovement implements PieceMovement {
    @Override public boolean canDescend(Board board, Tetromino piece, int row, int column) {
        return board.withinBounds(piece, row + 1, column);
    }

    @Override public boolean allowsHorizontal(boolean contacted) { return !contacted; }
    @Override public boolean allowsRotation() { return false; }

    @Override public void afterDescent(Board board, Tetromino piece, int row, int column) {
        board.removeCells(piece, row, column);
    }
}
