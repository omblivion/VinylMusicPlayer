package com.poupa.vinylmusicplayer.ui.activities.base;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.FrameLayout;

import com.poupa.vinylmusicplayer.App;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class PermissionRecoveryUiTest {
    public static class TestActivity extends AbsBaseActivity {
        @Override protected void onCreate(Bundle state) {
            super.onCreate(state);
            setContentView(new FrameLayout(this));
        }

        @Override protected String[] getPermissionsToRequest() {
            return new String[] { Manifest.permission.READ_MEDIA_AUDIO };
        }
    }

    @Test
    public void grantingAudioAccessRemovesOldDenialMessage() {
        try (var controller = Robolectric.buildActivity(TestActivity.class).setup()) {
            TestActivity activity = controller.get();
            String[] permissions = { Manifest.permission.READ_MEDIA_AUDIO };
            activity.onRequestPermissionsResult(AbsBaseActivity.PERMISSION_REQUEST, permissions,
                    new int[] { PackageManager.PERMISSION_DENIED });
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
            assertNotNull(activity.findViewById(com.google.android.material.R.id.snackbar_text));

            Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(permissions);
            activity.onRequestPermissionsResult(AbsBaseActivity.PERMISSION_REQUEST, permissions,
                    new int[] { PackageManager.PERMISSION_GRANTED });
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
            assertNull(activity.findViewById(com.google.android.material.R.id.snackbar_text));
        }
    }
}
