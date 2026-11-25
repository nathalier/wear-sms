package com.example.wearsms

import android.Manifest
import android.app.role.RoleManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

class MainActivity : ComponentActivity() {

    private val recipient get() = intent.getStringExtra("to")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val roleAvailable = getSystemService(RoleManager::class.java).isRoleAvailable(RoleManager.ROLE_SMS)
        val smsManager = getSystemService(SmsManager::class.java)

        setContent {
            var status by remember { mutableStateOf("idle") }
            val sendTestSms = { to: String ->
                status = runCatching { smsManager.sendTextMessage(to, null, "wear-sms spike", null, null) }
                    .fold({ "sent" }, { "failed: ${it.message}" })
            }
            val permission = rememberLauncherForActivityResult(RequestPermission()) { granted ->
                if (granted) recipient?.let(sendTestSms) else status = "SEND_SMS denied"
            }

            val roleManager = getSystemService(RoleManager::class.java)
            var isDefault by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_SMS)) }
            val roleRequest = rememberLauncherForActivityResult(StartActivityForResult()) {
                isDefault = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            }

            AppScaffold {
                ScreenScaffold {
                    ScalingLazyColumn {
                        item { Text("SMS role available: $roleAvailable") }
                        item { Text("SmsManager: ${smsManager != null}") }
                        item { Text("Default SMS app: $isDefault") }
                        item { Text("Test SMS: $status") }
                        item {
                            Button(onClick = { roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)) }) {
                                Text("Make default")
                            }
                        }
                        item {
                            Button(onClick = { permission.launch(Manifest.permission.SEND_SMS) }) {
                                Text("Send test")
                            }
                        }
                    }
                }
            }
        }
    }
}
