package com.zybooks.cs360_reynolds_jimmy;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/***********************************************************
 *  UserEntity
 *
 *  ENHANCEMENT NOTES (Milestone Four, CS 499):
 *   - Added a unique index on "username". The original table had
 *     no uniqueness constraint at the database level at all - the
 *     only thing preventing duplicate usernames was an application-
 *     level check-then-insert in LoginActivity, which is a real
 *     race condition (two concurrent registration attempts with
 *     the same username could both pass the check before either
 *     insert completes). The unique index makes SQLite itself the
 *     final authority: a duplicate insert now fails at the
 *     database layer regardless of what the application code did
 *     or didn't check first.
 *   - "password" now stores a PBKDF2 hash, never a plaintext
 *     password - see PasswordHasher.java.
 *   - Added "salt", a per-user random value required to verify a
 *     password against its stored hash (see PasswordHasher). Each
 *     user gets their own salt, generated at registration time, so
 *     two users who happen to choose the same password end up with
 *     completely different stored hashes.
 ***********************************************************/
@Entity(tableName = "users", indices = { @Index(value = "username", unique = true) })
public class UserEntity {
    @PrimaryKey(autoGenerate = true) // Auto-incremented ID
    public int id;

    public String username; // Username for login
    public String password; // PBKDF2 password hash (NOT plaintext - see PasswordHasher)
    public String salt;     // Per-user random salt used to compute/verify the hash above

    public UserEntity(String username, String password, String salt) {
        this.username = username;
        this.password = password;
        this.salt = salt;
    }
}
