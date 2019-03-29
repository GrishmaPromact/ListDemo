package com.promact.dropcontact;

/**
 * Created by grishma on 11-05-2017.
 */

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ContactListRecyclerviewAdapter extends RecyclerView.Adapter<ContactListRecyclerviewAdapter.MyViewHolder> implements RecyclerViewFastScroller.BubbleTextGetter {

    private List<Contact> contactList;
    private Context context;

    public ContactListRecyclerviewAdapter(Context context, List<Contact> contactList) {
        this.contactList = contactList;
        this.context = context;

    }

    @Override
    public String getTextToShowInBubble(double pos) {
        return Character.toString(contactList.get((int) pos).getName().charAt(0));
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView contactName;
        private ImageView contactImage;

        public MyViewHolder(View view) {
            super(view);
            contactImage = (ImageView) view.findViewById(R.id.contactProfile);
            contactName = (TextView) view.findViewById(R.id.contactName);
        }
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listview_layout, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        if (contact != null) {
            holder.contactName.setText(contact.getName());
            if (String.valueOf(contact.getProfile()) == "") {
                String firstLetter = String.valueOf(contact.getName().charAt(0));
                int colorcode = ContextCompat.getColor(context, R.color.colorAccent);
                TextDrawable drawable = TextDrawable.builder()
                        .buildRound(firstLetter, colorcode);
                holder.contactImage.setImageDrawable(drawable);
            } else {
                Picasso.with(context).load(contact.getProfile())
                        .transform(new CropCircleTransformationForContactProfile())
                        .into(holder.contactImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }
}
