package com.zybooks.cs360_reynolds_jimmy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class EditItemActivity extends AppCompatActivity {

    // Unique request code for SMS permission prompt
    private static final int REQUEST_SMS_PERMISSION = 1001;

    // Tracks whether SMS permission has been granted
    private boolean smsPermissionGranted = false;

    // UI components for item editing
    private EditText nameInput;                 // Editable item name
    private EditText locationInput;             // Editable item location
    private TextView quantityDisplay;           // Displays current quantity
    private Button addButton;                   // Increases quantity
    private Button subtractButton;              // Decreases quantity
    private TextView alertMessage;              // Fallback alert if SMS is denied
    private TextView notificationPreviewText;   // Always shows alert message on screen

    // Static phone number for SMS alerts (can be made dynamic later)
    private String phoneNumber = "5551234567";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item); // Inflate layout

        // Link UI elements to XML layout components
        nameInput = findViewById(R.id.item_name);
        locationInput = findViewById(R.id.item_location);
        quantityDisplay = findViewById(R.id.item_quantity);
        addButton = findViewById(R.id.button_add);
        subtractButton = findViewById(R.id.button_subtract);
        alertMessage = findViewById(R.id.inventoryAlertMessage);
        notificationPreviewText = findViewById(R.id.notificationPreviewText);

        // Prompt user for SMS permission at runtime
        requestSmsPermission();

        // Handle quantity increase button click
        addButton.setOnClickListener(v -> {
            int qty = parseQuantity(quantityDisplay.getText().toString());
            qty++; // Increment quantity
            quantityDisplay.setText(String.valueOf(qty)); // Update UI
            String itemName = nameInput.getText().toString().trim(); // Get item name
            checkInventoryAndNotify(qty, itemName); // Trigger alert logic
        });

        // Handle quantity decrease button click
        subtractButton.setOnClickListener(v -> {
            int qty = parseQuantity(quantityDisplay.getText().toString());
            if (qty > 0) {
                qty--; // Decrement quantity
                quantityDisplay.setText(String.valueOf(qty)); // Update UI
                String itemName = nameInput.getText().toString().trim(); // Get item name
                checkInventoryAndNotify(qty, itemName); // Trigger alert logic
            }
        });
    }

    // Request SEND_SMS permission if not already granted
    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Ask user for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQUEST_SMS_PERMISSION);
        } else {
            smsPermissionGranted = true; // Already granted
        }
    }

    // Handle result of permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            smsPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this,
                    smsPermissionGranted ? "SMS permission granted" : "SMS permission denied",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Safely parse quantity from string input
    private int parseQuantity(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return 0; // Default to 0 if input is invalid
        }
    }

    // Trigger alert when inventory is low
    private void checkInventoryAndNotify(int qty, String itemName) {
        if (qty <= 3) {
            // Compose alert message
            String message = " Low inventory alert: Item '" + itemName + "' is below threshold.";

            if (smsPermissionGranted) {
                // Send SMS alert
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } else {
                // Show fallback alert on screen
                alertMessage.setText(message);
                alertMessage.setVisibility(View.VISIBLE);
            }

            // Always show preview message on screen for testing
            notificationPreviewText.setText(message);
            notificationPreviewText.setVisibility(View.VISIBLE);
        } else {
            // Hide alerts if inventory is sufficient
            alertMessage.setVisibility(View.GONE);
            notificationPreviewText.setVisibility(View.GONE);
        }
    }
}