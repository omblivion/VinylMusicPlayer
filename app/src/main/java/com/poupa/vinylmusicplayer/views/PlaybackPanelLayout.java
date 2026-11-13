package com.poupa.vinylmusicplayer.views;

import android.content.Context;
import android.util.AttributeSet;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

/** Keeps structural mini-player height changes separate from user-driven panel animations. */
public class PlaybackPanelLayout extends SlidingUpPanelLayout {
    public PlaybackPanelLayout(Context context) {
        super(context);
    }

    public PlaybackPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlaybackPanelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void smoothToBottom() {
        // setPanelHeight otherwise starts a drag toward coordinates from the old
        // viewport. A navigation-inset update can move that target below the new
        // collapsed position, which the library interprets as HIDDEN. Recompute
        // collapsed geometry on the next layout instead; expand/collapse gestures
        // still use the library's normal animations.
        setAnchorPoint(getAnchorPoint());
    }
}
