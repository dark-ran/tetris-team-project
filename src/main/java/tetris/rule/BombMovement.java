package tetris.rule;

import tetris.board.Board;
import tetris.piece.Tetromino;

/** 다이너마이트는 가로 세 칸을 유지하며, 어느 칸이든 막히면 더 내려가지 않는다. */
public final class BombMovement implements PieceMovement {
    @Override public boolean canDescend(Board board, Tetromino piece, int row, int column) {
        return board.canPlace(piece, row + 1, column);
    }

    @Override public boolean allowsRotation() { return false; }
}
