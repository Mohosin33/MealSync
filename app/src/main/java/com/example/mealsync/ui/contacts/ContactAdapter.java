package com.example.mealsync.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(ContactEntry contact);
    }

    private final List<ContactEntry> contacts = new ArrayList<>();
    private final OnContactClickListener clickListener;

    public ContactAdapter(OnContactClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setContacts(List<ContactEntry> newContacts) {
        contacts.clear();
        if (newContacts != null) {
            contacts.addAll(newContacts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactEntry contact = contacts.get(position);
        holder.bind(contact, clickListener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView phone;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvContactName);
            phone = itemView.findViewById(R.id.tvContactPhone);
        }

        void bind(ContactEntry contact, OnContactClickListener listener) {
            name.setText(contact.getDisplayName());
            phone.setText(contact.getPhoneNumber());
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(contact);
                }
            });
        }
    }
}
