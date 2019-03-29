
package com.promact.dropcontact;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


public class DownloadHashCodeFileTask extends AsyncTask {
    private DbxClientV2 dbxClient;
    private DbxDownloader<FileMetadata> code;
    private Activity context;
    private static final String TAG = "DownloadHashCodeFileTask";
    private String path = "/DropContactsApp/ContactsHashCode.json";
    private IDownloadJsonString<String> callback;
    private ProgressDialog progressDialog;
    private Exception exception;
    private DownloadErrorException downloadErrorException;
    private List<Contact> contactList;
    private File file;
    private File hashCodeFile;

    DownloadHashCodeFileTask(DbxClientV2 dbxClient, File contactsFile, File hashCodeFile, Activity context, List<Contact> contactList, IDownloadJsonString<String> cb) {
        this.dbxClient = dbxClient;
        this.context = context;
        this.callback = cb;
        this.contactList = contactList;
        this.file = contactsFile;
        this.hashCodeFile = hashCodeFile;
    }

    @Override
    protected void onPreExecute() {
        try {
            String msgForDownloadHashCodeFile = "Checking ...";
            progressDialog = new Helper().getProgressDialog(context, msgForDownloadHashCodeFile);
            progressDialog.show();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG,e);
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            code = dbxClient.files().download(path);
        } catch (DownloadErrorException ex) {
            Log.e(TAG, "DownloadErrorException", ex);
            downloadErrorException = ex;
        } catch (NetworkIOException e1) {
            exception = e1;
        } catch (DbxException e2) {
            exception = e2;
        }
        return "";
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if (downloadErrorException != null) {
            new UploadHashCodeFileTask(dbxClient, hashCodeFile, file, contactList, context).execute();
        } else {
            Log.d(TAG, "Both File is not uploading", downloadErrorException);
        }
        if (exception != null) {
            Toast.makeText(context, "Please check your internet connection.", Toast.LENGTH_LONG).show();
            callback.onTaskException(exception);
        } else if (code == null) {
            Log.d(TAG, "Error in download hashcode file form server.");
        } else {
            InputStream downloadFileInputStream;
            downloadFileInputStream = code.getInputStream();
            Log.d(TAG, "download file input stream is:" + downloadFileInputStream);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(downloadFileInputStream))) {
                Log.d(TAG, "br is:" + bufferedReader);
                Log.d(TAG, "Done!!!!!!!!!!!!!!!!!!!!!!!");
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                Log.d(TAG, "String is = " + sb.toString());
                Log.d(TAG, "Done!");
                String downloadHexString;
                downloadHexString = sb.toString();
                JSONObject hashcodeJsonObject = new JSONObject();
                hashcodeJsonObject.put("HashCode", downloadHexString);
                callback.onTaskComplete(downloadHexString);
            } catch (IOException | JSONException e) {
                new ExceptionReportHandler().showCatchExceptions(TAG,e);
                callback.onTaskException(exception);
            }  finally {
                if (downloadFileInputStream != null) {
                    try {
                        downloadFileInputStream.close();
                    } catch (IOException e) {
                        new ExceptionReportHandler().showCatchExceptions(TAG,e);
                    }
                }
            }
        }
        try {
            progressDialog.dismiss();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG,e);
        }

    }
}

