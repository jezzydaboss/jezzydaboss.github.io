package com.zybooks.cs360_reynolds_jimmy;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

// Repository layer for inventory-related DB operations
public class InventoryRepository {
    private final ItemDao itemDao;

    public InventoryRepository(Application app) {
        itemDao = AppDatabase.getInstance(app).itemDao();
    }

    // Exposes LiveData for observing inventory list
    public LiveData<List<InventoryItemEntity>> getAllItems() {
        return itemDao.getAllItems();
    }

    // Insert item (background thread)
    public void insertItem(InventoryItemEntity item) {
        AppDatabase.databaseWriteExecutor.execute(() -> itemDao.insertItem(item));
    }

    // Update item (background thread)
    public void updateItem(InventoryItemEntity item) {
        AppDatabase.databaseWriteExecutor.execute(() -> itemDao.updateItem(item));
    }

    // Delete item (background thread)
    public void deleteItem(InventoryItemEntity item) {
        AppDatabase.databaseWriteExecutor.execute(() -> itemDao.deleteItem(item));
    }
    public LiveData<InventoryItemEntity> getItemById(int id) {
        return itemDao.getItemById(id);
    }
}
