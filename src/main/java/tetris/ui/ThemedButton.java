package tetris.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/** 키보드 포커스가 분명하게 보이는 각진 아케이드 메뉴 버튼. */
final class ThemedButton extends JButton {
    enum Style {
        PRIMARY, SECONDARY, DANGER
    }

    private final Style style;
    private boolean hovered;

    ThemedButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(AppTheme.font(Font.BOLD, 14f));
        setForeground(style == Style.PRIMARY ? AppTheme.BACKGROUND : foregroundColor());
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fillColor());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(borderColor());
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            if (isFocusOwner()) {
                g.setColor(AppTheme.YELLOW);
                g.fillRect(0, 0, 5, getHeight());
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    private Color fillColor() {
        if (!isEnabled()) {
            return AppTheme.GRID;
        }
        if (style == Style.PRIMARY) {
            return getModel().isPressed() || hovered ? AppTheme.YELLOW.brighter() : AppTheme.YELLOW;
        }
        if (style == Style.DANGER) {
            return hovered ? new Color(91, 39, 37) : AppTheme.SURFACE;
        }
        return hovered ? AppTheme.SURFACE_RAISED.brighter() : AppTheme.SURFACE_RAISED;
    }

    private Color foregroundColor() {
        return style == Style.DANGER ? AppTheme.RED : AppTheme.TEXT;
    }

    private Color borderColor() {
        if (isFocusOwner()) {
            return AppTheme.YELLOW;
        }
        if (style == Style.DANGER) {
            return AppTheme.RED.darker();
        }
        return AppTheme.BORDER;
    }
}
