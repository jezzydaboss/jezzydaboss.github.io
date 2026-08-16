package com.zybooks.cs360_reynolds_jimmy;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/***********************************************************
 *  UserDao
 *
 *  ENHANCEMENT NOTE (Milestone Four, CS 499): the original
 *  login(String username, String password) query compared a raw
 *  password string directly in SQL:
 *    "SELECT * FROM users WHERE username = ? AND password = ? LIMIT 1"
 *  That method is removed entirely. Password verification now
 *  happens in application code (UserRepository), using
 *  PasswordHasher.verify() against the stored hash + salt, which
 *  is the only correct place to do it once passwords are hashed
 *  you cannot verify a salted hash inside a SQL WHERE clause,
 *  since the whole point of the salt is that the same password
 *  produces a different stored value per user.
 ***********************************************************/
@Dao
public interface UserDao {

    // Fetches a user record by username - used both for the
    // duplicate-username check at registration and to retrieve the
    // stored hash/salt for verification at login
    @Query("SELECT * FROM users WHERE username = :username")
    UserEntity getUserByUsername(String username);

    // Inserts a new user into the database. Because UserEntity's
    // username column now has a unique index (see UserEntity.java),
    // Room's default insert conflict strategy (ABORT) will throw if
    // a duplicate username somehow reaches this call a database
    // level backstop behind the application-level check.
    @Insert
    void register(UserEntity user);
}
