package tetris.ui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import tetris.piece.ItemType;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

/** Draws documentation examples with the game's actual cell renderer. */
public final class GenerateItemGallery {
    private GenerateItemGallery() { }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        AppTheme.install();
        BufferedImage overview = canvas(1200, 360);
        Graphics2D g = graphics(overview);
        heading(g, "아이템 5종", 28, 44);
        text(g, "누적 10줄마다 다음 목록에 추가 · 종류별 20% · 일반 모드에서는 등장하지 않음", 28, 76);
        Tetromino[] items = {
            new Tetromino(PieceType.L).withItem(ItemType.LINE_CLEAR, 2),
            Tetromino.weight(), Tetromino.bomb(),
            new Tetromino(PieceType.T).withItem(ItemType.GOLDEN), Tetromino.singleCell()
        };
        String[] titles = {"L · 줄 삭제", "W · 무게추", "B · 폭탄", "G · 황금 블록", "1 · 1칸 블록"};
        String[][] notes = {{"L이 있는 줄 삭제", "일반 점수"}, {"너비 4칸 · 아래 칸 제거", "점수 0 · 회전 없음"},
            {"가로 3칸 · 5×5 삭제", "점수 0 · 회전 없음"}, {"일반 모양 · 모든 칸 G", "줄 삭제 점수 5배"},
            {"한 칸 · 자동 낙하 유지", "일반 점수"}};
        for (int i = 0; i < items.length; i++) {
            int x = 18 + i * 236;
            g.setColor(AppTheme.SURFACE); g.fillRoundRect(x, 100, 222, 240, 12, 12);
            heading(g, titles[i], x + 12, 132);
            piece(g, items[i], x + 111, 198, 40);
            text(g, notes[i][0], x + 12, 288);
            text(g, notes[i][1], x + 12, 314);
        }
        g.dispose();
        ImageIO.write(overview, "png", output.resolve("items-overview.png").toFile());

        BufferedImage variants = canvas(1000, 1100);
        g = graphics(variants);
        heading(g, "L 아이템의 부착 위치", 28, 44);
        text(g, "일반 블록 7종 × 네 칸 중 하나 · 각 위치 25% · 회전할 때 L도 함께 회전", 28, 76);
        for (int column = 0; column < 4; column++) text(g, "부착 위치 " + (column + 1), 170 + column * 220, 112);
        int row = 0;
        for (PieceType type : PieceType.standardTypes()) {
            int y = 186 + row * 134;
            heading(g, type.name(), 40, y + 6);
            for (int column = 0; column < 4; column++) {
                piece(g, new Tetromino(type).withItem(ItemType.LINE_CLEAR, column),
                        220 + column * 220, y, 32);
            }
            row++;
        }
        g.dispose();
        ImageIO.write(variants, "png", output.resolve("line-item-variants.png").toFile());
    }

    private static BufferedImage canvas(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(AppTheme.BACKGROUND); g.fillRect(0, 0, width, height); g.dispose();
        return image;
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    private static void heading(Graphics2D g, String value, int x, int y) {
        g.setColor(AppTheme.YELLOW); g.setFont(AppTheme.font(Font.BOLD, 21)); g.drawString(value, x, y);
    }

    private static void text(Graphics2D g, String value, int x, int y) {
        g.setColor(AppTheme.TEXT); g.setFont(AppTheme.font(Font.PLAIN, 15)); g.drawString(value, x, y);
    }

    private static void piece(Graphics2D g, Tetromino piece, int centerX, int centerY, int size) {
        int minRow = 99, minColumn = 99, maxRow = 0, maxColumn = 0;
        for (int[] cell : piece.cells()) {
            minRow = Math.min(minRow, cell[0]); minColumn = Math.min(minColumn, cell[1]);
            maxRow = Math.max(maxRow, cell[0]); maxColumn = Math.max(maxColumn, cell[1]);
        }
        int left = centerX - (maxColumn - minColumn + 1) * size / 2;
        int top = centerY - (maxRow - minRow + 1) * size / 2;
        for (int[] cell : piece.cells()) {
            int x = left + (cell[1] - minColumn) * size, y = top + (cell[0] - minRow) * size;
            PieceStyle.paintCell(g, piece.type(), x, y, size, ColorVisionMode.NORMAL, false);
            if (piece.hasItemAt(cell[0], cell[1])) PieceStyle.paintItem(g, piece.itemType(), x, y, size);
        }
    }
}
