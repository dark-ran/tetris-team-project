package tetris.integration;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.app.*;
import tetris.game.*;
import tetris.loop.GameLoop;
import tetris.scoreboard.*;
import tetris.settings.*;

/** Uses the real timer, key mapper, settings repository, UI and game engine. */
class MenuSettingsRestartTest {
    @TempDir Path directory;

    @Test void escapedSettingsSavePreservesGameAndSpaceOnlyRestartsAfterGameOver() throws Exception {
        AtomicReference<AppController> reference = new AtomicReference<>();
        AtomicReference<GameState> paused = new AtomicReference<>();
        SettingsRepository repository = new SettingsRepository(directory.resolve("settings"));
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {}, new GameEngine(new Random(21)),
                    new GameLoop(), null, null,
                    new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))),
                    new SettingsService(repository), null);
            reference.set(app);
        });
        AppController app = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> {
                app.start(); app.startGame();
                key(active(app), KeyEvent.VK_SPACE);
                assertTrue(app.snapshot().game().score() > 0);
                paused.set(app.snapshot().game());
                key(active(app), KeyEvent.VK_ESCAPE);
                assertEquals(AppState.GAME_MENU, app.state());
                click(app, "설정");
                assertEquals(AppState.SETTINGS, app.state());
                JComboBox<?> size = visible(app.view()).filter(JComboBox.class::isInstance)
                        .map(JComboBox.class::cast).findFirst().orElseThrow();
                size.setSelectedIndex(2);
                JCheckBox accessible = visible(app.view()).filter(JCheckBox.class::isInstance)
                        .map(JCheckBox.class::cast).findFirst().orElseThrow();
                accessible.setSelected(true);
                JButton left = button(app, KeyEvent.getKeyText(KeyEvent.VK_LEFT));
                left.doClick(0);
                KeyEvent input = new KeyEvent(left, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');
                for (KeyListener listener : left.getKeyListeners()) listener.keyPressed(input);
                click(app, "설정 저장");
                assertEquals(2, app.snapshot().settings().windowSizePreset());
                assertTrue(app.snapshot().settings().colorBlindMode());
                assertEquals(KeyEvent.VK_A, app.snapshot().settings().keyBindings().get("LEFT"));
                assertEquals(app.snapshot().settings(), repository.load());
                assertEquals(GamePhase.PAUSED, app.snapshot().game().phase());
                click(app, "돌아가기");
                assertEquals(AppState.GAME_MENU, app.state());
            });
            Thread.sleep(1200);
            SwingUtilities.invokeAndWait(() -> {
                GameState after = app.snapshot().game(), before = paused.get();
                assertArrayEquals(before.board().snapshot(), after.board().snapshot());
                assertEquals(before.currentRow(), after.currentRow());
                assertEquals(before.currentColumn(), after.currentColumn());
                assertEquals(before.nextPieces(), after.nextPieces());
                assertEquals(before.score(), after.score());
                assertEquals(before.level(), after.level());
                assertEquals(before.spawnedPieces(), after.spawnedPieces());
                click(app, "게임으로 돌아가기");
                assertEquals(GamePhase.RUNNING, app.snapshot().game().phase());
                key(active(app), KeyEvent.VK_A);
                assertEquals(before.currentColumn() - 1, app.snapshot().game().currentColumn());
                for (int count = 0; count < 100 && app.state() == AppState.GAME; count++)
                    key(active(app), KeyEvent.VK_SPACE);
                assertEquals(AppState.GAME_OVER, app.state());
                assertTrue(app.snapshot().awaitingScoreName());
                assertTrue(visible(app.view()).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                        .anyMatch(label -> label.getText().contains("Space") && label.getText().contains("다시 시작")));
                key(active(app), KeyEvent.VK_SPACE);
                assertEquals(AppState.GAME, app.state());
                assertEquals(0, app.snapshot().game().score());
                assertEquals(1, app.snapshot().game().level());
                assertEquals(0, app.snapshot().game().totalClearedRows());
                assertEquals(1, app.snapshot().game().spawnedPieces());
                assertEquals(1000, app.snapshot().gravityIntervalMillis());
                assertFalse(app.snapshot().awaitingScoreName());
            });
        } finally { SwingUtilities.invokeAndWait(app::exit); }
    }

    private static JComponent active(AppController app) {
        return Arrays.stream(app.view().getComponents()).filter(Component::isVisible)
                .map(JComponent.class::cast).findFirst().orElseThrow();
    }
    private static JButton button(AppController app, String text) {
        return visible(app.view()).filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> button.getText().equals(text)).findFirst().orElseThrow();
    }
    private static void click(AppController app, String text) { button(app, text).doClick(0); }
    private static void key(JComponent target, int code) {
        Object name = target.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke(code, 0));
        assertNotNull(name);
        target.getActionMap().get(name).actionPerformed(new ActionEvent(target, 0, ""));
    }
    private static Stream<Component> visible(Component component) {
        if (!component.isVisible()) return Stream.empty();
        return component instanceof Container container
                ? Stream.concat(Stream.of(component), Arrays.stream(container.getComponents()).flatMap(MenuSettingsRestartTest::visible))
                : Stream.of(component);
    }
}
