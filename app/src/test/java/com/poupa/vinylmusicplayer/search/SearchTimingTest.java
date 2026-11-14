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
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class SearchTimingTest {
    static class Samples implements SearchController.Listener {
        final List<SearchTimings.Sample> samples = new ArrayList<>();
        final int[] errors = new int[1];
        @Override public void onPending() {}
        @Override public void onResults(List<Object> results) {}
        @Override public void onError() { errors[0]++; }
        @Override public void onTiming(SearchTimings.Sample sample) { samples.add(sample); }
    }

    SearchControllerTest.ManualWorker worker;
    Samples samples;
    SearchRepositoryTest.Library library;
    SearchController controller;

    @Before public void setUp() {
        SearchTimings.setEnabledForTests(true);
        worker = new SearchControllerTest.ManualWorker();
        samples = new Samples();
        library = new SearchRepositoryTest.Library();
        controller = new SearchController(new SearchRepository(library),
                new Handler(Looper.getMainLooper()), worker, samples);
    }

    static void advance(long millis) {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS);
    }

    @Test public void reportsCacheMissThenHitWithoutRebuilding() {
        controller.submit("cafe");
        advance(150);
        worker.runAll();
        advance(0);
        assertEquals(1, samples.samples.size());
        assertFalse(samples.samples.get(0).snapshotReused);

        controller.submit("dancing");
        advance(150);
        worker.runAll();
        advance(0);
        assertEquals(2, samples.samples.size());
        assertTrue(samples.samples.get(1).snapshotReused);
        assertEquals(1, library.captures);
        controller.close();
    }

    @Test public void canceledRequestEmitsNoSampleAndSamplesCarryNoLibraryText() {
        controller.submit("cafe");
        advance(150);
        worker.runAll();
        controller.submit("dancing");
        advance(150);
        worker.runAll();
        advance(0);
        assertEquals(1, samples.samples.size());
        SearchTimings.Sample sample = samples.samples.get(0);
        assertTrue(sample.elapsedNanos >= 0);
        assertTrue(sample.queueNanos >= 0);
        assertTrue(sample.snapshotNanos >= 0);
        assertTrue(sample.matchNanos >= 0);
        assertFalse(sample.toString().contains("cafe"));
        controller.close();
    }

    @Test public void failedPreparationReportsErrorWithoutTimingSample() {
        SearchRepository broken = new SearchRepository(library) {
            @Override public SearchSnapshot getSnapshot(java.util.function.BooleanSupplier cancelled) {
                throw new IllegalStateException("synthetic failure");
            }
        };
        SearchController.Listener listener = new Samples();
        SearchController failing = new SearchController(broken,
                new Handler(Looper.getMainLooper()), worker, listener);
        failing.submit("cafe");
        advance(150);
        worker.runAll();
        advance(0);
        assertEquals(1, ((Samples) listener).errors[0]);
        assertTrue(((Samples) listener).samples.isEmpty());
        failing.close();
    }
}
