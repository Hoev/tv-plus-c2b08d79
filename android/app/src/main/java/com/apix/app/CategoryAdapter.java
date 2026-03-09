package com.apix.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for category tabs with icons
 * Supports both horizontal (bottom) and vertical (side) modes
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClick {
        void onClick(FirebaseModels.Category category);
    }

    private Context context;
    private List<FirebaseModels.Category> data;
    private OnCategoryClick listener;
    private int selectedPosition = 0;
    private boolean isSideMode = false;

    // Map category names to drawable icons
    private static final Map<String, Integer> ICON_MAP = new HashMap<>();
    static {
        ICON_MAP.put("sport", R.drawable.ic_sport);
        ICON_MAP.put("sports", R.drawable.ic_sport);
        ICON_MAP.put("movie", R.drawable.ic_movie);
        ICON_MAP.put("movies", R.drawable.ic_movie);
        ICON_MAP.put("network", R.drawable.ic_network);
        ICON_MAP.put("networks", R.drawable.ic_network);
        ICON_MAP.put("religion", R.drawable.ic_religion);
        ICON_MAP.put("settings", R.drawable.ic_settings);
    }

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

    private int getIconForCategory(String name) {
        if (name == null) return R.drawable.ic_category_default;
        String lower = name.toLowerCase().trim();
        for (Map.Entry<String, Integer> entry : ICON_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return R.drawable.ic_category_default;
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

        // إعداد الأيقونة
        int iconRes = getIconForCategory(cat.name);
        if (holder.icon != null) {
            holder.icon.setImageResource(iconRes);
        }

        boolean isSelected = position == selectedPosition;
        holder.itemView.setSelected(isSelected); // هذا السطر يفعل الخلفية الذهبية للتلفاز

        int goldColor = Color.parseColor("#FFC107");
        int whiteColor = Color.WHITE;
        int blackColor = Color.BLACK;

        // تطبيق الألوان بناءً على نوع الشاشة (هاتف أو تلفاز)
        if (isSideMode) {
            // وضع التلفاز: الخلفية ذهبية، إذن النص يجب أن يكون أسود
            int color = isSelected ? blackColor : whiteColor;
            holder.name.setTextColor(color);
            if (holder.icon != null) {
                holder.icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
            if (holder.indicator != null) holder.indicator.setVisibility(View.GONE);
        } else {
            // وضع الهاتف: الخلفية داكنة، إذن النص يجب أن يكون ذهبي
            int color = isSelected ? goldColor : whiteColor;
            holder.name.setTextColor(color);
            if (holder.icon != null) {
                holder.icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
            if (holder.indicator != null) holder.indicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        }

        // تفاعل النقر (اللمس)
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                setSelected(pos);
                listener.onClick(data.get(pos));
            }
        });

        // تفاعل الريموت كنترول (TV D-pad focus)
        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            boolean sel = holder.getAdapterPosition() == selectedPosition;
            
            if (isSideMode) {
                int color = (hasFocus || sel) ? blackColor : whiteColor;
                holder.name.setTextColor(color);
                if (holder.icon != null) {
                    holder.icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
                // تأثير تكبير ناعم للزر عند الوقوف عليه بالريموت
                float scale = hasFocus ? 1.05f : 1.0f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(150).start();
            } else {
                int color = (hasFocus || sel) ? goldColor : whiteColor;
                holder.name.setTextColor(color);
                if (holder.icon != null) {
                    holder.icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
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
        ImageView icon;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.category_name);
            indicator = v.findViewById(R.id.category_indicator);
            icon = v.findViewById(R.id.category_icon);
        }
    }
}
