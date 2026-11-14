package com.poupa.vinylmusicplayer.search;

import android.os.SystemClock;

import com.poupa.vinylmusicplayer.BuildConfig;

/**
 * Aggregate, content-free search timings. Debug-only: no query text, titles, artists or paths
 * are ever captured, and release builds silently ignore samples.
 */
public final class SearchTimings {
    public static final class Sample {
        public final long generation;
        public final boolean snapshotReused;
        public final long queueNanos;
        public final long snapshotNanos;
        public final long matchNanos;
        public final long elapsedNanos;

        Sample(long generation, boolean snapshotReused, long queueNanos,
                long snapshotNanos, long matchNanos, long elapsedNanos) {
            this.generation = generation;
            this.snapshotReused = snapshotReused;
            this.queueNanos = queueNanos;
            this.snapshotNanos = snapshotNanos;
            this.matchNanos = matchNanos;
            this.elapsedNanos = elapsedNanos;
        }
    }

    private static boolean forcedForTests;

    private SearchTimings() {}

    public static boolean isEnabled() {
        return BuildConfig.DEBUG || forcedForTests;
    }

    public static void setEnabledForTests(boolean enabled) {
        forcedForTests = enabled;
    }

    public static long now() {
        return SystemClock.elapsedRealtimeNanos();
    }

    public static void report(SearchController.Listener listener, long generation, boolean snapshotReused,
            long requestedNanos, long queueStartNanos, long snapshotNanos, long matchNanos) {
        if (!isEnabled()) return;
        long completed = now();
        listener.onTiming(new Sample(generation, snapshotReused, queueStartNanos - requestedNanos,
                snapshotNanos, matchNanos, completed - requestedNanos));
    }
}
