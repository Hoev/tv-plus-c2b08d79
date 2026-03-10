package com.apix.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Channel cards with 16:9 aspect ratio, name overlay at bottom-left
 * Touch + focus effects for both phone and TV
 */
public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    public interface OnChannelClick {
        void onClick(FirebaseModels.Channel channel);
    }

    private Context context;
    private List<FirebaseModels.Channel> data;
    private OnChannelClick listener;

    private static final int GOLD = Color.parseColor("#FFD700");

    public ChannelAdapter(Context ctx, List<FirebaseModels.Channel> data, OnChannelClick listener) {
        this.context = ctx;
        this.data = data;
        this.listener = listener;
    }

    public void updateData(List<FirebaseModels.Channel> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FirebaseModels.Channel channel = data.get(position);
        holder.name.setText(channel.name);

        // Force 16:9 aspect ratio
        holder.imageContainer.getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    holder.imageContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                    int width = holder.imageContainer.getMeasuredWidth();
                    if (width > 0) {
                        int height = (int) (width * 9.0 / 16.0);
                        ViewGroup.LayoutParams params = holder.imageContainer.getLayoutParams();
                        params.height = height;
                        holder.imageContainer.setLayoutParams(params);
                    }
                    return true;
                }
            });

        // Load image
        if (channel.imageUrl != null && !channel.imageUrl.isEmpty()) {
            ImageLoader.load(channel.imageUrl, holder.image);
        } else {
            holder.image.setImageResource(android.R.color.darker_gray);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(channel));

        // Touch effect for phones - pressed state
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);

        // Focus/touch visual feedback - gold border + scale for both phone and TV
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            applyFocusEffect(v, holder, hasFocus);
        });

        // Touch feedback for phones
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    applyFocusEffect(v, holder, true);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    applyFocusEffect(v, holder, false);
                    break;
            }
            return false; // Don't consume - let click handler work
        });
    }

    private void applyFocusEffect(View v, ViewHolder holder, boolean focused) {
        if (focused) {
            v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start();
            v.setElevation(16f);
            holder.card.setCardElevation(12f);
            // Gold border
            GradientDrawable glow = new GradientDrawable();
            glow.setCornerRadius(16f);
            glow.setStroke(4, GOLD);
            glow.setColor(Color.TRANSPARENT);
            v.setForeground(glow);
            // Gold name
            holder.name.setTextColor(GOLD);
        } else {
            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            v.setElevation(0f);
            holder.card.setCardElevation(4f);
            v.setForeground(null);
            holder.name.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        FrameLayout imageContainer;
        CardView card;

        ViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.channel_image);
            name = v.findViewById(R.id.channel_name);
            imageContainer = v.findViewById(R.id.image_container);
            card = v.findViewById(R.id.channel_card);
        }
    }
}
