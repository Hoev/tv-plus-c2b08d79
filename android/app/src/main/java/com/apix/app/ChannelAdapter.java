package com.apix.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for channel cards (grid layout)
 * Matches the website's card style: image + gradient overlay + name at bottom
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

        // Load image using simple URL loading (no Glide/Picasso dependency)
        if (channel.imageUrl != null && !channel.imageUrl.isEmpty()) {
            ImageLoader.load(channel.imageUrl, holder.image);
        } else {
            holder.image.setImageResource(android.R.color.darker_gray);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(channel));

        // TV D-pad focus handling
        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setScaleX(1.05f);
                v.setScaleY(1.05f);
                v.setElevation(12f);
                // Gold border
                GradientDrawable border = new GradientDrawable();
                border.setCornerRadius(16f);
                border.setStroke(4, Color.parseColor("#FFD700"));
                border.setColor(Color.parseColor("#1A1A1A"));
                v.setBackground(border);
            } else {
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
                v.setElevation(0f);
                GradientDrawable border = new GradientDrawable();
                border.setCornerRadius(16f);
                border.setColor(Color.parseColor("#1A1A1A"));
                v.setBackground(border);
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

        ViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.channel_image);
            name = v.findViewById(R.id.channel_name);
        }
    }
}
