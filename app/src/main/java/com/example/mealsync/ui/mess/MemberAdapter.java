package com.example.mealsync.ui.mess;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Member;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public static class PresenceStatus {
        private final boolean online;
        private final long lastSeen;

        public PresenceStatus(boolean online, long lastSeen) {
            this.online = online;
            this.lastSeen = lastSeen;
        }
    }

    public interface OnRemoveMemberClickListener {
        void onRemoveMember(Member member);
    }

    private final List<Member> members = new ArrayList<>();
    private final boolean isManager;
    private final String currentUserId;
    private final OnRemoveMemberClickListener removeClickListener;
    private final Map<String, PresenceStatus> presenceByUid = new HashMap<>();

    public MemberAdapter(boolean isManager, String currentUserId,
                         OnRemoveMemberClickListener removeClickListener) {
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

    public void setPresenceData(Map<String, PresenceStatus> presenceData) {
        presenceByUid.clear();
        if (presenceData != null) {
            presenceByUid.putAll(presenceData);
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
        holder.bind(member, isManager, currentUserId, removeClickListener,
                presenceByUid.get(member.getUid()));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvRoleBadge;
        private final View presenceDot;
        private final TextView tvPresence;
        private final MaterialButton btnRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvEmail = itemView.findViewById(R.id.tvMemberEmail);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            presenceDot = itemView.findViewById(R.id.memberPresenceDot);
            tvPresence = itemView.findViewById(R.id.tvMemberPresence);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }

        public void bind(Member member, boolean isManager, String currentUserId,
                         OnRemoveMemberClickListener removeListener, PresenceStatus presence) {
            tvName.setText(member.getName() != null && !member.getName().isEmpty() ? member.getName() : "Member");
            tvEmail.setText(member.getEmail() != null ? member.getEmail() : "");

            boolean online = presence != null && presence.online;
            int presenceColor = ContextCompat.getColor(itemView.getContext(),
                    online ? R.color.ms_online : R.color.ms_offline);
            presenceDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(presenceColor));
            if (online) {
                tvPresence.setText(R.string.member_online);
                tvPresence.setTextColor(presenceColor);
            } else if (presence != null && presence.lastSeen > 0) {
                String lastSeen = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(presence.lastSeen));
                tvPresence.setText(itemView.getContext().getString(R.string.member_last_seen, lastSeen));
                tvPresence.setTextColor(presenceColor);
            } else {
                tvPresence.setText(R.string.member_last_seen_unknown);
                tvPresence.setTextColor(presenceColor);
            }

            boolean managerRole = "manager".equalsIgnoreCase(member.getRole());
            tvRoleBadge.setText(managerRole ? "MANAGER" : "MEMBER");

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
