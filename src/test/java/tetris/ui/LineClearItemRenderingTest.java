package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.board.Board;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

class LineClearItemRenderingTest {
    @ParameterizedTest @EnumSource(ColorVisionMode.class)
    void letterAppearsOnBoardAndPreviewEvenWhenTheShapeDoesNotChange(ColorVisionMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino plain = new Tetromino(PieceType.O);
            Tetromino item = plain.withItem(ItemType.LINE_CLEAR, 0);
            NextPiecePreview preview = new NextPiecePreview();
            preview.setSize(144, 108);
            preview.setAppearance(mode, true);
            preview.setBlock(plain);
            BufferedImage before = GameMenuUiTest.paint(preview);
            preview.setBlock(item);
            assertEquals("다음 블록: O / 줄 삭제 (L)", preview.getAccessibleContext().getAccessibleName());
            assertMarkerOnly(before, GameMenuUiTest.paint(preview), new Rectangle(40, 22, 32, 32));
            preview.setPiece(PieceType.O);
            assertArrayEquals(pixels(before), pixels(GameMenuUiTest.paint(preview)));
            GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(() -> plain, 1), new CenteredPieceSpawnPolicy());
            engine.newGame(GameMode.ITEM);
            BoardPreview board = new BoardPreview();
            board.setSize(280, 540);
            board.setAppearance(mode, true);
            board.render(engine.state());
            before = GameMenuUiTest.paint(board);
            GameEngine itemEngine = new GameEngine(() -> PieceQueue.generatedBy(() -> item, 1), new CenteredPieceSpawnPolicy());
            itemEngine.newGame(GameMode.ITEM);
            board.render(itemEngine.state());
            Rectangle bounds = board.boardBounds();
            int cellSize = (bounds.width - 6) / Board.COLUMNS;
            assertMarkerOnly(before, GameMenuUiTest.paint(board), new Rectangle(bounds.x + 5 + 4 * cellSize,
                    bounds.y + 5, cellSize - 4, cellSize - 4));
        });
    }

    private static void assertMarkerOnly(BufferedImage before, BufferedImage after, Rectangle marker) {
        int changes = 0, white = 0;
        for (int y = 0; y < before.getHeight(); y++) for (int x = 0; x < before.getWidth(); x++) {
            if (marker.contains(x, y)) {
                if (before.getRGB(x, y) != after.getRGB(x, y)) changes++;
                if ((after.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) white++;
            } else assertEquals(before.getRGB(x, y), after.getRGB(x, y));
        }
        assertTrue(changes > 20);
        assertTrue(white > 5, "흰색 L 문자가 보여야 한다.");
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
