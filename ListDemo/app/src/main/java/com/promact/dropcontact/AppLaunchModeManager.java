package com.promact.dropcontact;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by grishma on 05/05/16.
 */
public class AppLaunchModeManager {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Context context;

    // shared pref mode
    private static final int PRIVATE_MODE = 0;

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    public AppLaunchModeManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(new GlobalStrings().getDropboxIntegration(), PRIVATE_MODE);
        editor = sharedPreferences.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return sharedPreferences.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

}
