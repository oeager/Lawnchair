/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher;

import android.content.Context;
import android.gadget.GadgetHost;
import android.gadget.GadgetHostView;
import android.gadget.GadgetInfo;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

/**
 * Specific {@link GadgetHost} that creates our {@link LauncherGadgetHostView} which correctly
 * captures all long-press events.  This ensures that users can always pick up and move gadgets. 
 */
public class LauncherGadgetHost extends GadgetHost {
    public LauncherGadgetHost(Context context, int hostId) {
        super(context, hostId);
    }
    
    protected GadgetHostView onCreateView(Context context, int gadgetId, GadgetInfo gadget) {
        return new LauncherGadgetHostView(context);
    }
    
    /**
     * {@inheritDoc}
     */
    public class LauncherGadgetHostView extends GadgetHostView {
        static final String TAG = "LauncherGadgetHostView";

        private boolean mHasPerformedLongPress;
        
        private CheckForLongPress mPendingCheckForLongPress;
        
        private LayoutInflater mInflater;
        
        public LauncherGadgetHostView(Context context) {
            super(context);
            
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            // Prepare our default transition animations
            setAnimateFirstView(true);
            setInAnimation(context, android.R.anim.fade_in);
            setOutAnimation(context, android.R.anim.fade_out);
        }
        
        @Override
        protected View getErrorView() {
            return mInflater.inflate(R.layout.gadget_error, this, false);
        }

        public boolean onInterceptTouchEvent(MotionEvent ev) {
            
            // Consume any touch events for ourselves after longpress is triggered
            if (mHasPerformedLongPress) {
                mHasPerformedLongPress = false;
                return true;
            }
                
            // Watch for longpress events at this level to make sure
            // users can always pick up this Gadget
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    postCheckForLongClick();
                    break;
                }
                
                case MotionEvent.ACTION_UP: {
                    mHasPerformedLongPress = false;
                    if (mPendingCheckForLongPress != null) {
                        removeCallbacks(mPendingCheckForLongPress);
                    }
                    break;
                }
            }
            
            // Otherwise continue letting touch events fall through to children
            return false;
        }
        
        class CheckForLongPress implements Runnable {
            private int mOriginalWindowAttachCount;

            public void run() {
                if ((mParent != null) && hasWindowFocus()
                        && mOriginalWindowAttachCount == getWindowAttachCount()
                        && !mHasPerformedLongPress) {
                    if (performLongClick()) {
                        mHasPerformedLongPress = true;
                    }
                }
            }

            public void rememberWindowAttachCount() {
                mOriginalWindowAttachCount = getWindowAttachCount();
            }
        }

        private void postCheckForLongClick() {
            mHasPerformedLongPress = false;

            if (mPendingCheckForLongPress == null) {
                mPendingCheckForLongPress = new CheckForLongPress();
            }
            mPendingCheckForLongPress.rememberWindowAttachCount();
            postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
        }

    }

}

