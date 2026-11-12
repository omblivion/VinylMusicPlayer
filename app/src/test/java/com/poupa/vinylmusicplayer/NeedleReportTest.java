package com.poupa.vinylmusicplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import com.poupa.vinylmusicplayer.ui.activities.bugreport.BugReportActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class NeedleReportTest {
    @Test
    public void reportUsesUserChosenDestinationInsteadOfVinylIssueTracker() {
        Intent launch = new Intent(Intent.ACTION_MAIN).putExtra(Intent.EXTRA_TEXT, "Needle test crash");
        try (var controller = Robolectric.buildActivity(BugReportActivity.class, launch).setup()) {
            BugReportActivity activity = controller.get();
            activity.findViewById(R.id.button_send).performClick();
            Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
            Intent report = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            assertEquals(Intent.ACTION_SEND, report.getAction());
            assertEquals("text/plain", report.getType());
            assertTrue(report.getStringExtra(Intent.EXTRA_TEXT).contains("Needle test crash"));
        }
    }
}
