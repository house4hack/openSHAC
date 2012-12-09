/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
 of its contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sonyericsson.extras.liveware.extension.shac;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.widget.Widget;
import com.sonyericsson.extras.liveware.extension.util.SmartWatchConst;
import com.sonyericsson.extras.liveware.extension.util.widget.WidgetExtension;

/**
 * The sample widget handles the widget on an accessory. This class exists in
 * one instance for every supported host application that we have registered to.
 */
class SHACWidget extends WidgetExtension {

    public static final int WIDTH = 128;

    public static final int HEIGHT = 110;

    private static final long UPDATE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;

    private static final SimpleDateFormat TIME_FORMAT_24_H = new SimpleDateFormat("HH:mm",
            new Locale("se"));

    private static final SimpleDateFormat TIME_FORMAT_AM_PM = new SimpleDateFormat("hh:mm a",
            new Locale("se"));

    /**
     * Create sample widget.
     *
     * @param hostAppPackageName Package name of host application.
     * @param context The context.
     */
    SHACWidget(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
    }

    /**
     * Start refreshing the widget. The widget is now visible.
     */
    @Override
    public void onStartRefresh() {
        Log.d(SHACExtensionService.LOG_TAG, "startRefresh");
        // Update now and every 10th second
        cancelScheduledRefresh(SHACExtensionService.EXTENSION_KEY);
        scheduleRepeatingRefresh(System.currentTimeMillis(), UPDATE_INTERVAL,
                 SHACExtensionService.EXTENSION_KEY);
    }

    /**
     * Stop refreshing the widget. The widget is no longer visible.
     */
    @Override
    public void onStopRefresh() {
        Log.d(SHACExtensionService.LOG_TAG, "stopRefesh");

        // Cancel pending clock updates
        cancelScheduledRefresh(SHACExtensionService.EXTENSION_KEY);
    }

    @Override
    public void onScheduledRefresh() {
        Log.d(SHACExtensionService.LOG_TAG, "scheduledRefresh()");
        updateWidget();
    }

    /**
     * Unregister update clock receiver, cancel pending updates
     */
    @Override
    public void onDestroy() {
        Log.d(SHACExtensionService.LOG_TAG, "onDestroy()");
        onStopRefresh();
    }

    /**
     * The widget has been touched.
     *
     * @param type The type of touch event.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     */
    @Override
    public void onTouch(final int type, final int x, final int y) {
        Log.d(SHACExtensionService.LOG_TAG, "onTouch() " + type);
        if (!SmartWatchConst.ACTIVE_WIDGET_TOUCH_AREA.contains(x, y)) {
            Log.d(SHACExtensionService.LOG_TAG, "Ignoring touch outside active area x: " + x
                    + " y: " + y);
            return;
        }

        if (type == Widget.Intents.EVENT_TYPE_SHORT_TAP) {
            // Change clock mode on short tap.
            setClockMode24h(!isClockMode24h());
            // Update clock widget now
            updateWidget();
        }
    }

    /**
     * Set clock format
     *
     * @return True if 24-h clock format, false to use 12-h am/pm
     */
    private boolean isClockMode24h() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean(mContext.getString(R.string.preference_key_clock_mode), true);
    }

    /**
     * Get clock format
     *
     * @param isClockMode24h True if 24-h clock format, false to use 12-h am/pm
     */
    private void setClockMode24h(boolean isClockMode24h) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putBoolean(mContext.getString(R.string.preference_key_clock_mode), isClockMode24h);
        editor.commit();
    }

    /**
     * Update the widget.
     */
    private void updateWidget() {
        Log.d(SHACExtensionService.LOG_TAG, "updateWidget");
        // Get time
        String time = null;
        if (isClockMode24h()) {
            time = TIME_FORMAT_24_H.format(new Date());
        } else {
            time = TIME_FORMAT_AM_PM.format(new Date());
        }

        showBitmap(new SmartWatchSHACWidgetImage(mContext, time).getBitmap());
    }
}
