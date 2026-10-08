package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.board.Board;
import tetris.game.*;
import tetris.piece.*;
import tetris.settings.*;

class ColorVisionRenderingTest {
    @ParameterizedTest @EnumSource(ColorVisionMode.class)
    void paletteAndPatternStayConsistentAcrossRotationFixedCellsAndNextPreview(ColorVisionMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (PieceType type : PieceType.values()) {
                GameEngine engine = new GameEngine(new Random(0) {
                    @Override public int nextInt(int bound) { return type.ordinal(); }
                });
                engine.newGame(); engine.apply(GameAction.DOWN);
                TetrominoMark expected = new TetrominoMark(type, mode, true);
                expected.setSize(expected.getPreferredSize());
                BufferedImage tile = GameMenuUiTest.paint(expected);
                int[] first = new Tetromino(type).cells()[0];
                int top = Arrays.stream(new Tetromino(type).cells()).mapToInt(cell -> cell[0]).min().orElseThrow();
                for (int rotation = 0; rotation < 4; rotation++) {
                    assertActiveTiles(engine.state(), mode, tile, first[1] * 15, (first[0] - top) * 15);
                    engine.apply(GameAction.ROTATE_CLOCKWISE);
                }
                engine.apply(GameAction.HARD_DROP);
                GameState state = engine.state();
                BoardPreview preview = board(state, mode);
                BufferedImage image = GameMenuUiTest.paint(preview);
                int[][] fixed = state.board().snapshot();
                for (int row = 0; row < Board.ROWS; row++) for (int column = 0; column < Board.COLUMNS; column++)
                    if (fixed[row][column] == type.cellValue()) assertTile(preview, image, row, column, tile, first[1] * 15, (first[0] - top) * 15);
                GameScreen screen = new GameScreen(() -> {}, () -> {}, () -> {});
                screen.render(state);
                screen.applySettings(new GameSettings(1, mode, true, GameSettings.DEFAULT_KEY_BINDINGS));
                TetrominoMark next = GameMenuUiTest.tree(screen).filter(TetrominoMark.class::isInstance)
                        .map(TetrominoMark.class::cast).toList().getLast();
                next.setSize(next.getPreferredSize());
                assertArrayEquals(pixels(tile), pixels(GameMenuUiTest.paint(next)));
            }
        });
    }

    @Test void sevenSparsePatternsRemainDifferentAtSmallCellSizes() {
        for (int size : new int[] {12, 15, 22}) {
            HashSet<String> masks = new HashSet<>();
            for (PieceType type : PieceType.values()) {
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                try { PieceStyle.paintCell(g, type, 0, 0, size, ColorVisionMode.MONOCHROME, true); }
                finally { g.dispose(); }
                int fill = PieceStyle.colorFor(type, ColorVisionMode.MONOCHROME).getRGB();
                StringBuilder mask = new StringBuilder();
                for (int y = 2; y < size - 2; y++) for (int x = 2; x < size - 2; x++) mask.append(image.getRGB(x, y) == fill ? '0' : '1');
                assertTrue(mask.indexOf("1") >= 0);
                assertTrue(masks.add(mask.toString()), "Patterns must differ even without color: " + type);
            }
        }
    }

    @Test void ghostIsAnOutlineAtTheLandingPositionAndDoesNotChangeTheEngine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(0)); engine.newGame();
            GameState state = engine.state(); int row = state.currentRow();
            while (state.board().canPlace(state.currentPiece(), row + 1, state.currentColumn())) row++;
            BoardPreview preview = board(state, ColorVisionMode.MONOCHROME);
            BufferedImage image = GameMenuUiTest.paint(preview);
            Rectangle bounds = preview.boardBounds();
            for (int[] cell : state.currentPiece().cells()) {
                int x = bounds.x + 5 + (state.currentColumn() + cell[1]) * 19;
                int y = bounds.y + 5 + (row + cell[0]) * 19;
                assertEquals(AppTheme.GRID.getRGB(), image.getRGB(x + 7, y + 7));
                boolean outline = false;
                for (int offset = 0; offset < 15; offset++) if (image.getRGB(x + offset, y + 1) != AppTheme.GRID.getRGB()) outline = true;
                assertTrue(outline);
            }
            assertArrayEquals(state.board().snapshot(), engine.state().board().snapshot());
            assertEquals(state.currentRow(), engine.state().currentRow());
        });
    }

    private static BoardPreview board(GameState state, ColorVisionMode mode) {
        BoardPreview preview = new BoardPreview(); preview.setSize(210, 400);
        preview.setAppearance(mode, true); preview.render(state); return preview;
    }
    private static void assertActiveTiles(GameState state, ColorVisionMode mode, BufferedImage tile, int tx, int ty) {
        BoardPreview preview = board(state, mode); BufferedImage image = GameMenuUiTest.paint(preview);
        for (int[] cell : state.currentPiece().cells()) assertTile(preview, image,
                state.currentRow() + cell[0], state.currentColumn() + cell[1], tile, tx, ty);
    }
    private static void assertTile(BoardPreview board, BufferedImage image, int row, int column, BufferedImage tile, int tx, int ty) {
        Rectangle bounds = board.boardBounds(); int x = bounds.x + 5 + column * 19, y = bounds.y + 5 + row * 19;
        for (int dy = 2; dy < 13; dy++) for (int dx = 2; dx < 13; dx++)
            assertEquals(tile.getRGB(tx + dx, ty + dy), image.getRGB(x + dx, y + dy));
    }
    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
