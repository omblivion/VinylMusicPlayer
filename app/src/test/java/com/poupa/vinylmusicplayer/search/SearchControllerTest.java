package com.poupa.vinylmusicplayer.search;

import static org.junit.Assert.*;

import android.os.Handler;
import android.os.Looper;

import com.poupa.vinylmusicplayer.App;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class SearchControllerTest {
    static class ManualWorker extends AbstractExecutorService {
        final List<Runnable> tasks = new ArrayList<>();
        boolean shutdown;
        @Override public void execute(Runnable task) { tasks.add(task); }
        void runAll() {
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            pending.forEach(Runnable::run);
        }
        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            return pending;
        }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown; }
        @Override public boolean awaitTermination(long n, TimeUnit unit) { return shutdown; }
    }

    static class Results implements SearchController.Listener {
        List<Object> results = List.of();
        int deliveries, pending, errors;
        @Override public void onPending() { pending++; }
        @Override public void onResults(List<Object> value) { results = value; deliveries++; }
        @Override public void onError() { errors++; }
    }

    ManualWorker worker;
    Results results;
    SearchRepositoryTest.Library library;
    SearchController controller;

    @Before public void setUp() {
        worker = new ManualWorker();
        results = new Results();
        library = new SearchRepositoryTest.Library();
        controller = new SearchController(new SearchRepository(library),
                new Handler(Looper.getMainLooper()), worker, results);
    }

    static void advance(long millis) {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS);
    }

    @Test public void debouncesForExactly150ms() {
        controller.submit("caf");
        advance(100);
        controller.submit("dancing");
        advance(149);
        assertEquals(0, worker.tasks.size());
        advance(1);
        assertEquals(1, worker.tasks.size());
        worker.runAll();
        advance(0);
        assertEquals(1, results.deliveries);
        assertEquals(1, results.results.size());
        controller.close();
    }

    @Test public void ignoresOutOfOrderCompletion() {
        controller.submit("cafe");
        advance(150);
        worker.runAll(); // Old successful callback is posted but not yet delivered.
        controller.submit("missing");
        advance(150);
        worker.runAll();
        advance(0);
        assertEquals(1, results.deliveries);
        assertTrue(results.results.isEmpty());
        assertEquals(0, results.errors);
        controller.close();
    }

    @Test public void boundsWorkDuringTypingAndImportBurst() {
        controller.submit("cafe");
        for (int i = 0; i < 1000; i++) controller.refresh();
        advance(200);
        assertEquals(1, worker.tasks.size());
        worker.runAll();
        advance(0);
        assertEquals(2, results.results.size());
        assertEquals(1, library.captures);
        controller.close();
    }

    @Test public void clearsImmediatelyForWhitespace() {
        controller.submit("cafe");
        advance(150);
        controller.submit("  ");
        assertTrue(results.results.isEmpty());
        worker.runAll();
        advance(0);
        assertEquals(1, results.deliveries);
        assertEquals(0, library.captures);
        controller.close();
    }

    @Test public void refreshesCurrentQueryAfterLibraryChange() {
        controller.submit("cafe");
        advance(150);
        worker.runAll();
        advance(0);
        assertEquals(1, library.captures);
        library.revision++;
        controller.refresh();
        advance(200);
        worker.runAll();
        advance(0);
        assertEquals(2, library.captures);
        assertEquals(2, results.deliveries);
        controller.close();
    }

    @Test public void closesWithoutCallbacks() {
        controller.submit("cafe");
        advance(150);
        worker.runAll();
        controller.close();
        advance(0);
        controller.submit("dancing");
        advance(300);
        assertEquals(0, results.deliveries);
        assertTrue(worker.shutdown);
    }
}
