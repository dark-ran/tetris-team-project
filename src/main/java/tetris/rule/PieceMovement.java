package tetris.rule;

import tetris.board.Board;
import tetris.piece.Tetromino;

/** 블록별 이동 규칙. 화면의 착지 예상 위치도 같은 규칙으로 계산한다. */
public interface PieceMovement {
    PieceMovement STANDARD = (board, piece, row, column) -> board.canPlace(piece, row + 1, column);

    boolean canDescend(Board board, Tetromino piece, int row, int column);

    default boolean allowsHorizontal(boolean contacted) { return true; }
    default boolean allowsRotation() { return true; }
    default void afterDescent(Board board, Tetromino piece, int row, int column) { }

    default int landingRow(Board board, Tetromino piece, int row, int column) {
        while (canDescend(board, piece, row, column)) row++;
        return row;
    }
}
