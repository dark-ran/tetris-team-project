package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.board.Board;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.game.GameAction;
import tetris.piece.PieceType;
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
            BufferedImage previewImage = GameMenuUiTest.paint(preview);
            assertWhiteLetter(previewImage, new Rectangle(56, 38, 32, 32));
            for (int x : new int[] {24, 88})
                assertEquals(0x181818, previewImage.getRGB(x, 38) & 0xFFFFFF, "양끝 칸도 미리보기에 표시한다.");
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

    @ParameterizedTest
    @CsvSource({"2,-1,3,17", "4,-1,4,17", "5,-1,5,17", "2,5,4,17", "-1,-1,4,19"})
    void landingTargetMatchesContactAndPaintingDoesNotChangeBoard(
            int firstSquare, int secondSquare, int centerColumn, int landingRow) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int[] columns = firstSquare < 0 ? new int[0] : secondSquare < 0
                    ? new int[] {firstSquare} : new int[] {firstSquare, secondSquare};
            GameEngine engine = new GameEngine(() -> {
                AtomicInteger index = new AtomicInteger();
                return PieceQueue.generatedBy(() -> index.getAndIncrement() < columns.length
                        ? new Tetromino(PieceType.O) : Tetromino.bomb(), 1);
            }, new CenteredPieceSpawnPolicy());
            engine.newGame(GameMode.ITEM);
            for (int column : columns) {
                while (engine.state().currentColumn() != column)
                    assertTrue(engine.apply(engine.state().currentColumn() < column
                            ? GameAction.RIGHT : GameAction.LEFT).changed());
                engine.apply(GameAction.HARD_DROP);
            }
            assertEquals(landingRow, engine.state().landingRow());
            int[][] before = engine.state().board().snapshot();
            BoardPreview preview = new BoardPreview();
            preview.setSize(280, 540);
            preview.render(engine.state());
            assertTarget(preview, landingRow, centerColumn);
            for (int i = 0; i < landingRow; i++) engine.apply(GameAction.TICK);
            assertEquals(landingRow, engine.state().currentRow());
            preview.render(engine.state());
            assertTarget(preview, landingRow, centerColumn);
            assertArrayEquals(before, engine.state().board().snapshot());
            if (firstSquare == 4) {
                assertTrue(engine.apply(GameAction.RIGHT).changed());
                preview.render(engine.state());
                assertTarget(preview, landingRow, 5);
                assertArrayEquals(before, engine.state().board().snapshot(), "이동 후 중심을 다시 계산한다.");
            }
        });
    }

    private static void assertTarget(BoardPreview board, int row, int column) {
        Rectangle bounds = board.boardBounds();
        int size = (bounds.width - 6) / Board.COLUMNS;
        BufferedImage image = GameMenuUiTest.paint(board);
        assertEquals(AppTheme.YELLOW.getRGB() & 0xFFFFFF, image.getRGB(
                bounds.x + 5 + column * size + (size - 4) / 2,
                bounds.y + 6 + row * size) & 0xFFFFFF, "실제 폭발 중심을 실선으로 강조한다.");
    }

    private static void assertWhiteLetter(BufferedImage image, Rectangle cell) {
        int white = 0;
        for (int y = cell.y; y < cell.y + cell.height; y++)
            for (int x = cell.x; x < cell.x + cell.width; x++)
                if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) white++;
        assertTrue(white > 5, "폭탄의 B 문자가 흰색으로 표시되어야 한다.");
    }
}
