package com.poupa.vinylmusicplayer.service.notification;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.app.Notification;
import android.content.pm.ServiceInfo;

import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.service.MusicService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class ForegroundNotificationTest {
    @Test
    public void startupNotificationPromotesMediaPlaybackService() {
        MusicService service = Robolectric.buildService(MusicService.class).get();
        IdleNotification notification = new IdleNotification();
        notification.init(service, IdleNotification.NOTIFICATION_CHANNEL_ID,
                R.string.idle_notification_name, R.string.idle_notification_description);
        notification.update();

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, service.getForegroundServiceType());
    }

    @Test
    public void playingNotificationPromotesMediaPlaybackService() {
        MusicService service = Robolectric.buildService(MusicService.class).get();
        PlayingNotification notification = new PlayingNotification() {
            @Override public void update() {}
        };
        notification.init(service, PlayingNotification.NOTIFICATION_CHANNEL_ID,
                R.string.playing_notification_name, R.string.playing_notification_description);
        notification.updateImpl(new Notification.Builder(service, PlayingNotification.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification).build());

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, service.getForegroundServiceType());
    }
}
