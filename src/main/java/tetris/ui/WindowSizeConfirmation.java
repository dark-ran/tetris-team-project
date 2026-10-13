package tetris.ui;

import java.awt.Component;
import java.awt.Dimension;

/** A window-size decision shown after the proposed size has already been applied. */
@FunctionalInterface
public interface WindowSizeConfirmation {
    boolean confirm(Component owner, Dimension previous, Dimension proposed);
}
