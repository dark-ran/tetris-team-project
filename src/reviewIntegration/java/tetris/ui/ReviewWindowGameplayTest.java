package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.settings.ColorVisionMode;
import tetris.settings.GameSettings;

/** 실제 창 크기에서 보드·미리보기·조작 안내의 배치와 색각 설정을 확인한다. */
class ReviewWindowGameplayTest {
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void previewAndBoardFitActualWindowsAcrossEveryColorMode(int preset) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame window = new JFrame("Tetris · gameplay layout verification");
            GameScreen screen = new GameScreen(() -> {}, () -> {}, window::dispose);
            try {
                window.setContentPane(screen); window.setSize(GameSettings.presetDimension(preset));
                window.setLocationRelativeTo(null); window.setVisible(true);
                GameEngine engine = new GameEngine(new Random(21)); engine.newGame();
                engine.apply(GameAction.HARD_DROP); screen.render(engine.state());
                for (ColorVisionMode mode : ColorVisionMode.values()) {
                    screen.applySettings(new GameSettings(preset, mode, mode.defaultPatternsEnabled(),
                            GameSettings.DEFAULT_KEY_BINDINGS));
                    window.validate();
                    NextPiecePreview next = find(screen, NextPiecePreview.class);
                    BoardPreview board = find(screen, BoardPreview.class);
                    Rectangle visible = new Rectangle(0, 0, screen.getWidth(), screen.getHeight());
                    Rectangle nextBounds = SwingUtilities.convertRectangle(next.getParent(), next.getBounds(), screen);
                    Rectangle boardBounds = SwingUtilities.convertRectangle(board, board.boardBounds(), screen);
                    assertTrue(visible.contains(nextBounds)); assertTrue(visible.contains(boardBounds));
                    assertTrue(nextBounds.x >= boardBounds.x + boardBounds.width);
                    var card = next.getParent().getParent();
                    Rectangle cardBounds = SwingUtilities.convertRectangle(card.getParent(), card.getBounds(), screen);
                    assertTrue(cardBounds.x - boardBounds.x - boardBounds.width <= 80);
                    assertEquals("다음 블록: " + engine.state().nextPieces().getFirst(),
                            next.getAccessibleContext().getAccessibleName());
                    assertTrue(next.getWidth() >= 128); assertTrue(next.getHeight() >= 64);
                    capture(screen, preset, mode);
                }
            } finally { window.dispose(); }
        });
    }

    private static <T> T find(java.awt.Component component, Class<T> type) {
        if (type.isInstance(component)) return type.cast(component);
        if (component instanceof java.awt.Container container)
            for (var child : container.getComponents()) {
                T result = find(child, type); if (result != null) return result;
            }
        return null;
    }

    private static void capture(GameScreen screen, int preset, ColorVisionMode mode) {
        BufferedImage image = new BufferedImage(screen.getWidth(), screen.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try { screen.paint(g); } finally { g.dispose(); }
        try {
            Path folder = Path.of("build", "review-gameplay"); Files.createDirectories(folder);
            ImageIO.write(image, "png", folder.resolve("preset-" + preset + "-" + mode.name() + ".png").toFile());
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
}
