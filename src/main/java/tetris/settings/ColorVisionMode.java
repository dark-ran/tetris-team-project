package tetris.settings;

/** The five display modes agreed in issue #4; patterns remain a separate preference. */
public enum ColorVisionMode {
    NORMAL("일반"),
    PROTAN("적색 계열 보정 (Protan)"),
    DEUTAN("녹색 계열 보정 (Deutan)"),
    TRITAN("청황 계열 보정 (Tritan)"),
    MONOCHROME("단색 (Monochrome)");

    private final String label;

    ColorVisionMode(String label) { this.label = label; }
    public boolean defaultPatternsEnabled() { return this != NORMAL; }
    @Override public String toString() { return label; }
}
