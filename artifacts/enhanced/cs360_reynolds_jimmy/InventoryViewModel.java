package com.zybooks.cs360_reynolds_jimmy;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

// ViewModel connects UI to Repository and handles business logic
public class InventoryViewModel extends AndroidViewModel {
    public LiveData<InventoryItemEntity> getItemById(int id) {
        return repo.getItemById(id);
    }

    private final InventoryRepository repo;
    private final LiveData<List<InventoryItemEntity>> allItems;

    public InventoryViewModel(Application app) {
        super(app);
        repo = new InventoryRepository(app);
        allItems = repo.getAllItems(); // Observe inventory list
    }

    public LiveData<List<InventoryItemEntity>> getAllItems() {
        return allItems;
    }

    public void insertItem(InventoryItemEntity item) {
        repo.insertItem(item); // Add item to DB
    }

    public void updateItem(InventoryItemEntity item) {
        repo.updateItem(item); // Modify item details
    }

    public void deleteItem(InventoryItemEntity item) {
        repo.deleteItem(item); // Remove item from DB
    }
}
