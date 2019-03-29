package com.promact.dropcontact;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by grishma on 09-05-2017.
 */
public class UploadImagesTask extends AsyncTask {
    private DbxClientV2 dbxClient;
    private Activity context;
    private static final String TAG = "UploadContactsFileTask";
    private String path = "/DropContactsApp/Images/";
    private List<Contact> localContactList;
    private ProgressDialog progressDialog;
    private Exception exception;

    UploadImagesTask(DbxClientV2 dbxClient, List<Contact> localContactList, Activity context) {
        this.dbxClient = dbxClient;
        this.context = context;
        this.localContactList = localContactList;
    }

    @Override
    protected void onPreExecute() {
        try {
            String msgForUploadImages = "Saving Contacts 3/3 ...";
            super.onPreExecute();
            progressDialog = new Helper().getProgressDialog(context, msgForUploadImages);
            progressDialog.show();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        String text = "";
        for (int i = 0; i < localContactList.size(); i++) {
            if (localContactList.get(i).getProfile() != null && !localContactList.get(i).getProfile().isEmpty()) {
                uploadImagesFolder(i);
            }
        }
        return text;
    }

    private void uploadImagesFolder(int i) {
        try {
            FileInputStream imageInputStream = (FileInputStream) context.getContentResolver().openInputStream(Uri.parse(localContactList.get(i).getProfile()));
            FileMetadata fileMetadata = dbxClient.files().uploadBuilder(path + localContactList.get(i).getName() + ".png")//Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(imageInputStream);
            if (fileMetadata == null) {
                Log.d(TAG, "Error in uploading image folder into dropbox.Please check your internet connection.");
            } else {
                Log.d(TAG, "Uploading images folder into dropbox succeesfully.");
            }
        } catch (DbxException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
            exception = e;
        } catch (IOException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }

    }

    @Override
    protected void onPostExecute(Object o) {
        try {
            progressDialog.dismiss();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
        if (exception != null) {
            new ExceptionReportHandler().showCatchExceptions(TAG, exception);
        }
        Toast.makeText(context, "Your contacts are synced now", Toast.LENGTH_LONG).show();
    }
}




