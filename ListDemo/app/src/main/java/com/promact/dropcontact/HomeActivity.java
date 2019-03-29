package com.promact.dropcontact;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.*;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private Snackbar snackbar;
    private static final String TAG = "HomeActivity";
    private SharedPreferences sharedPreferences;
    private PagerAdapter sectionsPagerAdapter;
    private Fragment fragment;
    private TabLayout tabLayout;
    private Tracker tracker;
    private String msg = "Please check your internet connection";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        sharedPreferences = getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("DropContact");
        // Create the adapter that will return a fragment for each of the two
        // primary sections of the activity.
        List<String> tabs = new ArrayList<>();
        tabs.add("Contacts");
        tabs.add("Settings");
        sectionsPagerAdapter = new PagerAdapter(getSupportFragmentManager(), (ArrayList<String>) tabs);
        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        tracker = application.getDefaultTracker();
        Log.i(TAG, "Setting screen name: " + getClass().getSimpleName());
        tracker.setScreenName("HomeActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            if (CheckNetwork.isInternetAvailable(HomeActivity.this)) //returns true if internet available
            {
                alertDialogShow(HomeActivity.this);
            } else {
                msgOfSnackBar(msg);

            }
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("Share")
                    .build());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void msgOfSnackBar(String msg) {
        final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                .coordinatorLayout);
        snackbar = Snackbar
                .make(coordinatorLayout, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("Ok", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Handle user action
                        snackbar.dismiss();
                    }
                });
        snackbar.setActionTextColor(Color.WHITE);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void alertDialogShow(HomeActivity homeActivity) {
        final Dialog alertDialog = new Dialog(homeActivity);
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setContentView(R.layout.custom_dialog_layout);
        TextView dropboxEmailId = (TextView) alertDialog.findViewById(R.id.dropboxAccountEmailID);
        String dropboxEmail = sharedPreferences.getString("dropboxAccountEmail", "");
        Log.d(TAG, "sharedprfs dropboxemail:" + dropboxEmail);
        dropboxEmailId.setText(dropboxEmail);
        Button buttonOk = (Button) alertDialog.findViewById(R.id.Ok);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                String accessToken = sharedPreferences.getString(new GlobalStrings().getAccessTokenOfDropbox(), "");
                Log.d(TAG, "AccessToken:" + accessToken);
                int position = tabLayout.getSelectedTabPosition();
                fragment = sectionsPagerAdapter.getFragment(tabLayout.getSelectedTabPosition());
                if (fragment != null && position == 0) {

                    try {
                        ((ContactsFragment) fragment).getUploadFileTask();
                    } catch (IOException e) {
                        new ExceptionReportHandler().showCatchExceptions(TAG, e);
                    }

                }
            }
        });
        Button buttonCancel = (Button) alertDialog.findViewById(R.id.cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
}
