package tetris.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

/** 시작 화면에서 글자를 블록 단위로 직접 그리는 장식용 워드마크. */
final class TetrisWordmark extends JComponent {
    private static final int CELL = 17;
    private static final int PITCH = 19;
    private static final int LETTER_GAP = 14;
    private static final String[][] LETTERS = {
        {"11111", "00100", "00100", "00100", "00100"},
        {"1111", "1000", "1110", "1000", "1111"},
        {"11111", "00100", "00100", "00100", "00100"},
        {"1110", "1001", "1110", "1010", "1001"},
        {"111", "010", "010", "010", "111"},
        {"0111", "1000", "0110", "0001", "1110"}
    };
    private static final Color[] COLORS = {
        AppTheme.CYAN, AppTheme.BLUE, AppTheme.ORANGE,
        AppTheme.PURPLE, AppTheme.YELLOW, AppTheme.GREEN
    };

    TetrisWordmark() {
        setPreferredSize(new Dimension(580, 112));
        setMinimumSize(getPreferredSize());
        setFocusable(false);
        setName("TETRIS");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int wordWidth = wordWidth();
        int x = Math.max(0, (getWidth() - wordWidth) / 2);
        int y = Math.max(0, (getHeight() - 5 * PITCH) / 2);

        for (int letter = 0; letter < LETTERS.length; letter++) {
            String[] rows = LETTERS[letter];
            for (int row = 0; row < rows.length; row++) {
                for (int column = 0; column < rows[row].length(); column++) {
                    if (rows[row].charAt(column) == '1') {
                        drawBlock(graphics, x + column * PITCH, y + row * PITCH, COLORS[letter]);
                    }
                }
            }
            x += rows[0].length() * PITCH + LETTER_GAP;
        }
    }

    private static void drawBlock(Graphics graphics, int x, int y, Color color) {
        graphics.setColor(color);
        graphics.fillRect(x, y, CELL, CELL);
        graphics.setColor(color.brighter());
        graphics.drawLine(x + 1, y + 1, x + CELL - 2, y + 1);
        graphics.drawLine(x + 1, y + 1, x + 1, y + CELL - 2);
        graphics.setColor(color.darker());
        graphics.drawLine(x + 1, y + CELL - 2, x + CELL - 2, y + CELL - 2);
        graphics.drawLine(x + CELL - 2, y + 1, x + CELL - 2, y + CELL - 2);
    }

    private static int wordWidth() {
        int width = 0;
        for (String[] letter : LETTERS) {
            width += letter[0].length() * PITCH;
        }
        return width + (LETTERS.length - 1) * LETTER_GAP;
    }
}
