package tetris.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import tetris.game.GameAction;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.game.GameEngine;
import tetris.game.GameState;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.piece.ItemType;
import tetris.settings.ColorVisionMode;

class SinglePieceRenderingTest {
    @Test void rewardedSingleKeepsItsIdentifierInEveryAppearanceAndPreview() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino single = Tetromino.singleCell();
            assertEquals(ItemType.SINGLE, single.itemType());
            for (ColorVisionMode mode : ColorVisionMode.values()) {
                for (boolean patterns : new boolean[] {false, true}) {
                    NextPiecePreview next = new NextPiecePreview();
                    next.setAppearance(mode, patterns);
                    next.setBlock(single);
                    next.setSize(next.getPreferredSize());
                    assertEquals("다음 아이템: 1칸 블록 (1)", next.getAccessibleContext().getAccessibleName());
                    BufferedImage cell = paint(next).getSubimage(56, 38, 32, 32);
                    int whiteGlyphPixels = 0;
                    for (int y = 8; y < 24; y++) for (int x = 8; x < 24; x++) {
                        if (cell.getRGB(x, y) == Color.WHITE.getRGB()) whiteGlyphPixels++;
                    }
                    assertTrue(whiteGlyphPixels > 0, "The item identifier must remain visible inside its dark badge");
                }
            }
            assertArrayEquals(single.cells(), single.rotateClockwise().cells());
            assertEquals("1", single.itemType().symbol());
            assertTrue(single.hasItemAt(0, 0));
        });
    }

    @Test
    void singlePieceRendersAcrossViewsModesAndPatternSettings() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino single = new Tetromino(PieceType.SINGLE);
            assertEquals(1, single.rotationSize());
            assertArrayEquals(new int[][] {{0, 0}}, single.cells());

            GameEngine engine = new GameEngine(
                    () -> new PieceQueue(new Random(17)), new CenteredPieceSpawnPolicy());

            for (ColorVisionMode mode : ColorVisionMode.values()) {
                for (boolean patterns : new boolean[] {false, true}) {
                    engine.newGame();
                    engine.replaceLastPreviewPiece(PieceType.SINGLE);
                    assertEquals(PieceType.SINGLE, engine.state().nextPieces().getFirst());

                    BufferedImage mark = paint(new TetrominoMark(PieceType.SINGLE, mode, patterns));
                    assertEquals(15, mark.getWidth());
                    assertEquals(15, mark.getHeight());
                    assertHasBlackIdentifier(mark);

                    NextPiecePreview next = new NextPiecePreview();
                    next.setAppearance(mode, patterns);
                    next.setPiece(engine.state().nextPieces().getFirst());
                    next.setSize(next.getPreferredSize());
                    BufferedImage nextImage = paint(next);
                    assertEquals("다음 블록: SINGLE", next.getAccessibleContext().getAccessibleName());
                    BufferedImage nextCell = nextImage.getSubimage(56, 38, 32, 32);
                    assertHasBlackIdentifier(nextCell);

                    engine.apply(GameAction.HARD_DROP);
                    assertEquals(PieceType.SINGLE, engine.state().currentPiece().type());
                    assertEquals(1, engine.state().currentPiece().cells().length);
                    assertBoardCellMatches(engine.state(), mark, mode, patterns);
                    assertHasBlackIdentifier(mark);

                    engine.apply(GameAction.HARD_DROP);
                    int[][] fixed = engine.state().board().snapshot();
                    int lockedRow = -1, lockedColumn = -1;
                    for (int row = 0; row < fixed.length; row++) {
                        for (int column = 0; column < fixed[row].length; column++) {
                            if (fixed[row][column] == PieceType.SINGLE.cellValue()) {
                                lockedRow = row;
                                lockedColumn = column;
                            }
                        }
                    }
                    assertTrue(lockedRow >= 0, "The dropped single cell must be stored on the board");
                    assertBoardCellMatches(engine.state(), lockedRow, lockedColumn, mark, mode, patterns);
                }
            }
        });
    }

    private static void assertBoardCellMatches(GameState state, BufferedImage expected,
            ColorVisionMode mode, boolean patterns) {
        int[] cell = state.currentPiece().cells()[0];
        assertBoardCellMatches(state, state.currentRow() + cell[0], state.currentColumn() + cell[1], expected,
                mode, patterns);
    }

    private static void assertBoardCellMatches(
            GameState state, int row, int column, BufferedImage expected, ColorVisionMode mode, boolean patterns) {
        BoardPreview board = new BoardPreview();
        board.setSize(210, 400);
        board.setAppearance(mode, patterns);
        board.render(state);
        BufferedImage actual = paint(board);
        var bounds = board.boardBounds();
        int x = bounds.x + 5 + column * 19;
        int y = bounds.y + 5 + row * 19;
        assertArrayEquals(pixels(expected), actual.getRGB(x, y, 15, 15, null, 0, 15));
    }

    private static void assertHasBlackIdentifier(BufferedImage image) {
        int blackPixels = 0;
        for (int y = 1; y < image.getHeight() - 1; y++) {
            for (int x = 1; x < image.getWidth() - 1; x++) {
                if (image.getRGB(x, y) == Color.BLACK.getRGB()) blackPixels++;
            }
        }
        assertTrue(blackPixels > 0, "SINGLE must show its identifier even when patterns are disabled");
    }

    private static BufferedImage paint(JComponent component) {
        if (component.getWidth() == 0 || component.getHeight() == 0) {
            component.setSize(component.getPreferredSize());
        }
        BufferedImage image = new BufferedImage(
                component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
