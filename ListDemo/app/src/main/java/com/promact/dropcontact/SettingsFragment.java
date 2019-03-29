package com.promact.dropcontact;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private SharedPreferences.Editor editor;

    public static SettingsFragment newInstance() {
        SettingsFragment settingsFragment = new SettingsFragment();
        Bundle args = new Bundle();
        settingsFragment.setArguments(args);
        return settingsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(new GlobalStrings().getAccessTokenOfDropbox(), "");
        editor = sharedPreferences.edit();

        if (accessToken != null) {
            String dropboxName = sharedPreferences.getString("dropboxAccountName", "");
            Log.d(TAG, "sharedprfs dropboxname:" + dropboxName);
            TextView dropboxAcctName = (TextView) view.findViewById(R.id.dropboxAccountName);
            dropboxAcctName.setText(dropboxName);

            String dropboxEmail = sharedPreferences.getString("dropboxAccountEmail", "");
            Log.d(TAG, "sharedprfs dropboxemail:" + dropboxEmail);
            TextView dropboxAcctEmail = (TextView) view.findViewById(R.id.dropboxAccountEmail);
            dropboxAcctEmail.setText(dropboxEmail);

            String dropboxPhoto = sharedPreferences.getString("dropboxAccountPhotoUrl", "null");
            Log.d(TAG, "sharedprfs dropboxphotouri:" + dropboxPhoto);
            ImageView dropboxAccntProfile = (ImageView) view.findViewById(R.id.dropboxProfile);

            if (("null").equalsIgnoreCase(dropboxPhoto)) {
                Log.d(TAG, "nulllllll" + dropboxPhoto);
                dropboxAccntProfile.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dropbox_account));

            } else {
                Picasso.with(getActivity()).load(dropboxPhoto)
                        .transform(new CropCircleTransformationForContactProfile())
                        .into(dropboxAccntProfile);
            }
        }
        Button logoutButton = (Button) view.findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.clear().apply();
                Intent i = new Intent(getActivity().getApplicationContext(), MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        });
        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_refresh).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

}
