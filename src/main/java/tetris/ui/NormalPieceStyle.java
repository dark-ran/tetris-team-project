package tetris.ui;

import java.awt.Color;
import java.awt.Graphics;
import tetris.piece.PieceType;
import tetris.settings.ColorVisionMode;

/** 색각 표현 이슈의 일반 모드 시안. 블록 색과 칸 경계선만 표시한다. */
final class NormalPieceStyle {

    private NormalPieceStyle() {
    }

    /** 색 번호를 가진 블록 종류에 대응하는 일반 모드 색상을 반환한다. */
    static Color colorFor(PieceType type) {
        return PieceStyle.colorFor(type, ColorVisionMode.NORMAL);
    }

    /** 한 칸을 단색으로 채우고, 이웃한 칸과 구분되는 어두운 경계선을 그린다. */
    static void paintCell(Graphics graphics, PieceType type, int x, int y, int cellSize) {
        PieceStyle.paintCell(graphics, type, x, y, cellSize, ColorVisionMode.NORMAL, false);
    }
    static void paintCell(Graphics graphics, PieceType type, int x, int y, int size, boolean accessible) {
        PieceStyle.paintCell(graphics, type, x, y, size,
                accessible ? ColorVisionMode.DEUTAN : ColorVisionMode.NORMAL, accessible);
    }

}
