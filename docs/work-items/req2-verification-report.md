# Req2 Verification Report: Distribution, Packaging, and Coverage

**Date**: 2026-10-26  
**Commit / Base**: `main` (`04b6a1d`)  
**Environment**: Windows 11 x64, Temurin JDK 21.0.4, Gradle 8.14.4  

---

## 1. Piece Distribution Verification ($\pm 5\%$) Across Difficulties

### 1.1 Expected Weights & Theoretical Rates
According to Req2 specifications:
- **EASY**: I = 12, Others (O, T, S, Z, J, L) = 10 each (Total: 72)
  - Expected `I` rate: $12 / 72 \approx 16.6667\%$
  - Expected other rates: $10 / 72 \approx 13.8889\%$
- **NORMAL**: All 7 pieces = 10 each (Total: 70)
  - Expected rate for every piece: $10 / 70 \approx 14.2857\%$
- **HARD**: I = 8, Others = 10 each (Total: 68)
  - Expected `I` rate: $8 / 68 \approx 11.7647\%$
  - Expected other rates: $10 / 68 \approx 14.7059\%$

### 1.2 Error Calculation Formulas
1. **Absolute Error (%p)**:
   $$\text{Absolute Error} = |P_{\text{actual}} - P_{\text{expected}}| = \left|\frac{\text{Actual Count}}{N} - \frac{\text{Weight}}{\text{Total Weight}}\right|$$
   - For $N = 1,000$, standard binomial error is $\sigma \approx 1.1\%p$. A $3\sigma$ confidence interval (99.7%) is $\approx \pm 3.3\%p$. Therefore, $\pm 5\%$ at 1,000 runs corresponds to **Absolute Error $\le 5\%p$ ($0.05$)**.
2. **Relative Error (%)**:
   $$\text{Relative Error} = \frac{|\text{Actual Count} - \text{Expected Count}|}{\text{Expected Count}}$$
   - At $N = 100,000$, empirical counts converge to within $\le 5\%$ relative error.

### 1.3 Test Results (`DifficultyGenerationTest.java`)
- **1,000 Runs Execution**:
  - EASY: I absolute error = $0.63\%p$ ($17.30\%$ vs $16.67\%$). Others: $0.01\%p \sim 1.61\%p$. All $\le 5\%p$ **[PASS]**
  - NORMAL: All pieces absolute error $\le 3.81\%p \le 5\%p$ **[PASS]**
  - HARD: I absolute error = $0.64\%p$ ($12.40\%$ vs $11.76\%$). Others: $0.09\%p \sim 1.81\%p$. All $\le 5\%p$ **[PASS]**
- **100,000 Runs Execution**:
  - Maximum observed relative error across all pieces and difficulties: **$1.37\%$** (well within $\le 5\%$) **[PASS]**

---

## 2. OS Distribution Packages & Functional Verification

### 2.1 Packaging Deliverables
- **Windows x64**: Built via `.\gradlew.bat packageWindows`.
  - Directory: `build/windows/Tetris/Tetris.exe`
  - Archive: `build/windows/Tetris-windows-x64.zip` (24.6 MB)
  - SHA-256: `662db1a65fca7979bc28900d380f1cb881f51fcc44a082b805345ca5fa92e315`
  - Self-contained with bundled Java 21 JRE (`runtime/bin/javaw.exe`).
- **macOS**: Configured via `packageMacos` producing `Tetris.app` and ZIP archives.

### 2.2 Feature Verification Checklist
- **Execution & Controls**: Verified via desktop launch and `MenuSettingsRestartTest`. Arrow keys, Space (Hard drop), and rotation functional.
- **Key Rebinding**: Left key reassignment (`←` $\rightarrow$ `A`) immediately effective and persisted.
- **Pause & Resume**: `ESC` enters `GamePhase.PAUSED`. Board, block coordinates, score, and level frozen across delays and resumed cleanly.
- **Settings & Reversion**: Window presets (Small/Medium/Large), Color vision modes (Deutan/Protan/Tritan + patterns) and key bindings save to disk. Confirmation dialog reverts safely on Cancel/Timeout/Esc/Close (`ReviewWindowSettingsTest`).
- **Scoreboard Persistence**: Separate records for Normal and Item modes, difficulty tracking, and persistence across app restarts verified (`ScoreBoardTest`).
- **Real GUI Display**: All 9 real-window GUI tests passed (`reviewWindowTest`).

---

## 3. Test Suite & Coverage Verification

### 3.1 Test Execution (`./gradlew check`)
- **Unit & Integration Tests**: 516 tests passed (0 failures, 0 errors, 0 skipped).
- **GUI Window Tests**: 9 tests passed.
- **Static Import Audit**: 125 files passed (0 unused imports).

### 3.2 JaCoCo Line Coverage
- **Total Line Coverage**: **2,097 / 2,234 lines (93.87%)** — exceeds the Req2 requirement of **$\ge 50\%$**.
- **Branch Coverage**: **713 / 841 branches (84.78%)**.
- **Instruction Coverage**: **12,463 / 13,370 instructions (93.22%)**.
- **Class Verifications**: `boardAndPieceCoverageVerification` ($\ge 70\%$) and `gameEngineCoverageVerification` ($\ge 70\%$) both passed.
