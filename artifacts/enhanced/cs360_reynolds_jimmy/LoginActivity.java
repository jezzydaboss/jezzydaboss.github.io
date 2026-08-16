package com.zybooks.cs360_reynolds_jimmy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/***********************************************************
 *  LoginActivity
 *
 *  ENHANCEMENT NOTES (Milestone Four, CS 499):
 *   - Removed the direct Room.databaseBuilder(...) call that
 *     previously bypassed AppDatabase's singleton and built a
 *     SECOND, separately-named database ("warehouse-db" with a
 *     hyphen, vs. "warehouse_db" with an underscore in
 *     AppDatabase). This activity now goes through
 *     AppDatabase.getInstance(), the same as every other part of
 *     the app, so there is exactly one database file, and it is
 *     the encrypted one (see AppDatabase.java).
 *   - The login button's logic previously silently registered a
 *     brand-new account whenever the entered username wasn't
 *     found meaning a typo'd username on login created a new
 *     account instead of surfacing an error. That entire fallback
 *     branch is removed; a failed login now always shows a generic
 *     "Invalid username or password" message, whether the username
 *     doesn't exist or the password was wrong for it.
 *   - Both buttons now go through UserRepository's hash-based
 *     register()/authenticate() methods instead of passing a raw
 *     password into a SQL query.
 ***********************************************************/
public class LoginActivity extends AppCompatActivity {
    private EditText usernameInput, passwordInput;
    private Button loginButton;
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.usernameField);
        passwordInput = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        Button createAccountButton = findViewById(R.id.createAccountButton);

        // ENHANCEMENT: goes through the singleton instead of constructing
        // a second, separate database instance directly
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        UserDao userDao = db.userDao();
        Executor executor = Executors.newSingleThreadExecutor();
        userRepo = new UserRepository(userDao, executor);

        // Create Account Button Logic
        createAccountButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            userRepo.register(username, password, success -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "Account created! You can now log in.", Toast.LENGTH_LONG).show();
                    } else {
                        // ENHANCEMENT: this now reflects the database's own
                        // unique-constraint rejection (see UserRepository.register()),
                        // not just an application-level pre-check, so this message
                        // is accurate even under concurrent registration attempts
                        Toast.makeText(this, "Username already exists. Try a different one.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        // Login Button Logic
        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            userRepo.authenticate(username, password, loggedInUser -> {
                runOnUiThread(() -> {
                    if (loggedInUser != null) {
                        startActivity(new Intent(this, InventoryActivity.class));
                        finish();
                    } else {
                        // ENHANCEMENT: previously, this branch silently created
                        // a brand-new account and logged the user in when the
                        // username wasn't found a typo on login could
                        // silently create an unintended account. It now always
                        // reports a generic authentication failure instead,
                        // covering both "no such user" and "wrong password"
                        // with the same message.
                        Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }
}
