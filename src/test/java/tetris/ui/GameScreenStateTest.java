package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import tetris.game.*;
import tetris.piece.*;

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
                assertEquals(2, marks.size(), "Only the title and the latest preview should remain");
                TetrominoMark mark = marks.getLast();
                mark.setSize(mark.getPreferredSize());
                BufferedImage image = GameMenuUiTest.paint(mark);
                int[][] cells = new Tetromino(type).cells();
                int first = Arrays.stream(cells).mapToInt(cell -> cell[0]).min().orElseThrow();
                for (int[] cell : cells) assertEquals(NormalPieceStyle.colorFor(type).getRGB(),
                        image.getRGB(cell[1] * 15 + 7, (cell[0] - first) * 15 + 7));
            }
            screen.render(new GameEngine().state());
            assertEquals(1, GameMenuUiTest.tree(screen).filter(TetrominoMark.class::isInstance).count());
        });
    }
}
