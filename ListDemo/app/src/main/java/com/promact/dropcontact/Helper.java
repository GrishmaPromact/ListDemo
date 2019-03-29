package com.promact.dropcontact;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by grishma on 19-05-2017.
 */
public class Helper {
    private static final String TAG = "Helper";

    public ProgressDialog getProgressDialog(Activity context, String msg) {
        ProgressDialog progressDialog = null;
        try {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(msg);
            progressDialog.setCancelable(true);

        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        }
        return progressDialog;
    }

    public void writePhotoInPhonebook(Contact contact, List<ContentProviderOperation> contentProviderOperations, int rawContactID) {
        if (contact.getProfile() == null || contact.getProfile().isEmpty()) {
            Log.e(TAG, "INSERTING IMAGE ==== " + contact.getName());
            if (contact.isHasImage()) {
                Log.d(TAG, "Image ========= " + contact.getName());
                byte[] byteArray = getBytesFromBitmap(contact);
                if (byteArray != null) {
                    Log.d(TAG, "Image byte array ------- not null");
                    contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray)
                            .build());
                }
            }
        }
    }

    public String getRawContactId(String contactId, FragmentActivity activity) {
        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{contactId};
        Cursor cur = activity.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);
        Log.d(TAG, "curser value:" + String.valueOf(cur.getCount()));
        cur.moveToNext();
        String id = cur.getString(cur.getColumnIndex(ContactsContract.RawContacts._ID));
        cur.close();
        return id;
    }

    public byte[] getBytesFromBitmap(Contact contact) {
        byte[] hello = null;
        try {
            File dirPath = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File imagefile = new File(dirPath, contact.getName() + ".png");
            if (imagefile.exists()) {
                Log.d(TAG, "Image exists ------- ");
                FileInputStream fis = null;
                fis = new FileInputStream(imagefile);
                Bitmap bm = BitmapFactory.decodeStream(fis);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
                Log.d(TAG, "Image byte array -------" + Arrays.toString(baos.toByteArray()));
                hello = baos.toByteArray();
                return hello;
            } else {
                Log.d(TAG, "Image byte array ------- null");
                return hello;
            }
        } catch (FileNotFoundException e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
        } catch (Exception e2) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e2);
        }
        return hello;
    }

    public Cursor getContactsCursor(FragmentActivity activity) {
        Cursor cursor = null;
        try {
            String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + "= 0" + " OR " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "= 1";
            String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
            ContentResolver cr = activity.getContentResolver();
            return cr.query(ContactsContract.Contacts.CONTENT_URI, null, selection, null, sortOrder);
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
            return cursor;
        }
    }

    public Cursor getContactPhoneNumberCursor(String id, FragmentActivity activity) {
        return activity.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + "!= ?", new String[]{id, "com.whatsapp"}, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
    }

    public Cursor getContactEmailCursor(String id, FragmentActivity activity) {
        return activity.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null);
    }

    public Cursor getContactsNameCursor(FragmentActivity activity, String id) {

        Cursor cursor = null;
        try {
            String selection = ContactsContract.Data.MIMETYPE
                    + " = ? AND "
                    + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID
                    + " = ?";
            String[] sortOrder = new String[]{
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    Long.valueOf(id).toString()};
            ContentResolver cr = activity.getContentResolver();
            return cr.query(ContactsContract.Data.CONTENT_URI, null, selection, sortOrder, null);
        } catch (Exception e) {
            new ExceptionReportHandler().showCatchExceptions(TAG, e);
            return cursor;
        }
    }

    public void recyclerViewOnClick(View view, int position, List<Contact> localContactList, FragmentActivity activity) {
        Contact contact = localContactList.get(position);
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
        View sheetView = activity.getLayoutInflater().inflate(R.layout.contacts_bottomsheet, (ViewGroup) view, false);
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
        BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        bottomSheetDialog.dismiss();
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:

                        break;
                    default:

                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Do nothing
            }
        };
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) sheetView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();
        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
        sheetView = (View) sheetView.getParent();
        sheetView.setFitsSystemWindows(true);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(sheetView);
        sheetView.measure(0, 0);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int screenHeight = displaymetrics.heightPixels;
        bottomSheetBehavior.setPeekHeight(screenHeight);
        if (params.getBehavior() instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) params.getBehavior()).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
        params.height = screenHeight;
        sheetView.setLayoutParams(params);
        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) sheetView.findViewById(R.id.profileactivity_collapsing_toolbar);
        collapsingToolbarLayout.setTitle(contact.getName());
        if (contact != null) {
            LinearLayout linearLayoutPhone = (LinearLayout) sheetView.findViewById(R.id.phoneLayout);
            LinearLayout linearLayoutEmail = (LinearLayout) sheetView.findViewById(R.id.emailLayout);
            CardView cardViewPhone = (CardView) sheetView.findViewById(R.id.card_view);
            CardView cardViewEmail = (CardView) sheetView.findViewById(R.id.card_view1);
            ImageView contactImage = (ImageView) sheetView.findViewById(R.id.contactImage);
            ImageView iconPhone = (ImageView) sheetView.findViewById(R.id.phone);
            ImageView iconEmail = (ImageView) sheetView.findViewById(R.id.emailID);
            if (contact.getProfile() == null || contact.getProfile().isEmpty()) {
                contactImage.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_person_white_48dp));
            } else {
                contactImage.setImageTintMode(null);
                contactImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                contactImage.setImageURI(Uri.parse(contact.getProfile()));
            }
            linearLayoutPhone.removeAllViews();
            for (int p = 0; p < contact.getPhoneNumberList().size(); p++) {
                LayoutInflater layoutInflater = LayoutInflater.from(activity);
                View subView = layoutInflater.inflate(R.layout.listview_layout_phone, null);
                TextView phoneTextView = (TextView) subView.findViewById(R.id.phno);
                TextView phoneType = (TextView) subView.findViewById(R.id.phoneType);
                phoneTextView.setText(contact.getPhoneNumberList().get(p).getPhoneNumber());
                String str = contact.getPhoneNumberList().get(p).getPhoneType();
                String[] strArray = str.split(" ");
                StringBuilder builder = new StringBuilder();
                for (String s : strArray) {
                    String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
                    builder.append(cap + " ");
                }
                Log.d(TAG, "builder string---------------:" + builder.toString());
                phoneType.setText(builder.toString());
                linearLayoutPhone.addView(subView);
            }
            if (contact.getPhoneNumberList().size() > 0) {
                Log.d("if", "IN APP PHONE VISIBLE");
                linearLayoutPhone.setVisibility(View.VISIBLE);
                cardViewPhone.setVisibility(View.VISIBLE);
                iconPhone.setVisibility(View.VISIBLE);
            } else {
                Log.d("else", "IN APP PHONE GONE");
                cardViewPhone.setVisibility(View.GONE);
                linearLayoutPhone.setVisibility(View.GONE);
                iconPhone.setVisibility(View.GONE);
            }
            linearLayoutEmail.removeAllViews();
            for (int g = 0; g < contact.getEmailList().size(); g++) {
                LayoutInflater layoutInflater;
                layoutInflater = LayoutInflater.from(activity);
                View subViewEmail = layoutInflater.inflate(R.layout.listview_layout_email, null);
                TextView emailTextView = (TextView) subViewEmail.findViewById(R.id.email);
                TextView emailType = (TextView) subViewEmail.findViewById(R.id.emailType);
                emailTextView.setText(contact.getEmailList().get(g).getPhoneNumber());
                String str = contact.getEmailList().get(g).getPhoneType();
                String[] strArray = str.split(" ");
                StringBuilder builder = new StringBuilder();
                for (String s : strArray) {
                    String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
                    builder.append(cap + " ");
                }
                Log.d(TAG, "builder string---------------:" + builder.toString());
                emailType.setText(builder.toString());
                linearLayoutEmail.addView(subViewEmail);
            }
            if (contact.getEmailList().size() > 0) {
                Log.d("if", "IN APP EMAIL VISIBLE");
                linearLayoutEmail.setVisibility(View.VISIBLE);
                cardViewEmail.setVisibility(View.VISIBLE);
                iconEmail.setVisibility(View.VISIBLE);
            } else {
                Log.d("else", "IN APP EMAIL GONE");
                linearLayoutEmail.setVisibility(View.GONE);
                cardViewEmail.setVisibility(View.GONE);
                iconEmail.setVisibility(View.GONE);
            }
        }
    }


    public FirstMiddleLastNameStrings getFirstMiddleLastName(Cursor nameCur) {

        String firstName = "";
        String lastName = "";
        String middleName = "";
        if (nameCur != null && nameCur.moveToFirst()) {
            firstName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            if (firstName == null) {
                firstName = "";
            }
            Log.d("firstName--> ", firstName.length() > 0 ? firstName : firstName);

            middleName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
            if (middleName == null) {
                middleName = "";
            }
            Log.d("middleName--> ", middleName.length() > 0 ? middleName : middleName);

            lastName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            if (lastName == null) {
                lastName = "";
            }
            Log.d("lastName--> ", lastName);
            nameCur.close();
        }
        FirstMiddleLastNameStrings firstMiddleLastNameStrings;
        firstMiddleLastNameStrings = new FirstMiddleLastNameStrings(firstName, middleName, lastName);
        return firstMiddleLastNameStrings;
    }
}
