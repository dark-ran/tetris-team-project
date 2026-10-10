package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tetris.game.*;
import tetris.piece.*;
import tetris.settings.GameSettings;

class GameScreenStateTest {
    @Test void everyNextPieceReplacesThePreviousPreviewAndScoreAndLevelFollowTheSnapshot() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameScreen screen = new GameScreen(() -> {}, () -> {}, () -> {});
            for (PieceType type : PieceType.values()) {
                GameEngine engine = new GameEngine(new Random(0) {
                    @Override public int nextInt(int bound) { return type.ordinal(); }
                });
                engine.newGame(); engine.updateScoreAndLevel(123, 2);
                screen.render(engine.state());
                assertTrue(GameMenuUiTest.tree(screen).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                        .anyMatch(label -> label.getText().equals("123")));
                assertTrue(GameMenuUiTest.tree(screen).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                        .anyMatch(label -> label.getText().equals("2")));
                var marks = GameMenuUiTest.tree(screen).filter(TetrominoMark.class::isInstance)
                        .map(TetrominoMark.class::cast).toList();
                assertEquals(1, marks.size(), "The small title mark is separate from the gameplay preview");
                assertPreview(preview(screen), type);
            }
            screen.render(new GameEngine().state());
            assertEquals(1, GameMenuUiTest.tree(screen).filter(TetrominoMark.class::isInstance).count());
            NextPiecePreview empty = preview(screen);
            assertEquals("다음 블록 없음", empty.getAccessibleContext().getAccessibleName());
            assertTrue(Arrays.stream(GameMenuUiTest.paint(empty).getRGB(0, 0, empty.getWidth(), empty.getHeight(),
                    null, 0, empty.getWidth())).allMatch(color -> color == 0xff000000));
        });
    }

    @Test void actualQueueAdvancesPreviewAndRepeatedMovementKeepsOnePreview() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(21)); engine.newGame();
            GameScreen screen = new GameScreen(() -> {}, () -> {}, () -> {});
            screen.render(engine.state());
            NextPiecePreview next = preview(screen);
            PieceType expectedCurrent = engine.state().nextPieces().getFirst();
            assertPreview(next, expectedCurrent);
            engine.apply(GameAction.HARD_DROP);
            assertEquals(expectedCurrent, engine.state().currentPiece().type());
            screen.render(engine.state());
            assertPreview(next, engine.state().nextPieces().getFirst());
            for (int i = 0; i < 1000; i++) {
                engine.apply(i % 2 == 0 ? GameAction.LEFT : GameAction.RIGHT);
                screen.render(engine.state());
            }
            assertSame(next, preview(screen));
            assertEquals(1, GameMenuUiTest.tree(screen).filter(NextPiecePreview.class::isInstance).count());
            assertPreview(next, engine.state().nextPieces().getFirst());
        });
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void nextPreviewIsLargeAndBesideTheTopOfTheBoardAtEveryWindowPreset(int preset) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameScreen screen = new GameScreen(() -> {}, () -> {}, () -> {});
            GameEngine engine = new GameEngine(new Random(21)); engine.newGame(); screen.render(engine.state());
            var dimension = GameSettings.presetDimension(preset);
            // 실제 창 테두리·제목줄이 차지하는 높이도 빼고 확인한다.
            screen.setSize(dimension.width, dimension.height - 40); GameMenuUiTest.layout(screen);
            BoardPreview board = GameMenuUiTest.tree(screen).filter(BoardPreview.class::isInstance)
                    .map(BoardPreview.class::cast).findFirst().orElseThrow();
            Rectangle drawnBoard = SwingUtilities.convertRectangle(board, board.boardBounds(), screen);
            NextPiecePreview next = preview(screen);
            Rectangle nextBounds = SwingUtilities.convertRectangle(next.getParent(), next.getBounds(), screen);
            var card = next.getParent().getParent();
            Rectangle cardBounds = SwingUtilities.convertRectangle(card.getParent(), card.getBounds(), screen);
            assertTrue(nextBounds.x >= drawnBoard.x + drawnBoard.width);
            assertTrue(cardBounds.x - drawnBoard.x - drawnBoard.width <= 80, "Preview card must stay close to the board");
            assertTrue(nextBounds.y + nextBounds.height / 2 <= drawnBoard.y + drawnBoard.height / 3,
                    "Preview belongs beside the upper board, where new pieces appear");
            assertTrue(new Rectangle(0, 0, screen.getWidth(), screen.getHeight()).contains(nextBounds));
            assertTrue(new Rectangle(0, 0, screen.getWidth(), screen.getHeight()).contains(drawnBoard));
            assertTrue(drawnBoard.width >= 206, "The nearby preview must not unnecessarily shrink board cells below 20px");
            assertTrue(next.getWidth() >= 128 && next.getHeight() >= 64, "Four 32px cells must fit without clipping");
            GameMenuUiTest.tree(screen).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .filter(label -> label.getText().equals("점수") || label.getText().equals("레벨")
                            || label.getText().startsWith("["))
                    .forEach(label -> {
                        Rectangle labelBounds = SwingUtilities.convertRectangle(label.getParent(), label.getBounds(), screen);
                        assertTrue(labelBounds.y >= cardBounds.y + cardBounds.height, "Stats and controls belong below the preview");
                        assertTrue(new Rectangle(0, 0, screen.getWidth(), screen.getHeight()).contains(labelBounds),
                                "Sidebar text must fit: " + label.getText());
                    });
            assertPreview(next, engine.state().nextPieces().getFirst());
        });
    }

    static NextPiecePreview preview(GameScreen screen) {
        return GameMenuUiTest.tree(screen).filter(NextPiecePreview.class::isInstance)
                .map(NextPiecePreview.class::cast).findFirst().orElseThrow();
    }
    private static void assertPreview(NextPiecePreview preview, PieceType type) {
        if (preview.getWidth() == 0) preview.setSize(preview.getPreferredSize());
        assertEquals("다음 블록: " + type.name(), preview.getAccessibleContext().getAccessibleName());
        BufferedImage image = GameMenuUiTest.paint(preview);
        int[][] cells = new Tetromino(type).cells();
        int top = Arrays.stream(cells).mapToInt(cell -> cell[0]).min().orElseThrow();
        int left = Arrays.stream(cells).mapToInt(cell -> cell[1]).min().orElseThrow();
        int rows = Arrays.stream(cells).mapToInt(cell -> cell[0]).max().orElseThrow() - top + 1;
        int columns = Arrays.stream(cells).mapToInt(cell -> cell[1]).max().orElseThrow() - left + 1;
        Point origin = new Point((image.getWidth() - columns * 32) / 2, (image.getHeight() - rows * 32) / 2);
        for (int[] cell : cells) assertEquals(NormalPieceStyle.colorFor(type).getRGB(),
                image.getRGB(origin.x + (cell[1] - left) * 32 + 16, origin.y + (cell[0] - top) * 32 + 16));
    }
}
