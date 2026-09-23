package tetris.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** 텍스트 기반 아케이드 화면에 사용하는 색상과 글꼴 규칙. */
public final class AppTheme {
    static final Color BACKGROUND = new Color(18, 18, 18);
    static final Color SURFACE = new Color(27, 27, 27);
    static final Color SURFACE_RAISED = new Color(38, 38, 36);
    static final Color BORDER = new Color(84, 83, 77);
    static final Color TEXT = new Color(242, 239, 226);
    static final Color MUTED = new Color(166, 163, 151);
    static final Color GRID = new Color(42, 42, 40);
    static final Color BOARD = new Color(8, 8, 8);

    static final Color YELLOW = new Color(255, 214, 10);
    static final Color CYAN = new Color(0, 215, 230);
    static final Color BLUE = new Color(47, 98, 255);
    static final Color ORANGE = new Color(255, 132, 0);
    static final Color GREEN = new Color(42, 205, 65);
    static final Color PURPLE = new Color(191, 60, 255);
    static final Color RED = new Color(255, 59, 48);

    private static boolean installed;

    private AppTheme() {
    }

    /** 컴포넌트 생성 전에 한 번 호출해 기본 Swing 색상과 글꼴을 맞춘다. */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;

        Font body = font(Font.PLAIN, 14f);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", body);
        UIManager.put("Button.font", font(Font.BOLD, 14f));
        UIManager.put("Table.font", body);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", YELLOW);
        UIManager.put("Table.selectionForeground", BACKGROUND);
        UIManager.put("Table.gridColor", GRID);
        UIManager.put("TableHeader.font", font(Font.BOLD, 13f));
        UIManager.put("TableHeader.background", BACKGROUND);
        UIManager.put("TableHeader.foreground", YELLOW);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
    }

    static Font font(int style, float size) {
        return new Font(Font.MONOSPACED, style, Math.round(size)).deriveFont(size);
    }

    static Border frameBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }
}
