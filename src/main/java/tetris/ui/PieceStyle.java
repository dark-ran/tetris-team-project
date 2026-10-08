package tetris.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.Map;
import tetris.piece.PieceType;
import tetris.settings.ColorVisionMode;

/** Shared palettes and sparse cell patterns for the board and every preview. */
final class PieceStyle {
    private static final Color CELL_BORDER = new Color(0x181818);
    // Order is I, O, T, S, Z, J, L. These are display palettes, not simulation matrices.
    private static final Map<ColorVisionMode, int[]> PALETTES = Map.of(
            ColorVisionMode.NORMAL, new int[] {0x00CFE8, 0xFFD32A, 0xA34DF4, 0x30C94E, 0xF14450, 0x2878F0, 0xFF891F},
            ColorVisionMode.PROTAN, new int[] {0x56B4E9, 0xF0E442, 0xCC79A7, 0x009E73, 0xE69F00, 0x0072B2, 0xF5A9B8},
            ColorVisionMode.DEUTAN, new int[] {0x56B4E9, 0xF0E442, 0xA57BE8, 0x17A398, 0xD55E00, 0x3960A8, 0xE69F00},
            ColorVisionMode.TRITAN, new int[] {0x00A4A8, 0xFFD1A1, 0xAD4B7A, 0x54C8BE, 0xD94848, 0x355F8D, 0xE28D52},
            ColorVisionMode.MONOCHROME, new int[] {0x545454, 0xF2F2F2, 0xA0A0A0, 0x737373, 0xD4D4D4, 0x383838, 0xB8B8B8});

    private PieceStyle() { }

    static Color colorFor(PieceType type, ColorVisionMode mode) {
        return new Color(PALETTES.get(mode)[type.ordinal()]);
    }

    static void paintCell(Graphics graphics, PieceType type, int x, int y, int size,
            ColorVisionMode mode, boolean patterns) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape originalClip = g.getClip();
            Color fill = colorFor(type, mode);
            g.setColor(fill); g.fillRect(x, y, size, size);
            if (patterns && size >= 10) {
                g.clipRect(x + 2, y + 2, size - 4, size - 4);
                g.setColor(patternColor(fill));
                g.setStroke(new BasicStroke(size >= 20 ? 1.5f : 1f));
                int third = Math.max(3, size / 3), last = size - third;
                switch (type) {
                    case I -> { horizontal(g, x, y, size, third); horizontal(g, x, y, size, last); }
                    case O -> {
                        int dot = Math.max(2, size / 7);
                        for (int dx : new int[] {third, last}) for (int dy : new int[] {third, last})
                            g.fillOval(x + dx - dot / 2, y + dy - dot / 2, dot, dot);
                    }
                    case T -> { vertical(g, x, y, size, third); vertical(g, x, y, size, last); }
                    case S, Z -> {
                        int step = Math.max(6, size / 2);
                        for (int offset = -size; offset <= size; offset += step) {
                            if (type == PieceType.S) g.drawLine(x, y + size + offset, x + size, y + offset);
                            else g.drawLine(x, y + offset, x + size, y + size + offset);
                        }
                    }
                    case J -> {
                        horizontal(g, x, y, size, third); horizontal(g, x, y, size, last);
                        vertical(g, x, y, size, third); vertical(g, x, y, size, last);
                    }
                    case L -> {
                        int mid = size / 2, edge = Math.max(3, size / 6);
                        g.drawPolygon(new Polygon(new int[] {x + mid, x + size - edge, x + mid, x + edge},
                                new int[] {y + edge, y + mid, y + size - edge, y + mid}, 4));
                    }
                }
            }
            g.setClip(originalClip); g.setStroke(new BasicStroke(1f));
            g.setColor(CELL_BORDER); g.drawRect(x, y, size - 1, size - 1);
        } finally { g.dispose(); }
    }

    static void paintGhostCell(Graphics2D graphics, int x, int y, int size) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(new Color(0xCFD7E6));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
            g.drawRect(x + 1, y + 1, size - 3, size - 3);
        } finally { g.dispose(); }
    }

    static Color patternColor(Color fill) {
        double luminance = 0.2126 * linear(fill.getRed()) + 0.7152 * linear(fill.getGreen()) + 0.0722 * linear(fill.getBlue());
        return luminance > 0.179 ? Color.BLACK : Color.WHITE;
    }
    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
    private static void horizontal(Graphics2D g, int x, int y, int size, int offset) {
        g.drawLine(x + 2, y + offset, x + size - 3, y + offset);
    }
    private static void vertical(Graphics2D g, int x, int y, int size, int offset) {
        g.drawLine(x + offset, y + 2, x + offset, y + size - 3);
    }
}
