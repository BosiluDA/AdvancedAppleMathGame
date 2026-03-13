package applegame;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/**
 * Dedicated loader for the Banana / Tomato puzzle image APIs.
 *
 * Usage:
 *   QuestionLoader.fetch(callback)   — async, calls callback on EDT when done
 *   QuestionLoader.prefetch()        — warms up the next question in background
 *
 * Result is always non-null: on total failure it returns an ApiUnavailableResult
 * so the caller never has to null-check.
 */
public class QuestionLoader {

    // ── API endpoints (tried in order) ───────────────────────────────────────
    private static final String[] ENDPOINTS = {
        "https://marcconrad.com/uob/tomato/api.php?out=csv&base64=yes",
        "https://marcconrad.com/uob/banana/api.php?out=csv&base64=yes",
    };

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS    = 6_000;
    private static final int MAX_RETRIES        = 2;

    // ── Pre-fetch cache: one question buffered ahead ─────────────────────────
    private static volatile Future<QuestionResult> prefetchFuture = null;
    private static final ExecutorService pool =
        Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "QuestionLoader");
            t.setDaemon(true);
            return t;
        });

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Asynchronously fetch a question and deliver it on the Swing EDT.
     * If a prefetched result is ready it is used immediately (fast path).
     */
    public static void fetch(java.util.function.Consumer<QuestionResult> callback) {
        // Fast path: prefetch already done
        Future<QuestionResult> cached = prefetchFuture;
        if (cached != null && cached.isDone()) {
            prefetchFuture = null;
            pool.submit(() -> {
                try {
                    QuestionResult r = cached.get();
                    javax.swing.SwingUtilities.invokeLater(() -> callback.accept(r));
                } catch (Exception e) {
                    javax.swing.SwingUtilities.invokeLater(
                        () -> callback.accept(new QuestionResult(null, -1, LoadStatus.API_ERROR,
                            "Prefetch error: " + e.getMessage())));
                }
            });
            return;
        }

        // Normal path: fetch now
        pool.submit(() -> {
            QuestionResult result = doFetch();
            javax.swing.SwingUtilities.invokeLater(() -> callback.accept(result));
        });
    }

    /**
     * Kick off a background prefetch so the next question is ready instantly.
     * Call this right after a question is displayed.
     */
    public static void prefetch() {
        prefetchFuture = pool.submit(QuestionLoader::doFetch);
    }

    /** Cancel any pending prefetch (e.g. on game end). */
    public static void cancelPrefetch() {
        Future<QuestionResult> f = prefetchFuture;
        if (f != null) f.cancel(true);
        prefetchFuture = null;
    }

    // ── Internal fetch logic ──────────────────────────────────────────────────

    private static QuestionResult doFetch() {
        for (String endpoint : ENDPOINTS) {
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    QuestionResult r = fetchFromEndpoint(endpoint);
                    if (r != null) return r;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new QuestionResult(null, -1, LoadStatus.API_ERROR, "Interrupted");
                } catch (Exception e) {
                    System.out.printf("[QuestionLoader] %s attempt %d failed: %s%n",
                        endpoint, attempt, e.getMessage());
                    if (attempt < MAX_RETRIES) {
                        try { Thread.sleep(400); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }
        // All endpoints failed
        return new QuestionResult(null, -1, LoadStatus.API_UNAVAILABLE,
            "All API endpoints unreachable");
    }

    private static QuestionResult fetchFromEndpoint(String urlStr) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "AppleMathGame/1.0");
        conn.setRequestProperty("Accept", "text/plain");

        int httpCode = conn.getResponseCode();
        if (httpCode != 200) {
            throw new IOException("HTTP " + httpCode + " from " + urlStr);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line = br.readLine();
            if (line == null || line.isBlank()) {
                throw new IOException("Empty response from " + urlStr);
            }
            return parseResponse(line.trim(), urlStr);
        } finally {
            conn.disconnect();
        }
    }

    private static QuestionResult parseResponse(String csv, String source) throws Exception {
        // Expected format: <base64_image>,<answer_digit>
        int commaIdx = csv.lastIndexOf(',');
        if (commaIdx < 0) throw new IOException("Unexpected CSV format: " + csv);

        String b64  = csv.substring(0, commaIdx).trim();
        String ansStr = csv.substring(commaIdx + 1).trim();

        int answer = Integer.parseInt(ansStr);
        if (answer < 0 || answer > 9) {
            throw new IOException("Answer out of range 0-9: " + answer);
        }

        byte[] imgBytes = Base64.getDecoder().decode(b64);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
        if (img == null) throw new IOException("Could not decode image from " + source);

        String apiName = source.contains("tomato") ? "Tomato API" : "Banana API";
        return new QuestionResult(img, answer, LoadStatus.OK, apiName);
    }

    // ── Result model ──────────────────────────────────────────────────────────

    public enum LoadStatus { OK, API_ERROR, API_UNAVAILABLE }

    public static class QuestionResult {
        public final BufferedImage image;   // null if status != OK
        public final int answer;            // -1 if status != OK
        public final LoadStatus status;
        public final String detail;         // API name on success, error msg on failure

        public QuestionResult(BufferedImage image, int answer,
                              LoadStatus status, String detail) {
            this.image  = image;
            this.answer = answer;
            this.status = status;
            this.detail = detail;
        }

        public boolean isOk() { return status == LoadStatus.OK; }
    }
}
