package com.promact.dropcontact;


import android.util.Log;

/**
 * Created by grishma on 30-05-2017.
 */
public class ExceptionReportHandler extends AnalyticsApplication {

    public void showCatchExceptions(String tag, Exception e) {
        Log.d(tag, "Exception msg:", e);
        AnalyticsApplication.getInstance().trackException(e);
    }

}