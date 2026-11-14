package com.poupa.vinylmusicplayer.search;

import android.os.Handler;
import android.os.SystemClock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BooleanSupplier;

/** Main-thread input/state, dedicated worker computation, latest generation alone may render. */
public final class SearchController implements AutoCloseable {
    public interface Listener {
        void onPending();
        void onResults(List<Object> results);
        void onError();
        default void onTiming(SearchTimings.Sample sample) {}
    }

    private final SearchRepository repository;
    private final Handler main;
    private final ExecutorService worker;
    private final Listener listener;
    private volatile long generation;
    private volatile boolean closed;
    private Future<?> running;
    private String query = "";
    private boolean refreshScheduled;

    public SearchController(SearchRepository repository, Handler mainHandler,
            ExecutorService worker, Listener listener) {
        this.repository = repository;
        this.main = mainHandler;
        this.worker = worker;
        this.listener = listener;
    }

    public void submit(String text) {
        if (closed) return;
        query = text == null ? "" : text;
        cancelWork();
        long request = ++generation;
        if (query.trim().isEmpty()) {
            listener.onResults(Collections.emptyList());
            return;
        }
        listener.onPending();
        String submitted = query;
        post(() -> launch(request, submitted), 150);
    }

    /** Clears rendered results without changing the current query text. */
    public void clear() {
        if (closed) return;
        cancelWork();
        generation++;
        listener.onResults(Collections.emptyList());
    }

    /** Fixed-window coalescing: continuous imports cannot postpone all updates indefinitely. */
    public void refresh() {
        if (closed || refreshScheduled || query.trim().isEmpty()) return;
        cancelWork();
        long request = ++generation;
        refreshScheduled = true;
        listener.onPending();
        String submitted = query;
        post(() -> {
            refreshScheduled = false;
            launch(request, submitted);
        }, 200);
    }

    private void launch(long request, String submitted) {
        if (!current(request)) return;
        BooleanSupplier cancelled = () -> !current(request);
        long queued = SearchTimings.now();
        running = worker.submit(() -> {
            try {
                long snapshotStart = SearchTimings.now();
                boolean hit = repository.isSnapshotWarm();
                SearchSnapshot snapshot = repository.getSnapshot(cancelled);
                long snapshotEnd = SearchTimings.now();
                List<Object> results = snapshot.search(submitted, cancelled);
                long matchEnd = SearchTimings.now();
                post(() -> {
                    if (current(request)) {
                        listener.onResults(results);
                        SearchTimings.report(listener, request, hit, queued, snapshotStart,
                                snapshotEnd - snapshotStart, matchEnd - snapshotEnd);
                    }
                }, 0);
            } catch (CancellationException ignored) {
                // Superseded work has no UI/error side effects.
            } catch (RuntimeException error) {
                post(() -> {
                    if (current(request)) listener.onError();
                }, 0);
            }
        });
    }

    private boolean current(long request) {
        return !closed && generation == request;
    }

    private void post(Runnable action, long delayMs) {
        main.postAtTime(action, this, SystemClock.uptimeMillis() + delayMs);
    }

    private void cancelWork() {
        main.removeCallbacksAndMessages(this);
        refreshScheduled = false;
        if (running != null) running.cancel(true);
        // Remove canceled queued FutureTasks instead of leaving an ever-growing input backlog.
        if (worker instanceof ThreadPoolExecutor) ((ThreadPoolExecutor) worker).purge();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        generation++;
        cancelWork();
        worker.shutdownNow();
    }
}
