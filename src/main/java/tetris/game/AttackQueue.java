package tetris.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 상대에게서 받아 아직 보드에 추가하지 않은 공격 줄. 최대 10줄까지 보관한다.
 * 상대 플레이어의 스레드가 추가하고 내 스레드가 꺼내므로 모든 메서드를 동기화한다.
 */
public final class AttackQueue {
    public static final int MAX_LINES = 10;

    private final List<AttackLine> lines = new ArrayList<>();

    /**
     * 새 공격 줄을 기존 줄의 아래쪽에 추가한다.
     * 이미 10줄이면 무시하고, 합쳐서 10줄을 넘으면 새 줄의 아래쪽 부분을 잘라낸다.
     *
     * @return 실제로 추가된 줄 수
     */
    public synchronized int offer(List<AttackLine> incoming) {
        Objects.requireNonNull(incoming, "추가할 공격 줄 목록이 필요합니다.");
        incoming.forEach(line -> Objects.requireNonNull(line, "공격 줄이 필요합니다."));
        int accepted = Math.min(incoming.size(), MAX_LINES - lines.size());
        if (accepted <= 0) return 0;
        lines.addAll(incoming.subList(0, accepted));
        return accepted;
    }

    /** 대기 중인 줄을 위에서 아래 순서로 모두 꺼내고 비운다. */
    public synchronized List<AttackLine> drain() {
        List<AttackLine> drained = List.copyOf(lines);
        lines.clear();
        return drained;
    }

    /** 대기 중인 줄의 사본. 위에서 아래 순서이다. */
    public synchronized List<AttackLine> pending() {
        return List.copyOf(lines);
    }

    public synchronized int size() {
        return lines.size();
    }

    public synchronized void clear() {
        lines.clear();
    }
}
