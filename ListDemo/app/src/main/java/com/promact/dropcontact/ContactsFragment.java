package com.promact.dropcontact;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

public class ContactsFragment extends Fragment implements AutoCloseable {
    private File file;
    private File hashCodeFile;
    private ContactListRecyclerviewAdapter adapter;
    private SharedPreferences sharedPreferences;
    private static final String TAG = "ContactsFragment";
    private View view;
    private Snackbar snackbar = null;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 100;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 110;
    private HashMap<String, Contact> modifiedContactHashMap;
    private List<Contact> localContactList;
    private List<Contact> modifiedContactList;
    private int i = 0;
    private boolean isDownlaodCompleted = false;


    public static ContactsFragment newInstance() {
        Bundle args = new Bundle();
        ContactsFragment fragment = new ContactsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_contacts, container, false);
        localContactList = new ArrayList<>();
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        adapter = new ContactListRecyclerviewAdapter(getActivity(), localContactList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        final RecyclerViewFastScroller fastScroller = (RecyclerViewFastScroller) view.findViewById(R.id.fastscroller);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutChildren(final RecyclerView.Recycler recycler, final RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                final int firstVisibleItemPosition = findFirstVisibleItemPosition();
                if (firstVisibleItemPosition != 0) {
                    // this avoids trying to handle un-needed calls
                    if (firstVisibleItemPosition == -1)
                        //not initialized, or no items shown, so hide fast-scroller
                        fastScroller.setVisibility(View.GONE);
                    return;
                }
                final int lastVisibleItemPosition = findLastVisibleItemPosition();
                int itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1;
                //if all items are shown, hide the fast-scroller
                fastScroller.setVisibility(adapter.getItemCount() > itemsShown ? View.VISIBLE : View.GONE);
            }
        });
        fastScroller.setRecyclerView(recyclerView);
        fastScroller.setViewsToUse(R.layout.recycler_view_fast_scroller__fast_scroller, R.id.fastscroller_bubble, R.id.fastscroller_handle);
        showContacts();
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                new Helper().recyclerViewOnClick(view, position, localContactList, getActivity());
            }

            @Override
            public void onLongClick(View view, int position) {
                //Do nothing
            }
        }));
        return view;
    }

    public void getUploadFileTask() throws IOException {
        try {
            final String hashCodeOfLocalContacts = putIntoHashcodeFile();
            Log.d(TAG, "File Size = " + hashCodeFile.length());
            JSONArray jsonArray = new JSONArray();
            Log.d(TAG, "JSONArray is:" + jsonArray.toString());
            Log.d(TAG, "ArrayList is:" + localContactList.size());
            putIntoJsonFile();
            sharedPreferences = this.getActivity().getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);
            String accessToken = sharedPreferences.getString(new GlobalStrings().getAccessTokenOfDropbox(), "");
            AsyncTask downloadHashCodeTask = new DownloadHashCodeFileTask(DropboxClient.getClient(accessToken), file, hashCodeFile, getActivity(), localContactList, new IDownloadJsonString<String>() {
                @Override
                public void onTaskException(Exception exception) {
                    new ExceptionReportHandler().showCatchExceptions(TAG, exception);
                }

                @Override
                public void onTaskComplete(String downloadHexString) {
                    String hexString = "";
                    JSONObject hashcodeJsonObject = null;
                    try {
                        hashcodeJsonObject = new JSONObject(downloadHexString);
                        hexString = hashcodeJsonObject.getString("HashCode");
                        Log.d(TAG, "hashcodestring:" + hexString);
                    } catch (JSONException e) {
                        new ExceptionReportHandler().showCatchExceptions(TAG, e);
                    }
                    if (hashCodeOfLocalContacts.equals(hexString)) {
                        Toast.makeText(getActivity(), "Your contacts are up-to-date", Toast.LENGTH_LONG).show();
                    } else {
                        String accessToken = sharedPreferences.getString(new GlobalStrings().getAccessTokenOfDropbox(), "");
                        new DownloadServerContactsTask(DropboxClient.getClient(accessToken), file, getActivity(), localContactList, view, new IDowanloadServerContacts() {
                            @Override
                            public void onTaskComplete(List<Contact> serverContactList) {
                                namingConflicts(serverContactList);
                            }

                            @Override
                            public void onTaskException(Exception exception) {
                                new ExceptionReportHandler().showCatchExceptions(TAG, exception);
                                Toast.makeText(getActivity(), "There is error in connecting internet..", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "UnknownHostException,ConnectException,SocketTimeoutException", exception);
                            }
                        }).execute();
                    }
                }
            });
            downloadHashCodeTask.execute();
        } catch (FileNotFoundException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    private String putIntoHashcodeFile() throws IOException {
        String hashCodeOfLocalPhonebookContacts = "";
        JSONArray jArray = new JSONArray();
        Log.d(TAG, "JSONArray is:" + jArray.toString());
        hashCodeFile = new File(getContext().getFilesDir().getPath().toString() + "ContactsHashcode.json");
        try (FileWriter fileWriterOfHashcodeFile = new FileWriter(hashCodeFile)) {
            Log.d(TAG, "ArrayList is:" + localContactList.size());
            for (int o = 0; o < localContactList.size(); o++) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("contactName", localContactList.get(o).getName());
                jsonObject.put("contactFirstName", localContactList.get(o).getFirstName());
                jsonObject.put("contactMiddleName", localContactList.get(o).getMiddleName());
                jsonObject.put("contactLastName", localContactList.get(o).getLastName());
                JSONArray phoneNumberJsonArray = new JSONArray();
                List<ContactType> localPhoneContactTypeList = localContactList.get(o).getPhoneNumberList();
                for (int j = 0; j < localPhoneContactTypeList.size(); j++) {
                    JSONObject phoneNumberGroup = new JSONObject();
                    phoneNumberGroup.put("PhoneNumber1", localPhoneContactTypeList.get(j).getPhoneNumber());
                    phoneNumberJsonArray.put(phoneNumberGroup);
                }
                jsonObject.put("contactPhone", phoneNumberJsonArray);
                JSONArray emailIdJsonArray = new JSONArray();
                List<ContactType> localEmailContactTypeList = localContactList.get(o).getEmailList();
                for (int j = 0; j < localEmailContactTypeList.size(); j++) {
                    JSONObject emailIdGroup = new JSONObject();
                    emailIdGroup.put("EmailId1", localEmailContactTypeList.get(j).getPhoneNumber());
                    emailIdJsonArray.put(emailIdGroup);
                }
                jsonObject.put("contactEmail", emailIdJsonArray);
                jArray.put(jsonObject);
            }
            String jsonStringFinal = jArray.toString();
            Log.d(TAG, "String is:" + jsonStringFinal);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(jsonStringFinal.getBytes());
            byte[] byteData = md.digest();
            //convert the byte to hex format method
            StringBuilder hexString = new StringBuilder();
            for (int m = 0; m < byteData.length; m++) {
                String hex = Integer.toHexString(0xff & byteData[m]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            Log.d(TAG, "Digest(in hex format):: " + hexString.toString());
            hashCodeOfLocalPhonebookContacts = hexString.toString();
            boolean haseCodeFileValue = hashCodeFile.createNewFile();
            if (haseCodeFileValue) {

                Log.d(TAG, "Hashcode file is not created");
            } else {
                JSONObject jObject = new JSONObject();
                jObject.put("HashCode", hashCodeOfLocalPhonebookContacts);
                Log.d("json object hashcode:", jObject.toString());
                fileWriterOfHashcodeFile.write(jObject.toString());
                fileWriterOfHashcodeFile.flush();
            }
        } catch (JSONException | IOException | NoSuchAlgorithmException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
        return hashCodeOfLocalPhonebookContacts;
    }

    private void putIntoJsonFile() {
        JSONArray jsonArray = new JSONArray();
        file = new File(getContext().getFilesDir().getPath().toString() + "ContactsJSONFile.json");
        try (FileWriter fileWriter = new FileWriter(file)) {
            for (int m = 0; m < localContactList.size(); m++) {
                JSONObject jObject = new JSONObject();
                jObject.put(new GlobalStrings().getName(), localContactList.get(m).getName());
                jObject.put(new GlobalStrings().getFirstName(), localContactList.get(m).getFirstName());
                jObject.put(new GlobalStrings().getMiddleName(), localContactList.get(m).getMiddleName());
                jObject.put(new GlobalStrings().getLastName(), localContactList.get(m).getLastName());
                if (localContactList.get(m).getProfile() != null && !localContactList.get(m).getProfile().isEmpty()) {
                    jObject.put(new GlobalStrings().getHasImage(), true);
                } else {
                    jObject.put(new GlobalStrings().getHasImage(), false);
                }
                JSONArray phoneJsonArray = new JSONArray();
                List<ContactType> localPhoneContactTypes = localContactList.get(m).getPhoneNumberList();
                for (int n = 0; n < localPhoneContactTypes.size(); n++) {
                    JSONObject phoneGroup = new JSONObject();
                    phoneGroup.put(new GlobalStrings().getType(), localPhoneContactTypes.get(n).getPhoneType());
                    phoneGroup.put(new GlobalStrings().getNumber(), localPhoneContactTypes.get(n).getPhoneNumber());
                    phoneJsonArray.put(phoneGroup);
                }
                jObject.put(new GlobalStrings().getPhone(), phoneJsonArray);
                JSONArray emailJsonArray = new JSONArray();
                List<ContactType> localEmailContactTypes = localContactList.get(m).getEmailList();
                for (int n = 0; n < localEmailContactTypes.size(); n++) {
                    JSONObject emailGroup = new JSONObject();
                    emailGroup.put(new GlobalStrings().getType(), localEmailContactTypes.get(n).getPhoneType());
                    emailGroup.put(new GlobalStrings().getId(), localEmailContactTypes.get(n).getPhoneNumber());
                    emailJsonArray.put(emailGroup);
                }
                jObject.put(new GlobalStrings().getEmail(), emailJsonArray);
                jsonArray.put(jObject);
            }
            boolean jsonFileValue = file.createNewFile();
            if (jsonFileValue) {
                Log.d(TAG, "contacts json file is not created.");
            } else {
                Log.d(TAG, "Writing JSON object to file");
                Log.d(TAG, "------------------------");
                Log.d(TAG, "contact object is:" + jsonArray);
                fileWriter.write(jsonArray.toString());
                fileWriter.flush();
            }
        } catch (JSONException | IOException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    private void readContacts() {
        try {
            Cursor cur = new Helper().getContactsCursor(getActivity());
            if (cur.getCount() > 0) {
                localContactList.clear();
                while (cur.moveToNext()) {
                    String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    Log.d("Id", "Id is:" + id);
                    String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    Log.d(TAG, name);

                    String firstName = "";
                    String lastName = "";
                    String middleName = "";
                    // get firstName, middleName & lastName
                    Cursor nameCur = new Helper().getContactsNameCursor(getActivity(), id);
                    FirstMiddleLastNameStrings firstMiddleLastNameStrings = new Helper().getFirstMiddleLastName(nameCur);
                    firstName = firstMiddleLastNameStrings.getFirstNameOfContact();
                    middleName = firstMiddleLastNameStrings.getMiddleNameOfContact();
                    lastName = firstMiddleLastNameStrings.getLastNameOfContact();
                    String imageUri = (cur.getString(cur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)));
                    Log.d(TAG, "Image URI:" + imageUri);
                    if (imageUri == null) {
                        imageUri = "";
                    }
                    Cursor pCur = new Helper().getContactPhoneNumberCursor(id, getActivity());
                    List<ContactType> phoneContactTypeList = new ArrayList<>();
                    while (pCur.moveToNext()) {
                        int phoneType = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        Log.e(TAG, "Phone Type " + phoneType);
                        String phoneNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        Log.e(TAG, "Phone Number: " + phoneNumber);
                        String phoneTypeString = getPhoneTypeString(phoneType, pCur);
                        phoneContactTypeList.add(new ContactType(phoneTypeString, phoneNumber));
                    }
                    pCur.close();
                    List<ContactType> emailContactTypeList = new ArrayList<>();
                    Cursor emailCur = new Helper().getContactEmailCursor(id, getActivity());
                    while (emailCur.moveToNext()) {
                        id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                        Log.d(TAG, "Id is:" + id);
                        String emailId = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        int emailType = emailCur.getInt(
                                emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                        Log.d(TAG, "Email type is:" + id);
                        String emailTypeString = getEmailTypeString(emailType, emailCur);
                        emailContactTypeList.add(new ContactType(emailTypeString, emailId));
                    }
                    emailCur.close();
                    localContactList.add(new Contact(name, firstName, middleName, lastName, phoneContactTypeList, emailContactTypeList, imageUri, false));
                }
                cur.close();
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    public String getPhoneTypeString(int phoneType, Cursor pCur) {

        String sType = "";
        if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
            Log.e(": TYPE_CUSTOM", " " + phoneType);
            String customLabel = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
            String phoneLabel = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(this.getResources(), phoneType, customLabel);
            sType = phoneLabel;
            Log.d(TAG, "Phone label is:" + phoneLabel);
        } else {
            Log.e(": TYPE", " " + phoneType);
            PhoneTypeEnum.Type type = PhoneTypeEnum.Type.fromInteger(phoneType);
            sType = type.getName();
        }
        return sType;
    }

    public String getEmailTypeString(int emailType, Cursor emailCur) {
        String eType = "";
        if (emailType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
            Log.e(": TYPE_CUSTOM", " " + emailType);
            String customLabel = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL));
            String emailIdLabel = (String) ContactsContract.CommonDataKinds.Email.getTypeLabel(this.getResources(), emailType, customLabel);
            Log.d(TAG, "Email id label is:" + emailIdLabel);
            eType = emailIdLabel;
        } else {
            Log.e(": TYPE_HOME", " " + emailType);
            EmailTypeEnum.Type type = EmailTypeEnum.Type.fromInteger(emailType);
            eType = type.getName();
        }
        return eType;
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, " From Close -  AutoCloseable  ");
    }

    private void namingConflicts(List<Contact> serverContactList) {
        modifiedContactHashMap = new HashMap<>();
        if (localContactList.isEmpty()) {
            for (Contact serverContact : serverContactList) {
                modifiedContactHashMap.put(serverContact.getName(), serverContact);
            }
        } else {
            for (Contact localContact : localContactList) {
                for (Contact serverContact : serverContactList) {
                    modifiedContactHashMap = removeNamingConflictsFromServerContactList(localContact, serverContact, serverContactList);
                }
            }
        }
        modifiedContactList = new ArrayList<>(modifiedContactHashMap.values());
        insertContactWrapper();
    }

    private HashMap<String, Contact> removeNamingConflictsFromServerContactList(Contact localContact, Contact serverContact, List<Contact> serverContactList) {
        if (serverContactList.isEmpty()) {
            modifiedContactHashMap.put(localContact.getName(), localContact);
        } else {
            if (localContact.getName().equalsIgnoreCase(serverContact.getName())) {
                Log.d(TAG, "phone issue - conflict  - if1 ");
                HashSet<ContactType> modifiedPhoneContactTypeHashSet = new HashSet<>();
                modifiedPhoneContactTypeHashSet.addAll(localContact.getPhoneNumberList());
                modifiedPhoneContactTypeHashSet.addAll(serverContact.getPhoneNumberList());
                List<ContactType> serverPhoneList = new ArrayList<>(modifiedPhoneContactTypeHashSet);

                HashSet<ContactType> modifiedEmailContactTypeHashSet = new HashSet<>();
                modifiedEmailContactTypeHashSet.addAll(localContact.getEmailList());
                modifiedEmailContactTypeHashSet.addAll(serverContact.getEmailList());
                List<ContactType> serverEmailsList = new ArrayList<>(modifiedEmailContactTypeHashSet);
                modifiedContactHashMap.put(localContact.getName(),
                        new Contact(localContact.getName(), localContact.getFirstName(),
                                localContact.getMiddleName(), localContact.getLastName(), serverPhoneList, serverEmailsList, localContact.getProfile(),
                                serverContact.isHasImage()));
            } else {
                Log.d(TAG, "+++++++++++else loop+++++++++++++");
                Log.d(TAG, "phone issue - conflict  - else1 ");
                if (!modifiedContactHashMap.containsKey(serverContact.getName())) {
                    modifiedContactHashMap.put(serverContact.getName(), serverContact);
                }
                if (!modifiedContactHashMap.containsKey(localContact.getName())) {
                    modifiedContactHashMap.put(localContact.getName(), localContact);
                }
            }
        }
        return modifiedContactHashMap;
    }

    private void insertContactWrapper() {
        try {
            Log.e(TAG, "In insertContactWrapper method ----------");
            // Check the SDK version and whether the permission is already granted or not.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
                //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
            } else {
                // Android version is lesser than 6.0 or the permission is already granted.
                i = 0;
                isDownlaodCompleted = false;
                getImages();
            }
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    private void getImages() {
        try {
            Log.e(TAG, "In get images method ----------");
            if (isDownlaodCompleted) {
                Log.e(TAG, "In isDownload completed method ---------- true");
                writeInPhoneBook(modifiedContactList);
            } else {
                Log.e(TAG, "In isDownload completed method ---------- false");

                if (i < modifiedContactList.size()) {
                    Log.e(TAG, "In get images method ---------- if");
                    if ((modifiedContactList.get(i).getProfile() == null
                            || modifiedContactList.get(i).getProfile().isEmpty()) && modifiedContactList.get(i).isHasImage()) {
                        Log.e(TAG, "getting image ---------- " + modifiedContactList.get(i).getName());
                        sharedPreferences = this.getActivity().getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);
                        String accessToken = sharedPreferences.getString(new GlobalStrings().getAccessTokenOfDropbox(), "");

                        new DownloadContactImageTask(DropboxClient.getClient(accessToken),
                                getActivity(), modifiedContactList.get(i), new IDowanloadImage() {
                            @Override
                            public void onTaskComplete() {
                                Log.d(TAG, "Download Image Complete");
                                i++;
                                getImages();
                            }

                            @Override
                            public void onTaskException(Exception exception) {
                                new ExceptionReportHandler().showCatchExceptions(TAG, exception);
                                Log.e(TAG, "Download Image Exception", exception);
                                i++;
                                getImages();
                            }
                        }).execute();
                    } else {
                        Log.e(TAG, "In get images method ----------else 2");
                        i++;
                        getImages();
                    }
                } else {
                    Log.e(TAG, "In get images method ----------else");
                    isDownlaodCompleted = true;
                    getImages();
                }
            }
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
    }

    private void showContacts() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_DENIED
                && checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            readContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           final int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                insertContactWrapper();
            } else {
                //Permission Denied
                Snackbar snackbarMsg = Snackbar.make(getView(), "Write Contacts Denied", Snackbar.LENGTH_LONG);
                View snackbarView = snackbar.getView();
                TextView tv = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.textcolor));
                snackbarMsg.show();
            }
        } else if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            } else {
                snackbar = Snackbar.make(getView(), "We will be able to sync your contacts only if you allow us to access it.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Handle user action
                                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                                    showContacts();
                                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                                    showContacts();
                                } else {
                                    showContacts();
                                }
                            }
                        });
                snackbar.setActionTextColor(Color.WHITE);
                View viewOfSnackbar = snackbar.getView();
                TextView tv = (TextView) viewOfSnackbar.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.textcolor));
                snackbar.show();
            }
        }
    }

    private void writeInPhoneBook(List<Contact> serverContactList) {
        new WriteIntoPhonebookTask(getActivity(), serverContactList).execute();
    }

    private int getServerFilePhoneType(String serverType) {
        int phType = 0;
        Log.d(TAG, "serverType" + serverType);
        Log.e(": Home", " " + serverType);
        PhoneTypeEnum.Type type = PhoneTypeEnum.Type.fromString(serverType);
        phType = type.getValue();
        Log.d(TAG, "phonetype" + phType);
        return phType;
    }

    private int getServerFileEmailtype(String serverEmailType) {
        int eType = 0;
        Log.e(": Home", " " + serverEmailType);
        EmailTypeEnum.Type type = EmailTypeEnum.Type.fromString(serverEmailType);
        eType = type.getValue();
        return eType;
    }

    public class WriteIntoPhonebookTask extends AsyncTask {
        private Activity context;
        private List<Contact> serverContactList;
        private ProgressDialog progressDialog;

        WriteIntoPhonebookTask(Activity context, List<Contact> serverContactList) {
            this.context = context;
            this.serverContactList = serverContactList;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                String msg = "Updating Phonebook ...";
                progressDialog = new Helper().getProgressDialog(context, msg);
                progressDialog.show();
            } catch (Exception e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
            }

        }

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                Cursor cursor = new Helper().getContactsCursor(getActivity());
                if (cursor != null && cursor.moveToFirst()) {
                    Log.d(TAG, "Cursor Size  = " + cursor.getCount());
                    for (Contact contact : serverContactList) {
                        boolean isNameExist = false;
                        String rawId = null;
                        cursor.moveToFirst();
                        Log.d(TAG, "Contact Name  = " + contact.getName());
                        do {
                            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            String profile = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                            Log.d(TAG, "Cursor Name  = " + name);
                            Log.d(TAG, "Cursor Profile  = " + profile);
                            if (name.equalsIgnoreCase(contact.getName())) {
                                isNameExist = true;
                                rawId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                                break;
                            }
                        } while (cursor.moveToNext());
                        ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();
                        if (isNameExist) {
                            Log.d(TAG, "Exist Name  = " + contact.getName());
                            List<ContactType> serverPhoneContactTypeList = new ArrayList<>(contact.getPhoneNumberList());
                            Cursor phCur = new Helper().getContactPhoneNumberCursor(rawId, getActivity());
                            Log.d(TAG, "phone cursor value:" + String.valueOf(phCur));

                            checkLocalPhoneAndServerPhone(phCur, serverPhoneContactTypeList);
                            writeServerPhoneNumberIntoPhone(rawId, contentProviderOperations, serverPhoneContactTypeList);

                            Log.d(TAG, "Exist Name  = " + contact.getName());
                            List<ContactType> serverEmailContactTypeList = new ArrayList<>(contact.getEmailList());
                            Cursor emailCur = new Helper().getContactEmailCursor(rawId, getActivity());

                            checkLocalEmailAndServerEmail(emailCur, serverEmailContactTypeList);
                            writeServerEmailIdIntoPhone(rawId, contentProviderOperations, serverEmailContactTypeList);

                            writeContactProfileIntoPhonebook(contact, rawId, contentProviderOperations);
                            applyBatchOperation(contentProviderOperations);
                        } else {
                            int rawContactID = 0;
                            Log.d(TAG, "New Name  = " + contact.getName());
                            contentProviderOperations.add(ContentProviderOperation.newInsert(
                                    ContactsContract.RawContacts.CONTENT_URI)
                                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                    .build());
                            contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract
                                            .CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName())

                                    .build());

                            List<ContactType> serverPhoneContactTypes = new ArrayList<>(contact.getPhoneNumberList());
                            newNameWriteServerPhoneNumberIntoPhoneBook(rawContactID, contentProviderOperations, serverPhoneContactTypes);

                            List<ContactType> serverEmailContactTypes = new ArrayList<>(contact.getEmailList());
                            newNameWriteServerEmailIdIntoPhoneBook(rawContactID, contentProviderOperations, serverEmailContactTypes);

                            new Helper().writePhotoInPhonebook(contact, contentProviderOperations, rawContactID);
                            applyBatchOperation(contentProviderOperations);
                        }
                    }
                    cursor.close();
                } else {
                    for (Contact contact : serverContactList) {
                        Log.d(TAG, "New Name  = " + contact.getName());
                        ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();
                        int rawContactID = contentProviderOperations.size();
                        contentProviderOperations.add(ContentProviderOperation.newInsert(
                                ContactsContract.RawContacts.CONTENT_URI)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                .build());
                        contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract
                                        .CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName())
                                .build());

                        List<ContactType> serverPhoneContactTypes = new ArrayList<>(contact.getPhoneNumberList());
                        newNameWriteServerPhoneNumberIntoPhoneBook(rawContactID, contentProviderOperations, serverPhoneContactTypes);
                        List<ContactType> serverEmailContactTypes = new ArrayList<>(contact.getEmailList());
                        newNameWriteServerEmailIdIntoPhoneBook(rawContactID, contentProviderOperations, serverEmailContactTypes);
                        new Helper().writePhotoInPhonebook(contact, contentProviderOperations, rawContactID);
                        applyBatchOperation(contentProviderOperations);
                    }
                }
                return null;
            } catch (Exception e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
                return null;
            }
        }

        private void newNameWriteServerEmailIdIntoPhoneBook(int rawContactID, List<ContentProviderOperation> contentProviderOperations, List<ContactType> serverEmailContactTypes) {
            for (int j = 0; j < serverEmailContactTypes.size(); j++) {
                int eType = getServerFileEmailtype(serverEmailContactTypes.get(j).getPhoneType());
                if (eType == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
                    Log.d(TAG, "New value = " + serverEmailContactTypes.get(j).getPhoneNumber());
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, serverEmailContactTypes.get(j).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, eType)
                            .withValue(ContactsContract.CommonDataKinds.Email.LABEL, serverEmailContactTypes.get(j).getPhoneType())
                            .build());
                } else {
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, serverEmailContactTypes.get(j).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, eType)
                            .build());
                }
            }
        }
        private void modifiedFileWriting() {
            try {
                putIntoHashcodeFile();
                Log.d(TAG, "File Size = " + hashCodeFile.length());
                putIntoJsonFile();
                FileInputStream imageInputStream = (FileInputStream) putIntoImagesFolder();
                Log.d(TAG, "IMAGE INPUT STREAM" + imageInputStream);
                sharedPreferences = getContext().getSharedPreferences(new GlobalStrings().getDropboxIntegration(), Context.MODE_PRIVATE);
                String accessToken = sharedPreferences.getString(new GlobalStrings().getAccessTokenOfDropbox(), "");
                new UploadHashCodeFileTask(DropboxClient.getClient(accessToken), hashCodeFile, file, localContactList, getActivity()).execute();

            } catch (Exception e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
            }
        }
        private void newNameWriteServerPhoneNumberIntoPhoneBook(int rawContactID, List<ContentProviderOperation> contentProviderOperations, List<ContactType> serverPhoneContactTypes) {
            for (int r = 0; r < serverPhoneContactTypes.size(); r++) {
                int phType = getServerFilePhoneType(serverPhoneContactTypes.get(r).getPhoneType());
                if (phType == (ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)) {
                    Log.d(TAG, "newNameWriteServerPhoneNumberIntoPhoneBook  if ---");
                    Log.d(TAG, "New value = " + serverPhoneContactTypes.get(r).getPhoneNumber());
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, serverPhoneContactTypes.get(r).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phType)
                            .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, serverPhoneContactTypes.get(r).getPhoneType())
                            .build());
                } else {
                    Log.d(TAG, "newNameWriteServerPhoneNumberIntoPhoneBook  else ---");
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, serverPhoneContactTypes.get(r).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phType)
                            .build());
                }
            }
        }

        private void applyBatchOperation(List<ContentProviderOperation> contentProviderOperations) {
            try {
                getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, (ArrayList<ContentProviderOperation>) contentProviderOperations);
            } catch (RemoteException | OperationApplicationException  e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
            } catch (Exception e3) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e3);
            }
        }

        private void writeContactProfileIntoPhonebook(Contact contact, String rawId, ArrayList<ContentProviderOperation> contentProviderOperations) {
            if (contact.getProfile() == null || contact.getProfile().isEmpty()) {
                Log.e(TAG, "INSERTING IMAGE" + contact.getName());
                if (contact.isHasImage()) {
                    Log.d(TAG, "Image" + contact.getName());
                    byte[] byteArray = new Helper().getBytesFromBitmap(contact);
                    if (byteArray != null) {
                        Log.d(TAG, "Image byte array not null");
                        String rawContactId = new Helper().getRawContactId(rawId, getActivity());
                        contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray)
                                .build());
                    }
                }
            }
        }

        private void writeServerEmailIdIntoPhone(String rawId, ArrayList<ContentProviderOperation> contentProviderOperations, List<ContactType> serverEmailContactTypeList) {
            for (int j = 0; j < serverEmailContactTypeList.size(); j++) {
                int eType = getServerFileEmailtype(serverEmailContactTypeList.get(j).getPhoneType());
                if (eType == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
                    Log.d(TAG, "Add Email = " + serverEmailContactTypeList.get(j).getPhoneNumber());
                    String rawContactId = new Helper().getRawContactId(rawId, getActivity());
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, serverEmailContactTypeList.get(j).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, eType)
                            .withValue(ContactsContract.CommonDataKinds.Email.LABEL, serverEmailContactTypeList.get(j).getPhoneType())
                            .build());
                } else {
                    Log.d(TAG, "Add Email = " + serverEmailContactTypeList.get(j).getPhoneNumber());
                    String rawContactId = new Helper().getRawContactId(rawId, getActivity());
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, serverEmailContactTypeList.get(j).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, eType)
                            .build());
                }
            }
        }

        private void writeServerPhoneNumberIntoPhone(String rawId, List<ContentProviderOperation> contentProviderOperations, List<ContactType> serverPhoneContactTypeList) {
            for (int j = 0; j < serverPhoneContactTypeList.size(); j++) {
                int phType = getServerFilePhoneType(serverPhoneContactTypeList.get(j).getPhoneType());
                Log.d(TAG, "Add Phone = " + serverPhoneContactTypeList.get(j).getPhoneNumber());
                String rawContactId = new Helper().getRawContactId(rawId, getActivity());
                if (phType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                    Log.d(TAG, "writeServerPhoneNumberIntoPhone  if ---");
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, serverPhoneContactTypeList.get(j).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phType)
                            .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, serverPhoneContactTypeList.get(j).getPhoneType())
                            .build());
                } else {
                    Log.d(TAG, "writeServerPhoneNumberIntoPhone  else ---");
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, serverPhoneContactTypeList.get(j).getPhoneNumber())
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phType)
                            .build());
                }
            }
        }

        private void checkLocalPhoneAndServerPhone(Cursor phCur, List<ContactType> serverPhoneContactTypeList) {
            if (phCur != null) {
                while (phCur.moveToNext()) {
                    int indexPh = phCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String localPh = phCur.getString(indexPh);
                    int indexPhType = phCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                    String localPhType = phCur.getString(indexPhType);
                    Log.d(TAG, "Exist Phone  = " + localPh);
                    for (int f = 0; f < serverPhoneContactTypeList.size(); f++) {
                        ContactType contactType = new ContactType(localPhType, localPh);
                        Log.d(TAG, "+++++++++++++++++++++++++" + serverPhoneContactTypeList.get(f).getPhoneNumber());
                        Log.d(TAG, "Phone type of phonelist:" + serverPhoneContactTypeList.get(f).getPhoneType());
                        Log.d(TAG, "Phone type of contactType:" + contactType.getPhoneType());
                        removePhoneNumber(contactType, serverPhoneContactTypeList, f);
                    }
                }
                phCur.close();
            }
        }

        private void removePhoneNumber(ContactType contactType, List<ContactType> serverPhoneContactTypeList, int f) {
            if (serverPhoneContactTypeList.get(f).getPhoneNumber().contains(contactType.getPhoneNumber())) {
                Log.d(TAG, "Remove Phone  = " + contactType.getPhoneNumber());
                serverPhoneContactTypeList.remove(f);
            }
        }

        private void checkLocalEmailAndServerEmail(Cursor emailCur, List<ContactType> serverEmailContactTypeList) {
            if (emailCur != null) {
                while (emailCur.moveToNext()) {
                    int indexEmail = emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                    String localEmail = emailCur.getString(indexEmail);
                    int indexEmailType = emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE);
                    String localEmailType = emailCur.getString(indexEmailType);
                    Log.d(TAG, "Exist Email  = " + localEmail);
                    for (int q = 0; q < serverEmailContactTypeList.size(); q++) {
                        ContactType contactType = new ContactType(localEmailType, localEmail);
                        Log.d(TAG, "+++++++++++++++++++++++++" + serverEmailContactTypeList.get(q).getPhoneNumber());
                        Log.d(TAG, "Phone type of phonelist:" + serverEmailContactTypeList.get(q).getPhoneType());
                        Log.d(TAG, "Phone type of contactType:" + contactType.getPhoneType());
                        removeEmailId(contactType, serverEmailContactTypeList, q);
                    }
                }
                emailCur.close();
            }
        }
        private InputStream putIntoImagesFolder() {
            FileInputStream imageInputStream = null;
            File imageFile;
            for (int m = 0; m < localContactList.size(); m++) {
                imageFile = new File(getContext().getFilesDir().getPath().toString() + localContactList.get(m).getName() + ".png");
                imageInputStream = writeIntoImageFolder(imageFile, m);
            }
            return imageInputStream;
        }
        private FileInputStream writeIntoImageFolder(File imageFile, int m) {
            FileInputStream imageInputStream = null;
            try (FileWriter fileWriter = new FileWriter(imageFile)) {
                String image = localContactList.get(m).getProfile();
                Log.d(TAG, "IMAGE PATH FROM ARRAYLIST::" + image);
                if (image != null && !image.isEmpty()) {
                    Log.d(TAG, "IFFFFFF::" + image);
                    imageInputStream = (FileInputStream) getActivity().getContentResolver().openInputStream(Uri.parse(image));
                    Log.d(TAG, "IMAGE PATH" + image);
                    boolean imageFileNewFile = imageFile.createNewFile();
                    if (imageFileNewFile) {
                        Log.d(TAG, "Images folder is not created.");
                    } else {
                        Log.d(TAG, "Writing images object to file");
                        Log.d(TAG, "------------------------");
                        fileWriter.write(image);
                        fileWriter.flush();
                    }
                }
            } catch (IOException e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
            }
            return imageInputStream;
        }

        private void removeEmailId(ContactType contactType, List<ContactType> serverEmailContactTypeList, int q) {
            if (serverEmailContactTypeList.get(q).getPhoneNumber().contains(contactType.getPhoneNumber())) {
                Log.d(TAG, "Remove Phone  = " + contactType.getPhoneNumber());
                serverEmailContactTypeList.remove(q);
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                new ExceptionReportHandler().showCatchExceptions(TAG, e);
            }
            readContacts();
            modifiedFileWriting();
        }
    }
}