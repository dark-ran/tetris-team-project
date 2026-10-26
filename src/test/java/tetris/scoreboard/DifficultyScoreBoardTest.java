package tetris.scoreboard;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import tetris.game.Difficulty;
import tetris.game.GameMode;

class DifficultyScoreBoardTest {
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(GameMode.class)
    void allDifficultiesSurviveServiceReloadWithinTheirMode(GameMode mode) {
        ScoreRepository repository = new ScoreRepository(directory.resolve("scores"));
        ScoreBoardService service = new ScoreBoardService(repository);
        for (Difficulty difficulty : Difficulty.values()) {
            service.register(difficulty.name(), 100 + difficulty.ordinal(), mode, difficulty);
        }
        List<ScoreEntry> restored = new ScoreBoardService(repository).top(mode);
        assertEquals(3, restored.size());
        assertEquals(List.of(Difficulty.HARD, Difficulty.NORMAL, Difficulty.EASY),
                restored.stream().map(ScoreEntry::difficulty).toList());
        assertEquals(service.top(mode), restored);
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void olderFilesKeepRecordsWhenUpgradedToTheDifficultyFormat(int version) throws Exception {
        Path file = directory.resolve("scores");
        Files.writeString(file, "TETRIS-SCORES-" + version + "\nold\t123"
                + (version == 2 ? "\tITEM" : "") + "\n");
        ScoreRepository repository = new ScoreRepository(file);
        ScoreEntry old = new ScoreEntry("old", 123,
                version == 2 ? GameMode.ITEM : GameMode.NORMAL, Difficulty.NORMAL);
        assertEquals(List.of(old), repository.load());
        assertNull(repository.loadWarning());
        ScoreBoardService service = new ScoreBoardService(repository);
        service.register("new", 10, GameMode.ITEM, Difficulty.HARD);
        List<ScoreEntry> restored = repository.load();
        assertTrue(restored.contains(old));
        assertTrue(restored.contains(new ScoreEntry("new", 10, GameMode.ITEM, Difficulty.HARD)));
        assertTrue(Files.readString(file).startsWith("TETRIS-SCORES-3\n"));
    }

    @Test void damagedDifficultyDoesNotDiscardOtherRecordsOrRewriteTheFile() throws Exception {
        Path file = directory.resolve("scores");
        String data = "TETRIS-SCORES-3\ngood\t100\tNORMAL\tEASY\n"
                + "bad\t200\tNORMAL\tUNKNOWN\nmissing\t300\tITEM\n";
        Files.writeString(file, data);
        ScoreRepository repository = new ScoreRepository(file);
        assertEquals(List.of(new ScoreEntry("good", 100, GameMode.NORMAL, Difficulty.EASY)), repository.load());
        assertNotNull(repository.loadWarning());
        assertEquals(data, Files.readString(file));
        assertThrows(NullPointerException.class, () -> new ScoreEntry("invalid", 0, GameMode.NORMAL, null));
    }
}
