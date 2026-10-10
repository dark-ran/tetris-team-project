package tetris.integration;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.app.AppController;
import tetris.app.AppState;
import tetris.game.GameEngine;
import tetris.game.GamePhase;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;
import tetris.settings.ColorVisionMode;
import tetris.settings.GameSettings;
import tetris.settings.SettingsRepository;
import tetris.settings.SettingsService;
import tetris.ui.SettingsScreen;
import tetris.ui.WindowSizeConfirmationDialog;

/** Tests the real modal dialog through Swing events, without injecting OS input. */
class ReviewWindowSettingsTest {
    enum Decision { KEEP, CANCEL, CLOSE, ESCAPE, TIMEOUT }
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(Decision.class)
    void actualWindowResizesBeforeTheDialogAndRestoresOnEveryCancelPath(Decision decision) throws Exception {
        SettingsRepository repository = new SettingsRepository(directory.resolve("settings"));
        GameSettings original = GameSettings.defaultSettings(); repository.save(original);
        byte[] originalFile = Files.readAllBytes(repository.filePath());
        SettingsService service = new SettingsService(repository);
        SwingUtilities.invokeAndWait(() -> {
            JFrame window = new JFrame("Tetris · display settings verification");
            AppController app = new AppController(window::dispose, new GameEngine(), new GameLoop(), null, null,
                    new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), service, null,
                    new WindowSizeConfirmationDialog(decision == Decision.TIMEOUT ? 1 : 15));
            AtomicReference<Dimension> sizeAtPrompt = new AtomicReference<>();
            AtomicReference<GameSettings> storedAtPrompt = new AtomicReference<>();
            AtomicReference<String> message = new AtomicReference<>();
            AtomicReference<Throwable> interactionFailure = new AtomicReference<>();
            // Use a preset that fits the display; the window manager can clamp oversized frames.
            int targetPreset = GameSettings.PRESET_SMALL;
            Timer interaction = new Timer(40, null);
            interaction.addActionListener(event -> {
                JDialog dialog = Arrays.stream(window.getOwnedWindows()).filter(JDialog.class::isInstance)
                        .map(JDialog.class::cast).filter(candidate -> candidate.isShowing()
                                && candidate.getTitle().equals("창 크기 확인")).findFirst().orElse(null);
                if (dialog == null) return;
                try {
                    Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    if (sizeAtPrompt.get() == null) {
                        sizeAtPrompt.set(window.getSize()); storedAtPrompt.set(repository.load());
                        assertDialogContentFits(dialog);
                        message.set(descendants(dialog).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                                .map(JLabel::getText).collect(java.util.stream.Collectors.joining("\n")));
                    }
                    if (decision == Decision.ESCAPE && (focus == null || !SwingUtilities.isDescendingFrom(focus, dialog))) return;
                    interaction.stop();
                    if (decision == Decision.KEEP) {
                        capture(window, "review-settings-small.png"); capture(dialog, "review-window-confirm.png");
                    }
                    switch (decision) {
                        case KEEP, CANCEL -> descendants(dialog).filter(JButton.class::isInstance).map(JButton.class::cast)
                                .filter(button -> button.getText().equals(decision == Decision.KEEP ? "유지" : "취소"))
                                .findFirst().orElseThrow().doClick(0);
                        case CLOSE -> dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                        case ESCAPE -> {
                            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                            manager.redispatchEvent(focus, new KeyEvent(focus, KeyEvent.KEY_PRESSED,
                                    System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
                        }
                        case TIMEOUT -> { /* Let the dialog's own countdown restore the previous size. */ }
                    }
                } catch (Throwable failure) {
                    interaction.stop(); interactionFailure.set(failure); dialog.dispose();
                }
            });
            try {
                window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                window.setContentPane(app.view()); window.setSize(original.windowDimension());
                window.setLocationRelativeTo(null); window.setVisible(true); app.start(); app.startGame(); app.showSettings();
                SettingsScreen editor = descendants(app.view()).filter(SettingsScreen.class::isInstance)
                        .map(SettingsScreen.class::cast).findFirst().orElseThrow();
                editor.showValues(new SettingsScreen.Values(original.windowSizePreset(), ColorVisionMode.MONOCHROME,
                        true, original.keyBindings()));
                JComboBox<?> size = descendants(editor).filter(JComboBox.class::isInstance)
                        .map(JComboBox.class::cast).findFirst().orElseThrow();
                interaction.start(); size.setSelectedIndex(targetPreset);
                assertNull(interactionFailure.get(), () -> String.valueOf(interactionFailure.get()));
                assertEquals(GameSettings.presetDimension(targetPreset), sizeAtPrompt.get());
                assertEquals(original, storedAtPrompt.get());
                assertNotNull(message.get());
                assertTrue(message.get().contains("변경한 창 크기를 유지하시겠습니까?"));
                assertTrue(message.get().contains(decision == Decision.TIMEOUT ? "1초" : "15초"));
                GameSettings expected = decision == Decision.KEEP ? original.withWindowSizePreset(targetPreset) : original;
                assertEquals(expected.windowDimension(), window.getSize());
                assertEquals(expected, service.current()); assertEquals(expected, repository.load());
                assertEquals(expected.windowSizePreset(), size.getSelectedIndex());
                assertEquals(ColorVisionMode.MONOCHROME, editor.values().colorVisionMode());
                assertTrue(editor.values().patternsEnabled());
                assertEquals(AppState.SETTINGS, app.state()); assertEquals(GamePhase.PAUSED, app.snapshot().game().phase());
                if (decision != Decision.KEEP) {
                    try { assertArrayEquals(originalFile, Files.readAllBytes(repository.filePath())); }
                    catch (IOException ex) { throw new UncheckedIOException(ex); }
                } else {
                    JComboBox<?> mode = descendants(editor).filter(JComboBox.class::isInstance)
                            .map(JComboBox.class::cast).skip(1).findFirst().orElseThrow();
                    for (ColorVisionMode color : ColorVisionMode.values()) {
                        mode.setSelectedItem(color); window.validate();
                        capture(window, "review-settings-" + color.name().toLowerCase(java.util.Locale.ROOT) + ".png");
                    }
                }
            } finally {
                interaction.stop();
                for (Window owned : window.getOwnedWindows()) owned.dispose();
                app.exit(); window.dispose();
            }
        });
    }

    private static void capture(Window window, String filename) {
        BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try { window.paint(graphics); } finally { graphics.dispose(); }
        try { ImageIO.write(image, "png", Path.of("build", filename).toFile()); }
        catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
    private static void assertDialogContentFits(JDialog dialog) {
        Container content = dialog.getContentPane();
        var insets = dialog.getInsets();
        assertEquals(dialog.getWidth() - insets.left - insets.right, content.getWidth());
        assertEquals(dialog.getHeight() - insets.top - insets.bottom, content.getHeight());
        assertTrue(content.isOpaque(), "The dialog must fill its content background");
        descendants(content).filter(JLabel.class::isInstance).map(JLabel.class::cast).forEach(label -> {
            assertTrue(label.getFontMetrics(label.getFont()).stringWidth(label.getText()) <= label.getWidth(),
                    "Dialog text must not be clipped: " + label.getText());
            assertTrue(label.getFontMetrics(label.getFont()).getHeight() <= label.getHeight());
        });
        descendants(content).filter(JButton.class::isInstance).map(JButton.class::cast).forEach(button -> {
            java.awt.Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), content);
            assertTrue(new java.awt.Rectangle(0, 0, content.getWidth(), content.getHeight()).contains(bounds),
                    "Buttons must remain inside the content area");
            assertTrue(button.getFontMetrics(button.getFont()).stringWidth(button.getText())
                    + button.getInsets().left + button.getInsets().right <= button.getWidth());
        });
    }
    private static Stream<Component> descendants(Component component) {
        return component instanceof Container container
                ? Stream.concat(Stream.of(component), Arrays.stream(container.getComponents()).flatMap(ReviewWindowSettingsTest::descendants))
                : Stream.of(component);
    }
}
