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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by grishma on 13-08-2016.
 */
public class UploadContactsFileTask extends AsyncTask {
    private DbxClientV2 dbxClient;
    private Activity context;
    private static final String TAG = "UploadContactsFileTask";
    private String path = "/DropContactsApp/Contacts.json";
    private List<Contact> contactArrayList = new ArrayList<>();
    private ProgressDialog progressDialog;
    private Exception exception;
    private File contacsFile;


    UploadContactsFileTask(DbxClientV2 dbxClient, File contactsFile, Activity context, List<Contact> contactArrayList) {
        this.dbxClient = dbxClient;
        this.context = context;
        this.contactArrayList = contactArrayList;
        this.contacsFile = contactsFile;
    }

    @Override
    protected void onPreExecute() {
        try {
            String msgForUploadContactsFile = "Saving Contacts 2/3 ...";
            progressDialog = new Helper().getProgressDialog(context, msgForUploadContactsFile);
            progressDialog.show();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        String text = "";
        try (FileInputStream contactsFileInputStream = new FileInputStream(contacsFile)) {
            FileMetadata metadata = dbxClient.files().uploadBuilder(path)//Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(contactsFileInputStream);
            if (metadata == null) {
                Log.d(TAG, "Error in upload contacts file into server");
            } else {
                Log.d(TAG, "Metadata of contacts file is not null");

            }
        } catch (DbxException | IOException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
            exception = e;
        }
        return text;
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
        new UploadImagesTask(dbxClient, contactArrayList, context).execute();

    }
}


