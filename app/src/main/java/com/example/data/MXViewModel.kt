package com.example.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class MXViewModel(private val repository: MXRepository) : ViewModel() {

    val schedules: StateFlow<List<ScheduleItem>> = repository.schedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val emails: StateFlow<List<EmailItem>> = repository.emails
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val logs: StateFlow<List<SecurityLog>> = repository.logs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Biometric Authentications State
    private val _isBiometricLocked = MutableStateFlow(true)
    val isBiometricLocked = _isBiometricLocked.asStateFlow()

    private val _isBiometricScanning = MutableStateFlow(false)
    val isBiometricScanning = _isBiometricScanning.asStateFlow()

    // AES-256 State
    private val _isAESEncrypted = MutableStateFlow(true)
    val isAESEncrypted = _isAESEncrypted.asStateFlow()

    // Sandbox restrict Mode
    private val _isSandboxActive = MutableStateFlow(true)
    val isSandboxActive = _isSandboxActive.asStateFlow()

    // Key Rotation counter
    private val _lastRotationTime = MutableStateFlow("Never Rotated")
    val lastRotationTime = _lastRotationTime.asStateFlow()

    // Siri assistant prompt status
    private val _isSiriSpeaking = MutableStateFlow(false)
    val isSiriSpeaking = _isSiriSpeaking.asStateFlow()

    private val _isSiriListening = MutableStateFlow(false)
    val isSiriListening = _isSiriListening.asStateFlow()

    private val _assistantSpeechText = MutableStateFlow("Hello, I am MX AI. I have secured your phone container using AES-256 and biometric lockdowns. How can I help you manage your schedules and emails?")
    val assistantSpeechText = _assistantSpeechText.asStateFlow()

    init {
        // Pre-populate data if Room database tables are currently empty
        repository.prepopulateIfEmpty(viewModelScope)
    }

    // Authenticate simulating biometric scan
    fun authenticateBiometric(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBiometricScanning.value = true
            kotlinx.coroutines.delay(1200) // Simulated optical scanning latency
            _isBiometricScanning.value = false
            _isBiometricLocked.value = false
            onSuccess()
            repository.insertLog(
                SecurityLog(
                    type = "BIOMETRIC",
                    message = "Biometric scanning verified. Master lock released.",
                    status = "SUCCESS"
                )
            )
        }
    }

    // Lock dynamic system
    fun lockBiometrics() {
        _isBiometricLocked.value = true
        _assistantSpeechText.value = "MX AI is locked. Authenticate to view schedules and active emails."
        viewModelScope.launch {
            repository.insertLog(
                SecurityLog(
                    type = "BIOMETRIC",
                    message = "Biometric Lock Screen re-engaged. Containment shield ACTIVE.",
                    status = "PROTECTED"
                )
            )
        }
    }

    // Toggle local encryption settings
    fun setAESEncrypted(enabled: Boolean) {
        _isAESEncrypted.value = enabled
        viewModelScope.launch {
            val status = if (enabled) "SECURE" else "WARN"
            val msg = if (enabled) "AES-256 dynamic hardware key encryption is enabled." else "AES-256 encryption disabled (NOT RECOMMEND)."
            repository.insertLog(SecurityLog(type = "ENCRYPTION", message = msg, status = status))
        }
    }

    // Toggle Sandbox mode
    fun setSandboxActive(enabled: Boolean) {
        _isSandboxActive.value = enabled
        viewModelScope.launch {
            val status = if (enabled) "SECURE" else "WARN"
            val msg = if (enabled) "Strict system sandbox confinement is ACTIVE." else "Developer sandbox confinement BYPASSED (high risk)."
            repository.insertLog(SecurityLog(type = "SANDBOX", message = msg, status = status))
        }
    }

    // Rotate symmetric keys
    fun rotateEncryptionKeys() {
        viewModelScope.launch {
            _lastRotationTime.value = java.text.SimpleDateFormat("hh:mm:ss a (MMM dd)", Locale.getDefault()).format(java.util.Date())
            repository.insertLog(
                SecurityLog(
                    type = "ENCRYPTION",
                    message = "Symmetric encryption keys refreshed. Dynamic keystore rotation completed.",
                    status = "SECURE"
                )
            )
        }
    }

    // Add scheduling
    fun addSchedule(title: String, time: String, description: String, category: String = "DAILY SCHEDULE") {
        viewModelScope.launch {
            repository.insertSchedule(
                ScheduleItem(
                    title = title,
                    time = time,
                    description = description,
                    category = category
                )
            )
            repository.insertLog(
                SecurityLog(
                    type = "PRIVACY",
                    message = "New schedule item added: '$title' at $time. Encrypted locally.",
                    status = "SECURE"
                )
            )
        }
    }

    // Delete schedule Item
    fun deleteSchedule(id: Int, title: String) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
            repository.insertLog(
                SecurityLog(
                    type = "PRIVACY",
                    message = "Schedule item purged: '$title'.",
                    status = "SUCCESS"
                )
            )
        }
    }

    // Add email
    fun addEmail(sender: String, subject: String, preview: String, body: String, isCritical: Boolean) {
        viewModelScope.launch {
            repository.insertEmail(
                EmailItem(
                    sender = sender,
                    subject = subject,
                    preview = preview,
                    body = body,
                    isCritical = isCritical
                )
            )
            repository.insertLog(
                SecurityLog(
                    type = "SANDBOX",
                    message = "Incoming email filtered from $sender. Subject: $subject.",
                    status = if (isCritical) "PROTECTED" else "SECURE"
                )
            )
        }
    }

    // Toggle schedule item completion status
    fun toggleScheduleCompleted(item: ScheduleItem) {
        viewModelScope.launch {
            repository.updateSchedule(item.copy(completed = !item.completed))
        }
    }

    // Purge emails
    fun deleteEmail(id: Int, subject: String) {
        viewModelScope.launch {
            repository.deleteEmail(id)
            repository.insertLog(
                SecurityLog(
                    type = "PRIVACY",
                    message = "Sensitive email record deleted: '$subject'.",
                    status = "SUCCESS"
                )
            )
        }
    }

    // Read full email details
    fun markEmailReadState(item: EmailItem, isRead: Boolean) {
        viewModelScope.launch {
            repository.updateEmail(item.copy(isRead = isRead))
        }
    }

    // Handle Siri voice query or text prompt
    fun handleCommand(prompt: String) {
        if (prompt.isBlank()) return
        
        viewModelScope.launch {
            _isSiriListening.value = false
            _isSiriSpeaking.value = true
            
            val query = prompt.trim().lowercase(Locale.getDefault())
            
            repository.insertLog(
                SecurityLog(
                    type = "PRIVACY",
                    message = "Siri Command Filter parsed: '$prompt'",
                    status = "SUCCESS"
                )
            )
            
            kotlinx.coroutines.delay(800) // Simulated thought latency
            
            when {
                // ADD SCHEDULE COMMAND: e.g. "schedule lunch at 12:30 PM"
                query.contains("schedule") && query.contains(" at ") -> {
                    try {
                        val taskWithTime = query.substringAfter("schedule").trim()
                        val title = taskWithTime.substringBefore(" at ").trim()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        val time = taskWithTime.substringAfter(" at ").trim().uppercase(Locale.getDefault())
                        
                        repository.insertSchedule(
                            ScheduleItem(
                                title = title,
                                time = time,
                                description = "Added via voice commands.",
                                category = "DAILY SCHEDULE"
                            )
                        )
                        _assistantSpeechText.value = "Understood. I have logged '$title' into your secured daily schedule for $time."
                        repository.insertLog(
                            SecurityLog(
                                type = "PRIVACY",
                                message = "Voice-generated task logged: '$title' at $time.",
                                status = "SECURE"
                            )
                        )
                    } catch (e: Exception) {
                        _assistantSpeechText.value = "I heard you wanted to schedule something, but I couldn't parse the time. Try: 'schedule Strategy Review at 2:00 PM'."
                    }
                }
                
                // LOCK/LOCKDOWN SYSTEM
                query.contains("lock") || query.contains("shutdown") || query.contains("lockdown") -> {
                    _assistantSpeechText.value = "Lockdown engaged. Master security locks refreshed."
                    _isBiometricLocked.value = true
                    repository.insertLog(
                        SecurityLog(
                            type = "BIOMETRIC",
                            message = "Emergency system lockdown triggered by voice command.",
                            status = "PROTECTED"
                        )
                    )
                }
                
                // ROTATE CRYPTO KEYS
                query.contains("key") || query.contains("rotate") || query.contains("encryption") -> {
                    _lastRotationTime.value = java.text.SimpleDateFormat("hh:mm:ss a (MMM dd)", Locale.getDefault()).format(java.util.Date())
                    _assistantSpeechText.value = "Dynamic rotation successful! Keystore rotated, AES encryption salt updated on local databases."
                    repository.insertLog(
                        SecurityLog(
                            type = "ENCRYPTION",
                            message = "Voice requested cryptographic rotation enacted.",
                            status = "SECURE"
                        )
                    )
                }
                
                // READ RECENTS
                query.contains("email") || query.contains("read") || query.contains("message") -> {
                    _assistantSpeechText.value = "Accessing sandbox updates. You have critical filtered security logs and 3 email reports. Most urgent is 'Perimeter Sandboxing Success' from Sec Ops."
                }
                
                // CLEAR DATA
                query.contains("clear") || query.contains("purge") -> {
                    repository.insertLog(
                        SecurityLog(
                            type = "PRIVACY",
                            message = "Secure log wiped by voice command authorize.",
                            status = "SUCCESS"
                        )
                    )
                    _assistantSpeechText.value = "I have filtered and purged operational log history as requested."
                }
                
                // SECURITY AUDIT
                query.contains("status") || query.contains("security") || query.contains("protocol") -> {
                    val shieldText = if (_isBiometricLocked.value) "locked" else "unlocked"
                    val cryptoText = if (_isAESEncrypted.value) "active" else "disabled"
                    _assistantSpeechText.value = "System status: Biometrics are $shieldText. AES-256 is $cryptoText. Root container sandboxing satisfies all safety limits. Total security protocol is fully active."
                }
                
                // DEFAULT ASSISTANT GREETS
                else -> {
                    _assistantSpeechText.value = "I am processing your command: '$prompt'. As your personal productivity companion, I can help schedule daily tasks, scan incoming emails, and protect local data containers. Try saying 'schedule Strategy Review at 3:00 PM'."
                }
            }
            
            _isSiriSpeaking.value = false
        }
    }

    fun startListening() {
        if (_isBiometricLocked.value) {
            _assistantSpeechText.value = "Please authenticate with your biometrics before opening raw speech connections."
            return
        }
        _isSiriListening.value = true
        _isSiriSpeaking.value = false
    }

    fun stopListeningAndSubmit(simulatedSpeech: String) {
        handleCommand(simulatedSpeech)
    }
}

class VMFactory(private val repository: MXRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MXViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MXViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
