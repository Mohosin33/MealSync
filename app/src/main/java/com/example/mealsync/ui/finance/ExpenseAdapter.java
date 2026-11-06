package com.example.mealsync.ui.finance;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Expense;
import com.example.mealsync.util.MoneyUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseClickListener {
        void onExpenseLongClick(Expense expense);
    }

    private final List<Expense> expenses = new ArrayList<>();
    private final Set<String> reversedExpenseIds = new HashSet<>();
    private final boolean isManager;
    private final OnExpenseClickListener clickListener;

    public ExpenseAdapter(boolean isManager, OnExpenseClickListener clickListener) {
        this.isManager = isManager;
        this.clickListener = clickListener;
    }

    public void setExpenses(List<Expense> newExpenses, Set<String> reversedIds) {
        expenses.clear();
        if (newExpenses != null) {
            expenses.addAll(newExpenses);
        }
        reversedExpenseIds.clear();
        if (reversedIds != null) {
            reversedExpenseIds.addAll(reversedIds);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        boolean isReversed = reversedExpenseIds.contains(expense.getId());
        holder.bind(expense, isReversed, isManager, clickListener);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDescription;
        private final TextView tvAmount;
        private final TextView tvCategory;
        private final TextView tvTypeBadge;
        private final TextView tvReversedBadge;
        private final TextView tvDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvExpenseDescription);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
            tvTypeBadge = itemView.findViewById(R.id.tvExpenseTypeBadge);
            tvReversedBadge = itemView.findViewById(R.id.tvReversedBadge);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
        }

        public void bind(Expense expense, boolean isReversed, boolean isManager, OnExpenseClickListener listener) {
            tvDescription.setText(expense.getDescription() != null ? expense.getDescription() : "");
            tvAmount.setText(MoneyUtils.formatPaisa(expense.getAmountPaisa()));
            tvCategory.setText(expense.getCategory() != null ? expense.getCategory() : "other");
            tvTypeBadge.setText(R.string.type_fixed);
            tvDate.setText(expense.getExpenseDate() != null ? expense.getExpenseDate() : "");

            if (isReversed) {
                tvDescription.setPaintFlags(tvDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvAmount.setPaintFlags(tvAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvReversedBadge.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setPaintFlags(tvDescription.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvAmount.setPaintFlags(tvAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvReversedBadge.setVisibility(View.GONE);
            }

            if (isManager && !isReversed) {
                itemView.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onExpenseLongClick(expense);
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
