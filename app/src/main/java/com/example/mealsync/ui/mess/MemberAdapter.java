package com.example.mealsync.ui.mess;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

    public interface OnIncludeInFixedToggleListener {
        void onIncludeInFixedToggled(Member member, boolean includeInFixed);
    }

    private final List<Member> members = new ArrayList<>();
    private final boolean isManager;
    private final String currentUserId;
    private final OnRemoveMemberClickListener removeClickListener;
    private final OnIncludeInFixedToggleListener toggleListener;

    public MemberAdapter(boolean isManager, String currentUserId,
                         OnRemoveMemberClickListener removeClickListener,
                         OnIncludeInFixedToggleListener toggleListener) {
        this.isManager = isManager;
        this.currentUserId = currentUserId;
        this.removeClickListener = removeClickListener;
        this.toggleListener = toggleListener;
    }

    public MemberAdapter(boolean isManager, String currentUserId, OnRemoveMemberClickListener removeClickListener) {
        this(isManager, currentUserId, removeClickListener, null);
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
        holder.bind(member, isManager, currentUserId, removeClickListener, toggleListener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvRoleBadge;
        private final CheckBox cbFixed;
        private final MaterialButton btnRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvEmail = itemView.findViewById(R.id.tvMemberEmail);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            cbFixed = itemView.findViewById(R.id.cbIncludeInFixed);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }

        public void bind(Member member, boolean isManager, String currentUserId,
                         OnRemoveMemberClickListener removeListener,
                         OnIncludeInFixedToggleListener toggleListener) {
            tvName.setText(member.getName() != null && !member.getName().isEmpty() ? member.getName() : "Member");
            tvEmail.setText(member.getEmail() != null ? member.getEmail() : "");

            boolean managerRole = "manager".equalsIgnoreCase(member.getRole());
            tvRoleBadge.setText(managerRole ? "MANAGER" : "MEMBER");

            cbFixed.setEnabled(isManager);
            cbFixed.setOnCheckedChangeListener(null);
            cbFixed.setChecked(member.isIncludeInFixed());

            if (isManager) {
                cbFixed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (toggleListener != null) {
                        toggleListener.onIncludeInFixedToggled(member, isChecked);
                    }
                });
            }

            boolean canRemove = isManager && member.getUid() != null && !member.getUid().equals(currentUserId);
            btnRemove.setVisibility(canRemove ? View.VISIBLE : View.GONE);
            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onRemoveMember(member);
                }
            });
        }
    }
}
