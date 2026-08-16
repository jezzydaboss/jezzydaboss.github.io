package com.zybooks.cs360_reynolds_jimmy;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Main inventory screen: displays grid, handles SMS alerts, and CRUD triggers
public class InventoryActivity extends AppCompatActivity {
    private InventoryViewModel viewModel;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data); // XML layout for inventory screen

        // Setup RecyclerView
        recyclerView = findViewById(R.id.inventoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(this, item -> {
            Intent intent = new Intent(InventoryActivity.this, EditItemActivity.class);
            intent.putExtra("itemId", item.itemId);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Bind ViewModel and observe inventory list
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        viewModel.getAllItems().observe(this, items -> adapter.submitList(items));

        // Add item button logic
        findViewById(R.id.addItemButton).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, EditItemActivity.class);
            // No itemId passed → EditItemActivity will treat this as a new item
            startActivity(intent);
        });

        // Bulk delete button logic
        findViewById(R.id.deleteSelectedButton).setOnClickListener(v -> {
            // Handle bulk delete (e.g., selected items from adapter)
        });
    }
}