package com.apix.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for category tabs - supports both horizontal (bottom) and vertical (side) modes
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClick {
        void onClick(FirebaseModels.Category category);
    }

    private Context context;
    private List<FirebaseModels.Category> data;
    private OnCategoryClick listener;
    private int selectedPosition = 0;
    private boolean isSideMode = false; // false=horizontal bottom, true=vertical side

    public CategoryAdapter(Context ctx, List<FirebaseModels.Category> data, OnCategoryClick listener) {
        this.context = ctx;
        this.data = data;
        this.listener = listener;
    }

    public void setSideMode(boolean sideMode) {
        this.isSideMode = sideMode;
    }

    public void updateData(List<FirebaseModels.Category> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    public void setSelected(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isSideMode ? R.layout.item_category_side : R.layout.item_category;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FirebaseModels.Category cat = data.get(position);
        holder.name.setText(cat.name);

        boolean isSelected = position == selectedPosition;
        holder.name.setTextColor(isSelected ? Color.parseColor("#FFD700") : Color.WHITE);
        holder.indicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            setSelected(pos);
            listener.onClick(data.get(pos));
        });

        // TV D-pad focus with strong visual feedback
        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                holder.name.setTextColor(Color.parseColor("#FFD700"));
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
            } else {
                boolean sel = holder.getAdapterPosition() == selectedPosition;
                holder.name.setTextColor(sel ? Color.parseColor("#FFD700") : Color.WHITE);
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        View indicator;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.category_name);
            indicator = v.findViewById(R.id.category_indicator);
        }
    }
}
