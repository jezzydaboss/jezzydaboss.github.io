package com.zybooks.cs360_reynolds_jimmy;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***********************************************************
 *  AppDatabase
 *
 *  ENHANCEMENT NOTES (Milestone Four, CS 499):
 *   - The database is now opened through SQLCipher's SupportFactory
 *     instead of plain Room/SQLite. This is the core fix for this
 *     artifact: it was named the "encrypted database wrapper" but
 *     the original Room.databaseBuilder(...) call configured no
 *     encryption at all the .db file on disk was plain, readable
 *     SQLite. With SupportFactory wired in, the database file is
 *     encrypted at rest using a key from DatabaseKeyProvider, which
 *     is itself generated randomly and stored via the Android
 *     Keystore rather than hardcoded.
 *   - getInstance() is now the ONLY way to construct this database
 *     anywhere in the app. Previously, LoginActivity bypassed this
 *     singleton entirely and called Room.databaseBuilder(...)
 *     directly with a different literal name ("warehouse-db" with a
 *     hyphen, vs. "warehouse_db" with an underscore here) meaning
 *     the app was silently creating and using TWO separate database
 *     files depending on which code path ran. LoginActivity.java has
 *     been updated to call AppDatabase.getInstance() like every
 *     other part of the app.
 *
 *  Required build.gradle dependency (see build.gradle.additions.txt
 *  in this package for the exact lines):
 *     implementation "net.zetetic:android-database-sqlcipher:4.5.4"
 *     implementation "androidx.sqlite:sqlite:2.4.0"
 *     implementation "androidx.security:security-crypto:1.1.0-alpha06"
 ***********************************************************/
@Database(entities = {UserEntity.class, InventoryItemEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // DAO accessors
    public abstract UserDao userDao();
    public abstract ItemDao itemDao();

    // Singleton instance to prevent multiple DB objects
    private static volatile AppDatabase INSTANCE;

    // Thread pool for background DB operations
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    // Returns the singleton DB instance - encrypted via SQLCipher
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();

                    // ENHANCEMENT: load libsqlcipher before opening the database
                    // required once by SQLCipher for Android
                    SQLiteDatabase.loadLibs(appContext);

                    char[] passphrase = DatabaseKeyProvider.getOrCreateDatabaseKey(appContext);
                    SupportSQLiteOpenHelper.Factory factory = new SupportFactory(
                            net.sqlcipher.database.SQLiteDatabase.getBytes(passphrase));

                    INSTANCE = Room.databaseBuilder(appContext, AppDatabase.class, "warehouse_db")
                            .openHelperFactory(factory)
                            // ENHANCEMENT: version bumped from 1 to 2 above because
                            // UserEntity's schema changed. Room requires either a
                            // Migration or, during development, a fallback strategy
                            // fallbackToDestructiveMigration() is appropriate here
                            // since this is a student project with no production
                            // user data to preserve across the schema change. A real
                            // production app would write an explicit Migration
                            // instead, to avoid deleting existing users' data.
                            .fallbackToDestructiveMigration()
                            .build();

                    // the raw passphrase char[] is no longer needed once the
                    // database is open clearing it limits how long the key
                    // material sits in memory
                    java.util.Arrays.fill(passphrase, '\0');
                }
            }
        }
        return INSTANCE;
    }
}
