package applegame;

import java.util.*;

public class GameState {

    // ── Identity ──────────────────────────────────────────────────────────────
    private String playerName = "Player";
    private int    userId     = -1;           // -1 = not logged in

    // ── Session scores ────────────────────────────────────────────────────────
    private int totalScore     = 0;
    private int currentLevel   = 1;
    private int equationScore  = 0;
    private int equationStreak = 0;

    // ── Persistent progress (loaded from DB on login) ─────────────────────────
    private Set<Integer> completedLevels = new HashSet<>();

    // ── In-session leaderboard (populated from DB + local) ───────────────────
    private List<DataBaseManager.ScoreEntry> leaderboard = new ArrayList<>();

    // ── Level config ──────────────────────────────────────────────────────────
    public static final int[][] LEVEL_CONFIG = {
        {4, 8,  90, 4},
        {4, 8,  65, 3},
        {6, 12,100, 3},
        {6, 16, 80, 2},
        {8, 16, 65, 1},
    };
    public static final String[] LEVEL_LABELS = {
        "Seedling", "Sapling", "Orchard", "Harvest", "Cider Master"
    };

    // ── Math fun facts ────────────────────────────────────────────────────────
    public static final String[][] MATH_FACTS = {
        {"🍏", "Zero Hero",
         "Zero wasn't always a number — ancient civilisations had no symbol for nothing. India first formalised zero around 628 AD and changed mathematics forever."},
        {"🍎", "Pi Never Ends",
         "π (pi) has been computed to over 100 trillion decimal places and never repeats. Even NASA only needs 15 digits of pi for interplanetary navigation!"},
        {"🍏", "Golden Ratio",
         "The Golden Ratio φ ≈ 1.618 appears in apple seeds, sunflower spirals, nautilus shells, and even the human face — nature is a mathematician."},
        {"🍎", "Birthday Paradox",
         "In a group of just 23 people there's a 50% chance two share a birthday. With 70 people it jumps to 99.9%. Probability loves to surprise us!"},
        {"🍏", "Fibonacci Apples",
         "Slice an apple horizontally and you'll see a perfect 5-pointed star — that's Fibonacci at work. Apple seeds arrange themselves in Fibonacci spirals too!"},
        {"🍎", "Googol",
         "A googol is 10¹⁰⁰ — a 1 followed by 100 zeros. The entire observable universe contains only about 10⁸⁰ atoms. Google named itself after this number!"},
        {"🍏", "Gauss's Trick",
         "At age 10 Carl Gauss instantly summed 1 to 100 = 5050 by pairing: (1+100)×50. This shortcut works for any consecutive sequence."},
        {"🍎", "Prime Infinity",
         "There are infinitely many prime numbers — Euclid proved this over 2300 years ago. The largest known prime (as of 2024) has over 41 million digits!"},
    };

    // ── Identity ──────────────────────────────────────────────────────────────
    public String getPlayerName()          { return playerName; }
    public void   setPlayerName(String n)  { playerName = n; }
    public int    getUserId()              { return userId; }
    public void   setUserId(int id)        { userId = id; }
    public boolean isLoggedIn()            { return userId > 0; }

    // ── Score ─────────────────────────────────────────────────────────────────
    public int  getTotalScore()          { return totalScore; }
    public void addScore(int s)          { totalScore += s; }

    // ── Level ─────────────────────────────────────────────────────────────────
    public int  getCurrentLevel()        { return currentLevel; }
    public void setCurrentLevel(int l)   { currentLevel = l; }

    public boolean isLevelCompleted(int l)  { return completedLevels.contains(l); }
    public void    markLevelCompleted(int l){ completedLevels.add(l); }
    public Set<Integer> getCompletedLevels(){ return completedLevels; }

    /** Called on login to restore DB-persisted progress. */
    public void loadCompletedLevels(Set<Integer> levels) {
        completedLevels = new HashSet<>(levels);
    }

    // ── Equation ──────────────────────────────────────────────────────────────
    public int  getEquationScore()       { return equationScore; }
    public void addEquationScore(int s)  { equationScore += s; }
    public void resetEquationScore()     { equationScore = 0; equationStreak = 0; }

    public int  getEquationStreak()      { return equationStreak; }
    public void incrementStreak()        { equationStreak++; }
    public void resetStreak()            { equationStreak = 0; }

    // ── Leaderboard (local cache) ─────────────────────────────────────────────
    public List<DataBaseManager.ScoreEntry> getLeaderboard()    { return leaderboard; }
    public void setLeaderboard(List<DataBaseManager.ScoreEntry> l) { leaderboard = l; }

    // ── Fact helper ───────────────────────────────────────────────────────────
    public String[] getFactForLevel(int level) {
        int idx = Math.max(0, level - 1) % MATH_FACTS.length;
        return MATH_FACTS[idx];
    }
}
