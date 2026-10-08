package tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.board.Board;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameState;
import tetris.piece.PieceType;

/** 실제 엔진에서 생성·고정한 일곱 블록의 위치와 종류별 색상을 화면에서 검증한다. */
class EngineBoardRenderingTest {
    @ParameterizedTest
    @EnumSource(PieceType.class)
    void spawnedAndLockedCellsMatchEngineState(PieceType type) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Random selectedType = new Random(0) {
                @Override public int nextInt(int bound) { return type.ordinal(); }
            };
            GameEngine engine = new GameEngine(selectedType);
            engine.newGame();
            GameState spawned = engine.state();
            int topRow = Arrays.stream(spawned.currentPiece().cells())
                    .mapToInt(cell -> spawned.currentRow() + cell[0]).min().orElseThrow();
            assertEquals(0, topRow, "실제 블록의 가장 위쪽 칸은 첫 행에 나타나야 한다.");
            assertRenderedCells(spawned);
            engine.apply(GameAction.HARD_DROP);
            GameState locked = engine.state();
            long lockedCellCount = Arrays.stream(locked.board().snapshot())
                    .flatMapToInt(Arrays::stream).filter(value -> value == type.cellValue()).count();
            assertEquals(4, lockedCellCount);
            assertRenderedCells(locked);
        });
    }

    private static void assertRenderedCells(GameState state) {
        BoardPreview preview = new BoardPreview();
        preview.setSize(preview.getPreferredSize());
        preview.render(state);
        BufferedImage image = new BufferedImage(preview.getWidth(), preview.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { preview.paint(graphics); }
        finally { graphics.dispose(); }

        int[][] expected = state.board().snapshot();
        for (int[] cell : state.currentPiece().cells()) {
            expected[state.currentRow() + cell[0]][state.currentColumn() + cell[1]] =
                    state.currentPiece().type().cellValue();
        }
        Rectangle bounds = preview.boardBounds();
        int cellSize = (bounds.width - 6) / Board.COLUMNS;
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int value = expected[row][column];
                int color = value == Board.EMPTY_CELL ? AppTheme.GRID.getRGB()
                        : NormalPieceStyle.colorFor(PieceType.fromCellValue(value)).getRGB();
                assertEquals(color, image.getRGB(bounds.x + 3 + column * cellSize + cellSize / 2,
                        bounds.y + 3 + row * cellSize + cellSize / 2),
                        "화면의 " + row + "행 " + column + "열은 엔진 상태와 일치해야 한다.");
            }
        }
    }
}
