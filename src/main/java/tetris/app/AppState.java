package tetris.app;

/** 화면 전환 상태. 게임의 진행·일시정지 여부는 별도의 GamePhase로 구분한다. */
public enum AppState {
    START_MENU,
    GAME,
    GAME_MENU,
    GAME_OVER,
    SETTINGS,
    SCOREBOARD,
    EXIT;

    /**
     * 엔진 조회 사본과 앱 정보를 한 번에 전달한다. settings는 설정 서비스의 조회 객체이다.
     * lastRegisteredScore와 settings는 미제공 시 null, persistenceError는 오류가 없으면 null이다.
     * awaitingScoreName은 랭킹 등록을 기다리는지, lastRegisteredScore는 UI가 강조할 기록을 나타낸다.
     */
    public record Snapshot(AppState screen, tetris.game.GameState game,
            long gravityIntervalMillis, long highScore, boolean awaitingScoreName,
            tetris.scoreboard.ScoreEntry lastRegisteredScore,
            tetris.settings.GameSettings settings, String persistenceError) { }
}
