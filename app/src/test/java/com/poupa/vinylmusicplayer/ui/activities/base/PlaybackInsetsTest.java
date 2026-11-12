package com.poupa.vinylmusicplayer.ui.activities.base;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.poupa.vinylmusicplayer.util.PlaybackInsets;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class PlaybackInsetsTest {
    @Test
    public void threeButtonNavigationKeepsCollapsedPlayerAboveSystemBar() {
        SlidingUpPanelLayout panel = panel();
        PlaybackInsets.apply(panel);
        ViewCompat.dispatchApplyWindowInsets(panel, insets(72));
        layout(panel);

        assertEquals(72, panel.getPaddingBottom());
        assertEquals(728, panel.getChildAt(1).getTop() + panel.getPanelHeight());
        assertEquals(80, panel.getPanelHeight());
    }

    @Test
    @Config(sdk = 34)
    public void android14KeepsExistingDecorFitting() {
        SlidingUpPanelLayout panel = panel();
        PlaybackInsets.apply(panel);
        ViewCompat.dispatchApplyWindowInsets(panel, insets(72));
        assertEquals(0, panel.getPaddingBottom());
    }

    @Test
    public void gestureInsetChangesDoNotAccumulateOrDoubleTopStatusBar() {
        SlidingUpPanelLayout panel = panel();
        panel.setPadding(3, 7, 5, 11);
        PlaybackInsets.apply(panel);
        ViewCompat.dispatchApplyWindowInsets(panel, insets(72));
        ViewCompat.dispatchApplyWindowInsets(panel, insets(24));

        assertEquals(35, panel.getPaddingBottom());
        assertEquals(7, panel.getPaddingTop());
        assertEquals(3, panel.getPaddingLeft());
        assertEquals(5, panel.getPaddingRight());
    }

    private SlidingUpPanelLayout panel() {
        var context = RuntimeEnvironment.getApplication();
        SlidingUpPanelLayout panel = new SlidingUpPanelLayout(context);
        panel.setGravity(Gravity.BOTTOM);
        panel.addView(new FrameLayout(context), new SlidingUpPanelLayout.LayoutParams(-1, -1));
        panel.addView(new FrameLayout(context), new SlidingUpPanelLayout.LayoutParams(-1, -1));
        panel.setPanelHeight(80);
        return panel;
    }

    private WindowInsetsCompat insets(int bottom) {
        return new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, bottom))
                .build();
    }

    private void layout(View panel) {
        panel.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
        panel.layout(0, 0, 400, 800);
    }
}
