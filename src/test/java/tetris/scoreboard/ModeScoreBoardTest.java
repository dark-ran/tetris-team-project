package tetris.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.GameMode;

class ModeScoreBoardTest {
    @TempDir Path directory;

    @Test void eachModeKeepsItsOwnTopTenAfterReload() {
        ScoreRepository repository = new ScoreRepository(directory.resolve("scores"));
        ScoreBoardService scores = new ScoreBoardService(repository);
        for (int i = 0; i < 10; i++) scores.register("normal" + i, 1_000 + i);
        assertFalse(scores.qualifies(0, GameMode.NORMAL));
        assertTrue(scores.qualifies(0, GameMode.ITEM));
        for (int i = 0; i < 12; i++) scores.register("item" + i, i, GameMode.ITEM);
        ScoreBoardService restored = new ScoreBoardService(repository);
        assertEquals(10, restored.top(GameMode.NORMAL).size());
        assertEquals(10, restored.top(GameMode.ITEM).size());
        assertEquals(1_009, restored.highScore(GameMode.NORMAL));
        assertEquals(11, restored.highScore(GameMode.ITEM));
        assertEquals(2, restored.top(GameMode.ITEM).getLast().score());
        assertEquals(20, restored.allModes().size());
        restored.reset();
        assertTrue(restored.allModes().isEmpty());
    }

    @Test void legacyRecordsBecomeNormalAndSurviveTheFirstItemSave() throws Exception {
        Path file = directory.resolve("scores");
        Files.writeString(file, "TETRIS-SCORES-1\n기존 사용자\t123\n");
        ScoreRepository repository = new ScoreRepository(file);
        ScoreBoardService scores = new ScoreBoardService(repository);
        assertEquals(GameMode.NORMAL, scores.top().getFirst().mode());
        scores.register("아이템 사용자", 9, GameMode.ITEM);
        assertEquals("기존 사용자", new ScoreBoardService(repository).top().getFirst().name());
        assertEquals(GameMode.ITEM, repository.load().getLast().mode());
        assertTrue(Files.readString(file).startsWith("TETRIS-SCORES-3\n"));
    }
}
