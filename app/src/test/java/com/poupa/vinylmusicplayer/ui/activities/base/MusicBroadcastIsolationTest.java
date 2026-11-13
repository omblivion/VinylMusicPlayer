package com.poupa.vinylmusicplayer.ui.activities.base;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class MusicBroadcastIsolationTest {
    @Test
    public void permissionChangeIsDeliveredOnlyToThisInstallation() {
        MusicPermissionsTest.TestActivity activity = Robolectric.buildActivity(MusicPermissionsTest.TestActivity.class).get();
        activity.onHasPermissionsChanged(true);

        Intent broadcast = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().get(0);
        assertEquals(activity.getPackageName(), broadcast.getPackage());
        assertEquals(activity.getPackageName() + ".mediastorechanged", broadcast.getAction());
    }
}
