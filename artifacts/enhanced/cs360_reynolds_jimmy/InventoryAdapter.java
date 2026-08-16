package com.zybooks.cs360_reynolds_jimmy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private List<InventoryItemEntity> itemList = new ArrayList<>();
    private final OnItemClickListener listener;
    private final Context context;

    // Constructor now takes context for Glide
    public InventoryAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, quantityText, locationText;
        ImageView itemImage;

        public InventoryViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.itemName);
            quantityText = itemView.findViewById(R.id.itemQuantity);
            locationText = itemView.findViewById(R.id.itemLocation);
            itemImage = itemView.findViewById(R.id.itemImage);
        }

        // Binds data to UI and sets click listener
        public void bind(InventoryItemEntity item, OnItemClickListener listener, Context context) {
            nameText.setText(item.name);
            quantityText.setText("Qty: " + item.quantity);
            locationText.setText("Location: " + item.location);

            // Load image with fallback
            if (item.imageUri != null && !item.imageUri.isEmpty()) {
                Glide.with(context).load(item.imageUri).into(itemImage);
            } else {
                itemImage.setImageResource(R.drawable.placeholder_item);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }

    @Override
    public InventoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(InventoryViewHolder holder, int position) {
        holder.bind(itemList.get(position), listener, context);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void submitList(List<InventoryItemEntity> items) {
        itemList.clear();
        itemList.addAll(items);
        notifyDataSetChanged();
    }

    public InventoryItemEntity getItemById(int id) {
        for (InventoryItemEntity item : itemList) {
            if (item.itemId == id) return item;
        }
        return null;
    }

    public interface OnItemClickListener {
        void onItemClick(InventoryItemEntity item);
    }
}
