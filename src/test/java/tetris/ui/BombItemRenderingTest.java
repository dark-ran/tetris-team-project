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
import tetris.piece.PieceQueue;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

class BombItemRenderingTest {
    @ParameterizedTest @EnumSource(ColorVisionMode.class)
    void bombLetterAppearsInBoardAndCenteredPreview(ColorVisionMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino bomb = Tetromino.bomb();
            NextPiecePreview preview = new NextPiecePreview();
            preview.setSize(144, 108);
            preview.setAppearance(mode, true);
            preview.setBlock(bomb);
            assertEquals("다음 아이템: 폭탄 (B)", preview.getToolTipText());
            assertEquals(preview.getToolTipText(), preview.getAccessibleContext().getAccessibleName());
            assertWhiteLetter(GameMenuUiTest.paint(preview), new Rectangle(56, 38, 32, 32));
            GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(() -> bomb, 1),
                    new CenteredPieceSpawnPolicy());
            engine.newGame(GameMode.ITEM);
            BoardPreview board = new BoardPreview();
            board.setSize(280, 540);
            board.setAppearance(mode, true);
            board.render(engine.state());
            Rectangle bounds = board.boardBounds();
            int size = (bounds.width - 6) / Board.COLUMNS;
            assertWhiteLetter(GameMenuUiTest.paint(board), new Rectangle(
                    bounds.x + 5 + 4 * size, bounds.y + 5, size - 4, size - 4));
        });
    }

    private static void assertWhiteLetter(BufferedImage image, Rectangle cell) {
        int white = 0;
        for (int y = cell.y; y < cell.y + cell.height; y++)
            for (int x = cell.x; x < cell.x + cell.width; x++)
                if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) white++;
        assertTrue(white > 5, "폭탄의 B 문자가 흰색으로 표시되어야 한다.");
    }
}
