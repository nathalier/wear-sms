package com.example.wearsms.ui

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.input.RemoteInputIntentHelper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.wearsms.data.Contact
import com.example.wearsms.data.Message
import com.example.wearsms.data.MessageType
import com.example.wearsms.data.ThreadSummary
import com.example.wearsms.data.statusParts
import com.example.wearsms.data.validNumber

@Composable
fun ThreadListScreen(
    threads: List<ThreadSummary>,
    onNewMessage: () -> Unit,
    onExportAll: () -> Unit,
    onDelete: (ThreadSummary) -> Unit,
    onOpen: (ThreadSummary) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    var pendingDelete by remember { mutableStateOf<ThreadSummary?>(null) }
    ConfirmDelete("Delete conversation?", pendingDelete != null, onDismiss = { pendingDelete = null }) { pendingDelete?.let(onDelete) }
    ScreenScaffold(listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item { ListHeader { Text("Messages") } }
            item {
                Button(
                    onClick = onNewMessage,
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors(LightYellow),
                    label = { Text("New message") },
                )
            }
            if (threads.isEmpty()) item { Text("No conversations") }
            items(threads, key = { it.threadId }) { thread ->
                Button(
                    onClick = { onOpen(thread) },
                    onLongClick = { pendingDelete = thread },
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors(LightBlue),
                    label = { Text(thread.title, maxLines = 1) },
                    secondaryLabel = { Text(thread.snippet, maxLines = 2) },
                )
            }
            if (threads.isNotEmpty()) item { Button(onClick = onExportAll, modifier = Modifier.fillMaxWidth(), colors = buttonColors(LightYellow), label = { Text("Export all") }) }
        }
    }
}

@Composable
fun ConversationScreen(
    title: String,
    messages: List<Message>,
    onSend: (String) -> Unit,
    onRetry: (Message) -> Unit,
    onDelete: (Message) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    var pendingDelete by remember { mutableStateOf<Message?>(null) }
    ConfirmDelete("Delete message?", pendingDelete != null, onDismiss = { pendingDelete = null }) { pendingDelete?.let(onDelete) }
    val composeReply = rememberTextInputLauncher("Message", onSend)
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.scrollToItem(messages.size + REPLY_ITEM_OFFSET) }
    ScreenScaffold(listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding, autoCentering = null) {
            item { ListHeader { Text(title, maxLines = 1) } }
            if (messages.isEmpty()) item { Text("No messages yet", textAlign = TextAlign.Center) }
            items(messages, key = { it.id }) { MessageBubble(it, onRetry, onLongPress = { pendingDelete = it }) }
            item { Button(onClick = composeReply, label = { Text("Reply") }) }
        }
    }
}

@Composable
private fun rememberTextInputLauncher(label: String, onText: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.let { RemoteInput.getResultsFromIntent(it) }?.getCharSequence(INPUT_KEY)?.toString()
        if (!text.isNullOrBlank()) onText(text)
    }
    return {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(RemoteInput.Builder(INPUT_KEY).setLabel(label).build()))
        launcher.launch(intent)
    }
}

private val LightYellow = Color(0xFFF5E6A8)
private val LightBlue = Color(0xFFBCD7F2)
private val BrandDarkBlue = Color(0xFF0B2A6B)

@Composable
private fun buttonColors(container: Color) =
    ButtonDefaults.buttonColors(containerColor = container, contentColor = BrandDarkBlue, secondaryContentColor = BrandDarkBlue)

/** Header comes first, so the reply button sits at index messages.size + 1. */
private const val REPLY_ITEM_OFFSET = 1

private const val INPUT_KEY = "input"

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: Message, onRetry: (Message) -> Unit, onLongPress: () -> Unit) {
    val outgoing = message.type != MessageType.INBOX
    val failed = message.type == MessageType.FAILED
    val textAlign = if (outgoing) TextAlign.End else TextAlign.Start
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = { if (failed) onRetry(message) }, onLongClick = onLongPress).padding(horizontal = 8.dp)) {
        Text(
            text = coloredStatus(message),
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = message.body,
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            color = when {
                failed -> MaterialTheme.colorScheme.error
                outgoing -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground
            },
        )
    }
}

@Composable
fun SetDefaultScreen(onRequest: () -> Unit) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding, horizontalAlignment = Alignment.CenterHorizontally) {
            item { Text("Set Wear SMS as the default SMS app to read and send messages.", textAlign = TextAlign.Center) }
            item { Button(onClick = onRequest, label = { Text("Make default") }) }
        }
    }
}

@Composable
fun ContactSearchScreen(query: String, contacts: List<Contact>, onQueryChange: (String) -> Unit, onPick: (address: String) -> Unit) {
    val listState = rememberScalingLazyListState()
    val enterQuery = rememberTextInputLauncher("Name or number", onQueryChange)
    ScreenScaffold(listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item { ListHeader { Text("New message") } }
            item { Button(onClick = enterQuery, modifier = Modifier.fillMaxWidth(), colors = buttonColors(LightYellow), label = { Text(query.ifBlank { "Search or type number" }, maxLines = 1) }) }
            validNumber(query)?.let { number ->
                item { Button(onClick = { onPick(number) }, modifier = Modifier.fillMaxWidth(), colors = buttonColors(LightYellow), label = { Text("Send to $number", maxLines = 1) }) }
            }
            if (contacts.isEmpty() && validNumber(query) == null) item { Text("No contacts found", textAlign = TextAlign.Center) }
            items(contacts, key = { "${it.name}|${it.number}" }) { contact ->
                Button(
                    onClick = { onPick(contact.number) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors(LightBlue),
                    label = { Text(contact.name, maxLines = 1) },
                    secondaryLabel = contact.number.takeIf { it != contact.name }?.let { { Text(it, maxLines = 1) } },
                )
            }
        }
    }
}

@Composable
private fun ConfirmDelete(title: String, visible: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = { onConfirm(); onDismiss() }) },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = onDismiss) },
        title = { Text(title) },
    )
}

private fun coloredStatus(message: Message): AnnotatedString {
    val parts = statusParts(message)
    return buildAnnotatedString {
        parts.timestamp?.let { withStyle(SpanStyle(color = LightBlue)) { append(it) } }
        if (parts.timestamp != null && parts.status != null) append(" · ")
        parts.status?.let { withStyle(SpanStyle(color = LightYellow)) { append(it) } }
    }
}
