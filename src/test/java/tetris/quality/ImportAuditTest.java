package tetris.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ImportAuditTest {
    @TempDir Path directory;

    static Object[][] sources() {
        return new Object[][] {
                {"import java.util.Objects; class Example { String value = \"Objects\"; /* Objects */ }", 1},
                {"import java.util.Objects; class Example { Object value = Objects.requireNonNull(\"ok\"); }", 0},
                {"import java.awt.*; class Example {}", 1},
                {"import java.util.*; class Example { List<String> value = List.of(); }", 0},
                {"import static java.lang.Math.max; class Example { int value = max(1, 2); }", 0},
                {"import static java.lang.Math.*; class Example { int value = max(1, 2); }", 0},
                {"import java.util.Objects; class Example { Object value = java.util.Objects.requireNonNull(\"ok\"); }", 1},
                {"import missing.Type; class Example {}", 2},
                {"import java.util.Map.Entry; class Example { Entry<String,Integer> value; }", 0},
                {"import static fixture.Child.value; class Example { int result = value(); }", 0}
        };
    }

    @ParameterizedTest @MethodSource("sources")
    void importsAreCheckedByResolvedSymbols(String source, int expected) throws Exception {
        Files.writeString(directory.resolve("Example.java"), source);
        if (source.contains("fixture.Child")) {
            Path fixture = Files.createDirectory(directory.resolve("fixture"));
            Files.writeString(fixture.resolve("Parent.java"),
                    "package fixture; public class Parent { public static int value() { return 1; } }");
            Files.writeString(fixture.resolve("Child.java"), "package fixture; public class Child extends Parent {}");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output)) {
            assertEquals(expected, ImportAudit.verify(directory, "", stream), output::toString);
        }
        if (expected == 1) assertTrue(output.toString().contains("1 unused imports"));
    }
}
