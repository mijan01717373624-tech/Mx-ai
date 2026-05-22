package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items ORDER BY timestamp ASC")
    fun getAllSchedules(): Flow<List<ScheduleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: ScheduleItem)

    @Update
    suspend fun updateSchedule(item: ScheduleItem)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteScheduleById(id: Int)

    @Query("DELETE FROM schedule_items")
    suspend fun clearAll()
}

@Dao
interface EmailDao {
    @Query("SELECT * FROM email_items ORDER BY timestamp DESC")
    fun getAllEmails(): Flow<List<EmailItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailItem)

    @Update
    suspend fun updateEmail(email: EmailItem)

    @Query("DELETE FROM email_items WHERE id = :id")
    suspend fun deleteEmailById(id: Int)

    @Query("DELETE FROM email_items")
    suspend fun clearAll()
}

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentSecurityLogs(): Flow<List<SecurityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLog)

    @Query("DELETE FROM security_logs")
    suspend fun clearAll()
}

@Database(
    entities = [ScheduleItem::class, EmailItem::class, SecurityLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun emailDao(): EmailDao
    abstract fun securityLogDao(): SecurityLogDao
}
