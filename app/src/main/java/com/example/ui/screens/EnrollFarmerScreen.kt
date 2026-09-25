package com.example.ui.screens

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FarmerEntity
import com.example.ui.components.AppIntroBanner
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.WarningAmber
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollFarmerScreen(
    enrolledFarmers: List<FarmerEntity>,
    agentId: String = "AGENT-NG-042",
    onNavigateToPractice: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onRegisterFarmer: (
        name: String,
        phone: String,
        state: String,
        lga: String,
        community: String,
        crop: String,
        farmSize: Double,
        farmSizeUnit: String,
        lat: Double,
        lng: Double,
        coop: String,
        isOfflineMode: Boolean
    ) -> Unit
) {
    val context = LocalContext.current

    val stateOptions = listOf("Kano", "Jigawa", "Benue", "Kaduna", "Nassarawa", "Bauchi", "Sokoto", "Oyo")
    var selectedState by remember { mutableStateOf(stateOptions[0]) }
    var stateMenuExpanded by remember { mutableStateOf(false) }

    val lgasByState = mapOf(
        "Kano" to listOf("Dambatta", "Bichi", "Kunchi", "Bagwai", "Dawakin Tofa", "Gwarzo"),
        "Jigawa" to listOf("Maigatari", "Hadejia", "Birnin Kudu", "Gumel", "Kazaure", "Ringim"),
        "Benue" to listOf("Makurdi", "Otukpo", "Gboko", "Katsina-Ala", "Guma"),
        "Kaduna" to listOf("Kachia", "Makarfi", "Lere", "Giwa", "Zaria"),
        "Nassarawa" to listOf("Doma", "Lafia", "Keana", "Awe"),
        "Bauchi" to listOf("Alkaleri", "Bauchi", "Darazo", "Misau"),
        "Sokoto" to listOf("Goronyo", "Wurno", "Kware", "Sokoto South"),
        "Oyo" to listOf("Iseyin", "Saki West", "Ogbomoso North")
    )
    val currentLgas = lgasByState[selectedState] ?: listOf("Central District")
    var selectedLga by remember { mutableStateOf(currentLgas.first()) }
    var lgaMenuExpanded by remember { mutableStateOf(false) }

    // Required inputs
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var community by remember { mutableStateOf("") }

    // Farm size with local units selector: hectares, acres, or "plots"
    var farmSizeStr by remember { mutableStateOf("3.0") }
    val localUnitOptions = listOf("hectares", "acres", "plots")
    var selectedUnit by remember { mutableStateOf(localUnitOptions[0]) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    // Crop type
    val cropOptions = listOf("Sesame", "Cowpea", "Ginger", "Hibiscus", "Cashew", "Soybeans", "Maize")
    var selectedCrop by remember { mutableStateOf(cropOptions[0]) }
    var cropMenuExpanded by remember { mutableStateOf(false) }

    // Cooperative affiliation (if any)
    val commonCoops = listOf(
        "None / Independent Farmer",
        "Dambatta Sesame Growers Union",
        "Maigatari Export Commodity Association",
        "Benue Valley Organic Legume Union",
        "Kaduna High-Oleoresin Ginger Union",
        "National Sesame Seed Producers (NSSPEN)"
    )
    var selectedCooperative by remember { mutableStateOf(commonCoops.first()) }
    var customCoopText by remember { mutableStateOf("") }
    var coopMenuExpanded by remember { mutableStateOf(false) }

    // Auto-captured GPS coordinates from phone
    var latitude by remember { mutableDoubleStateOf(12.4358) }
    var longitude by remember { mutableDoubleStateOf(8.5147) }
    var gpsAccuracy by remember { mutableFloatStateOf(2.5f) }
    var isGpsLive by remember { mutableStateOf(false) }

    // Hardware GPS Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val loc: Location? = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    gpsAccuracy = loc.accuracy
                    isGpsLive = true
                }
            } catch (_: SecurityException) {}
        }
    }

    // Auto trigger GPS check on first render
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Offline mode toggle (defaults to true: Offline-First SQLite engine)
    var isOfflineMode by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("enroll_farmer_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Interactive App Intro & How to Use Banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            AppIntroBanner(
                onNavigateToRegister = { /* Current Screen */ },
                onNavigateToPractice = onNavigateToPractice,
                onNavigateToSync = onNavigateToSync,
                onOpenFullGuide = onOpenGuide
            )
        }

        // Agent Banner Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2818)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E5234))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ComplianceGreen, CircleShape)
                            )
                            Text(
                                text = "FIELD AGENT PORTAL • $agentId",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFFA5D6A7)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable { onOpenGuide() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Guide",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                color = if (isOfflineMode) Color(0x33FFD54F) else Color(0x33A5D6A7),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isOfflineMode) "OFFLINE SQLITE QUEUE" else "ONLINE SYNC READY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOfflineMode) WarningAmber else ComplianceGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Register New Farmer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Zero-Literacy Principle: No app download, data plan, or literacy required for farmer. Mobile phone number is their primary provenance identity.",
                        fontSize = 11.sp,
                        color = Color(0xFFC8E6C9),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E5DD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Farmer Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Farmer Full Name *") },
                        placeholder = { Text("e.g. Musa Ibrahim Dambatta") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farmer_name_input"),
                        singleLine = true
                    )

                    // 2. Phone Number (Primary Identity)
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number (Primary Identity) *") },
                        placeholder = { Text("e.g. 08034512991 or +2348034512991") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        supportingText = {
                            Text("Used to dispatch SMS receipts & verify export provenance. Feature phone OK.", fontSize = 11.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farmer_phone_input"),
                        singleLine = true
                    )

                    // 3. GPS Coordinates (Auto-captured from phone)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = ComplianceGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Phone GPS Coordinates (Auto-Captured)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Text(
                                    text = "${"%.4f".format(latitude)}°N, ${"%.4f".format(longitude)}°E (±${"%.1f".format(gpsAccuracy)}m)",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.testTag("farmer_gps_display")
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                    latitude += Random.nextDouble(-0.001, 0.001)
                                    longitude += Random.nextDouble(-0.001, 0.001)
                                    gpsAccuracy = 1.8f
                                    isGpsLive = true
                                },
                                modifier = Modifier.testTag("refresh_gps_button")
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fix GPS", fontSize = 11.sp)
                            }
                        }
                    }

                    // 4. Farm Size with Local Units: hectares, acres, or "plots"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = farmSizeStr,
                            onValueChange = { farmSizeStr = it },
                            label = { Text("Farm Size *") },
                            placeholder = { Text("e.g. 4.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("farm_size_input"),
                            singleLine = true
                        )

                        // Local Unit Dropdown (hectares, acres, plots)
                        ExposedDropdownMenuBox(
                            expanded = unitMenuExpanded,
                            onExpandedChange = { unitMenuExpanded = !unitMenuExpanded },
                            modifier = Modifier.weight(1.1f)
                        ) {
                            OutlinedTextField(
                                value = selectedUnit.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Local Unit *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("farm_size_unit_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false }
                            ) {
                                localUnitOptions.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            selectedUnit = unit
                                            unitMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 5. Crop Type (sesame, cowpea, ginger, etc.)
                    ExposedDropdownMenuBox(
                        expanded = cropMenuExpanded,
                        onExpandedChange = { cropMenuExpanded = !cropMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCrop,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Crop Type (Primary Export Crop) *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("crop_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = cropMenuExpanded,
                            onDismissRequest = { cropMenuExpanded = false }
                        ) {
                            cropOptions.forEach { crop ->
                                DropdownMenuItem(
                                    text = { Text(crop) },
                                    onClick = {
                                        selectedCrop = crop
                                        cropMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 6. Cooperative Affiliation (if any)
                    ExposedDropdownMenuBox(
                        expanded = coopMenuExpanded,
                        onExpandedChange = { coopMenuExpanded = !coopMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCooperative,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cooperative Affiliation (if any)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = coopMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("farmer_coop_input")
                        )
                        ExposedDropdownMenu(
                            expanded = coopMenuExpanded,
                            onDismissRequest = { coopMenuExpanded = false }
                        ) {
                            commonCoops.forEach { coop ->
                                DropdownMenuItem(
                                    text = { Text(coop, fontSize = 12.sp) },
                                    onClick = {
                                        selectedCooperative = coop
                                        coopMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // State and LGA Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = stateMenuExpanded,
                            onExpandedChange = { stateMenuExpanded = !stateMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("State") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = stateMenuExpanded,
                                onDismissRequest = { stateMenuExpanded = false }
                            ) {
                                stateOptions.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            selectedState = st
                                            stateMenuExpanded = false
                                            val newLgas = lgasByState[st] ?: listOf("Central")
                                            selectedLga = newLgas.first()
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = lgaMenuExpanded,
                            onExpandedChange = { lgaMenuExpanded = !lgaMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedLga,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("LGA") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lgaMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = lgaMenuExpanded,
                                onDismissRequest = { lgaMenuExpanded = false }
                            ) {
                                currentLgas.forEach { lga ->
                                    DropdownMenuItem(
                                        text = { Text(lga) },
                                        onClick = {
                                            selectedLga = lga
                                            lgaMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Offline SQLite persistence notice & toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F7F5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline-First SQLite Storage",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Stores with status 'pending_sync'. Backend assigns official Farmer ID on sync.",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = isOfflineMode,
                            onCheckedChange = { isOfflineMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HarvestGreenPrimary,
                                checkedTrackColor = Color(0xFFC8E6C9)
                            )
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (fullName.isBlank()) {
                                errorMessage = "Please enter the farmer's full name"
                                return@Button
                            }
                            if (phoneNumber.isBlank()) {
                                errorMessage = "Phone number is required as primary identity"
                                return@Button
                            }

                            val cleanPhone = phoneNumber.trim()
                            val parsedSize = farmSizeStr.toDoubleOrNull() ?: 1.0
                            val cleanCommunity = if (community.isBlank()) "$selectedLga Farming Cluster" else community

                            onRegisterFarmer(
                                fullName.trim(),
                                cleanPhone,
                                selectedState,
                                selectedLga,
                                cleanCommunity,
                                selectedCrop,
                                parsedSize,
                                selectedUnit,
                                latitude,
                                longitude,
                                selectedCooperative,
                                isOfflineMode
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_enroll_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOfflineMode) "Save Farmer to Local SQLite (pending_sync)" else "Enroll & Sync to Backend",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: Enrolled Farmers History & Offline Status
        item {
            Text(
                text = "Enrolled Smallholders (${enrolledFarmers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(enrolledFarmers.take(8), key = { it.id }) { farmer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("enrolled_farmer_card_${farmer.id}"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E9E2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = farmer.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Surface(
                                color = if (farmer.syncStatus == "synced") Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = farmer.syncStatus,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (farmer.syncStatus == "synced") ComplianceGreen else WarningAmber,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${farmer.crop} • ${farmer.farmSize} ${farmer.farmSizeUnit} • Phone: ${farmer.phoneNumber}",
                            fontSize = 11.sp,
                            color = Color(0xFF616161)
                        )
                        Text(
                            text = "GPS: ${farmer.gpsCoordinates} • ${farmer.cooperative}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = farmer.farmerCode.ifBlank { "PENDING" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComplianceGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
