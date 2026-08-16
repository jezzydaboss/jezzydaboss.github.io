package com.zybooks.cs360_reynolds_jimmy;

import android.database.sqlite.SQLiteConstraintException;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/***********************************************************
 *  UserRepository
 *
 *  ENHANCEMENT NOTES (Milestone Four, CS 499):
 *   - register() now hashes the password (via PasswordHasher)
 *     before it ever reaches the database, instead of storing it
 *     as plaintext.
 *   - authenticate() replaces the old login(username, password,
 *     callback) method. It fetches the user by username, then
 *     verifies the password attempt against the stored hash+salt
 *     using PasswordHasher.verify() in application code - this is
 *     necessarily different from the old approach of comparing
 *     passwords directly inside a SQL query, since a salted hash
 *     can't be verified in a WHERE clause.
 *   - register() catches SQLiteConstraintException, which is what
 *     Room/SQLite throws when the new unique index on
 *     UserEntity.username (see UserEntity.java) rejects a duplicate
 *     insert. This is the actual fix for the check-then-insert race
 *     condition: the getUserByUsername() pre-check in LoginActivity
 *     is kept for fast, friendly UX ("that username is taken" before
 *     the user even submits), but the real guarantee against two
 *     concurrent registrations both succeeding with the same
 *     username now comes from the database's own unique constraint,
 *     which is atomic, not from the application-level check, which
 *     is not.
 ***********************************************************/
public class UserRepository {
    private final UserDao userDao;
    private final Executor databaseExecutor;

    public UserRepository(UserDao userDao, Executor databaseExecutor) {
        this.userDao = userDao;
        this.databaseExecutor = databaseExecutor;
    }

    /**
     * Registers a new user. The password is hashed with a fresh random
     * salt before being persisted. callback receives true if
     * registration succeeded, or false if it failed because the
     * username was already taken (caught via the unique index
     * constraint at the database layer).
     */
    public void register(String username, String plainTextPassword, Consumer<Boolean> callback) {
        databaseExecutor.execute(() -> {
            String salt = PasswordHasher.generateSalt();
            String hashedPassword = PasswordHasher.hash(plainTextPassword, salt);
            UserEntity newUser = new UserEntity(username, hashedPassword, salt);
            try {
                userDao.register(newUser);
                callback.accept(true);
            } catch (SQLiteConstraintException e) {
                // the unique index on username rejected this insert -
                // someone else registered this exact username first
                callback.accept(false);
            }
        });
    }

    /**
     * Attempts to authenticate a username/password pair. callback
     * receives the matching UserEntity if the username exists AND the
     * password verifies against its stored hash, or null otherwise -
     * callers should treat both "no such user" and "wrong password"
     * identically (a generic "invalid username or password" message),
     * so as not to leak which usernames exist to an attacker.
     */
    public void authenticate(String username, String plainTextPassword, Consumer<UserEntity> callback) {
        databaseExecutor.execute(() -> {
            UserEntity user = userDao.getUserByUsername(username);
            if (user != null && PasswordHasher.verify(plainTextPassword, user.salt, user.password)) {
                callback.accept(user);
            } else {
                callback.accept(null);
            }
        });
    }

    public void getUserByUsername(String username, Consumer<UserEntity> callback) {
        databaseExecutor.execute(() -> {
            UserEntity user = userDao.getUserByUsername(username);
            callback.accept(user);
        });
    }
}
