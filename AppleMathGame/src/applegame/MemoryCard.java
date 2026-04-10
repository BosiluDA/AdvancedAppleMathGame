package applegame;

public class MemoryCard {
    // ── Apple-themed symbol set ──────────────────────────────────────────────
    // Each card has a symbol (math or apple-themed) and a color pair
    public static final String[] SYMBOLS = {
        "2+2",  "5-1",   // pair 0 — both equal 4
        "3×3",  "√81",   // pair 1 — both equal 9
        "10÷2", "5×1",   // pair 2 — both equal 5
        "8-3",  "15÷3",  // pair 3 — both equal 5  (use index for matching)
        "4²",   "16",    // pair 4 — 16
        "6+7",  "13",    // pair 5 — 13
        "3!",   "6",     // pair 6 — 6
        "7×8",  "56",    // pair 7 — 56
        "9²",   "81",    // pair 8 — 81
        "12÷4", "3",     // pair 9 — 3
        "2⁵",   "32",    // pair 10 — 32
        "11×11","121",   // pair 11 — 121
        "π≈",   "3.14",  // pair 12
        "√4",   "2",     // pair 13 — 2
        "5!",   "120",   // pair 14 — 120
        "e≈",   "2.72",  // pair 15
    };

    // Apple-green gradient variants for each pair id
    public static final int[][] COLORS = {
        {0xC0392B, 0x922B21},  // red
        {0x27AE60, 0x1E8449},  // green
        {0xF39C12, 0xD68910},  // gold
        {0x8E44AD, 0x6C3483},  // purple
        {0x2980B9, 0x1F618D},  // blue
        {0xD35400, 0xA04000},  // orange
        {0x16A085, 0x0E6655},  // teal
        {0xC0392B, 0x7B241C},  // dark red
        {0x1ABC9C, 0x148F77},  // mint
        {0xF1C40F, 0xD4AC0D},  // yellow
        {0x3498DB, 0x2471A3},  // sky
        {0xE74C3C, 0xCB4335},  // crimson
        {0x2ECC71, 0x239B56},  // lime
        {0x9B59B6, 0x7D3C98},  // violet
        {0xE67E22, 0xCA6F1E},  // amber
        {0x1ABC9C, 0x0E6655},  // cyan
    };

    private final int pairId;      // which pair this belongs to
    private final int symbolIndex; // index into SYMBOLS array
    private boolean faceUp = false;
    private boolean matched = false;
    private float flipAngle = 0f;   // for animation 0=back, 1=front
    private boolean glowMatch = false;
    private boolean glowWrong = false;

    public MemoryCard(int pairId, int symbolIndex) {
        this.pairId = pairId;
        this.symbolIndex = symbolIndex;
    }

    public int getPairId()     { return pairId; }
    public String getSymbol()  { return SYMBOLS[symbolIndex]; }
    public int[] getColors()   { return COLORS[pairId % COLORS.length]; }
    public boolean isFaceUp()  { return faceUp; }
    public boolean isMatched() { return matched; }
    public float getFlipAngle(){ return flipAngle; }
    public boolean isGlowMatch(){ return glowMatch; }
    public boolean isGlowWrong(){ return glowWrong; }

    public void setFaceUp(boolean v)   { faceUp = v; }
    public void setMatched(boolean v)  { matched = v; }
    public void setFlipAngle(float v)  { flipAngle = v; }
    public void setGlowMatch(boolean v){ glowMatch = v; }
    public void setGlowWrong(boolean v){ glowWrong = v; }
}
