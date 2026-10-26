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

class WeightItemRenderingTest {
    @ParameterizedTest @EnumSource(ColorVisionMode.class)
    void allSixWeightLettersAppearInPreviewAndBoard(ColorVisionMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino weight = Tetromino.weight();
            NextPiecePreview preview = new NextPiecePreview();
            preview.setSize(144, 108);
            preview.setAppearance(mode, true);
            preview.setBlock(weight);
            assertEquals("다음 아이템: 무게추 (W)", preview.getToolTipText());
            assertEquals(preview.getToolTipText(), preview.getAccessibleContext().getAccessibleName());
            BufferedImage image = GameMenuUiTest.paint(preview);
            for (int[] cell : weight.cells()) assertWhiteLetter(image,
                    new Rectangle(8 + cell[1] * 32, 22 + cell[0] * 32, 32, 32));
            GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(() -> weight, 1),
                    new CenteredPieceSpawnPolicy());
            engine.newGame(GameMode.ITEM);
            BoardPreview board = new BoardPreview();
            board.setSize(280, 540);
            board.setAppearance(mode, true);
            board.render(engine.state());
            image = GameMenuUiTest.paint(board);
            Rectangle bounds = board.boardBounds();
            int size = (bounds.width - 6) / Board.COLUMNS;
            for (int[] cell : weight.cells()) assertWhiteLetter(image,
                    new Rectangle(bounds.x + 5 + (3 + cell[1]) * size,
                            bounds.y + 5 + cell[0] * size, size - 4, size - 4));
        });
    }

    private static void assertWhiteLetter(BufferedImage image, Rectangle cell) {
        int white = 0;
        for (int y = cell.y; y < cell.y + cell.height; y++)
            for (int x = cell.x; x < cell.x + cell.width; x++)
                if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) white++;
        assertTrue(white > 5, "각 칸의 W 문자가 흰색으로 표시되어야 한다.");
    }
}
