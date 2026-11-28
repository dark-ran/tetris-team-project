package tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SwingKeyBindingsTest {
    /** 실행 중인 앱처럼 AWT 툴킷을 초기화해야 macOS의 기호형 키 이름이 나온다. */
    @BeforeAll
    static void initializeToolkitLikeTheRunningApplication() {
        Toolkit.getDefaultToolkit();
    }

    @Test
    void spaceAndEnterUseNamesTheBundledFontCanDraw() {
        assertEquals("Space", SwingKeyBindings.keyText(KeyEvent.VK_SPACE));
        assertEquals("Enter", SwingKeyBindings.keyText(KeyEvent.VK_ENTER));
        assertEquals("←", SwingKeyBindings.keyText(KeyEvent.VK_LEFT));
    }

    @Test
    void everyKeyNameCanBeDrawnWithTheBundledFont() throws Exception {
        Font font = AppTheme.font(Font.BOLD, 14f);
        List<String> undrawable = new ArrayList<>();
        for (Field field : KeyEvent.class.getFields()) {
            if (!field.getName().startsWith("VK_") || field.getType() != int.class
                    || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            int code = field.getInt(null);
            String text = SwingKeyBindings.keyText(code);
            if (font.canDisplayUpTo(text) >= 0) {
                undrawable.add(field.getName() + "=" + text);
            }
        }
        assertTrue(undrawable.isEmpty(), "Key names missing from the font: " + undrawable);
    }
}
