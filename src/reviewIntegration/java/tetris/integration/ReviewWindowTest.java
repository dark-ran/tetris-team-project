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

/** Checks real keyboard focus, including Space in the name editor and on menu buttons. */
class ReviewWindowTest {
    @TempDir Path directory;
    @Test void realWindowEscapeSettingsReturnAndSpaceRestartUseTheCorrectContext() throws Exception {
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
        Robot robot = new Robot(); robot.setAutoDelay(70); robot.waitForIdle();
        try {
            press(robot, KeyEvent.VK_ESCAPE);
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
            robot.waitForIdle(); press(robot, KeyEvent.VK_ESCAPE);
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(AppState.GAME_MENU, app.state());
                button(app, "게임으로 돌아가기").doClick(0);
                for (int i = 0; i < 100 && app.state() == AppState.GAME; i++) app.handleAction(GameAction.HARD_DROP);
                assertEquals(AppState.GAME_OVER, app.state());
                JTextField name = visible(app.view()).filter(JTextField.class::isInstance)
                        .map(JTextField.class::cast).findFirst().orElseThrow();
                name.requestFocusInWindow();
            });
            robot.waitForIdle();
            press(robot, KeyEvent.VK_A); press(robot, KeyEvent.VK_SPACE); press(robot, KeyEvent.VK_B);
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(AppState.GAME_OVER, app.state(), "Typing a space in a name must not restart");
                JTextField name = visible(app.view()).filter(JTextField.class::isInstance)
                        .map(JTextField.class::cast).findFirst().orElseThrow();
                assertEquals("a b", name.getText());
                button(app, "시작 메뉴").requestFocusInWindow();
            });
            robot.waitForIdle(); press(robot, KeyEvent.VK_SPACE);
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
    private static void press(Robot robot, int code) {
        robot.keyPress(code); robot.keyRelease(code); robot.waitForIdle();
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
