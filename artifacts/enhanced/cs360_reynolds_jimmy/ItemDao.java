package com.zybooks.cs360_reynolds_jimmy;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

// DAO interface for inventory CRUD operations
@Dao
public interface ItemDao {

    // Retrieves all inventory items
    @Query("SELECT * FROM inventory_items")
    LiveData<List<InventoryItemEntity>> getAllItems();

    // Adds a new item
    @Insert
    void insertItem(InventoryItemEntity item);

    // Updates an existing item
    @Update
    void updateItem(InventoryItemEntity item);

    // Deletes an item
    @Delete
    void deleteItem(InventoryItemEntity item);
    // Retrieves a single item by ID for editing
    @Query("SELECT * FROM inventory_items WHERE itemId = :id")
    LiveData<InventoryItemEntity> getItemById(int id);
}
