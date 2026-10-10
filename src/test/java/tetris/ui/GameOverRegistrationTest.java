package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tetris.settings.GameSettings;

class GameOverRegistrationTest {
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void guidanceAndWrappedErrorsStayInsideTheCompactRegistrationCard(int preset) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameOverScreen screen = new GameOverScreen(() -> {}, () -> {}, () -> {}, () -> {});
            screen.showScreen(Long.MAX_VALUE, true);
            Dimension size = GameSettings.presetDimension(preset);
            screen.setSize(size.width, size.height - 40); GameMenuUiTest.layout(screen);
            JTextField field = nameField(screen);
            JLabel guide = GameMenuUiTest.tree(screen).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .filter(label -> label.getText().contains("1~20자")).findFirst().orElseThrow();
            assertTrue(guide.getFontMetrics(guide.getFont()).stringWidth(guide.getText())
                    <= guide.getWidth() - guide.getInsets().left - guide.getInsets().right);
            Rectangle guideBounds = SwingUtilities.convertRectangle(guide.getParent(), guide.getBounds(), screen);
            Rectangle fieldBounds = SwingUtilities.convertRectangle(field.getParent(), field.getBounds(), screen);
            assertEquals(fieldBounds.getCenterX(), guideBounds.getCenterX(), 1.0);
            JTextArea error = GameMenuUiTest.tree(screen).filter(JTextArea.class::isInstance)
                    .map(JTextArea.class::cast).findFirst().orElseThrow();
            int cardWidth = guide.getParent().getWidth();
            for (String message : new String[] {
                    "name must contain 1 to 20 characters without controls",
                    "기록을 저장하지 못했습니다. 저장 위치에 쓰기 권한이 있는지 확인한 뒤 다시 시도해 주세요."}) {
                screen.showSaveError(message); GameMenuUiTest.layout(screen); GameMenuUiTest.paint(screen);
                assertEquals(cardWidth, guide.getParent().getWidth(), "Long feedback must not widen the input form");
                assertEquals(message, error.getText());
                assertFalse(error.isEditable()); assertFalse(error.isFocusable());
                Rectangle bounds = SwingUtilities.convertRectangle(error.getParent(), error.getBounds(), screen);
                assertTrue(new Rectangle(0, 0, screen.getWidth(), screen.getHeight()).contains(bounds));
                try {
                    for (int offset = 0; offset <= message.length(); offset++) {
                        var glyph = error.modelToView2D(offset);
                        assertNotNull(glyph);
                        assertTrue(glyph.getMinX() >= 0 && glyph.getMaxX() <= error.getWidth(), "No horizontal clipping");
                        assertTrue(glyph.getMaxY() <= error.getHeight(), "Every wrapped line must remain visible");
                    }
                } catch (javax.swing.text.BadLocationException ex) { throw new AssertionError(ex); }
            }
        });
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void registrationFieldStaysReadableAndCompactAtEveryWindowSize(int preset) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameOverScreen screen = new GameOverScreen(() -> {}, () -> {}, () -> {}, () -> {});
            screen.showScreen(Long.MAX_VALUE, true);
            Dimension size = GameSettings.presetDimension(preset); screen.setSize(size); GameMenuUiTest.layout(screen);
            JTextField field = nameField(screen);
            assertTrue(field.getFont().getSize() >= 18, "The name must use readable text");
            int lineHeight = field.getFontMetrics(field.getFont()).getHeight();
            assertTrue(field.getHeight() >= lineHeight + field.getInsets().top + field.getInsets().bottom);
            assertTrue(field.getHeight() <= lineHeight * 2, "Do not stretch the editor to fill the result card");
            assertTrue(field.getWidth() < size.width * 0.6, "Keep the name editor proportional to the screen");
            assertTrue(field.getFontMetrics(field.getFont()).stringWidth("가".repeat(20))
                    <= field.getWidth() - field.getInsets().left - field.getInsets().right,
                    "A valid 20-character Korean name must fit without horizontal scrolling");
            var image = GameMenuUiTest.paint(screen);
            var point = SwingUtilities.convertPoint(field, 4, 4, screen);
            assertEquals(AppTheme.SURFACE_RAISED.getRGB(), image.getRGB(point.x, point.y), "No native white box");
            GameMenuUiTest.tree(screen).filter(JButton.class::isInstance).map(JButton.class::cast).forEach(button -> {
                Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), screen);
                assertTrue(new Rectangle(size).contains(bounds), "All actions must fit inside the window");
            });
        });
    }

    @Test void validationSaveRetryAndRegistrationVisibilityPreserveTheirBehavior() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<String> registered = new AtomicReference<>();
            AtomicReference<String> failure = new AtomicReference<>();
            AtomicInteger skips = new AtomicInteger();
            GameOverScreen screen = new GameOverScreen(() -> {}, () -> {}, () -> {}, () -> {});
            screen.setOnRegister(name -> {
                if (name.isBlank()) throw new IllegalArgumentException("이름을 입력하세요.");
                registered.set(name); failure.set("기록 저장 실패");
            });
            screen.setSaveErrorSupplier(failure::get);
            screen.setOnRetrySave(() -> { failure.set(null); return true; });
            screen.setOnSkip(skips::incrementAndGet);
            screen.showScreen(42, true);
            button(screen, "기록 등록").doClick(0);
            assertMessage(screen, "이름을 입력하세요.");
            JTextField name = nameField(screen); name.setText("한 글 이름 😀");
            button(screen, "기록 등록").doClick(0);
            assertEquals("한 글 이름 😀", registered.get()); assertMessage(screen, "기록 저장 실패");
            assertEquals("한 글 이름 😀", name.getText());
            button(screen, "저장 재시도").doClick(0); assertMessage(screen, " ");
            button(screen, "등록 건너뛰기").doClick(0); assertEquals(1, skips.get());
            screen.showScreen(0, false); screen.setSize(900, 760); GameMenuUiTest.layout(screen);
            assertTrue(name.getText().isEmpty());
            assertFalse(visibleInScreen(name, screen));
            // A later eligible result must show a clean editor rather than the previous player's name.
            screen.showScreen(7, true); assertTrue(name.getText().isEmpty()); assertTrue(visibleInScreen(name, screen));
        });
    }

    private static JTextField nameField(GameOverScreen screen) {
        return GameMenuUiTest.tree(screen).filter(JTextField.class::isInstance).map(JTextField.class::cast).findFirst().orElseThrow();
    }
    private static boolean visibleInScreen(Component component, GameOverScreen screen) {
        for (Component current = component; current != null; current = current.getParent()) {
            if (!current.isVisible()) return false;
            if (current == screen) return true;
        }
        return false;
    }
    private static JButton button(GameOverScreen screen, String text) {
        return GameMenuUiTest.tree(screen).filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> button.getText().equals(text)).findFirst().orElseThrow();
    }
    private static void assertMessage(GameOverScreen screen, String text) {
        assertTrue(GameMenuUiTest.tree(screen).filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                .anyMatch(label -> label.getText().equals(text)));
    }
}
