package tetris.scoreboard;

import java.util.Objects;

/** 불변 기록. 이름은 공백을 제거한 1~20개 Unicode 코드 포인트. */
public record ScoreEntry(String name, long score) {
    public ScoreEntry {
        name = Objects.requireNonNull(name, "name").strip();
        // emoji처럼 UTF-16 두 칸으로 표현되는 문자도 한 글자로 세고, 파일 구분용 제어 문자는 금지한다.
        if (name.isEmpty() || name.codePointCount(0, name.length()) > 20
                || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("name must contain 1 to 20 characters without controls");
        }
        if (score < 0) throw new IllegalArgumentException("score must be nonnegative");
    }
}
