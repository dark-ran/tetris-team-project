package tetris.game;

import java.util.Objects;
import tetris.board.Board;
import tetris.piece.Tetromino;

/** 초기 블록의 가장 위쪽 칸을 보드 첫 행에 놓고, 회전 기준 정사각형을 가운데에 배치한다. */
public final class CenteredPieceSpawnPolicy implements PieceSpawnPolicy {
    @Override
    public int startingRow(Tetromino piece) {
        Objects.requireNonNull(piece, "생성할 블록이 필요합니다.");
        int firstRow = Integer.MAX_VALUE;
        for (int[] cell : piece.cells()) firstRow = Math.min(firstRow, cell[0]);
        return -firstRow;
    }

    @Override
    public int startingColumn(Tetromino piece) {
        Objects.requireNonNull(piece, "생성할 블록이 필요합니다.");
        return (Board.COLUMNS - piece.rotationSize()) / 2;
    }
}
