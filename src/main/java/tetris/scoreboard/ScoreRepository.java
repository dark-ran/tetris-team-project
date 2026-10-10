package tetris.scoreboard;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 사용자 홈의 버전 있는 UTF-8 파일. 저장 실패는 호출자에게 전달한다. */
public class ScoreRepository {
    // 랭킹 파일인지 확인하기 위한 첫 줄의 표식이다.
    private static final String HEADER = "TETRIS-SCORES-1";
    private static final int MAX_ATTEMPTS = 3;
    private final Path path;
    private String loadWarning;
    public ScoreRepository() {
        // 설치 폴더 쓰기 권한과 관계없이 사용자별로 기록을 분리한다.
        this(Path.of(System.getProperty("user.home"), ".tetris-team-project", "scores.tsv"));
    }
    public ScoreRepository(Path path) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    }
    public Path path() { return path; }
    public String loadWarning() { return loadWarning; }
    /** 파일이 없거나 읽을 수 없으면 빈 목록을 반환하고, 읽기 문제는 loadWarning에 남긴다. */
    public List<ScoreEntry> load() {
        loadWarning = null;
        if (Files.notExists(path)) return List.of();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.isEmpty() || !HEADER.equals(lines.getFirst()))
                throw new IllegalArgumentException("unsupported score file header");
            List<ScoreEntry> entries = new ArrayList<>();
            for (String line : lines.subList(1, lines.size())) {
                try {
                    String[] fields = line.split("\t", -1);
                    if (fields.length != 2) throw new IllegalArgumentException("invalid row");
                    entries.add(new ScoreEntry(fields[0], Long.parseLong(fields[1])));
                } catch (IllegalArgumentException ex) {
                    loadWarning = "Some score rows were damaged and skipped";
                }
            }
            return List.copyOf(entries);
        } catch (IOException | IllegalArgumentException ex) {
            loadWarning = "Could not load scores: " + ex.getMessage();
            return List.of();
        }
    }
    // 원자적 교체만 허용하고 일시적인 I/O 실패는 최초 시도를 포함해 최대 세 번 시도한다.
    public void save(List<ScoreEntry> entries) {
        List<ScoreEntry> copy = List.copyOf(entries);
        StringBuilder data = new StringBuilder(HEADER).append('\n');
        // 이름에 탭·줄바꿈을 허용하지 않으므로 이름과 점수를 탭으로 직접 구분할 수 있다.
        for (ScoreEntry entry : copy) {
            data.append(entry.name()).append('\t').append(entry.score()).append('\n');
        }
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                saveOnce(bytes);
                return;
            } catch (AtomicMoveNotSupportedException ex) {
                // 원자적 교체가 불가능하면 기존 파일을 보호하고 일반 교체로 우회하지 않는다.
                throw new UncheckedIOException("Atomic score replacement is not supported", ex);
            } catch (IOException ex) {
                if (attempt == MAX_ATTEMPTS)
                    throw new UncheckedIOException("Could not save scores after " + MAX_ATTEMPTS + " attempts", ex);
            }
        }
    }
    private void saveOnce(byte[] bytes) throws IOException {
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "scores-", ".tmp");
            // 임시 파일의 내용을 디스크에 반영하고, 파일을 닫은 뒤 원자적으로 교체한다.
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            replaceAtomically(temporary);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* 정리 실패로 원래 저장 오류를 가리지 않는다. */ }
            }
        }
    }
    /** 파일 원자적으로 교체 */
    protected void replaceAtomically(Path temporary) throws IOException {
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    /**최대 3번(MAX_ATTEMPTS)까지 시도 */
    public void clear() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try { Files.deleteIfExists(path); loadWarning = null; return; }
            catch (IOException ex) {
                if (attempt == MAX_ATTEMPTS)
                    throw new UncheckedIOException("Could not clear scores after " + MAX_ATTEMPTS + " attempts", ex);
            }
        }
    }
}
