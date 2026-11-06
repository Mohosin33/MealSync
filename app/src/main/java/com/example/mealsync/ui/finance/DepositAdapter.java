package com.example.mealsync.ui.finance;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Deposit;
import com.example.mealsync.util.MoneyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DepositAdapter extends RecyclerView.Adapter<DepositAdapter.DepositViewHolder> {

    public interface OnDepositClickListener {
        void onDepositLongClick(Deposit deposit);
    }

    private final List<Deposit> deposits = new ArrayList<>();
    private final Set<String> reversedDepositIds = new HashSet<>();
    private final Map<String, String> memberNames = new HashMap<>();
    private final boolean isManager;
    private final OnDepositClickListener clickListener;

    public DepositAdapter(boolean isManager, OnDepositClickListener clickListener) {
        this.isManager = isManager;
        this.clickListener = clickListener;
    }

    public void setDeposits(List<Deposit> newDeposits, Set<String> reversedIds, Map<String, String> namesMap) {
        deposits.clear();
        if (newDeposits != null) {
            deposits.addAll(newDeposits);
        }
        reversedDepositIds.clear();
        if (reversedIds != null) {
            reversedDepositIds.addAll(reversedIds);
        }
        memberNames.clear();
        if (namesMap != null) {
            memberNames.putAll(namesMap);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DepositViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deposit, parent, false);
        return new DepositViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DepositViewHolder holder, int position) {
        Deposit deposit = deposits.get(position);
        boolean isReversed = reversedDepositIds.contains(deposit.getId());
        String memberName = memberNames.getOrDefault(deposit.getMemberId(), "Member");
        holder.bind(deposit, memberName, isReversed, isManager, clickListener);
    }

    @Override
    public int getItemCount() {
        return deposits.size();
    }

    static class DepositViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMemberName;
        private final TextView tvAmount;
        private final TextView tvMethod;
        private final TextView tvDescription;
        private final TextView tvReversedBadge;

        public DepositViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvDepositMemberName);
            tvAmount = itemView.findViewById(R.id.tvDepositAmount);
            tvMethod = itemView.findViewById(R.id.tvDepositMethod);
            tvDescription = itemView.findViewById(R.id.tvDepositDescription);
            tvReversedBadge = itemView.findViewById(R.id.tvDepositReversedBadge);
        }

        public void bind(Deposit deposit, String memberName, boolean isReversed, boolean isManager, OnDepositClickListener listener) {
            tvMemberName.setText(memberName);
            tvAmount.setText("+" + MoneyUtils.formatPaisa(deposit.getAmountPaisa()));
            tvMethod.setText(deposit.getMethod() != null ? deposit.getMethod() : "cash");
            tvDescription.setText(deposit.getDescription() != null ? deposit.getDescription() : "");

            if (isReversed) {
                tvMemberName.setPaintFlags(tvMemberName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvAmount.setPaintFlags(tvAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvReversedBadge.setVisibility(View.VISIBLE);
            } else {
                tvMemberName.setPaintFlags(tvMemberName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvAmount.setPaintFlags(tvAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvReversedBadge.setVisibility(View.GONE);
            }

            if (isManager && !isReversed) {
                itemView.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onDepositLongClick(deposit);
                        return true;
                    }
                    return false;
                });
            } else {
                itemView.setOnLongClickListener(null);
            }
        }
    }
}
