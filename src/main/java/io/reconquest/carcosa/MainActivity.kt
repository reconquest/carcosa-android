package io.reconquest.carcosa

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.reconquest.carcosa.lib.Carcosa
import io.reconquest.carcosa.lib.ListResult
import io.reconquest.carcosa.lib.Repo
import io.reconquest.carcosa.lib.Token
import io.reconquest.carcosa.ui.main.AuditEvent
import io.reconquest.carcosa.ui.main.AuditLevel
import io.reconquest.carcosa.ui.main.MainScreen
import java.nio.file.Paths

class MainActivity : AppCompatActivity() {
    private val carcosa = Carcosa()
    private lateinit var session: Session

    private val repos = mutableStateListOf<Repo>()
    private val auditEvents = mutableStateListOf<AuditEvent>()

    private var loading by mutableStateOf(true)
    private var syncing by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        session = Session(this, carcosa) {
            repos.clear()
            loading = false
            audit("session", "expired; native state destroyed", AuditLevel.Warning)
        }

        if (!initCarcosa()) {
            return
        }

        window.statusBarColor = android.graphics.Color.rgb(3, 4, 6)
        window.navigationBarColor = android.graphics.Color.rgb(3, 4, 6)

        setContent {
            MainScreen(
                repos = repos,
                loading = loading,
                syncing = syncing,
                error = error,
                auditEvents = auditEvents,
                onSync = { sync() },
                onAddRepo = { gotoRepoActivity(null) },
                onSettings = { gotoSettingsActivity() },
                onAbout = { gotoAboutActivity() },
                onUnlockRepo = { repo -> gotoRepoActivity(repo) },
                onViewToken = { token -> viewToken(token) },
                onCopyToken = { token -> copyToken(token) },
            )
        }

        list()
    }

    override fun onPause() {
        super.onPause()
        session.onPause()
    }

    override fun onResume() {
        super.onResume()
        session.onResume()
    }

    private fun initCarcosa(): Boolean {
        if (carcosa.hasState()) {
            return true
        }

        val pin = intent.getStringExtra("pin")
        if (pin == null) {
            audit("auth", "missing session pin; redirecting to biometric gate", AuditLevel.Warning)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return false
        }

        val init = carcosa.init(
            Paths.get(applicationContext.filesDir.path).toString(),
            pin,
        )
        if (init.error != null) {
            FatalErrorDialog(this, init.error).show()
            return false
        }

        audit("auth", "native vault state initialized", AuditLevel.Success)
        return true
    }

    fun list() {
        loading = true
        error = null
        audit("list", "scanning connected repositories", AuditLevel.Info)

        Thread {
            val list: Maybe<ListResult> = carcosa.list()
            runOnUiThread {
                loading = false
                if (list.error != null) {
                    error = list.error
                    audit("list", list.error, AuditLevel.Error)
                    FatalErrorDialog(this, list.error).show()
                } else {
                    repos.clear()
                    repos.addAll(list.result.repos.orEmpty())
                    audit(
                        "list",
                        "loaded ${list.result.repos.size} repositories",
                        AuditLevel.Success,
                    )
                }
            }
        }.start()
    }

    private fun sync() {
        if (syncing) {
            return
        }

        syncing = true
        error = null
        audit("sync", "sync started", AuditLevel.Info)

        Thread {
            val sync = carcosa.sync()
            runOnUiThread {
                syncing = false
                if (sync.error != null) {
                    error = sync.error
                    audit("sync", sync.error, AuditLevel.Error)
                } else {
                    audit("sync", "sync completed", AuditLevel.Success)
                    list()
                }
            }
        }.start()
    }

    private fun viewToken(token: Token) {
        audit("decrypt", "view ${token.name ?: "secret"}", AuditLevel.Info)
    }

    private fun copyToken(token: Token) {
        Clipboard(this).clip(
            token.name ?: "secret",
            (token.payload ?: "").trim(),
            "Secret copied to clipboard.",
        )
        audit("copy", token.name ?: "secret", AuditLevel.Success)
    }

    private fun audit(type: String, message: String, level: AuditLevel) {
        auditEvents += AuditEvent(type = type, message = message, level = level)
        if (auditEvents.size > 200) {
            auditEvents.removeRange(0, auditEvents.size - 200)
        }
    }

    fun gotoRepoActivity(repo: Repo?) {
        val intent = Intent(this, RepoActivity::class.java)
        intent.putExtra("carcosa", carcosa)
        intent.putExtra("repo", repo)
        startActivity(intent)
    }

    fun gotoSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra("carcosa", carcosa)
        startActivity(intent)
    }

    fun gotoAboutActivity() {
        val intent = Intent(this, AboutActivity::class.java)
        intent.putExtra("carcosa", carcosa)
        startActivity(intent)
    }
}
