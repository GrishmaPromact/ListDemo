package com.promact.dropcontact;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.EnumMap;


/**
 * Created by grishma on 30-05-2017.
 */
public final class AnalyticsTrackers {
    private static AnalyticsTrackers analyticsTrackers;
    private static final EnumMap<Target, Tracker> mTrackers = new EnumMap<>(Target.class);
    private static Context mContext;
    private AnalyticsTrackers(Context context) {
        mContext = context.getApplicationContext();
    }

    public enum Target {
        APP,
        // Add more trackers here if you need, and update the code in #get(Target) below
    }
    public static synchronized void initialize(Context context) {
        if (analyticsTrackers != null) {
            throw new IllegalStateException("Extra call to initialize analytics trackers");
        }

        analyticsTrackers = new AnalyticsTrackers(context);
    }

    public static synchronized AnalyticsTrackers getInstance() {
        if (analyticsTrackers == null) {
            throw new IllegalStateException("Call initialize() before getInstance()");
        }

        return analyticsTrackers;
    }
    public synchronized Tracker get(Target target) {
        if (!mTrackers.containsKey(target)) {
            Tracker tracker = null;
            if(target==Target.APP) {
                tracker = GoogleAnalytics.getInstance(mContext).newTracker(R.xml.app_tracker);
            }
            mTrackers.put(target, tracker);
        }

        return mTrackers.get(target);
    }
}
