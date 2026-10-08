package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import tetris.settings.*;

class SettingsAppearanceTest {
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
