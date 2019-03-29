package com.promact.dropcontact;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by grishma on 01-09-2016.
 */
public class UploadHashCodeFileTask extends AsyncTask {
    private DbxClientV2 dbxClient;
    private Activity context;
    private static final String TAG = "UploadHashCodeFileTask";
    private String path = "/DropContactsApp/ContactsHashCode.json";
    private ProgressDialog progressDialog;
    private Exception exception;
    private File hashCodeFile;
    private File contactsFile;
    private List<Contact> contactArraylist;

    UploadHashCodeFileTask(DbxClientV2 dbxClient, File hashCodeFile, File contactsFile, List<Contact> contactArrayList, Activity context) {
        this.dbxClient = dbxClient;
        this.context = context;
        this.hashCodeFile = hashCodeFile;
        this.contactsFile = contactsFile;
        this.contactArraylist = contactArrayList;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        try {
            String msgForUploadContactsFile = "Saving Contacts 1/3 ...";
            progressDialog = new Helper().getProgressDialog(context, msgForUploadContactsFile);
            progressDialog.show();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        try (FileInputStream hashCodeFileInputStream = new FileInputStream(hashCodeFile)) {
            FileMetadata hashcode = dbxClient.files().uploadBuilder(path)//Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(hashCodeFileInputStream);
            if (hashcode != null) {
                Log.d(TAG, "Hashcode is not null");
            } else {
                Log.d(TAG, "Error in upload hashcode file into server");
            }
        } catch (DbxException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
            exception = e;
        } catch (FileNotFoundException e1) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e1);
        } catch (IOException e2) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e2);
        }
        return "";
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        new UploadContactsFileTask(dbxClient, contactsFile, context, contactArraylist).execute();

        try {
            progressDialog.dismiss();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
        if (exception != null) {
            new ExceptionReportHandler().showCatchExceptions(TAG, exception);
        }
    }
}

