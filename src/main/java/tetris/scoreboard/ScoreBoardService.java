package tetris.scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 상위 10개 유지. 동점은 먼저 등록된 기록을 우선한다. */
public class ScoreBoardService {
    public static final int CAPACITY = 10;
    private final ScoreRepository repository;
    private List<ScoreEntry> entries;
    public ScoreBoardService() { this(new ScoreRepository()); }
    public ScoreBoardService(ScoreRepository repository) {
        this.repository = Objects.requireNonNull(repository);
        entries = ranked(repository.load());
    }
    // 정원이 차면 마지막 기록보다 높은 점수만 등록한다.
    public boolean qualifies(long score) {
        if (score < 0) throw new IllegalArgumentException("score must be nonnegative");
        return entries.size() < CAPACITY || score > entries.getLast().score();
    }
    public void register(String name, long score) {
        ScoreEntry entry = new ScoreEntry(name, score);
        if (!qualifies(score)) return;
        List<ScoreEntry> next = new ArrayList<>(entries);
        next.add(entry);
        next = ranked(next);
        // 저장 실패 시 화면의 랭킹과 실제 파일이 어긋나지 않도록 메모리는 성공 후에 교체한다.
        repository.save(next);
        entries = next;
    }

    public List<ScoreEntry> top() { return entries; }
    public long highScore() { return entries.isEmpty() ? 0 : entries.getFirst().score(); }
    public String loadWarning() { return repository.loadWarning(); }
    public void reset() {
        // 파일 삭제 실패 시 메모리에 남아 있는 기록도 유지한다.
        repository.clear();
        entries = List.of();
    }
    /** 순서 있는 스트림의 안정 정렬로 동점 기록의 기존 등록 순서를 보존한다. */
    private static List<ScoreEntry> ranked(List<ScoreEntry> records) {
        return records.stream().sorted(Comparator.comparingLong(ScoreEntry::score).reversed())
                .limit(CAPACITY).toList();
    }
}
