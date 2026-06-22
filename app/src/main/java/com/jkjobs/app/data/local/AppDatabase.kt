package com.jkjobs.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jkjobs.app.data.JobPosting

@Database(entities = [JobPosting::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jkjobs.db"
                )
                    // v1 -> v2 changed the primary key format (hashCode -> SHA-256), which makes
                    // old rows' IDs meaningless against freshly-scraped data anyway. Rather than
                    // write a no-op migration that keeps stale rows around, we wipe and re-seed
                    // from a fresh scrape on next refresh - acceptable since this table is a cache
                    // of public web data, not user-authored content (saved/bookmark state is the
                    // only thing that could be lost, and only for users upgrading from a pre-release build).
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
