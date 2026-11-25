package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AdhanVoiceDoc
import com.example.data.firebase.AdhkarItemDoc
import com.example.data.firebase.AdminAccess
import com.example.data.firebase.AdminRepository
import com.example.data.firebase.AiRuntimeConfig
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.MihrabRepository
import com.example.data.firebase.QuranDefaultsConfig
import com.example.data.firebase.ReciterDoc
import com.example.data.firebase.UserSettingsDoc
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val mihrabRepository: MihrabRepository,
    private val currentUserId: String,
    private val isDelegatedAdmin: Boolean = false
) : ViewModel() {

    val isAuthorized: Boolean
        get() = AdminAccess.isSuperAdmin(Firebase.auth.currentUser) || isDelegatedAdmin

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _backupExportJson = MutableStateFlow<String?>(null)
    val backupExportJson: StateFlow<String?> = _backupExportJson.asStateFlow()

    val recitersAdminState: StateFlow<List<ReciterDoc>> =
        if (isAuthorized) {
            adminRepository.observeAllRecitersAdmin()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            flowOf(emptyList<ReciterDoc>()).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }

    val adhkarAdminState: StateFlow<List<AdhkarItemDoc>> =
        if (isAuthorized) {
            adminRepository.observeAllAdhkarAdmin()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            flowOf(emptyList<AdhkarItemDoc>()).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }

    val adhanVoicesAdminState: StateFlow<List<AdhanVoiceDoc>> =
        if (isAuthorized) {
            adminRepository.observeAllAdhanVoicesAdmin()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            flowOf(emptyList<AdhanVoiceDoc>()).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }

    val usersAdminState: StateFlow<List<UserSettingsDoc>> =
        if (isAuthorized) {
            adminRepository.observeAllUsersAdmin()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            flowOf(emptyList<UserSettingsDoc>()).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }

    val quranDefaultsState: StateFlow<QuranDefaultsConfig> =
        mihrabRepository.observeQuranDefaults(currentUserId)
            .catch { emit(QuranDefaultsConfig()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), QuranDefaultsConfig())

    val aiConfigState: StateFlow<AiRuntimeConfig> =
        mihrabRepository.observeAiConfig(currentUserId)
            .catch { emit(AiRuntimeConfig()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AiRuntimeConfig())

    val appOperationsState: StateFlow<AppOperationsConfig> =
        mihrabRepository.observeAppOperations(currentUserId)
            .catch { emit(AppOperationsConfig()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppOperationsConfig())

    fun clearMessage() {
        _adminMessage.value = null
    }

    fun clearBackupExport() {
        _backupExportJson.value = null
    }

    private fun runAdminTask(successMsg: String, block: suspend () -> Result<Unit>) {
        if (!isAuthorized) {
            _adminMessage.value = "غير مصرح: هذه العملية متاحة للمشرف الأعلى فقط."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            block()
                .onSuccess { _adminMessage.value = successMsg }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "تعذر تنفيذ العملية" }
            _isBusy.value = false
        }
    }

    fun saveQuranDefaults(cfg: QuranDefaultsConfig) =
        runAdminTask("تم حفظ إعدادات المصحف والافتراضيات") {
            adminRepository.saveQuranDefaults(cfg)
        }

    fun saveAiConfig(cfg: AiRuntimeConfig) =
        runAdminTask("تم تحديث إعدادات المساعد الذكي") {
            adminRepository.saveAiConfig(cfg)
        }

    fun saveAppOperations(cfg: AppOperationsConfig) =
        runAdminTask("تم نشر إعدادات العمليات والأعلام العامة") {
            adminRepository.saveAppOperations(cfg)
        }

    fun upsertReciter(reciter: ReciterDoc) =
        runAdminTask("تم حفظ بيانات القارئ") {
            adminRepository.upsertReciter(reciter)
        }

    fun deleteReciter(reciterId: String) =
        runAdminTask("تم حذف القارئ") {
            adminRepository.deleteReciter(reciterId)
        }

    fun seedDefaultReciters() {
        if (!isAuthorized) return
        viewModelScope.launch {
            _isBusy.value = true
            adminRepository.seedDefaultReciters()
                .onSuccess { n -> _adminMessage.value = "تمت مزامنة $n قراء للسحابة" }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "تعذر المزامنة" }
            _isBusy.value = false
        }
    }

    fun upsertAdhkar(item: AdhkarItemDoc) =
        runAdminTask("تم حفظ الذكر في الموسوعة") {
            adminRepository.upsertAdhkar(item)
        }

    fun deleteAdhkar(itemId: String) =
        runAdminTask("تم حذف الذكر") {
            adminRepository.deleteAdhkar(itemId)
        }

    fun seedDefaultAdhkar() {
        if (!isAuthorized) return
        viewModelScope.launch {
            _isBusy.value = true
            adminRepository.seedDefaultAdhkar()
                .onSuccess { n -> _adminMessage.value = "تمت مزامنة $n ذكراً للسحابة" }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "تعذر المزامنة" }
            _isBusy.value = false
        }
    }

    fun upsertAdhanVoice(voice: AdhanVoiceDoc) =
        runAdminTask("تم حفظ صوت المؤذن") {
            adminRepository.upsertAdhanVoice(voice)
        }

    fun deleteAdhanVoice(voiceId: String) =
        runAdminTask("تم حذف صوت المؤذن") {
            adminRepository.deleteAdhanVoice(voiceId)
        }

    fun seedDefaultAdhanVoices() {
        if (!isAuthorized) return
        viewModelScope.launch {
            _isBusy.value = true
            adminRepository.seedDefaultAdhanVoices()
                .onSuccess { n -> _adminMessage.value = "تمت مزامنة $n أصوات أذان" }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "تعذر المزامنة" }
            _isBusy.value = false
        }
    }

    fun updateUserAsAdmin(targetUid: String, fields: Map<String, Any>) =
        runAdminTask("تم تحديث بيانات المستخدم") {
            adminRepository.updateUserAsAdmin(targetUid, fields)
        }

    fun setUserVerified(targetUid: String, verified: Boolean) =
        runAdminTask(if (verified) "تم توثيق الحساب بنجاح ✓" else "تم إلغاء توثيق الحساب") {
            adminRepository.setUserVerified(targetUid, verified)
        }

    fun setUserSuspended(targetUid: String, suspended: Boolean) =
        runAdminTask(if (suspended) "تم تعليق الحساب بنجاح" else "تم رفع التعليق عن الحساب ✓") {
            adminRepository.setUserSuspended(targetUid, suspended)
        }

    fun setUserAdminRole(targetUid: String, makeAdmin: Boolean) =
        runAdminTask(if (makeAdmin) "تم تعيين المستخدم كمسؤول (أدمن) ✓" else "تمت إزالة صلاحية الأدمن") {
            adminRepository.setUserAdminRole(targetUid, makeAdmin)
        }

    fun exportFullDatabaseBackup() {
        if (!isAuthorized) return
        viewModelScope.launch {
            _isBusy.value = true
            adminRepository.exportFullDatabaseBackupJson()
                .onSuccess { _backupExportJson.value = it }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "تعذر التصدير" }
            _isBusy.value = false
        }
    }

    fun importFullDatabaseBackup(rawJson: String) {
        if (!isAuthorized) return
        viewModelScope.launch {
            _isBusy.value = true
            adminRepository.importFullDatabaseBackupJson(rawJson)
                .onSuccess { count -> _adminMessage.value = "تم استيراد $count سجل إلى قاعدة البيانات" }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "ملف النسخة الاحتياطية غير صالح" }
            _isBusy.value = false
        }
    }
}
