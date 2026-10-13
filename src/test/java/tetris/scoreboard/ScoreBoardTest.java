package tetris.scoreboard;

import static org.junit.jupiter.api.Assertions.*;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 임시 사용자 경로에서 기록 복원·동점·손상 파일·저장 실패 시 기록 보존을 검증한다. */
class ScoreBoardTest {
    @TempDir Path directory;
    @Test void retriesTransientAtomicReplacementAndCleansTemporaryFiles() throws Exception {
        Path file = directory.resolve("scores.tsv");
        class TransientRepository extends ScoreRepository {
            int attempts;
            TransientRepository() { super(file); }
            @Override protected void replaceAtomically(Path temporary) throws IOException {
                if (++attempts < 3) throw new IOException("temporarily locked");
                super.replaceAtomically(temporary);
            }
        }
        TransientRepository repository = new TransientRepository();
        List<ScoreEntry> records = List.of(new ScoreEntry("player", 42));
        repository.save(records);
        assertEquals(3, repository.attempts);
        assertEquals(records, repository.load());
        try (var files = Files.list(directory)) { assertEquals(List.of(file), files.toList()); }
    }
    @Test void unsupportedAtomicMoveNeverFallsBackAndKeepsOriginalFile() throws Exception {
        Path file = directory.resolve("scores.tsv");
        new ScoreRepository(file).save(List.of(new ScoreEntry("old", 50)));
        byte[] original = Files.readAllBytes(file);
        AtomicMoveNotSupportedException unsupported =
                new AtomicMoveNotSupportedException("temp", file.toString(), "unsupported");
        AtomicInteger attempts = new AtomicInteger();
        ScoreRepository repository = new ScoreRepository(file) {
            @Override protected void replaceAtomically(Path temporary) throws IOException {
                attempts.incrementAndGet();
                throw unsupported;
            }
        };
        ScoreBoardService service = new ScoreBoardService(repository);
        UncheckedIOException failure = assertThrows(UncheckedIOException.class,
                () -> service.register("new", 100));
        assertSame(unsupported, failure.getCause());
        assertEquals(1, attempts.get());
        assertArrayEquals(original, Files.readAllBytes(file));
        assertEquals(List.of(new ScoreEntry("old", 50)), service.top());
        try (var files = Files.list(directory)) { assertEquals(List.of(file), files.toList()); }
    }
    @Test void exhaustedRetriesPreserveOriginalAndLaterRegistrationSucceedsOnce() throws Exception {
        Path file = directory.resolve("scores.tsv");
        new ScoreRepository(file).save(List.of(new ScoreEntry("old", 50)));
        byte[] original = Files.readAllBytes(file);
        class LockedRepository extends ScoreRepository {
            boolean locked = true;
            int attempts;
            LockedRepository() { super(file); }
            @Override protected void replaceAtomically(Path temporary) throws IOException {
                attempts++;
                if (locked) throw new IOException("locked");
                super.replaceAtomically(temporary);
            }
        }
        LockedRepository repository = new LockedRepository();
        ScoreBoardService service = new ScoreBoardService(repository);
        assertThrows(UncheckedIOException.class, () -> service.register("new", 100));
        assertEquals(3, repository.attempts);
        assertArrayEquals(original, Files.readAllBytes(file));
        assertEquals(50, service.highScore());
        repository.locked = false;
        service.register("new", 100);
        assertEquals(List.of(new ScoreEntry("new", 100), new ScoreEntry("old", 50)), repository.load());
        try (var files = Files.list(directory)) { assertEquals(List.of(file), files.toList()); }
    }
    @Test void roundTripUnicodeLongScoresAndResetAcrossServiceInstances() throws Exception {
        ScoreRepository repository = new ScoreRepository(directory.resolve("user/scores.tsv"));
        assertTrue(repository.load().isEmpty());
        assertNull(repository.loadWarning());
        ScoreBoardService service = new ScoreBoardService(repository);
        service.register("  플레이어  ", 6_000_000_000L);
        service.register("two", 10);
        assertEquals("TETRIS-SCORES-1\n플레이어\t6000000000\ntwo\t10\n",
                Files.readString(repository.path(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(new ScoreEntry("플레이어", 6_000_000_000L), service.top().getFirst());
        assertEquals(service.top(), new ScoreBoardService(repository).top());
        assertEquals(6_000_000_000L, service.highScore());
        assertThrows(UnsupportedOperationException.class, () -> service.top().clear());
        service.reset();
        assertFalse(Files.exists(repository.path()));
        assertTrue(new ScoreBoardService(repository).top().isEmpty());
    }
    @Test void topTenStableTiesAndQualificationBoundary() {
        ScoreBoardService service = new ScoreBoardService(new ScoreRepository(directory.resolve("scores.tsv")));
        assertTrue(service.qualifies(0));
        for (int i = 0; i < 10; i++) service.register("name" + i, 100);
        assertFalse(service.qualifies(100));
        service.register("rejected", 100);
        assertEquals("name0", service.top().getFirst().name());
        assertTrue(service.qualifies(101));
        service.register("winner", 101);
        assertEquals(10, service.top().size());
        assertEquals("name8", service.top().getLast().name());
        assertThrows(IllegalArgumentException.class, () -> service.qualifies(-1));
    }
    @Test void damagedRowsRetainGoodRecordsAndWrongHeaderIsNotOverwrittenOnLoad() throws Exception {
        Path file = directory.resolve("scores.tsv");
        ScoreRepository repository = new ScoreRepository(file);
        repository.save(List.of(new ScoreEntry("한글", 42)));
        // 구분자 오류·음수 점수·숫자가 아닌 점수를 정상 UTF-8 기록 뒤에 섞는다.
        Files.writeString(file, Files.readString(file) + "invalid row\nextra\ttab\t20\nnegative\t-1\nname\tbad\n");
        assertEquals(List.of(new ScoreEntry("한글", 42)), repository.load());
        assertNotNull(repository.loadWarning());
        Files.writeString(file, "old format");
        assertTrue(repository.load().isEmpty());
        assertNotNull(repository.loadWarning());
        assertEquals("old format", Files.readString(file));
    }
    @Test void invalidUtf8IsReportedWithoutChangingFile() throws Exception {
        Path file = directory.resolve("scores.tsv");
        ScoreRepository repository = new ScoreRepository(file);
        List<ScoreEntry> records = List.of(new ScoreEntry("player", 42));
        repository.save(records);
        assertEquals(records, repository.load());
        byte[] valid = Files.readAllBytes(file);
        byte[] damaged = java.util.Arrays.copyOf(valid, valid.length + 1);
        damaged[valid.length] = (byte) 0xff;
        Files.write(file, damaged);
        assertTrue(repository.load().isEmpty());
        assertNotNull(repository.loadWarning());
        assertArrayEquals(damaged, Files.readAllBytes(file));
    }
    @Test void failedSaveOrClearDoesNotChangeInMemoryRanking() throws Exception {
        Path blockedParent = directory.resolve("file");
        // 디렉터리 자리에 일반 파일을 두어 OS 권한 설정 없이도 쓰기 실패를 재현한다.
        Files.writeString(blockedParent, "not a directory");
        ScoreBoardService service = new ScoreBoardService(new ScoreRepository(blockedParent.resolve("scores.tsv")));
        assertThrows(UncheckedIOException.class, () -> service.register("player", 42));
        assertTrue(service.top().isEmpty());
        ScoreRepository failing = new ScoreRepository(directory.resolve("other")) {
            @Override public List<ScoreEntry> load() { return List.of(new ScoreEntry("old", 50)); }
            @Override public void clear() { throw new UncheckedIOException(new java.io.IOException("denied")); }
        };
        ScoreBoardService preserved = new ScoreBoardService(failing);
        assertThrows(UncheckedIOException.class, preserved::reset);
        assertEquals(50, preserved.highScore());
    }
    @Test void resetClearsTheDamagedFileAndItsLoadWarning() throws Exception {
        Path file = directory.resolve("damaged-scores.tsv");
        Files.writeString(file, "invalid header");
        ScoreBoardService service = new ScoreBoardService(new ScoreRepository(file));
        assertNotNull(service.loadWarning());
        service.reset();
        assertNull(service.loadWarning()); assertTrue(service.top().isEmpty());
        assertEquals(0, service.highScore()); assertFalse(Files.exists(file));
    }

    @Test void validatesNamesAndScores() {
        assertThrows(NullPointerException.class, () -> new ScoreEntry(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ScoreEntry("   ", 0));
        assertThrows(IllegalArgumentException.class, () -> new ScoreEntry("a\tb", 0));
        assertThrows(IllegalArgumentException.class, () -> new ScoreEntry("a".repeat(21), 0));
        assertThrows(IllegalArgumentException.class, () -> new ScoreEntry("a", -1));
        assertEquals(20, new ScoreEntry("😀".repeat(20), Long.MAX_VALUE).name().codePointCount(0, 40));
    }
}
