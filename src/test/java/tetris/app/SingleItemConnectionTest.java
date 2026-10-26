package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.loop.GameLoop;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;

class SingleItemConnectionTest {
    @TempDir Path directory;

    static Stream<Arguments> modesAndDrops() {
        return Stream.of(GameMode.values()).flatMap(mode -> Stream.of(
                GameAction.TICK, GameAction.DOWN, GameAction.HARD_DROP).map(action -> Arguments.of(mode, action)));
    }

    @ParameterizedTest @MethodSource("modesAndDrops")
    void productionRewardPreviewsSpawnsAndScoresSingleOnlyInItemMode(GameMode mode, GameAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(42) {
                int pieces;
                @Override public int nextInt(int bound) {
                    if (bound == 7) return pieces++ % 3 == 2 ? PieceType.O.ordinal() : PieceType.I.ordinal();
                    if (bound == 5) return 4;
                    return super.nextInt(bound);
                }
            });
            AppController app = app(engine);
            try {
                app.startGame(mode);
                while (engine.state().totalClearedRows() < 10) {
                    assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
                    assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
                    moveTo(app, engine, engine.state().currentPiece().type() == PieceType.O ? 8
                            : engine.state().board().snapshot()[19][0] == 0 ? 0 : 4);
                    app.handleAction(GameAction.HARD_DROP);
                    assertFalse(engine.isGameOver());
                    assertTrue(engine.state().spawnedPieces() < 40);
                }
                Tetromino preview = engine.state().upcomingPieces().getFirst();
                assertEquals(mode == GameMode.ITEM ? ItemType.SINGLE : ItemType.NONE, preview.itemType());
                assertEquals(mode == GameMode.ITEM, preview.isSingleCell());
                moveTo(app, engine, 8);
                app.handleAction(GameAction.HARD_DROP);
                assertSame(preview, engine.state().currentPiece());
                if (mode == GameMode.ITEM) {
                    moveTo(app, engine, 0);
                    app.handleAction(GameAction.ROTATE_CLOCKWISE);
                    assertSame(preview, engine.state().currentPiece());
                    long score = engine.state().score();
                    int distance = engine.state().landingRow() - engine.state().currentRow();
                    int steps = 0;
                    while (engine.state().currentPiece() == preview) {
                        app.handleAction(action);
                        assertTrue(++steps <= 21);
                    }
                    assertEquals(score + distance * 2, engine.state().score());
                    assertEquals(PieceType.SINGLE.cellValue(), engine.state().board().snapshot()[19][0]);
                    assertEquals(ItemType.SINGLE, engine.state().board().itemAt(19, 0));
                    assertEquals(10, engine.state().totalClearedRows());
                }
                app.restartGame();
                assertEquals(mode, engine.state().mode());
                assertEquals(0, engine.state().totalClearedRows());
                assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
                assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
            } finally { app.exit(); }
        });
    }

    @Test void singleFillsTheLastGapAndReceivesNormalLineAndDropPoints() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger blocks = new AtomicInteger();
            GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(
                    () -> blocks.getAndIncrement() == 9 ? Tetromino.singleCell() : new Tetromino(PieceType.SINGLE), 1),
                    new CenteredPieceSpawnPolicy());
            AppController app = app(engine);
            try {
                app.startGame(GameMode.ITEM);
                for (int col = 0; col < 9; col++) {
                    moveTo(app, engine, col);
                    app.handleAction(GameAction.HARD_DROP);
                }
                assertEquals(ItemType.SINGLE, engine.state().currentPiece().itemType());
                moveTo(app, engine, 9);
                long before = engine.state().score();
                int distance = engine.state().landingRow() - engine.state().currentRow();
                app.handleAction(GameAction.HARD_DROP);
                assertEquals(1, engine.state().totalClearedRows());
                assertEquals(before + distance * 2 + 100, engine.state().score());
                assertArrayEquals(new int[20][10], engine.state().board().snapshot());
            } finally { app.exit(); }
        });
    }

    private AppController app(GameEngine engine) {
        GameLoop loop = new GameLoop() { @Override public void start() { } };
        return new AppController(() -> {}, engine, loop, ignored -> {}, ignored -> {},
                new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), null, ignored -> {});
    }

    private static void moveTo(AppController app, GameEngine engine, int column) {
        while (engine.state().currentColumn() != column) {
            int before = engine.state().currentColumn();
            app.handleAction(before < column ? GameAction.RIGHT : GameAction.LEFT);
            assertNotEquals(before, engine.state().currentColumn());
        }
    }
}
