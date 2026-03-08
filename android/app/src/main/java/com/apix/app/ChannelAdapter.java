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
 * Adapter for channel cards (grid layout)
 * 16:9 aspect ratio cards with strong gold focus effect for TV remote
 */
public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    public interface OnChannelClick {
        void onClick(FirebaseModels.Channel channel);
    }

    private Context context;
    private List<FirebaseModels.Channel> data;
    private OnChannelClick listener;

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

        // Force 16:9 aspect ratio on image container
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

        // TV D-pad focus handling with strong gold glow
        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();
                v.setElevation(16f);
                // Strong gold border on card
                holder.card.setCardElevation(12f);
                holder.card.setCardBackgroundColor(Color.parseColor("#1A1A1A"));
                // Gold outline via stroke on card's parent
                GradientDrawable glow = new GradientDrawable();
                glow.setCornerRadius(16f);
                glow.setStroke(5, Color.parseColor("#FFD700"));
                glow.setColor(Color.TRANSPARENT);
                v.setForeground(glow);
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                v.setElevation(0f);
                holder.card.setCardElevation(4f);
                v.setForeground(null);
            }
        });
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
