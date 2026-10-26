package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

class BoardPreviewAnimationTest {
    @Test void immediateRestartShowsTheNewBoardWithoutWaitingForTheOldFlash() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = lineEngine();
            BoardPreview preview = new BoardPreview();
            engine.newGame(GameMode.ITEM);
            preview.render(engine.state());
            engine.apply(GameAction.HARD_DROP);
            preview.render(engine.state());
            assertFalse(Arrays.equals(pixels(paint(preview)), freshPixels(engine)));
            engine.newGame(GameMode.ITEM);
            preview.render(engine.state());
            assertArrayEquals(freshPixels(engine), pixels(paint(preview)));
            preview.removeNotify();
        });
    }

    @Test void removingTheBoardCancelsTheFlashAndAllowsTheNextClear() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = lineEngine();
            engine.newGame(GameMode.ITEM);
            BoardPreview preview = new BoardPreview();
            preview.render(engine.state());
            engine.apply(GameAction.HARD_DROP);
            preview.render(engine.state());
            preview.removeNotify();
            assertArrayEquals(freshPixels(engine), pixels(paint(preview)));
            engine.apply(GameAction.HARD_DROP);
            preview.render(engine.state());
            assertFalse(Arrays.equals(freshPixels(engine), pixels(paint(preview))));
            preview.removeNotify();
        });
    }

    @Test void flashFinishesAndShowsTheActualPostClearState() throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        Timer[] poll = new Timer[1];
        BoardPreview[] preview = new BoardPreview[1];
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = lineEngine();
            engine.newGame(GameMode.ITEM);
            preview[0] = new BoardPreview();
            preview[0].render(engine.state());
            engine.apply(GameAction.HARD_DROP);
            preview[0].render(engine.state());
            int[] expected = freshPixels(engine);
            assertFalse(Arrays.equals(expected, pixels(paint(preview[0]))));
            poll[0] = new Timer(20, event -> {
                if (Arrays.equals(expected, pixels(paint(preview[0])))) {
                    ((Timer) event.getSource()).stop();
                    finished.countDown();
                }
            });
            poll[0].start();
        });
        try { assertTrue(finished.await(3, TimeUnit.SECONDS), "The flash must reveal the cleared board"); }
        finally { SwingUtilities.invokeAndWait(() -> { poll[0].stop(); preview[0].removeNotify(); }); }
    }

    @ParameterizedTest @EnumSource(ColorVisionMode.class)
    void unclearedGoldenCellsKeepTheirColorAndIdentifierDuringFlash(ColorVisionMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger blocks = new AtomicInteger();
            Tetromino line = new Tetromino(PieceType.I).withItem(ItemType.LINE_CLEAR, 0);
            GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(
                    () -> blocks.getAndIncrement() == 0 ? new Tetromino(PieceType.O).withItem(ItemType.GOLDEN) : line, 1),
                    new CenteredPieceSpawnPolicy(), piece -> piece);
            engine.newGame(GameMode.ITEM);
            while (engine.state().currentColumn() > 0) engine.apply(GameAction.LEFT);
            engine.apply(GameAction.HARD_DROP);
            BoardPreview preview = new BoardPreview();
            preview.setAppearance(mode, true);
            preview.render(engine.state());
            BufferedImage before = paint(preview);
            engine.apply(GameAction.HARD_DROP);
            preview.render(engine.state());
            BufferedImage flashing = paint(preview);
            var bounds = preview.boardBounds();
            int cell = (bounds.height - 6) / 20;
            int x = bounds.x + 5, y = bounds.y + 5 + 18 * cell;
            assertArrayEquals(before.getRGB(x, y, cell - 4, cell - 4, null, 0, cell - 4),
                    flashing.getRGB(x, y, cell - 4, cell - 4, null, 0, cell - 4));
            preview.removeNotify();
        });
    }

    private static GameEngine lineEngine() {
        Tetromino line = new Tetromino(PieceType.I).withItem(ItemType.LINE_CLEAR, 0);
        return new GameEngine(() -> PieceQueue.generatedBy(() -> line, 1),
                new CenteredPieceSpawnPolicy(), piece -> piece);
    }

    private static int[] freshPixels(GameEngine engine) {
        BoardPreview fresh = new BoardPreview();
        fresh.render(engine.state());
        return pixels(paint(fresh));
    }

    private static BufferedImage paint(BoardPreview preview) {
        preview.setSize(280, 540);
        BufferedImage image = new BufferedImage(280, 540, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { preview.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
