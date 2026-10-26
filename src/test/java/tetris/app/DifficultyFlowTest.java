package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.Difficulty;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.loop.GameLoop;
import tetris.piece.PieceType;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;
import tetris.scoreboard.ScoreEntry;
import tetris.settings.GameSettings;
import tetris.settings.SettingsRepository;
import tetris.settings.SettingsService;

class DifficultyFlowTest {
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(GameMode.class)
    void finalRecordAndSaveRetryKeepThePlayedDifficultyAfterSettingsChange(GameMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsService settings = new SettingsService(new SettingsRepository(directory.resolve("settings")));
            settings.update(settings.current().withDifficulty(Difficulty.HARD));
            ScoreRepository repository = new ScoreRepository(directory.resolve("scores")) {
                boolean fail = true;
                @Override public void save(List<ScoreEntry> entries) {
                    if (fail) { fail = false; throw new UncheckedIOException(new IOException("locked")); }
                    super.save(entries);
                }
            };
            GameEngine engine = new GameEngine(new Random(21));
            GameLoop loop = new GameLoop() { @Override public void start() { } };
            AppController app = new AppController(() -> {}, engine, loop, ignored -> {}, ignored -> {},
                    new ScoreBoardService(repository), settings, ignored -> {});
            try {
                app.startGame(mode);
                settings.update(settings.current().withDifficulty(Difficulty.EASY));
                assertEquals(Difficulty.HARD, engine.difficulty());
                for (int i = 0; i < 100 && !engine.isGameOver(); i++) app.handleAction(GameAction.HARD_DROP);
                assertTrue(engine.isGameOver());
                app.registerScore("player");
                ScoreEntry expected = new ScoreEntry("player", engine.state().score(), mode, Difficulty.HARD);
                assertEquals(expected, app.pendingScore());
                assertTrue(app.retryScoreSave());
                assertEquals(List.of(expected), repository.load());
                assertEquals(expected, app.snapshot().lastRegisteredScore());
            } finally { app.exit(); }
        });
    }

    private static Stream<Component> tree(Component component) {
        return Stream.concat(Stream.of(component), component instanceof Container container
                ? Arrays.stream(container.getComponents()).flatMap(DifficultyFlowTest::tree) : Stream.empty());
    }

    @Test void selectedDifficultyIsSavedAndOnlyChangesTheNextGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsRepository repository = new SettingsRepository(directory.resolve("settings"));
            SettingsService settings = new SettingsService(repository);
            GameEngine engine = new GameEngine(new Random(21));
            GameLoop loop = new GameLoop() { @Override public void start() { } };
            AppController app = new AppController(() -> {}, engine, loop, null, null,
                    new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), settings, null);
            try {
                app.start(); app.showSettings();
                JComboBox<?> selector = tree(app.view()).filter(JComboBox.class::isInstance)
                        .map(JComboBox.class::cast).filter(combo -> combo.getItemAt(0) instanceof Difficulty)
                        .findFirst().orElseThrow();
                assertEquals(Difficulty.NORMAL, selector.getSelectedItem());
                selector.setSelectedItem(Difficulty.EASY);
                JButton save = tree(app.view()).filter(JButton.class::isInstance).map(JButton.class::cast)
                        .filter(button -> button.getText().equals("설정 저장")).findFirst().orElseThrow();
                save.doClick(0);
                assertEquals(Difficulty.EASY, repository.load().difficulty());
                app.startGame();
                assertEquals(Difficulty.EASY, app.snapshot().game().difficulty());
                for (int i = 0; i < 9; i++) {
                    engine.replaceLastPreviewPiece(PieceType.SINGLE);
                    app.handleAction(GameAction.HARD_DROP);
                }
                assertEquals(10, app.snapshot().game().spawnedPieces());
                assertEquals(920, app.snapshot().gravityIntervalMillis());
                app.showSettings();
                selector.setSelectedItem(Difficulty.HARD); save.doClick(0);
                assertEquals(Difficulty.HARD, repository.load().difficulty());
                assertEquals(Difficulty.EASY, app.snapshot().game().difficulty());
                app.closeSettings(); app.resumeGame();
                assertEquals(920, app.snapshot().gravityIntervalMillis());
                app.startGame();
                assertEquals(Difficulty.HARD, app.snapshot().game().difficulty());
                assertEquals(1000, app.snapshot().gravityIntervalMillis());
                assertNotEquals(PieceType.SINGLE, app.snapshot().game().currentPiece().type());
                app.restoreDefaultSettings();
                assertEquals(GameSettings.defaultSettings(), repository.load());
            } finally { app.exit(); }
        });
    }
}
