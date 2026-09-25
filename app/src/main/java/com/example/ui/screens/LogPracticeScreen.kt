package com.example.ui.screens

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.catalog.AgrochemicalCategory
import com.example.data.catalog.NafdacAgrochemical
import com.example.data.catalog.NafdacCatalog
import com.example.data.catalog.NafdacExportCompliance
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.PracticeLogEntity
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.ViolationRed
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class PracticeMenuItem(
    val typeName: String,
    val emoji: String,
    val icon: ImageVector,
    val description: String,
    val isChemicalRequired: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogPracticeScreen(
    farmers: List<FarmerEntity>,
    recentLogs: List<PracticeLogEntity>,
    agentId: String = "AGENT-NG-042",
    pendingSyncCount: Int = 0,
    onSyncAllPending: () -> Unit = {},
    onLogPractice: (
        farmerCode: String,
        farmerName: String,
        crop: String,
        practiceType: String,
        productName: String,
        activeIngredient: String,
        quantityUsed: Double,
        quantityUnit: String,
        calendarDateApplied: Long,
        verificationPhotoUri: String,
        liveLatitude: Double,
        liveLongitude: Double,
        gpsAccuracyMeters: Float,
        phiDays: Int,
        farmerLocalId: Long,
        farmerDisplayId: String,
        nafdacRegNo: String,
        gpsCoordinates: String,
        syncStatus: String
    ) -> Unit
) {
    val context = LocalContext.current

    if (farmers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "No Farmers Enrolled Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Please register a farmer first before logging seasonal practices.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        return
    }

    var selectedFarmerIndex by remember { mutableStateOf(0) }
    var farmerDropdownExpanded by remember { mutableStateOf(false) }

    val currentFarmer = farmers.getOrElse(selectedFarmerIndex) { farmers.first() }

    // Icon-based Practice Menu items as specified:
    // 🌱 Planting, 💧 Irrigation, 🧪 Pesticide application, 🌿 Fertilizer application, ✂️ Harvest
    val practiceMenuItems = listOf(
        PracticeMenuItem(
            typeName = "🌱 Planting",
            emoji = "🌱",
            icon = Icons.Default.Eco,
            description = "Certified seed sowing & emergence",
            isChemicalRequired = false
        ),
        PracticeMenuItem(
            typeName = "💧 Irrigation",
            emoji = "💧",
            icon = Icons.Default.WaterDrop,
            description = "Water management & furrow irrigation",
            isChemicalRequired = false
        ),
        PracticeMenuItem(
            typeName = "🧪 Pesticide application",
            emoji = "🧪",
            icon = Icons.Default.Science,
            description = "Insecticide / herbicide & MRL control",
            isChemicalRequired = true
        ),
        PracticeMenuItem(
            typeName = "🌿 Fertilizer application",
            emoji = "🌿",
            icon = Icons.Default.Agriculture,
            description = "NPK / Urea soil nutrient enrichment",
            isChemicalRequired = true
        ),
        PracticeMenuItem(
            typeName = "✂️ Harvest",
            emoji = "✂️",
            icon = Icons.Default.ContentCut,
            description = "Crop maturity reaping & drying",
            isChemicalRequired = false
        )
    )

    var selectedPractice by remember { mutableStateOf(practiceMenuItems[2]) } // default to pesticide

    // Approved products dropdown list (from NAFDAC catalog)
    val approvedChemicals = remember(selectedPractice) {
        if (selectedPractice.typeName.contains("Fertilizer")) {
            NafdacCatalog.catalog.filter { it.category == AgrochemicalCategory.FERTILIZER }
        } else {
            NafdacCatalog.catalog.filter { it.category != AgrochemicalCategory.FERTILIZER }
        }
    }

    var selectedProduct by remember { mutableStateOf(approvedChemicals.first()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    // Quantity used
    var quantityValue by remember { mutableDoubleStateOf(1.0) }
    var quantityUnit by remember { mutableStateOf("Litres") }
    val quantityUnits = listOf("Litres", "Bags", "Sachets", "kg", "ml")
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // Exact calendar date (defaults to today)
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    val dateFormatted = remember(selectedDateMillis) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    // Optional photo of product label via Android Photo Picker
    var labelPhotoUri by remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            labelPhotoUri = uri.toString()
        }
    }

    // Live auto-captured phone GPS
    var liveLat by remember { mutableDoubleStateOf(currentFarmer.latitude) }
    var liveLng by remember { mutableDoubleStateOf(currentFarmer.longitude) }
    var gpsAcc by remember { mutableFloatStateOf(2.2f) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val loc: Location? = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    liveLat = loc.latitude
                    liveLng = loc.longitude
                    gpsAcc = loc.accuracy
                }
            } catch (_: SecurityException) {}
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Evaluate NAFDAC validation and PHI if chemical application
    val isChemical = selectedPractice.isChemicalRequired
    val phiDays = if (isChemical) selectedProduct.preHarvestIntervalDays else 0

    val earliestSafeHarvestStr = remember(selectedDateMillis, phiDays) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        cal.add(Calendar.DAY_OF_YEAR, phiDays)
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("log_practice_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stage 2 Header Banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2417)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E5234))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            Icon(
                                Icons.Default.Agriculture,
                                contentDescription = null,
                                tint = Color(0xFFA5D6A7),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "STAGE 2: PRACTICE LOGGING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFFA5D6A7)
                            )
                        }

                        Surface(
                            color = Color(0x33A5D6A7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "AGENT ID: $agentId",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC8E6C9),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Agent-Led Practice Logging",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Triggered when farmer applies input, irrigates, or harvests. Persists locally in SQLite with status 'pending_sync'.",
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // STEP 1: Select Farmer by Name or ID
                    Text(
                        text = "1. Select Farmer (by Name or ID) *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E4A32)
                    )

                    ExposedDropdownMenuBox(
                        expanded = farmerDropdownExpanded,
                        onExpandedChange = { farmerDropdownExpanded = !farmerDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${currentFarmer.fullName} (${currentFarmer.farmerDisplayId}) - ${currentFarmer.crop}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Farmer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = farmerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("select_farmer_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = farmerDropdownExpanded,
                            onDismissRequest = { farmerDropdownExpanded = false }
                        ) {
                            farmers.forEachIndexed { index, farmer ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${farmer.fullName} • ${farmer.crop}",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "ID: ${farmer.farmerDisplayId} • Phone: ${farmer.phoneNumber} • ${farmer.lga}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedFarmerIndex = index
                                        farmerDropdownExpanded = false
                                        liveLat = farmer.latitude
                                        liveLng = farmer.longitude
                                    }
                                )
                            }
                        }
                    }

                    // Verified human farmer identity card with photo
                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC5E1A5)),
                        modifier = Modifier.fillMaxWidth().testTag("selected_farmer_profile_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_farmer_portrait),
                                contentDescription = "Farmer Photo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, HarvestGreenPrimary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentFarmer.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Surface(
                                        color = ComplianceGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ENROLLED",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ComplianceGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Phone: ${currentFarmer.phoneNumber} • ${currentFarmer.crop} (${currentFarmer.farmSizeHectares} ha)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF558B2F)
                                )
                            }
                        }
                    }

                    // STEP 2: Icon-based practice type menu
                    // 🌱 Planting, 💧 Irrigation, 🧪 Pesticide application, 🌿 Fertilizer application, ✂️ Harvest
                    Text(
                        text = "2. Select Practice Type (Icon Menu) *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E4A32)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(practiceMenuItems) { item ->
                            val isSelected = selectedPractice == item
                            Surface(
                                color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF7F8F6),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ComplianceGreen else Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        selectedPractice = item
                                        if (item.typeName.contains("Fertilizer")) {
                                            selectedProduct = NafdacCatalog.catalog.first { it.category == AgrochemicalCategory.FERTILIZER }
                                            quantityUnit = "Bags"
                                            quantityValue = 3.0
                                        } else if (item.typeName.contains("Pesticide")) {
                                            selectedProduct = NafdacCatalog.catalog.first { it.category == AgrochemicalCategory.PESTICIDE }
                                            quantityUnit = "Litres"
                                            quantityValue = 1.0
                                        } else if (item.typeName.contains("Planting")) {
                                            quantityUnit = "kg"
                                            quantityValue = 10.0
                                        } else {
                                            quantityUnit = "Bags"
                                            quantityValue = 25.0
                                        }
                                    }
                                    .testTag("practice_type_${item.typeName.take(6)}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) ComplianceGreen else Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = item.typeName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ComplianceGreen else Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // STEP 3: Details Capture (Date, Product if chemical, Quantity used, Optional label photo)
                    Text(
                        text = "3. Practice Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E4A32)
                    )

                    // A. Calendar Date (defaults to today)
                    Surface(
                        color = Color(0xFFF5F7F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("calendar_date_picker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = HarvestGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text("Application Date (defaults to today)", fontSize = 10.sp, color = Color.Gray)
                                    Text(dateFormatted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Change Date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HarvestGreenPrimary
                            )
                        }
                    }

                    // B. Product Name (if pesticide/fertilizer) — selected from dropdown of approved products
                    if (isChemical) {
                        ExposedDropdownMenuBox(
                            expanded = productDropdownExpanded,
                            onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "${selectedProduct.tradeName} (NAFDAC: ${selectedProduct.nafdacRegNo})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Approved Product (NAFDAC Registered) *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("approved_product_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = productDropdownExpanded,
                                onDismissRequest = { productDropdownExpanded = false }
                            ) {
                                approvedChemicals.forEach { product ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(product.tradeName, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "NAFDAC: ${product.nafdacRegNo} • PHI: ${product.preHarvestIntervalDays}d • ${product.activeIngredient}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedProduct = product
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // PHI Info Banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = ComplianceGreen, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(
                                        text = "Mandatory Pre-Harvest Interval (PHI): ${selectedProduct.preHarvestIntervalDays} Days",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = ComplianceGreen
                                    )
                                    Text(
                                        text = "Earliest Safe Export Harvest Date: $earliestSafeHarvestStr",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    // C. Quantity Used (numeric input + unit dropdown)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (quantityValue == quantityValue.toLong().toDouble()) quantityValue.toLong().toString() else quantityValue.toString(),
                            onValueChange = {
                                val v = it.toDoubleOrNull()
                                if (v != null && v >= 0) {
                                    quantityValue = v
                                }
                            },
                            label = { Text("Quantity Used *") },
                            placeholder = { Text("e.g. 1.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quantity_used_input"),
                            singleLine = true
                        )

                        ExposedDropdownMenuBox(
                            expanded = unitDropdownExpanded,
                            onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                            modifier = Modifier.weight(1.1f)
                        ) {
                            OutlinedTextField(
                                value = quantityUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("quantity_unit_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false }
                            ) {
                                quantityUnits.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            quantityUnit = u
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // D. Optional: Photo of Product Label (via Android Photo Picker)
                    Text(
                        text = "Optional: Photo of Product Label / Container",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        color = Color(0xFFF7F8F6),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (labelPhotoUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AsyncImage(
                                        model = labelPhotoUri,
                                        contentDescription = "Product Label Photo",
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column {
                                        Text("Label Photo Attached", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Physical container verification", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                IconButton(
                                    onClick = { labelPhotoUri = null },
                                    modifier = Modifier.testTag("remove_photo_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = ViolationRed)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(12.dp)
                                    .testTag("pick_label_photo_button"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = HarvestGreenPrimary)
                                    Text(
                                        text = "Attach Container / Label Photo Proof",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Optional",
                                        fontSize = 10.sp,
                                        color = ComplianceGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Auto GPS Fix & Agent ID indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F8E9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto GPS: ${"%.4f".format(liveLat)}°N, ${"%.4f".format(liveLng)}°E",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Agent: $agentId",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    // Submit Button: "Agent submits. Offline-first: stored locally, synced when online."
                    Button(
                        onClick = {
                            val prodName = if (isChemical) selectedProduct.tradeName else ""
                            val activeIng = if (isChemical) selectedProduct.activeIngredient else ""
                            val nafdacNo = if (isChemical) selectedProduct.nafdacRegNo else ""
                            val phi = if (isChemical) selectedProduct.preHarvestIntervalDays else 0

                            onLogPractice(
                                currentFarmer.farmerCode,
                                currentFarmer.fullName,
                                currentFarmer.crop,
                                selectedPractice.typeName,
                                prodName,
                                activeIng,
                                quantityValue,
                                quantityUnit,
                                selectedDateMillis,
                                labelPhotoUri ?: "",
                                liveLat,
                                liveLng,
                                gpsAcc,
                                phi,
                                currentFarmer.farmerLocalId,
                                currentFarmer.farmerDisplayId,
                                nafdacNo,
                                "${"%.4f".format(liveLat)}°N, ${"%.4f".format(liveLng)}°E",
                                "pending_sync"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_log_practice_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Practice (Stored Offline: pending_sync)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: Recent Practices Audit Trail
        item {
            Text(
                text = "Recent Seasonal Practice Trail (${recentLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(recentLogs.take(8), key = { it.id }) { log ->
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(log.dateApplied))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("practice_log_item_${log.id}"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E9E2))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = log.practiceType.ifBlank { log.category },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Surface(
                                    color = if (log.syncStatus == "synced") Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.syncStatus,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.syncStatus == "synced") ComplianceGreen else WarningAmber,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${log.farmerName} (${log.farmerDisplayId}) • $dateStr",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        if (log.verificationPhotoUri.isNotBlank()) {
                            AsyncImage(
                                model = log.verificationPhotoUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (log.productName.isNotBlank()) {
                        Text(
                            text = "${log.productName} • ${log.dosage}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agent: ${log.agentId}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                        if (log.gpsCoordinates.isNotBlank()) {
                            Text(
                                text = "GPS: ${log.gpsCoordinates}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = HarvestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
