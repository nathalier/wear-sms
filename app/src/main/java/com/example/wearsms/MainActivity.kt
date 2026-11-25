package com.example.wearsms

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.wearsms.data.ContactsRepository
import com.example.wearsms.data.Message
import com.example.wearsms.data.SmsRepository
import com.example.wearsms.data.isUnreadInbox
import com.example.wearsms.ui.ContactSearchScreen
import com.example.wearsms.ui.ConversationScreen
import com.example.wearsms.ui.SetDefaultScreen
import com.example.wearsms.ui.ThreadListScreen

class MainActivity : ComponentActivity() {

    private val openThread = mutableStateOf<OpenThread?>(null)
    private val contactsGranted = mutableStateOf(false)
    private val isDefault = mutableStateOf(false)
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        contactsGranted.value = hasPermission(Manifest.permission.READ_CONTACTS)
    }

    private val exporter by lazy { MessageExporter(this) }

    private fun exportAndToast(label: String, load: () -> List<Message>) {
        lifecycleScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { exporter.export(label, load()) } }
            Toast.makeText(this@MainActivity, result.fold({ "Saved to $it" }, { "Export failed" }), Toast.LENGTH_LONG).show()
        }
    }

    private fun hasPermission(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    override fun onResume() {
        super.onResume()
        isDefault.value = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_SMS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openThread.value = threadFrom(intent)
    }

    private fun threadFrom(intent: Intent): OpenThread? {
        val threadId = intent.getLongExtra(MessageNotifier.EXTRA_THREAD_ID, -1)
        val address = intent.getStringExtra(MessageNotifier.EXTRA_ADDRESS)
        return if (threadId >= 0 && address != null) OpenThread(threadId, address) else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val roleManager = getSystemService(RoleManager::class.java)
        val repository = SmsRepository(contentResolver)
        val sender = SmsSender(this)
        val notifier = MessageNotifier(this)
        val contacts = ContactsRepository(contentResolver)
        contactsGranted.value = hasPermission(Manifest.permission.READ_CONTACTS)
        val missing = listOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_CONTACTS).filterNot(::hasPermission)
        if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray())
        openThread.value = threadFrom(intent)

        setContent {
            val roleRequest = rememberLauncherForActivityResult(StartActivityForResult()) {}
            val navController = rememberSwipeDismissableNavController()
            val scope = rememberCoroutineScope()

            AppScaffold {
                if (!isDefault.value) {
                    SetDefaultScreen { roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)) }
                    return@AppScaffold
                }
                LaunchedEffect(openThread.value) {
                    openThread.value?.let { navController.navigate("thread/${it.id}/${Uri.encode(it.address)}") }
                    openThread.value = null
                }
                SwipeDismissableNavHost(navController, startDestination = "threads") {
                    composable("threads") {
                        val named = contactsGranted.value
                        val threads by remember(named) { repository.threads().map { list -> list.map { it.copy(name = contacts.nameFor(it.address)) } }.flowOn(Dispatchers.IO) }.collectAsState(emptyList())
                        ThreadListScreen(
                            threads,
                            onNewMessage = { navController.navigate("search") },
                            onExportAll = { exportAndToast("all") { repository.all() } },
                            onDelete = { thread -> scope.launch(Dispatchers.IO) { repository.deleteThread(thread.threadId) } },
                        ) { navController.navigate("thread/${it.threadId}/${Uri.encode(it.address)}") }
                    }
                    composable("search") {
                        var query by remember { mutableStateOf("") }
                        val results by produceState(emptyList(), query, contactsGranted.value) { value = withContext(Dispatchers.IO) { contacts.search(query) } }
                        ContactSearchScreen(query, results, onQueryChange = { query = it }) { address ->
                            scope.launch {
                                val threadId = withContext(Dispatchers.IO) { Telephony.Threads.getOrCreateThreadId(this@MainActivity, address) }
                                navController.navigate("thread/$threadId/${Uri.encode(address)}") { popUpTo("threads") }
                            }
                        }
                    }
                    composable(
                        "thread/{id}/{address}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("address") { type = NavType.StringType }),
                    ) { entry ->
                        val threadId = entry.arguments!!.getLong("id")
                        val address = entry.arguments!!.getString("address").orEmpty()
                        val messages by remember { repository.conversation(threadId) }.collectAsState(emptyList())
                        LaunchedEffect(messages) {
                            if (messages.any { it.isUnreadInbox }) {
                                repository.markRead(threadId)
                                notifier.dismiss(threadId)
                            }
                        }
                        val title = remember(address, contactsGranted.value) { contacts.nameFor(address) ?: address }
                        ConversationScreen(title, messages, onSend = { sender.send(address, it) },
                            onRetry = sender::retry,
                            onDelete = { message -> scope.launch(Dispatchers.IO) { repository.deleteMessage(message.id) } },
                        )
                    }
                }
            }
        }
    }
}

private data class OpenThread(val id: Long, val address: String)
