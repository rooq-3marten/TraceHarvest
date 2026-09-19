package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gateway.GatewayConfig
import com.example.data.gateway.GatewayProviderType
import com.example.data.gateway.UssdResponseType
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.SmsLogEntity
import com.example.ui.theme.BlockchainBlue
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.SesameAmberSecondary
import com.example.ui.theme.ViolationRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UssdGatewayScreen(
    smsLogs: List<SmsLogEntity>,
    farmers: List<FarmerEntity>,
    gatewayConfig: GatewayConfig,
    onSelectProvider: (GatewayProviderType) -> Unit,
    onExecuteUssd: (hops: String, phone: String) -> com.example.data.gateway.UssdResponse,
    onDispatchSms: (toPhone: String, farmerName: String, text: String, messageType: String) -> Unit,
    onProcessInboundSms: (fromPhone: String, text: String) -> Unit
) {
    val subTabs = listOf("Interactive USSD", "Two-Way SMS Gateway", "Gateway Providers")
    var selectedSubTab by remember { mutableStateOf(subTabs[0]) }

    val defaultFarmer = farmers.firstOrNull()
    var selectedPhone by remember { mutableStateOf(defaultFarmer?.phoneNumber ?: "+2348034512991") }

    // USSD Session State
    var currentHops by remember { mutableStateOf("") }
    var currentUssdScreen by remember {
        mutableStateOf(
            onExecuteUssd("", selectedPhone).message
        )
    }
    var isSessionEnded by remember { mutableStateOf(false) }
    var userUssdInput by remember { mutableStateOf("") }

    // Telco Carrier State
    val telcos = listOf("MTN Nigeria", "Airtel NG", "Glo Nigeria", "9mobile")
    var selectedTelco by remember { mutableStateOf(telcos[0]) }

    // SMS composer state
    var customSmsReply by remember { mutableStateOf("1 (CONFIRM)") }
    var outboundRecipientIndex by remember { mutableStateOf(0) }
    var recipientMenuExpanded by remember { mutableStateOf(false) }
    var outboundMessageText by remember {
        mutableStateOf("TraceHarvest: Batch NG-SES-2026-0042 verified for export. Moisture: 7.4%. Reply 1 to confirm.")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("ussd_gateway_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SesameAmberSecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "USSD & SMS Gateway Integration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${gatewayConfig.provider.displayName} • ${gatewayConfig.ussdServiceCode} • Shortcode ${gatewayConfig.shortCode}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Subtab Navigation
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subTabs) { tab ->
                    FilterChip(
                        selected = selectedSubTab == tab,
                        onClick = { selectedSubTab = tab },
                        label = { Text(tab, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HarvestGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("gateway_subtab_${tab.take(4)}")
                    )
                }
            }
        }

        when (selectedSubTab) {
            "Interactive USSD" -> {
                // USSD Feature Phone Simulator
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B231C)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Carrier & Status Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selectedTelco,
                                        fontSize = 11.sp,
                                        color = Color(0xFFA5D6A7),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Surface(
                                    color = if (isSessionEnded) Color(0xFF424242) else Color(0xFF1B5E20),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isSessionEnded) "SESSION ENDED" else "USSD ACTIVE (${gatewayConfig.ussdServiceCode})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Green phosphor monochrome screen
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF071107), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = currentUssdScreen,
                                        color = Color(0xFF76FF03),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                    if (currentHops.isNotEmpty()) {
                                        Text(
                                            text = "Raw Hops: ${gatewayConfig.ussdServiceCode}*${currentHops}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }

                            // Interactive Input Controls
                            if (!isSessionEnded) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = userUssdInput,
                                        onValueChange = { userUssdInput = it },
                                        placeholder = { Text("Enter choice (e.g. 1, 2, 3)", color = Color.Gray, fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ussd_reply_field"),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            if (userUssdInput.isNotBlank()) {
                                                val nextHop = if (currentHops.isEmpty()) userUssdInput.trim() else "$currentHops*${userUssdInput.trim()}"
                                                currentHops = nextHop
                                                userUssdInput = ""
                                                val response = onExecuteUssd(currentHops, selectedPhone)
                                                currentUssdScreen = response.message
                                                if (response.responseType == UssdResponseType.END) {
                                                    isSessionEnded = true
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ComplianceGreen),
                                        modifier = Modifier.testTag("ussd_submit_button")
                                    ) {
                                        Text("Send")
                                    }
                                }

                                // Quick Number Keypad Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("1", "2", "3", "4", "5", "0").forEach { num ->
                                        OutlinedButton(
                                            onClick = {
                                                val nextHop = if (currentHops.isEmpty()) num else "$currentHops*$num"
                                                currentHops = nextHop
                                                val response = onExecuteUssd(currentHops, selectedPhone)
                                                currentUssdScreen = response.message
                                                if (response.responseType == UssdResponseType.END) {
                                                    isSessionEnded = true
                                                }
                                            },
                                            modifier = Modifier.size(44.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(num, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }

                            // Dial / Restart button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        currentHops = ""
                                        isSessionEnded = false
                                        val response = onExecuteUssd("", selectedPhone)
                                        currentUssdScreen = response.message
                                    },
                                    modifier = Modifier.testTag("redial_ussd_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Redial ${gatewayConfig.ussdServiceCode}", fontSize = 11.sp)
                                }

                                Text(
                                    text = "Provider: ${gatewayConfig.provider.displayName}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFA5D6A7)
                                )
                            }
                        }
                    }
                }
            }

            "Two-Way SMS Gateway" -> {
                // Inbound Simulator Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Simulate Inbound Farmer SMS (Zero-Literacy Reply)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simulates incoming SMS to shortcode ${gatewayConfig.shortCode}. Automatic keyword parser will verify harvest batches and trigger responses.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            // Quick Keyword Chips
                            Text("Farmer Quick Reply Codes:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val quickCodes = listOf(
                                    "1 (CONFIRM)",
                                    "2 (DISPUTE)",
                                    "CONFIRM 0042",
                                    "STATUS",
                                    "REPORT SNIPER",
                                    "HELP"
                                )
                                items(quickCodes) { code ->
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ComplianceGreen),
                                        modifier = Modifier
                                            .clickable {
                                                onProcessInboundSms(selectedPhone, code)
                                            }
                                            .testTag("quick_code_${code.take(4)}")
                                    ) {
                                        Text(
                                            text = code,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ComplianceGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Custom SMS input row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customSmsReply,
                                    onValueChange = { customSmsReply = it },
                                    placeholder = { Text("Custom SMS text (e.g. BATCH 4821 CONFIRMED)") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("inbound_sms_text"),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (customSmsReply.isNotBlank()) {
                                            onProcessInboundSms(selectedPhone, customSmsReply)
                                            customSmsReply = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .background(HarvestGreenPrimary, CircleShape)
                                        .testTag("send_inbound_sms")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // Outbound SMS Dispatcher Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF8)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Dispatch Outbound Alert via ${gatewayConfig.provider.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Recipient selector
                            if (farmers.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = recipientMenuExpanded,
                                    onExpandedChange = { recipientMenuExpanded = !recipientMenuExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val currentRecipient = farmers.getOrElse(outboundRecipientIndex) { farmers.first() }
                                    OutlinedTextField(
                                        value = "${currentRecipient.fullName} (${currentRecipient.phoneNumber})",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Recipient Farmer") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipientMenuExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = recipientMenuExpanded,
                                        onDismissRequest = { recipientMenuExpanded = false }
                                    ) {
                                        farmers.forEachIndexed { idx, f ->
                                            DropdownMenuItem(
                                                text = { Text("${f.fullName} (${f.crop} • ${f.phoneNumber})") },
                                                onClick = {
                                                    outboundRecipientIndex = idx
                                                    selectedPhone = f.phoneNumber
                                                    recipientMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = outboundMessageText,
                                onValueChange = { outboundMessageText = it },
                                label = { Text("Message Body (160 GSM chars)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Button(
                                onClick = {
                                    val farmer = farmers.getOrElse(outboundRecipientIndex) { farmers.firstOrNull() }
                                    val name = farmer?.fullName ?: "Farmer"
                                    onDispatchSms(selectedPhone, name, outboundMessageText, "GATEWAY_OUTBOUND")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispatch_sms_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary)
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dispatch SMS via ${gatewayConfig.provider.displayName}", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // SMS Live Logs Stream
                item {
                    Text(
                        text = "Two-Way Gateway Message History (${smsLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(smsLogs, key = { it.id }) { sms ->
                    val isOutbound = sms.direction == "OUTBOUND"
                    val dateFormat = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
                    val timeStr = dateFormat.format(Date(sms.timestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isOutbound) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOutbound) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                            ),
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isOutbound) 2.dp else 12.dp,
                                bottomEnd = if (isOutbound) 12.dp else 2.dp
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isOutbound) "${gatewayConfig.provider.displayName} (To: ${sms.farmerName})" else "From: ${sms.farmerName} (${sms.farmerPhone})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isOutbound) ComplianceGreen else Color(0xFF0D47A1)
                                    )
                                    Text(
                                        text = timeStr,
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sms.content,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E231E),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isOutbound) ComplianceGreen else Color(0xFF0D47A1),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isOutbound) "DLR: Delivered in 920ms via ${selectedTelco}" else "Inbound Webhook Received",
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "Gateway Providers" -> {
                // Providers Evaluation Card
                item {
                    Text(
                        text = "Nigerian USSD & SMS Gateway Providers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TraceHarvest supports multiple gateway providers. For Nigerian export pilots, Africa's Talking provides established sandbox and live APIs; Streamcomm and Telkosh offer direct telco operator SMPP routing.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                items(GatewayProviderType.entries) { provider ->
                    val isSelected = gatewayConfig.provider == provider
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProvider(provider) }
                            .testTag("provider_card_${provider.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF1F8E9) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) ComplianceGreen else Color(0xFFE0E5DD)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = provider.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) ComplianceGreen else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Surface(
                                        color = ComplianceGreen,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE GATEWAY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = provider.description,
                                fontSize = 12.sp,
                                color = Color(0xFF424242),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Coverage", fontSize = 9.sp, color = Color.Gray)
                                    Text(provider.telcoCoverage, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("USSD Code", fontSize = 9.sp, color = Color.Gray)
                                    Text(provider.defaultUssdCode, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Sender ID", fontSize = 9.sp, color = Color.Gray)
                                    Text(provider.defaultSenderId, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
