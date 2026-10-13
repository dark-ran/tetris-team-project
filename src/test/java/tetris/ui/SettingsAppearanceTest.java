package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import tetris.settings.*;

class SettingsAppearanceTest {
    @Test void arrowLabelsRemainConsistentAfterCapturingAndReloadingCustomKeys() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsScreen settings = new SettingsScreen(() -> {});
            for (String arrow : new String[] {"←", "→", "↑", "↓"})
                assertTrue(GameMenuUiTest.tree(settings).filter(JButton.class::isInstance).map(JButton.class::cast)
                        .anyMatch(button -> button.getText().equals(arrow)));
            JButton left = GameMenuUiTest.tree(settings).filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> button.getText().equals("←")).findFirst().orElseThrow();
            left.doClick(0);
            KeyEvent input = new KeyEvent(left, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');
            for (var listener : left.getKeyListeners()) listener.keyPressed(input);
            assertEquals("A", left.getText());
            assertEquals(KeyEvent.VK_A, settings.values().keys().get("LEFT"));
            var keys = new LinkedHashMap<>(GameSettings.DEFAULT_KEY_BINDINGS);
            keys.put("LEFT", KeyEvent.VK_A);
            GameSettings custom = new GameSettings(1, ColorVisionMode.PROTAN, false, keys);
            settings.showSettings(custom);
            assertEquals("A", left.getText());
            assertEquals(ColorVisionMode.PROTAN, settings.values().colorVisionMode());
            assertFalse(settings.values().patternsEnabled());
            GameScreen game = new GameScreen(() -> {}, () -> {}, () -> {});
            game.applySettings(custom);
            for (String label : new String[] {"[A]  왼쪽", "[→]  오른쪽", "[↑]  시계방향 회전", "[↓]  아래로 한 칸"})
                assertTrue(GameMenuUiTest.tree(game).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                        .anyMatch(component -> component.getText().equals(label)), label);
            settings.showSettings(GameSettings.defaultSettings());
            assertEquals("←", left.getText());
        });
    }

    @Test void modesHaveTheirOwnDefaultsAndPatternsRemainAnIndependentSavedPreference() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsScreen screen = new SettingsScreen(() -> {});
            JComboBox<?> modes = GameMenuUiTest.tree(screen).filter(JComboBox.class::isInstance)
                    .map(JComboBox.class::cast).filter(combo -> combo.getItemAt(0) instanceof ColorVisionMode).findFirst().orElseThrow();
            JCheckBox patterns = GameMenuUiTest.tree(screen).filter(JCheckBox.class::isInstance)
                    .map(JCheckBox.class::cast).findFirst().orElseThrow();
            assertEquals(5, modes.getItemCount());
            for (ColorVisionMode mode : ColorVisionMode.values()) {
                modes.setSelectedItem(mode);
                assertEquals(mode.defaultPatternsEnabled(), screen.values().patternsEnabled());
                assertEquals(8, GameMenuUiTest.tree(screen).filter(TetrominoMark.class::isInstance).count());
                patterns.doClick(0);
                assertEquals(!mode.defaultPatternsEnabled(), screen.values().patternsEnabled());
            }
            screen.showSettings(new GameSettings(1, ColorVisionMode.PROTAN, false, GameSettings.DEFAULT_KEY_BINDINGS));
            assertEquals(ColorVisionMode.PROTAN, screen.values().colorVisionMode());
            assertFalse(screen.values().patternsEnabled(), "Loading must preserve the user's override");
        });
    }
}
