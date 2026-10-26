package tetris.game;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class BombItemEngineTest {
    @ParameterizedTest @EnumSource(value = GameAction.class, names = {"TICK", "DOWN", "HARD_DROP"})
    void explodesAtActualLandingCenterOnlyOnceAndPreservesOtherCells(GameAction action) {
        GameEngine engine = sequence(5);
        engine.newGame(GameMode.ITEM);
        for (int column : new int[] {4, 4, 4, 0, 8}) {
            moveToColumn(engine, column);
            engine.apply(GameAction.HARD_DROP);
        }
        moveToColumn(engine, 3);
        Tetromino bomb = engine.state().currentPiece();
        Tetromino preview = engine.state().upcomingPieces().getFirst();
        int[][] expected = engine.state().board().snapshot();
        assertEquals(13, engine.state().landingRow(), "기존 블록 위에서 멈추며 관통하지 않는다.");
        for (int i = 0; i < 10; i++) engine.state();
        assertArrayEquals(expected, engine.state().board().snapshot(), "착지 위치 조회는 폭발시키지 않는다.");
        assertFalse(engine.apply(GameAction.ROTATE_CLOCKWISE).changed());
        for (int row = 11; row <= 15; row++)
            for (int col = 2; col <= 6; col++) expected[row][col] = 0;
        int steps = 0;
        GameActionResult result;
        do {
            result = engine.apply(action);
            assertEquals(0, result.clearedRows());
            assertTrue(++steps <= 20);
        } while (engine.state().currentPiece() == bomb);
        assertTrue(result.pieceLocked());
        assertTrue(result.pieceSpawned());
        assertSame(preview, engine.state().currentPiece());
        assertEquals(0, engine.state().totalClearedRows());
        assertArrayEquals(expected, engine.state().board().snapshot(), "폭탄은 남지 않고 영역 밖은 보존한다.");
        engine.apply(GameAction.LEFT);
        assertArrayEquals(expected, engine.state().board().snapshot(), "다음 행동에서 다시 폭발하지 않는다.");
    }

    @Test void pausedActionsAndMovementDoNotExplodeAndBombCanSlideAfterContact() {
        GameEngine engine = sequence(1);
        engine.newGame(GameMode.ITEM);
        engine.apply(GameAction.HARD_DROP);
        int[][] before = engine.state().board().snapshot();
        engine.apply(GameAction.TOGGLE_PAUSE);
        for (GameAction action : new GameAction[] {GameAction.TICK, GameAction.DOWN, GameAction.HARD_DROP})
            assertFalse(engine.apply(action).changed());
        assertArrayEquals(before, engine.state().board().snapshot());
        engine.apply(GameAction.TOGGLE_PAUSE);
        for (int i = 0; i < 17; i++) engine.apply(GameAction.TICK);
        assertEquals(17, engine.state().currentRow());
        assertTrue(engine.apply(GameAction.LEFT).changed());
        assertTrue(engine.apply(GameAction.RIGHT).changed());
        assertArrayEquals(before, engine.state().board().snapshot(), "접촉과 좌우 이동만으로 폭발하지 않는다.");
        engine.apply(GameAction.TICK);
        assertArrayEquals(new int[20][10], engine.state().board().snapshot());
    }

    @Test void emptyBoardFloorAndBothSideWallsAreValidBlastCenters() {
        for (int column : new int[] {0, 7}) {
            GameEngine engine = sequence(0);
            engine.newGame(GameMode.ITEM);
            moveToColumn(engine, column);
            assertFalse(engine.apply(column == 0 ? GameAction.LEFT : GameAction.RIGHT).changed());
            assertTrue(engine.state().board().canPlace(engine.state().currentPiece(), 19, column));
            assertEquals(19, engine.state().landingRow());
            assertTrue(engine.apply(GameAction.HARD_DROP).pieceLocked());
            assertArrayEquals(new int[20][10], engine.state().board().snapshot());
            assertEquals(0, engine.state().totalClearedRows());
        }
    }

    @ParameterizedTest @EnumSource(value = GameAction.class, names = {"TICK", "DOWN", "HARD_DROP"})
    void eitherEndStopsAboveTheStackWithoutOverlappingIt(GameAction action) {
        for (int obstacleColumn : new int[] {2, 5}) {
            GameEngine engine = sequence(1);
            engine.newGame(GameMode.ITEM);
            moveToColumn(engine, obstacleColumn);
            engine.apply(GameAction.HARD_DROP);
            Tetromino bomb = engine.state().currentPiece();
            assertEquals(3, engine.state().currentColumn());
            assertEquals(17, engine.state().landingRow());
            int[][] before = engine.state().board().snapshot();
            assertFalse(engine.apply(GameAction.ROTATE_CLOCKWISE).changed());
            assertSame(bomb, engine.state().currentPiece());
            int steps = 0;
            while (engine.state().currentPiece() == bomb) {
                GameActionResult result = engine.apply(action);
                assertTrue(++steps <= 20);
                if (!result.pieceLocked()) {
                    assertTrue(engine.state().currentRow() <= 17);
                    assertTrue(engine.state().board().canPlace(bomb,
                            engine.state().currentRow(), engine.state().currentColumn()));
                    assertArrayEquals(before, engine.state().board().snapshot());
                }
            }
            assertArrayEquals(new int[20][10], engine.state().board().snapshot());
            assertEquals(0, engine.state().totalClearedRows());
        }
    }

    private static GameEngine sequence(int squaresBeforeBomb) {
        return new GameEngine(() -> {
            AtomicInteger index = new AtomicInteger();
            return PieceQueue.generatedBy(() -> {
                int next = index.getAndIncrement();
                return next < squaresBeforeBomb ? new Tetromino(PieceType.O)
                        : next == squaresBeforeBomb ? Tetromino.bomb() : new Tetromino(PieceType.I);
            }, 1);
        }, new CenteredPieceSpawnPolicy());
    }

    private static void moveToColumn(GameEngine engine, int column) {
        while (engine.state().currentColumn() != column)
            assertTrue(engine.apply(engine.state().currentColumn() < column
                    ? GameAction.RIGHT : GameAction.LEFT).changed());
    }
}
