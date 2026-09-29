package tetris.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** 창 높이가 줄어도 20×10 보드의 바깥 테두리가 잘리지 않는지 확인한다. */
class BoardPreviewTest {
    @Test
    void completeBoardBorderFitsInsideCompactGameArea() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPreview preview = new BoardPreview();
            assertBoardFits(preview, preview.getMinimumSize());
            assertBoardFits(preview, new Dimension(250, 420));
        });
    }

    private static void assertBoardFits(BoardPreview preview, Dimension size) {
        preview.setSize(size);
        Rectangle board = preview.boardBounds();
        assertTrue(board.x >= 0 && board.y >= 0, "보드의 위쪽과 왼쪽 테두리가 보여야 한다");
        assertTrue(board.x + board.width <= size.width, "보드의 오른쪽 테두리가 보여야 한다");
        assertTrue(board.y + board.height <= size.height, "보드의 아래쪽 테두리가 보여야 한다");
    }
}
