package com.example.mealsync.ui.finance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Period;
import com.example.mealsync.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder> {

    public interface OnArchiveClickListener {
        void onArchiveClick(Period period);
        void onArchiveLongClick(Period period);
    }

    private final List<Period> periods = new ArrayList<>();
    private final boolean isManager;
    private final OnArchiveClickListener clickListener;

    public ArchiveAdapter(boolean isManager, OnArchiveClickListener clickListener) {
        this.isManager = isManager;
        this.clickListener = clickListener;
    }

    public void setPeriods(List<Period> newPeriods) {
        periods.clear();
        if (newPeriods != null) {
            periods.addAll(newPeriods);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArchiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archive_period, parent, false);
        return new ArchiveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchiveViewHolder holder, int position) {
        Period period = periods.get(position);
        holder.bind(period, isManager, clickListener);
    }

    @Override
    public int getItemCount() {
        return periods.size();
    }

    static class ArchiveViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPeriodId;
        private final TextView tvRateBadge;
        private final TextView tvTotals;

        public ArchiveViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPeriodId = itemView.findViewById(R.id.tvArchivePeriodId);
            tvRateBadge = itemView.findViewById(R.id.tvArchiveRateBadge);
            tvTotals = itemView.findViewById(R.id.tvArchiveTotals);
        }

        public void bind(Period period, boolean isManager, OnArchiveClickListener listener) {
            tvPeriodId.setText(period.getPeriodId() != null ? period.getPeriodId() : "");

            Long rate = period.getFinalRatePaisa();
            if (rate != null) {
                tvRateBadge.setText(MoneyUtils.formatPaisa(rate) + " / meal");
            } else {
                tvRateBadge.setText("Rate unavailable");
            }

            Map<String, Long> totals = period.getTotals();
            long meals = totals != null && totals.containsKey("totalMeals") ? totals.get("totalMeals") : 0L;
            long storedVariablePaisa = totals != null && totals.containsKey("totalVariablePaisa") ? totals.get("totalVariablePaisa") : 0L;
            long mealChargesPaisa = storedVariablePaisa > 0
                    ? storedVariablePaisa
                    : rate != null ? rate * meals : 0L;
            long fixPaisa = totals != null && totals.containsKey("totalFixedPaisa") ? totals.get("totalFixedPaisa") : 0L;

            tvTotals.setText("Meals: " + meals + " | Meal Charges: " + MoneyUtils.formatPaisa(mealChargesPaisa)
                    + " | Fixed Expenses: " + MoneyUtils.formatPaisa(fixPaisa));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onArchiveClick(period);
                }
            });

            if (isManager) {
                itemView.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onArchiveLongClick(period);
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
