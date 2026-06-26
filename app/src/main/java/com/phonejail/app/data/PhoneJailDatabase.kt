package com.phonejail.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FocusSession::class], version = 1, exportSchema = false)
abstract class PhoneJailDatabase : RoomDatabase() {

    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var instance: PhoneJailDatabase? = null

        fun get(context: Context): PhoneJailDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhoneJailDatabase::class.java,
                    "phonejail.db",
                ).build().also { instance = it }
            }
    }
}
