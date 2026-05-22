package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MXRepository(
    private val scheduleDao: ScheduleDao,
    private val emailDao: EmailDao,
    private val logDao: SecurityLogDao
) {
    val schedules: Flow<List<ScheduleItem>> = scheduleDao.getAllSchedules()
    val emails: Flow<List<EmailItem>> = emailDao.getAllEmails()
    val logs: Flow<List<SecurityLog>> = logDao.getRecentSecurityLogs()

    suspend fun insertSchedule(item: ScheduleItem) = scheduleDao.insertSchedule(item)
    suspend fun updateSchedule(item: ScheduleItem) = scheduleDao.updateSchedule(item)
    suspend fun deleteSchedule(id: Int) = scheduleDao.deleteScheduleById(id)

    suspend fun insertEmail(email: EmailItem) = emailDao.insertEmail(email)
    suspend fun updateEmail(email: EmailItem) = emailDao.updateEmail(email)
    suspend fun deleteEmail(id: Int) = emailDao.deleteEmailById(id)

    suspend fun insertLog(log: SecurityLog) = logDao.insertLog(log)

    fun prepopulateIfEmpty(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val existingSchedules = schedules.first()
            if (existingSchedules.isEmpty()) {
                // Pre-populate schedules
                scheduleDao.insertSchedule(
                    ScheduleItem(
                        title = "Morning Brief with CEO",
                        time = "09:00 AM",
                        description = "Schedules, email updates, and priority routing review.",
                        category = "DAILY SCHEDULE"
                    )
                )
                scheduleDao.insertSchedule(
                    ScheduleItem(
                        title = "Product Strategy session",
                        time = "02:00 PM",
                        description = "Key architecture review on phone local biometric encryption.",
                        category = "DAILY SCHEDULE"
                    )
                )
                scheduleDao.insertSchedule(
                    ScheduleItem(
                        title = "AES-256 Key Audit",
                        time = "04:30 PM",
                        description = "Review master seed key storage in Keystore system.",
                        category = "SENSITIVE SYSTEM"
                    )
                )
            }

            val existingEmails = emails.first()
            if (existingEmails.isEmpty()) {
                // Pre-populate emails
                emailDao.insertEmail(
                    EmailItem(
                        sender = "sec-ops@mx.ai",
                        subject = "Perimeter Sandboxing Success",
                        preview = "All app sandbox protocols verified. No issues detected.",
                        body = "The Android OS sandbox and restricted storage parameters have been successfully validated for MX AI. Root access restrictions and biometric guardrails are executing with 0% latency.",
                        isCritical = true
                    )
                )
                emailDao.insertEmail(
                    EmailItem(
                        sender = "ceo@mx.ai",
                        subject = "Strategy Call Deck Review",
                        preview = "Please inspect the slide outlines for today's briefing at 2:00 PM.",
                        body = "Hello Partner! We need to focus on marketing the unique user-exclusive phone control and local encrypted storage. See you on the Call.",
                        isCritical = false
                    )
                )
                emailDao.insertEmail(
                    EmailItem(
                        sender = "biometrics@security.mx",
                        subject = "Intrusion Block Shield [Active]",
                        preview = "Physical biometric protection reports completely locked down.",
                        body = "Lock state fully validated. The app interface requires physical confirmation. Secure container is fully live under cryptographic keys.",
                        isCritical = true
                    )
                )
            }

            val existingLogs = logs.first()
            if (existingLogs.isEmpty()) {
                // Pre-populate logs
                logDao.insertLog(
                    SecurityLog(
                        type = "BIOMETRIC",
                        message = "Biometric screen shield initiated successfully.",
                        status = "PROTECTED"
                    )
                )
                logDao.insertLog(
                    SecurityLog(
                        type = "ENCRYPTION",
                        message = "AES-256 local relational DB is active and fully sealed.",
                        status = "SECURE"
                    )
                )
                logDao.insertLog(
                    SecurityLog(
                        type = "SANDBOX",
                        message = "Master sandbox rules loaded. Full control restricted to user phone.",
                        status = "SECURE"
                    )
                )
            }
        }
    }
}
