package com.example.mealsync.ui.mess;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Meal;

import java.util.ArrayList;
import java.util.List;

public class MealHistoryAdapter extends RecyclerView.Adapter<MealHistoryAdapter.MealViewHolder> {

    private final List<Meal> meals = new ArrayList<>();

    public void setMeals(List<Meal> newMeals) {
        meals.clear();
        if (newMeals != null) {
            meals.addAll(newMeals);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meal_history, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = meals.get(position);
        holder.bind(meal);
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate;
        private final TextView tvLunch;
        private final TextView tvDinner;
        private final TextView tvGuests;
        private final TextView tvPending;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvMealDate);
            tvLunch = itemView.findViewById(R.id.tvLunchStatus);
            tvDinner = itemView.findViewById(R.id.tvDinnerStatus);
            tvGuests = itemView.findViewById(R.id.tvGuestsCount);
            tvPending = itemView.findViewById(R.id.tvPendingBadge);
        }

        public void bind(Meal meal) {
            tvDate.setText(meal.getDate() != null ? meal.getDate() : "");
            android.content.Context context = itemView.getContext();
            tvLunch.setText(context.getString(R.string.label_lunch_status,
                    context.getString(meal.isLunch() ? R.string.meal_status_on : R.string.meal_status_off)));
            tvDinner.setText(context.getString(R.string.label_dinner_status,
                    context.getString(meal.isDinner() ? R.string.meal_status_on : R.string.meal_status_off)));
            tvGuests.setText(context.getString(R.string.label_guests_count, meal.getGuests()));

            tvPending.setVisibility(meal.isPending() ? View.VISIBLE : View.GONE);
        }
    }
}
