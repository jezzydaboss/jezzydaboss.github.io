package com.zybooks.cs360_reynolds_jimmy;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Room entity representing an inventory item
@Entity(tableName = "inventory_items")
public class InventoryItemEntity {
    public String imageUri; // Optional: path to image or null if not set
    @PrimaryKey(autoGenerate = true) // Unique item ID
    public int itemId;

    public String name;      // Item name
    public int quantity;     // Current stock level
    public String location;  // Warehouse location
}