package com.poupa.vinylmusicplayer.ui.activities.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.poupa.vinylmusicplayer.App;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class BackNavigationTest {
    public static class TestActivity extends AbsThemeActivity {
        int backEvents;
        boolean consumeBack;

        protected boolean handleBackPressed() {
            backEvents++;
            return consumeBack;
        }
    }

    @Test
    public void systemBackUsesScreenHandlerBeforeFinishing() {
        try (var controller = Robolectric.buildActivity(TestActivity.class).setup()) {
            TestActivity activity = controller.get();
            activity.consumeBack = true;
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertEquals(1, activity.backEvents);
            assertFalse(activity.isFinishing());
        }
    }

    @Test
    public void unhandledBackFallsThroughOnce() {
        try (var controller = Robolectric.buildActivity(TestActivity.class).setup()) {
            TestActivity activity = controller.get();
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertEquals(1, activity.backEvents);
            assertTrue(activity.isFinishing());
        }
    }
}
