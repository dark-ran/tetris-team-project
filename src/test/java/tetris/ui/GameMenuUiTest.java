package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import tetris.game.*;
import tetris.input.InputHandler;
import tetris.piece.PieceType;

class GameMenuUiTest {
    @Test void escapeOpensMenuWithBlurredBoardAndBlocksGameplayKeys() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger keys = new AtomicInteger();
            GameScreen[] screen = new GameScreen[1];
            screen[0] = new GameScreen(() -> screen[0].showMenu(), () -> {}, () -> {});
            GameScreen game = screen[0];
            game.setInputHandler(new InputHandler() {
                @Override public void handleKey(int code) { keys.incrementAndGet(); }
            });
            GameEngine engine = new GameEngine(new java.util.Random(0) {
                @Override public int nextInt(int bound) { return PieceType.O.ordinal(); }
            });
            engine.newGame(); engine.apply(GameAction.HARD_DROP); engine.updateScoreAndLevel(123, 2);
            game.render(engine.state()); game.setSize(900, 760); layout(game);
            BufferedImage before = paint(game);
            BoardPreview board = tree(game).filter(BoardPreview.class::isInstance)
                    .map(BoardPreview.class::cast).findFirst().orElseThrow();
            Rectangle bounds = board.boardBounds();
            int cell = (bounds.width - 6) / 10;
            Point sample = SwingUtilities.convertPoint(board,
                    bounds.x + 3 + 4 * cell + cell / 2, bounds.y + 3 + 19 * cell + cell / 2, game);
            key(game, KeyEvent.VK_LEFT);
            assertEquals(1, keys.get());
            key(game, KeyEvent.VK_ESCAPE); layout(game);
            assertTrue(game.isMenuVisible());
            BufferedImage after = paint(game);
            assertNotEquals(before.getRGB(sample.x, sample.y), after.getRGB(sample.x, sample.y));
            for (String text : List.of("게임으로 돌아가기", "설정", "시작 메뉴로 이동", "프로그램 종료"))
                assertTrue(tree(game).filter(JButton.class::isInstance).map(JButton.class::cast)
                        .anyMatch(button -> button.getText().equals(text)));
            key(game, KeyEvent.VK_SPACE); key(game, KeyEvent.VK_LEFT);
            assertEquals(1, keys.get(), "Menu keys must never reach Hard Drop or movement");
            assertTrue(tree(game).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> label.getText().equals("123")));
            game.hideMenu(); key(game, KeyEvent.VK_SPACE);
            assertEquals(2, keys.get());
            assertEquals(123, engine.state().score());
            assertFalse(tree(game).filter(JButton.class::isInstance).map(JButton.class::cast)
                    .anyMatch(button -> button.getText().equals("종료 화면 보기")));
        });
    }

    @Test void gameOverSpaceRestartsFromScreenAndButtonsButTypingSpacesDoesNotRestart() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger restarts = new AtomicInteger();
            GameOverScreen screen = new GameOverScreen(restarts::incrementAndGet, () -> {}, () -> {}, () -> {});
            screen.showScreen(42, true);
            key(screen, KeyEvent.VK_SPACE);
            assertEquals(1, restarts.get());
            JButton back = tree(screen).filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> button.getText().equals("시작 메뉴")).findFirst().orElseThrow();
            Object action = back.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
            back.getActionMap().get(action).actionPerformed(new ActionEvent(back, 0, ""));
            assertEquals(2, restarts.get());
            JTextField name = tree(screen).filter(JTextField.class::isInstance).map(JTextField.class::cast).findFirst().orElseThrow();
            name.setText("two words");
            assertEquals(2, restarts.get());
            assertTrue(tree(screen).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> label.getText().contains("Space") && label.getText().contains("다시 시작")));
        });
    }

    @Test void settingsSaveReportsFailureAndKeepsEditedValuesForRetry() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsScreen screen = new SettingsScreen(() -> {});
            AtomicInteger saves = new AtomicInteger();
            AtomicReference<SettingsScreen.Values> saved = new AtomicReference<>();
            screen.setOnSaveValues(values -> {
                if (saves.getAndIncrement() == 0) throw new UncheckedIOException("저장 실패", new IOException("denied"));
                saved.set(values);
            });
            JComboBox<?> preset = tree(screen).filter(JComboBox.class::isInstance)
                    .map(JComboBox.class::cast).findFirst().orElseThrow();
            preset.setSelectedIndex(2);
            JButton save = tree(screen).filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> button.getText().equals("설정 저장")).findFirst().orElseThrow();
            save.doClick(0);
            assertTrue(tree(screen).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> label.getText().equals("저장 실패")));
            assertEquals(2, screen.values().preset());
            save.doClick(0);
            assertEquals(2, saved.get().preset());
        });
    }

    private static void key(JComponent screen, int code) {
        Object name = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .get(KeyStroke.getKeyStroke(code, 0));
        assertNotNull(name);
        screen.getActionMap().get(name).actionPerformed(new ActionEvent(screen, 0, ""));
    }
    static Stream<Component> tree(Component component) {
        return component instanceof Container container
                ? Stream.concat(Stream.of(component), Arrays.stream(container.getComponents()).flatMap(GameMenuUiTest::tree))
                : Stream.of(component);
    }
    static void layout(Component component) {
        if (component instanceof Container container) {
            container.doLayout(); for (Component child : container.getComponents()) layout(child);
        }
    }
    static BufferedImage paint(JComponent component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { component.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }
}
