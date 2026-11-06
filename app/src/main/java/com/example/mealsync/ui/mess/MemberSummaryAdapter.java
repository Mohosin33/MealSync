package com.example.mealsync.ui.mess;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;
import com.example.mealsync.finance.MemberFinanceResult;
import com.example.mealsync.util.MoneyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberSummaryAdapter extends RecyclerView.Adapter<MemberSummaryAdapter.MemberSummaryViewHolder> {

    private final List<MemberFinanceResult> results = new ArrayList<>();
    private final Map<String, String> memberNames = new HashMap<>();

    public void setResults(List<MemberFinanceResult> newResults, Map<String, String> namesMap) {
        results.clear();
        if (newResults != null) {
            results.addAll(newResults);
        }
        memberNames.clear();
        if (namesMap != null) {
            memberNames.putAll(namesMap);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_summary, parent, false);
        return new MemberSummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberSummaryViewHolder holder, int position) {
        MemberFinanceResult res = results.get(position);
        String name = memberNames.getOrDefault(res.getUid(), "Member");
        holder.bind(res, name);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class MemberSummaryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvBalance;
        private final TextView tvMeals;
        private final TextView tvDeposits;
        private final TextView tvCost;

        public MemberSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvBalance = itemView.findViewById(R.id.tvMemberNetBalance);
            tvMeals = itemView.findViewById(R.id.tvMemberMeals);
            tvDeposits = itemView.findViewById(R.id.tvMemberDeposits);
            tvCost = itemView.findViewById(R.id.tvMemberCost);
        }

        public void bind(MemberFinanceResult res, String name) {
            Context context = itemView.getContext();
            tvName.setText(name);

            long balance = res.getNetBalancePaisa();
            tvBalance.setText(MoneyUtils.formatPaisa(balance));
            if (balance > 0) {
                tvBalance.setTextColor(ContextCompat.getColor(context, R.color.ms_teal));
            } else if (balance < 0) {
                tvBalance.setTextColor(ContextCompat.getColor(context, R.color.ms_negative));
            } else {
                tvBalance.setTextColor(ContextCompat.getColor(context, R.color.ms_text_secondary));
            }

            int totalMeals = res.getLunches() + res.getDinners() + res.getGuests();
            tvMeals.setText("Meals: " + totalMeals);
            tvDeposits.setText("Deposits: " + MoneyUtils.formatPaisa(res.getTotalDepositsPaisa()));
            tvCost.setText("Cost: " + MoneyUtils.formatPaisa(res.getMealCostPaisa() + res.getFixedSharePaisa()));
        }
    }
}
