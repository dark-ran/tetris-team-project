package tetris.ui;

import java.awt.Color;
import java.awt.Graphics;
import tetris.piece.PieceType;

/** 색각 표현 이슈의 일반 모드 시안. 블록 색과 칸 경계선만 표시한다. */
final class NormalPieceStyle {
    private static final Color CELL_BORDER = new Color(0x181818);

    private NormalPieceStyle() {
    }

    /** 색 번호를 가진 블록 종류에 대응하는 일반 모드 색상을 반환한다. */
    static Color colorFor(PieceType type) {
        return switch (type) {
            case I -> new Color(0x00CFE8);
            case O -> new Color(0xFFD32A);
            case T -> new Color(0xA34DF4);
            case S -> new Color(0x30C94E);
            case Z -> new Color(0xF14450);
            case J -> new Color(0x2878F0);
            case L -> new Color(0xFF891F);
        };
    }

    /** 한 칸을 단색으로 채우고, 이웃한 칸과 구분되는 어두운 경계선을 그린다. */
    static void paintCell(Graphics graphics, PieceType type, int x, int y, int cellSize) {
        graphics.setColor(colorFor(type));
        graphics.fillRect(x, y, cellSize, cellSize);
        graphics.setColor(CELL_BORDER);
        graphics.drawRect(x, y, cellSize - 1, cellSize - 1);
    }
    static void paintCell(Graphics graphics, PieceType type, int x, int y, int size, boolean accessible) {
        paintCell(graphics, type, x, y, size);
        if (!accessible || size < 10) return;
        graphics.setFont(AppTheme.font(java.awt.Font.BOLD, Math.max(9, size * 0.65f)));
        java.awt.FontMetrics metrics = graphics.getFontMetrics();
        String mark = type.name();
        graphics.setColor(java.awt.Color.BLACK);
        graphics.drawString(mark, x + (size - metrics.stringWidth(mark)) / 2,
                y + (size - metrics.getHeight()) / 2 + metrics.getAscent());
    }

}
