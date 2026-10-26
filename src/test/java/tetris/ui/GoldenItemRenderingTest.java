package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.board.Board;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

class GoldenItemRenderingTest {
    @ParameterizedTest @EnumSource(ColorVisionMode.class)
    void previewFallingRotatedAndFixedCellsKeepGoldAndLetters(ColorVisionMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino golden = new Tetromino(PieceType.T).withItem(ItemType.GOLDEN);
            NextPiecePreview preview = new NextPiecePreview();
            preview.setSize(144, 108);
            preview.setAppearance(mode, true);
            preview.setBlock(golden);
            assertEquals("다음 블록: T / 황금 블록 (G)", preview.getToolTipText());
            assertEquals(preview.getToolTipText(), preview.getAccessibleContext().getAccessibleName());
            BufferedImage image = GameMenuUiTest.paint(preview);
            for (int[] cell : golden.cells())
                assertGoldenCell(image, new Rectangle(24 + cell[1] * 32, 22 + cell[0] * 32, 32, 32));
            GameEngine engine = new GameEngine(() -> {
                AtomicInteger index = new AtomicInteger();
                return PieceQueue.generatedBy(() -> index.getAndIncrement() == 0
                        ? golden : new Tetromino(PieceType.I), 1);
            }, new CenteredPieceSpawnPolicy());
            engine.newGame(GameMode.ITEM);
            BoardPreview board = new BoardPreview();
            board.setSize(280, 540);
            board.setAppearance(mode, true);
            board.render(engine.state());
            assertPiece(board, golden, engine.state().currentRow(), engine.state().currentColumn());
            assertTrue(engine.apply(GameAction.ROTATE_CLOCKWISE).changed());
            Tetromino rotated = engine.state().currentPiece();
            board.render(engine.state());
            assertPiece(board, rotated, engine.state().currentRow(), engine.state().currentColumn());
            int row = engine.state().landingRow(), column = engine.state().currentColumn();
            engine.apply(GameAction.HARD_DROP);
            board.render(engine.state());
            assertPiece(board, rotated, row, column);
            for (int[] cell : rotated.cells())
                assertEquals(ItemType.GOLDEN, engine.state().board().itemAt(row + cell[0], column + cell[1]));
        });
    }

    private static void assertPiece(BoardPreview board, Tetromino piece, int row, int column) {
        Rectangle bounds = board.boardBounds();
        int size = (bounds.width - 6) / Board.COLUMNS;
        BufferedImage image = GameMenuUiTest.paint(board);
        for (int[] cell : piece.cells())
            assertGoldenCell(image, new Rectangle(bounds.x + 5 + (column + cell[1]) * size,
                    bounds.y + 5 + (row + cell[0]) * size, size - 4, size - 4));
    }

    private static void assertGoldenCell(BufferedImage image, Rectangle cell) {
        int color = image.getRGB(cell.x + 1, cell.y + 1);
        int red = (color >> 16) & 255, green = (color >> 8) & 255, blue = color & 255;
        assertTrue(red > green && green > blue + 30, "황금색 테두리가 표시되어야 한다.");
        int white = 0;
        for (int y = cell.y; y < cell.y + cell.height; y++)
            for (int x = cell.x; x < cell.x + cell.width; x++)
                if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) white++;
        assertTrue(white > 5, "각 황금 칸에 흰색 G가 표시되어야 한다.");
    }
}
