package com.promact.dropcontact;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.dropbox.core.DbxException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private boolean isLoginInProgress = false;
    private String msg = "Please check your internet connection";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        Fabric.with(this, crashlyticsKit);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SharedPreferences sharedPreferences = getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);
        boolean flag = sharedPreferences.getBoolean("isDropBoxLoginSuccessful", false);
        if (flag) {
            Intent i = new Intent(getApplicationContext(), HomeActivity.class);
            startActivityForResult(i, 1);
            finish();
        }
        setContentView(R.layout.activity_main);
        Button loginButton = (Button) findViewById(R.id.loginbutton);
        loginButton.setOnClickListener(this);
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        Tracker tracker = application.getDefaultTracker();
        Log.i(TAG, "Setting screen name: " + getClass().getSimpleName());
        tracker.setScreenName("LoginActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.loginbutton) {
            if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
                doLogin();
            } else {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        if (isLoginInProgress) {
            String accessToken = getAccessToken();
            Log.d(TAG, "accessToken = " + accessToken);
            if (accessToken != null) {
                //generate Access Token
                Log.d(TAG, "Dropbox token is:" + accessToken);
                if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
                    new DropboxAccountInfoTask(DropboxClient.getClient(accessToken)).execute();

                } else {
                    isLoginInProgress = false;
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                }
                SharedPreferences sharedPreferences = getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);
                sharedPreferences.edit().putString(new GlobalStrings().getAccessTokenOfDropbox(),
                        accessToken).apply();
            } else {
                isLoginInProgress = false;
            }
        }
    }

    private void doLogin() {
        isLoginInProgress = true;
        Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.APP_KEY));
    }

    public String getAccessToken() {
        return Auth.getOAuth2Token();
    }

    public void onBackPressed() {
        finish();
    }


    private class DropboxAccountInfoTask extends AsyncTask<String, String, List<String>> {
        private DbxClientV2 dbxClient;
        ProgressDialog progressDialog;
        private Exception exception;

        DropboxAccountInfoTask(DbxClientV2 dbxClient) {
            this.dbxClient = dbxClient;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
        }

        @Override
        protected List<String> doInBackground(String[] strings) {
            FullAccount account = null;
            List<String> dropboxInfoArrayList = null;
            try {
                account = dbxClient.users().getCurrentAccount();
                Log.d(TAG, "account is:" + account);
            } catch (NetworkIOException e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
                exception = e;
            } catch (DbxException e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
            }
            if (account == null) {
                isLoginInProgress = false;
            } else {
                SharedPreferences sharedPreferences = getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);

                String dropboxAccountEmail = account.getEmail();
                String dropboxAccountPhotoUrl = account.getProfilePhotoUrl();
                String dropboxAccountName = account.getName().getDisplayName();
                Log.d(TAG, "dropbox account email is:" + dropboxAccountEmail);
                Log.d(TAG, "dropbox account uri is:" + dropboxAccountPhotoUrl);
                Log.d(TAG, "dropbox account name is:" + dropboxAccountName);
                sharedPreferences.edit().putString("dropboxAccountEmail", dropboxAccountEmail).apply();
                sharedPreferences.edit().putString("dropboxAccountPhotoUrl", dropboxAccountPhotoUrl).apply();
                sharedPreferences.edit().putString("dropboxAccountName", dropboxAccountName).apply();

                sharedPreferences.edit().putBoolean("isDropBoxLoginSuccessful", true).apply();
                dropboxInfoArrayList = new ArrayList<>();
                dropboxInfoArrayList.add(dropboxAccountPhotoUrl);
                dropboxInfoArrayList.add(dropboxAccountName);
                dropboxInfoArrayList.add(dropboxAccountEmail);
            }
            return dropboxInfoArrayList;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            if (exception != null) {
                new ExceptionReportHandler().showCatchExceptions(TAG, exception);
                isLoginInProgress = false;
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

            }
            progressDialog.dismiss();
            if (isLoginInProgress) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
