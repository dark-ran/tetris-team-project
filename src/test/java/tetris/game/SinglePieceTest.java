package tetris.game;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tetris.board.Board;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class SinglePieceTest {
    @Test void oneCellCanRotateMoveLockAndCompleteALine() {
        Tetromino single = new Tetromino(PieceType.SINGLE);
        assertEquals(1, single.rotationSize());
        assertArrayEquals(new int[][] {{0, 0}}, single.rotateClockwise().cells());
        Board board = new Board();
        for (int col = 0; col < Board.COLUMNS - 1; col++) board.lock(single, 19, col);
        assertEquals(0, board.clearFullRows());
        board.lock(single, 19, 9);
        assertFalse(board.canPlace(single, 19, 9));
        assertFalse(board.canPlace(single, 20, 9));
        assertEquals(1, board.clearFullRows());
        assertArrayEquals(new int[10], board.snapshot()[19]);
    }

    @Test void itemPreviewSpawnsOneCellAndLocksAtItsActualPosition() {
        GameEngine engine = new GameEngine(() -> {
            PieceQueue queue = new PieceQueue(new Random(1));
            queue.replaceLastPreview(PieceType.SINGLE);
            return queue;
        }, new CenteredPieceSpawnPolicy());
        assertThrows(IllegalStateException.class, () -> engine.replaceLastPreviewPiece(PieceType.SINGLE));
        engine.newGame();
        assertEquals(PieceType.SINGLE, engine.state().currentPiece().type());
        assertEquals(0, engine.state().currentRow());
        assertEquals(4, engine.state().currentColumn());
        assertFalse(engine.apply(GameAction.ROTATE_CLOCKWISE).changed());
        engine.apply(GameAction.LEFT);
        engine.apply(GameAction.DOWN);
        engine.apply(GameAction.HARD_DROP);
        assertEquals(8, engine.state().board().snapshot()[19][3]);
        engine.replaceLastPreviewPiece(PieceType.SINGLE);
        assertEquals(List.of(PieceType.SINGLE), engine.state().nextPieces());
        engine.apply(GameAction.TOGGLE_PAUSE);
        engine.replaceLastPreviewPiece(PieceType.T);
        assertEquals(List.of(PieceType.T), engine.state().nextPieces());
    }

    @Test void gameSnapshotKeepsItsOriginalDifficulty() {
        GameEngine engine = new GameEngine(new Random(17));
        engine.newGame(Difficulty.EASY);
        GameState old = engine.state();
        engine.newGame(Difficulty.HARD);
        assertEquals(Difficulty.EASY, old.difficulty());
        assertEquals(Difficulty.HARD, engine.state().difficulty());
        assertThrows(NullPointerException.class, () -> engine.newGame(null));
    }
}
