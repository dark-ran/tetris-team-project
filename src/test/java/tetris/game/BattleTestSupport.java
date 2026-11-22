package tetris.game;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import tetris.board.Board;
import tetris.piece.PieceQueue;
import tetris.piece.Tetromino;

/** 대전 테스트용 고정 순서 엔진. 블록과 시작 열을 순서대로 반복한다. */
final class BattleTestSupport {
    private BattleTestSupport() { }

    static GameEngine engine(List<Tetromino> pieces, int... columns) {
        AtomicInteger nextPiece = new AtomicInteger();
        Supplier<Tetromino> generator = () -> pieces.get(nextPiece.getAndIncrement() % pieces.size());
        AtomicInteger nextColumn = new AtomicInteger();
        PieceSpawnPolicy policy = new PieceSpawnPolicy() {
            @Override public int startingRow(Tetromino piece) { return 0; }
            @Override public int startingColumn(Tetromino piece) {
                return columns[nextColumn.getAndIncrement() % columns.length];
            }
        };
        return new GameEngine(() -> PieceQueue.generatedBy(generator, 1), policy);
    }

    static AttackLine line(String pattern) {
        boolean[] filled = new boolean[Board.COLUMNS];
        for (int column = 0; column < Board.COLUMNS; column++) filled[column] = pattern.charAt(column) == '#';
        return new AttackLine(filled);
    }
}
