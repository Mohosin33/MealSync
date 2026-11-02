package com.example.mealsync.ui.mess;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Member;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public interface OnRemoveMemberClickListener {
        void onRemoveMember(Member member);
    }

    private final List<Member> members = new ArrayList<>();
    private final boolean isManager;
    private final String currentUserId;
    private final OnRemoveMemberClickListener removeClickListener;

    public MemberAdapter(boolean isManager, String currentUserId, OnRemoveMemberClickListener removeClickListener) {
        this.isManager = isManager;
        this.currentUserId = currentUserId;
        this.removeClickListener = removeClickListener;
    }

    public void setMembers(List<Member> newMembers) {
        members.clear();
        if (newMembers != null) {
            members.addAll(newMembers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = members.get(position);
        holder.bind(member, isManager, currentUserId, removeClickListener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvRoleBadge;
        private final MaterialButton btnRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvEmail = itemView.findViewById(R.id.tvMemberEmail);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }

        public void bind(Member member, boolean isManager, String currentUserId, OnRemoveMemberClickListener listener) {
            tvName.setText(member.getName() != null && !member.getName().isEmpty() ? member.getName() : "Member");
            tvEmail.setText(member.getEmail() != null ? member.getEmail() : "");

            boolean managerRole = "manager".equalsIgnoreCase(member.getRole());
            tvRoleBadge.setText(managerRole ? "MANAGER" : "MEMBER");

            boolean canRemove = isManager && member.getUid() != null && !member.getUid().equals(currentUserId);
            btnRemove.setVisibility(canRemove ? View.VISIBLE : View.GONE);
            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveMember(member);
                }
            });
        }
    }
}
