# 🍎 Apple Math Puzzle Game
### A Java Swing Desktop Application

---

## Overview
A polished, apple-themed math puzzle game built in Java Swing.
Features two game modes, 5 progressive levels, and a math fun fact after every level completion.

---

## Features

### 🃏 Memory Match Mode (5 Levels)
Match pairs of equivalent math expressions — e.g. `3×3` pairs with `√81`.

| Level | Grid  | Pairs | Time  | Hints | Rank         |
|-------|-------|-------|-------|-------|--------------|
| 1     | 4×4   | 8     | 90s   | 4     | Seedling     |
| 2     | 4×4   | 8     | 65s   | 3     | Sapling      |
| 3     | 6×6   | 12    | 100s  | 3     | Orchard      |
| 4     | 6×6   | 16    | 80s   | 2     | Harvest      |
| 5     | 8×8   | 16    | 65s   | 1     | Cider Master |

- 🃏 Cards have smooth 3D flip animations
- 💡 Hint system briefly reveals a matching pair
- ⭐ Score = 10 per match + time bonus for speed
- 🔒 Levels unlock in order
- 🍏 **Math Fun Fact shown after each level!**

### 🧮 Equation Challenge Mode
Find the missing number in math equations.

- Fetches real puzzles from the Banana/Tomato API (requires internet)
- Falls back to built-in 🍎 apple equations if offline
- 🔥 Streak multiplier — consecutive correct answers earn bonus points
- ⏱ 60-second timed rounds
- 🍏 **Math Fun Fact shown every 3 correct answers!**

### 🏆 Leaderboard
- Tracks top 10 scores across both game modes this session
- Highlights your own scores

---

## Requirements
- Java JDK 11 or higher
- No external libraries needed

---

## How to Run

### Windows
```
run.bat
```

### macOS / Linux
```bash
chmod +x run.sh
./run.sh
```

### Manual
```bash
mkdir out
javac -sourcepath src -d out src/applegame/*.java
java -cp out applegame.Main
```

---

## Math Fun Facts Included
1. Zero Hero — How zero was invented
2. Pi Never Ends — 100 trillion digits and counting
3. Golden Ratio — φ in nature and apples
4. Birthday Paradox — 23 people, 50% chance
5. Fibonacci Apples — Apple seeds and spirals
6. Googol — 10¹⁰⁰ and the Google name origin
7. Gauss's Trick — Summing 1–100 instantly
8. Prime Infinity — Euclid's 2300-year-old proof

---

## Project Structure
```
AppleMathGame/
├── src/applegame/
│   ├── Main.java               — Entry point
│   ├── GameState.java          — Session data, scores, facts
│   ├── Theme.java              — Colors, fonts, paint helpers
│   ├── MainWindow.java         — Root JFrame, screen navigation
│   ├── HomeScreen.java         — Animated splash with name entry
│   ├── LevelSelectScreen.java  — 5-level grid with lock/unlock
│   ├── MemoryCard.java         — Card data model
│   ├── CardPanel.java          — Card with 3D flip animation
│   ├── MemoryGameScreen.java   — Full memory matching game
│   ├── EquationGameScreen.java — Equation challenge (API + fallback)
│   └── LeaderboardScreen.java  — Top 10 scores display
├── run.bat   — Windows build & run
├── run.sh    — macOS/Linux build & run
└── README.md
```
