package com.example.ui.screens

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.location.GeoLocationNamingService
import com.example.core.zone.GeopoliticalZone
import com.example.core.zone.ZoneRegistry
import com.example.data.local.entity.FarmerEntity
import com.example.ui.components.*
import com.example.ui.theme.*

/**
 * Screen 4: Human-Centered Smallholder Enrollment
 * 5-Step Progressive Enrollment with Google Satellite Boundary Mapping,
 * Zone-driven crops & LGAs, and ethical photo consent (zero AI stock faces).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollFarmerScreen(
    enrolledFarmers: List<FarmerEntity>,
    agentId: String = "AGENT-NG-042",
    zone: GeopoliticalZone = GeopoliticalZone.NW,
    onNavigateToPractice: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenFullSatelliteMap: ((lat: Double, lng: Double, crop: String, name: String) -> Unit)? = null,
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
    val zoneProfile = remember(zone) { ZoneRegistry.getProfile(zone) }

    // Form Field States
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }

    val stateOptions = remember(zone) { zoneProfile.states }
    var selectedState by remember { mutableStateOf(stateOptions.first()) }
    var stateMenuExpanded by remember { mutableStateOf(false) }

    val lgasByState = remember {
        mapOf(
            "Kano" to listOf("Dambatta", "Bichi", "Kunchi", "Bagwai", "Gwarzo"),
            "Kaduna" to listOf("Zaria", "Giwa", "Ikara", "Makarfi", "Kubau"),
            "Jigawa" to listOf("Maigatari", "Hadejia", "Kazaure", "Ringim"),
            "Sokoto" to listOf("Wamakko", "Goronyo", "Tambuwal"),
            "Benue" to listOf("Makurdi", "Gboko", "Otukpo", "Katsina-Ala"),
            "Oyo" to listOf("Ibadan North", "Iseyin", "Saki West", "Ogbomoso"),
            "Ondo" to listOf("Akure South", "Owo", "Ondo West", "Idanre"),
            "Enugu" to listOf("Nkanu West", "Udi", "Nsukka", "Awgu"),
            "Rivers" to listOf("Port Harcourt", "Eleme", "Obio-Akpor", "Ikwerre")
        )
    }

    var selectedLga by remember(selectedState) {
        mutableStateOf(lgasByState[selectedState]?.firstOrNull() ?: "Central District")
    }
    var lgaMenuExpanded by remember { mutableStateOf(false) }
    var community by remember { mutableStateOf("") }

    // Crop Selection (Driven by Geopolitical Zone)
    val cropOptions = remember(zone) { zoneProfile.primaryCrops }
    var selectedCrop by remember { mutableStateOf(cropOptions.first()) }
    var cropMenuExpanded by remember { mutableStateOf(false) }

    var farmSizeStr by remember { mutableStateOf("2.5") }
    var selectedUnit by remember { mutableStateOf("hectares") }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    val commonCoops = remember(zoneProfile) {
        listOf(
            "${zoneProfile.states.first()} Export Commodity Union",
            "Local Farmers Association",
            "Independent Outgrower"
        )
    }
    var selectedCooperative by remember { mutableStateOf(commonCoops.first()) }
    var coopMenuExpanded by remember { mutableStateOf(false) }

    // GPS & Satellite Boundary States
    var latitude by remember { mutableDoubleStateOf(zoneProfile.aggregationHubs.first().latitude) }
    var longitude by remember { mutableDoubleStateOf(zoneProfile.aggregationHubs.first().longitude) }
    var gpsAccuracy by remember { mutableFloatStateOf(2.5f) }
    var polygonVertices by remember { mutableStateOf<List<FarmVertex>>(emptyList()) }
    var physicalLocationName by remember { mutableStateOf("${selectedLga} Farm Plot, ${selectedState}") }

    // Photo Capture & Ethical Consent States
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var hasPhotoConsent by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            hasPhotoConsent = true
        }
    }

    var isOfflineMode by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var enrolledFarmersExpanded by remember { mutableStateOf(false) }

    // Auto resolve physical location name
    LaunchedEffect(latitude, longitude, selectedState, selectedLga, community) {
        physicalLocationName = GeoLocationNamingService.resolveLocationName(
            context = context,
            latitude = latitude,
            longitude = longitude,
            fallbackState = selectedState,
            fallbackLga = selectedLga,
            fallbackCommunity = community
        )
    }

    // Photo consent dialog
    if (showConsentDialog) {
        FarmerPhotoConsentDialog(
            zone = zone,
            onConsentGiven = {
                showConsentDialog = false
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onConsentDeclined = {
                showConsentDialog = false
                hasPhotoConsent = false
                photoUri = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhiteBackground)
            .padding(horizontal = 20.dp)
            .testTag("enroll_farmer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Top Regional Title
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${zoneProfile.zone.zoneName} Enrollment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = CharcoalBrownText
                )
                Text(
                    text = "Register a smallholder for export traceability and EUDR compliance.",
                    fontSize = 13.sp,
                    color = MutedBrownText
                )
            }
        }

        // Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Step 1: Smallholder Identity & Avatar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Step 1: Smallholder Identity",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = LateriteRedPrimary
                            )
                            Text(
                                text = "Authentic regional profile",
                                fontSize = 11.sp,
                                color = MutedBrownText
                            )
                        }

                        // Regional Illustrated Avatar / Real Photo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FarmerAvatarView(
                                photoPath = photoUri?.toString(),
                                farmerName = fullName.ifBlank { "Farmer" },
                                zone = zone,
                                gender = gender,
                                size = 52.dp
                            )

                            OutlinedButton(
                                onClick = { showConsentDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (photoUri != null) "Change" else "Photo", fontSize = 10.sp)
                            }
                        }
                    }

                    // Gender Selector (for authentic regional headwear)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { gender = "MALE" },
                            color = if (gender == "MALE") LateriteRedPrimary else WarmOffWhiteBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Male Profile",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gender == "MALE") Color.White else CharcoalBrownText,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        Surface(
                            onClick = { gender = "FEMALE" },
                            color = if (gender == "FEMALE") LateriteRedPrimary else WarmOffWhiteBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Female Profile",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gender == "FEMALE") Color.White else CharcoalBrownText,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Ibrahim Danladi") },
                        modifier = Modifier.fillMaxWidth().testTag("farmer_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Mobile Phone Number *") },
                        placeholder = { Text("e.g. 08031234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("farmer_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Step 2: Location
                    Text(
                        text = "Step 2: Location & Region (${zoneProfile.zone.zoneName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LateriteRedPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // State Dropdown
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
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
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
                                            selectedLga = lgasByState[st]?.firstOrNull() ?: "Central"
                                        }
                                    )
                                }
                            }
                        }

                        // LGA Dropdown
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
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = lgaMenuExpanded,
                                onDismissRequest = { lgaMenuExpanded = false }
                            ) {
                                (lgasByState[selectedState] ?: listOf("Central")).forEach { lga ->
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

                    // Community
                    OutlinedTextField(
                        value = community,
                        onValueChange = { community = it },
                        label = { Text("Farming Village / Community") },
                        placeholder = { Text("e.g. Dambatta East Village") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Step 3: Google Satellite Imagery & Farm Boundary Mapping
                    Text(
                        text = "Step 3: Farm Boundary & Geotag",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LateriteRedPrimary
                    )

                    // Advanced GPS & Satellite Map Card
                    AdvancedGpsPolygonCaptureCard(
                        currentLat = latitude,
                        currentLng = longitude,
                        accuracyMeters = gpsAccuracy,
                        locationName = physicalLocationName,
                        polygonVertices = polygonVertices,
                        onLocationUpdated = { lat, lng, acc ->
                            latitude = lat
                            longitude = lng
                            gpsAccuracy = acc
                        },
                        onVertexCaptured = { vertex ->
                            polygonVertices = polygonVertices + vertex
                        },
                        onResetPolygon = {
                            polygonVertices = emptyList()
                        }
                    )

                    // Button to launch Fullscreen Satellite Perimeter Walk
                    if (onOpenFullSatelliteMap != null) {
                        OutlinedButton(
                            onClick = {
                                onOpenFullSatelliteMap(latitude, longitude, selectedCrop, fullName.ifBlank { "Farmer" })
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenSecondary)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = ForestGreenSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Fullscreen Satellite Walk", color = ForestGreenSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Step 4: Crop & Farm Size
                    Text(
                        text = "Step 4: Export Crop & Size",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LateriteRedPrimary
                    )

                    // Crop Dropdown (Filtered to active zone)
                    ExposedDropdownMenuBox(
                        expanded = cropMenuExpanded,
                        onExpandedChange = { cropMenuExpanded = !cropMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCrop,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Primary Export Crop *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("crop_dropdown"),
                            shape = RoundedCornerShape(12.dp)
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

                    // Farm Size & Local Units
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = farmSizeStr,
                            onValueChange = { farmSizeStr = it },
                            label = { Text("Farm Size *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("farm_size_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = unitMenuExpanded,
                            onExpandedChange = { unitMenuExpanded = !unitMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedUnit.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("farm_size_unit_dropdown"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false }
                            ) {
                                listOf("hectares", "acres", "plots").forEach { unit ->
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

                    // Cooperative Affiliation
                    ExposedDropdownMenuBox(
                        expanded = coopMenuExpanded,
                        onExpandedChange = { coopMenuExpanded = !coopMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCooperative,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cooperative Union") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = coopMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("farmer_coop_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = coopMenuExpanded,
                            onDismissRequest = { coopMenuExpanded = false }
                        ) {
                            commonCoops.forEach { coop ->
                                DropdownMenuItem(
                                    text = { Text(coop) },
                                    onClick = {
                                        selectedCooperative = coop
                                        coopMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Offline SQLite Storage Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarmOffWhiteBackground, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offline SQLite Storage", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalBrownText)
                            Text("Stored locally in pending queue. Safely uploaded when online.", fontSize = 10.sp, color = MutedBrownText)
                        }
                        Switch(
                            checked = isOfflineMode,
                            onCheckedChange = { isOfflineMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ForestGreenSecondary)
                        )
                    }

                    // Error message
                    errorMessage?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    // Submit Button: Warm, human copy
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
                            .height(52.dp)
                            .testTag("submit_enroll_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LateriteRedPrimary)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save this farmer's details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Section: Enrolled Smallholders History (Collapsible)
        item {
            DropdownSectionHeader(
                title = "Enrolled Smallholders",
                count = enrolledFarmers.size,
                isExpanded = enrolledFarmersExpanded,
                onToggle = { enrolledFarmersExpanded = !enrolledFarmersExpanded },
                icon = Icons.Default.Person,
                subtitle = if (enrolledFarmersExpanded) "Tap to collapse" else "Tap to view list",
                testTag = "enrolled_farmers_dropdown_header"
            )
        }

        if (enrolledFarmersExpanded) {
            if (enrolledFarmers.isEmpty()) {
                item {
                    Text(
                        text = "Nothing here yet. Registered farmers will appear in this list.",
                        fontSize = 12.sp,
                        color = MutedBrownText
                    )
                }
            } else {
                items(enrolledFarmers) { farmer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftCreamSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FarmerAvatarView(
                                photoPath = farmer.photoPath,
                                farmerName = farmer.fullName,
                                zone = zone,
                                size = 44.dp
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(farmer.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CharcoalBrownText)
                                Text("${farmer.crop} • ${farmer.farmSizeHectares} ha • ${farmer.lga}", fontSize = 11.sp, color = MutedBrownText)
                                Text(
                                    text = if (farmer.isSynced) "ID: ${farmer.farmerCode}" else "Offline (Pending Sync)",
                                    fontSize = 10.sp,
                                    color = if (farmer.isSynced) ForestGreenSecondary else WarmOchreDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
