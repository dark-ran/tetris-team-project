package tetris.game;

import java.util.Objects;
import tetris.board.Board;
import tetris.piece.Tetromino;

/** 회전 기준 정사각형을 보드 위쪽 가운데에 배치하는 기본 생성 규칙. */
public final class CenteredPieceSpawnPolicy implements PieceSpawnPolicy {
    @Override
    public int startingRow(Tetromino piece) {
        Objects.requireNonNull(piece, "생성할 블록이 필요합니다.");
        return 0;
    }

    @Override
    public int startingColumn(Tetromino piece) {
        Objects.requireNonNull(piece, "생성할 블록이 필요합니다.");
        return (Board.COLUMNS - piece.rotationSize()) / 2;
    }
}
