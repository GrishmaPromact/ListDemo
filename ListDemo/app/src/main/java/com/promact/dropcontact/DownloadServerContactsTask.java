package com.promact.dropcontact;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListRevisionsErrorException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by grishma on 19-05-2017.
 */
public class DownloadServerContactsTask extends AsyncTask<String, Void, String> {
    private DbxClientV2 dbxClient;
    private Activity context;
    private InputStream downloadJSONFileInputStream;
    private BufferedReader bufferedReader;
    private static final String TAG = "DownloadServerContactsTask";
    private String path = "/DropContactsApp/Contacts.json";
    private ProgressDialog progressDialog;
    private View view;
    private Exception exception;
    private List<Contact> contactArrayList;
    private DownloadErrorException downloadErrorException;
    private File contactsFile;
    private IDowanloadServerContacts callback;

    DownloadServerContactsTask(DbxClientV2 dbxClient, File contactsFile,
                               Activity context, List<Contact> contactArrayList, View view,
                               IDowanloadServerContacts cb) {
        this.dbxClient = dbxClient;
        this.context = context;
        this.view = view;
        this.callback = cb;
        this.contactArrayList = contactArrayList;
        this.contactsFile = contactsFile;
    }

    @Override
    protected void onPreExecute() {
        try {
            String msgForDownloadContactsFile = "Fetching Contacts ...";
            super.onPreExecute();
            progressDialog = new Helper().getProgressDialog(context, msgForDownloadContactsFile);
            progressDialog.show();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    @Override
    protected String doInBackground(String... mvoid) {
        String downloadJSONString = "";
        try (DbxDownloader<FileMetadata> jsonData = dbxClient.files().download(path)) {
            if (jsonData == null) {
                Log.d("Error in download", "Error in download contacts file form server");
            }
            if (jsonData != null) {
                downloadJSONFileInputStream = jsonData.getInputStream();
                Log.d(TAG, "download file input stream is:" + downloadJSONFileInputStream);
                bufferedReader = new BufferedReader(new InputStreamReader(downloadJSONFileInputStream));
                Log.d(TAG, "DownloadJSONfileDone!!!!!!!!!!!!!!!!!!!!!!!");
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                Log.d(TAG, "String is = " + sb.toString());
                Log.d(TAG, "PerfectDone!");
                downloadJSONString = sb.toString();
                Log.d(TAG, "Download json string is:" + downloadJSONString);
            }
        } catch (ListRevisionsErrorException e1) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e1);
        } catch (DownloadErrorException e1) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e1);
            downloadErrorException = e1;
        } catch (DbxException | UnknownHostException | ConnectException | SocketTimeoutException e1) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e1);
            exception = e1;
        } catch (IOException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        } finally {
            if (downloadJSONFileInputStream != null) {
                try {
                    downloadJSONFileInputStream.close();
                } catch (IOException e) {
                    new ExceptionReportHandler().showCatchExceptions(TAG, e);
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    new ExceptionReportHandler().showCatchExceptions(TAG, e);
                }
            }
        }
        return downloadJSONString;
    }

    @Override
    public void onPostExecute(String downloadJSONString) {
        try {
            progressDialog.dismiss();
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
        if (downloadErrorException != null) {
            new UploadContactsFileTask(dbxClient, contactsFile, context, contactArrayList).execute();
        } else {
            Log.d(TAG, "Both File is not uploading", downloadErrorException);
        }
        Log.d(TAG, "download string::::" + downloadJSONString);
        try {
            List<Contact> serverContactList = new ArrayList<>();
            Log.d(TAG, "download JSON string:" + downloadJSONString);
            JSONArray jsonArray = new JSONArray(downloadJSONString);
            for (int h = 0; h < jsonArray.length(); h++) {
                JSONObject jsonObject = jsonArray.getJSONObject(h);
                String servername = jsonObject.getString(new GlobalStrings().getName());
                String serverFirstName = jsonObject.getString(new GlobalStrings().getFirstName());
                String serverMiddleName = jsonObject.getString(new GlobalStrings().getMiddleName());
                String serverLastName = jsonObject.getString(new GlobalStrings().getLastName());
                Log.d(TAG, "Name --------------- " + servername);
                boolean hasImage = false;
                if (jsonObject.has(new GlobalStrings().getHasImage())) {
                    hasImage = jsonObject.getBoolean(new GlobalStrings().getHasImage());
                }
                JSONArray phoneJsonArray = jsonObject.getJSONArray(new GlobalStrings().getPhone());
                List<ContactType> serverPhoneNumberList =
                        new ArrayList<>();
                for (int j = 0; j < phoneJsonArray.length(); j++) {
                    JSONObject pjsonObject = phoneJsonArray.getJSONObject(j);
                    String phoneType = pjsonObject.getString(new GlobalStrings().getType());
                    String phoneNumber = pjsonObject.getString(new GlobalStrings().getNumber());
                    Log.d(TAG, "phone issue - download  - " + phoneJsonArray.getString(j));
                    serverPhoneNumberList.add(new ContactType((phoneType), phoneNumber));
                }
                JSONArray emailJsonArray = jsonObject.getJSONArray(new GlobalStrings().getEmail());
                List<ContactType> serverEmailList = new ArrayList<>();
                for (int k = 0; k < emailJsonArray.length(); k++) {
                    JSONObject ejsonObject = emailJsonArray.getJSONObject(k);
                    String emailType = ejsonObject.getString(new GlobalStrings().getType());
                    String emailId = ejsonObject.getString(new GlobalStrings().getId());
                    Log.d(TAG, "email issue - download  - " + emailJsonArray.getString(k));
                    serverEmailList.add(new ContactType((emailType), emailId));
                }
                serverContactList.add(new Contact(servername, serverFirstName, serverMiddleName, serverLastName, serverPhoneNumberList, serverEmailList, "", hasImage));
            }
            callback.onTaskComplete(serverContactList);
        } catch (JSONException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
        if (exception != null) {
            callback.onTaskException(exception);
        }
    }
}
