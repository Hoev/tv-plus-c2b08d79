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
 * Adapter for category tabs (horizontal list)
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClick {
        void onClick(FirebaseModels.Category category);
    }

    private Context context;
    private List<FirebaseModels.Category> data;
    private OnCategoryClick listener;
    private int selectedPosition = 0;

    public CategoryAdapter(Context ctx, List<FirebaseModels.Category> data, OnCategoryClick listener) {
        this.context = ctx;
        this.data = data;
        this.listener = listener;
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
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

        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            holder.name.setTextColor(hasFocus || (holder.getAdapterPosition() == selectedPosition)
                ? Color.parseColor("#FFD700") : Color.WHITE);
            if (hasFocus) v.setScaleX(1.1f);
            else v.setScaleX(1.0f);
            if (hasFocus) v.setScaleY(1.1f);
            else v.setScaleY(1.0f);
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
