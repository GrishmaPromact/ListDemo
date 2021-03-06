package com.promact.dropcontact;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by grishma on 04-10-2016.
 */
public class CheckNetwork {
    private static final String TAG = "CheckNetwork";

    private CheckNetwork() {
    }

    public static boolean isInternetAvailable(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info == null) {
            Log.d(TAG, "No internet connection");
            return false;
        } else {
            if (info.isConnected()) {
                Log.d(TAG, " Internet connection available...");
                return true;
            } else {
                Log.d(TAG, "Internet connection");
                return true;
            }
        }
    }
}
