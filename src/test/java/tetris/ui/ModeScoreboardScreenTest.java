package tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import tetris.game.GameMode;
import tetris.scoreboard.ScoreEntry;

class ModeScoreboardScreenTest {
    @Test void selectorAndHighlightedRecordShowOnlyTheSelectedMode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ScoreboardScreen screen = new ScoreboardScreen(() -> {});
            ScoreEntry normal = new ScoreEntry("normal", 100);
            ScoreEntry item = new ScoreEntry("item", 10, GameMode.ITEM);
            screen.showScreen(List.of(normal, item), item);
            JTable table = find(screen, JTable.class);
            assertEquals(1, table.getRowCount());
            assertEquals("item", table.getValueAt(0, 1));
            assertEquals(0, table.getSelectedRow());
            find(screen, JComboBox.class).setSelectedItem(GameMode.NORMAL);
            assertEquals("normal", table.getValueAt(0, 1));
        });
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = find(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
