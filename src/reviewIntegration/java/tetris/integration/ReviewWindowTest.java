package tetris.integration;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.app.*;
import tetris.game.*;
import tetris.loop.GameLoop;
import tetris.scoreboard.*;
import tetris.settings.*;

/** Sends Swing key events through the actual focus owner; does not inject OS input. */
class ReviewWindowTest {
    @TempDir Path directory;

    @Test void windowFocusEscapeSettingsReturnAndSpaceRestartUseTheCorrectContext() throws Exception {
        AtomicReference<JFrame> windowRef = new AtomicReference<>();
        AtomicReference<AppController> appRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JFrame window = new JFrame("Tetris · review verification");
            AppController app = new AppController(window::dispose, new GameEngine(new Random(21)),
                    new GameLoop(), null, null,
                    new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))),
                    new SettingsService(new SettingsRepository(directory.resolve("settings"))), null);
            windowRef.set(window); appRef.set(app);
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) { app.exit(); }
            });
            window.setContentPane(app.view()); window.setSize(1040, 860);
            window.setLocationRelativeTo(null); window.setVisible(true); app.start(); app.startGame();
        });
        JFrame window = windowRef.get(); AppController app = appRef.get();
        try {
            focus(window, () -> visible(app.view()).filter(component ->
                    component.getClass().getSimpleName().equals("BoardPreview")).findFirst().orElseThrow());
            press(KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(AppState.GAME_MENU, app.state());
                assertEquals(GamePhase.PAUSED, app.snapshot().game().phase());
                try {
                    BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    try { window.paint(graphics); } finally { graphics.dispose(); }
                    ImageIO.write(image, "png", Path.of("build", "review-pause-menu.png").toFile());
                } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
                button(app, "설정").doClick(0);
                assertEquals(AppState.SETTINGS, app.state());
            });
            focus(window, () -> button(app, "돌아가기"));
            press(KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(AppState.GAME_MENU, app.state());
                button(app, "게임으로 돌아가기").doClick(0);
                for (int i = 0; i < 100 && app.state() == AppState.GAME; i++) app.handleAction(GameAction.HARD_DROP);
                assertEquals(AppState.GAME_OVER, app.state());
            });
            focus(window, () -> nameField(app));
            SwingUtilities.invokeAndWait(() -> {
                window.validate();
                JTextField name = nameField(app);
                assertTrue(name.getHeight() <= name.getFontMetrics(name.getFont()).getHeight() * 2);
                assertTrue(name.getFont().getSize() >= 18);
                name.setText("플레이어 1");
                try {
                    BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    try { window.paint(graphics); } finally { graphics.dispose(); }
                    ImageIO.write(image, "png", Path.of("build", "review-game-over-registration.png").toFile());
                } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
                name.setText("");
            });
            press(KeyEvent.VK_A, 'a'); press(KeyEvent.VK_SPACE, ' '); press(KeyEvent.VK_B, 'b');
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(AppState.GAME_OVER, app.state(), "A space in the name editor must not restart");
                assertEquals("a b", nameField(app).getText());
            });
            focus(window, () -> button(app, "시작 메뉴"));
            press(KeyEvent.VK_SPACE, ' ');
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(AppState.GAME, app.state());
                assertEquals(0, app.snapshot().game().score());
                assertEquals(GamePhase.RUNNING, app.snapshot().game().phase());
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                assertEquals(AppState.EXIT, app.state());
                assertFalse(window.isDisplayable());
            });
        } finally { SwingUtilities.invokeAndWait(() -> { app.exit(); window.dispose(); }); }
    }

    private static void focus(JFrame window, java.util.function.Supplier<Component> target) throws Exception {
        AtomicReference<Component> expected = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            window.toFront(); window.requestFocus();
            expected.set(target.get()); expected.get().requestFocusInWindow();
        });
        long deadline = System.nanoTime() + 2_000_000_000L;
        AtomicReference<Component> owner = new AtomicReference<>();
        do {
            SwingUtilities.invokeAndWait(() -> owner.set(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()));
            if (owner.get() == expected.get()) return;
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);
        assertSame(expected.get(), owner.get(), "The requested visible component must own focus");
    }

    private static void press(int code, char character) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            Component owner = manager.getFocusOwner();
            assertNotNull(owner);
            long time = System.currentTimeMillis();
            manager.redispatchEvent(owner, new KeyEvent(owner, KeyEvent.KEY_PRESSED, time, 0, code, character));
            if (character != KeyEvent.CHAR_UNDEFINED) {
                manager.redispatchEvent(owner, new KeyEvent(owner, KeyEvent.KEY_TYPED, time, 0, KeyEvent.VK_UNDEFINED, character));
            }
            manager.redispatchEvent(owner, new KeyEvent(owner, KeyEvent.KEY_RELEASED, time, 0, code, character));
        });
    }
    private static JTextField nameField(AppController app) {
        return visible(app.view()).filter(JTextField.class::isInstance).map(JTextField.class::cast).findFirst().orElseThrow();
    }
    private static JButton button(AppController app, String text) {
        return visible(app.view()).filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> button.getText().equals(text)).findFirst().orElseThrow();
    }
    private static Stream<Component> visible(Component component) {
        if (!component.isVisible()) return Stream.empty();
        return component instanceof Container container
                ? Stream.concat(Stream.of(component), Arrays.stream(container.getComponents()).flatMap(ReviewWindowTest::visible))
                : Stream.of(component);
    }
}
