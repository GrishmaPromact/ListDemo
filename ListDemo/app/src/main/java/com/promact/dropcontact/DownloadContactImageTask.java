package com.promact.dropcontact;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Created by Grishma on 11-05-2017.
 */

public class DownloadContactImageTask extends AsyncTask<Void, Void, Void> {
    private DbxClientV2 dbxClient;
    private static final String TAG = "DownloadContactImage";
    private String path = "/DropContactsApp/Images/";
    private IDowanloadImage callback;
    private Exception exception;
    private Contact contact;
    private Activity activityContext;
    private ProgressDialog progressDialog;

    DownloadContactImageTask(DbxClientV2 dbxClient,
                             Activity context,
                             Contact contact,
                             IDowanloadImage cb) {
        this.dbxClient = dbxClient;
        this.activityContext = context;
        this.callback = cb;
        this.contact = contact;
    }

    @Override
    protected void onPreExecute() {
        try {
            String msgForDownloadImage = "Syncing ...";
            progressDialog = new Helper().getProgressDialog(activityContext, msgForDownloadImage);
            progressDialog.show();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    @Override
    protected Void doInBackground(Void... mvoid) {

        File dirPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        try (OutputStream outputStream = new FileOutputStream(new File(dirPath, contact.getName() + ".png"))) {

            if (!dirPath.exists()) {
                if (!dirPath.mkdirs()) {
                    exception = new RuntimeException("Unable to create directory: " + path);
                }
            } else if (!dirPath.isDirectory()) {
                exception = new IllegalStateException("Download path is not a directory: " + path);
            }

            FileMetadata code = dbxClient.files()
                    .download(String.format(Locale.getDefault(), path + contact.getName() + ".png")).download(outputStream);
            if (code == null) {
                Log.d(TAG, "Error in upload images folder into server");
            } else {
                Log.d(TAG, "Metadata of images is not null");
            }
        } catch (IOException e1) {
            exception = e1;
        } catch (Exception e1) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e1);
            exception = e1;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void o) {
        try {
            progressDialog.dismiss();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
        if (exception != null) {
            callback.onTaskException(exception);
        } else {
            callback.onTaskComplete();

        }
    }
}
