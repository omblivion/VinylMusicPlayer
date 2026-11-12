package com.poupa.vinylmusicplayer.ui.activities.base;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class MusicPermissionsTest {
    public static class TestActivity extends AbsMusicServiceActivity {}

    private TestActivity activity() {
        // Attach without onCreate: these tests exercise permission policy, not playback/DB setup.
        TestActivity activity = Robolectric.buildActivity(TestActivity.class).get();
        ReflectionHelpers.setField(activity, "permissions", activity.getPermissionsToRequest());
        return activity;
    }

    @Test
    public void audioAccessIsEnoughWhenImagesAndNotificationsAreDenied() {
        Application application = RuntimeEnvironment.getApplication();
        org.robolectric.Shadows.shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO);
        org.robolectric.Shadows.shadowOf(application).denyPermissions(
                Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS);

        assertTrue(activity().hasPermissions());
    }

    @Test
    public void revokedAudioAccessIsDetected() {
        Application application = RuntimeEnvironment.getApplication();
        org.robolectric.Shadows.shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO);
        TestActivity activity = activity();
        assertTrue(activity.hasPermissions());

        org.robolectric.Shadows.shadowOf(application).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO);
        assertFalse(activity.hasPermissions());
    }

    @Test
    public void emptyPermissionResultDoesNotReportAccessGranted() {
        TestActivity activity = activity();
        activity.onRequestPermissionsResult(AbsBaseActivity.PERMISSION_REQUEST, new String[0], new int[0]);
        assertFalse(ReflectionHelpers.getField(activity, "hadPermissions"));
    }
}
