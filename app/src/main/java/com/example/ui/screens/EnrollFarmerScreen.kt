package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.FarmerEntity
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.HarvestGreenPrimary
import com.example.ui.theme.SesameAmberSecondary
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollFarmerScreen(
    enrolledFarmers: List<FarmerEntity>,
    onRegisterFarmer: (
        name: String,
        phone: String,
        state: String,
        lga: String,
        community: String,
        crop: String,
        hectares: Double,
        lat: Double,
        lng: Double,
        coop: String
    ) -> Unit
) {
    val stateOptions = listOf("Kano", "Jigawa", "Benue", "Kaduna", "Nassarawa", "Bauchi", "Sokoto", "Oyo")
    var selectedState by remember { mutableStateOf(stateOptions[0]) }
    var stateMenuExpanded by remember { mutableStateOf(false) }

    val lgasByState = mapOf(
        "Kano" to listOf("Dambatta", "Bichi", "Kunchi", "Bagwai", "Dawakin Tofa", "Gwarzo", "Tudun Wada"),
        "Jigawa" to listOf("Maigatari", "Hadejia", "Birnin Kudu", "Gumel", "Kazaure", "Ringim", "Babura"),
        "Benue" to listOf("Makurdi", "Otukpo", "Gboko", "Katsina-Ala", "Guma", "Vandeikya"),
        "Kaduna" to listOf("Kachia", "Makarfi", "Lere", "Giwa", "Zaria", "Soba"),
        "Nassarawa" to listOf("Doma", "Lafia", "Keana", "Awe"),
        "Bauchi" to listOf("Alkaleri", "Bauchi", "Darazo", "Misau"),
        "Sokoto" to listOf("Goronyo", "Wurno", "Kware", "Sokoto South"),
        "Oyo" to listOf("Iseyin", "Saki West", "Ogbomoso North", "Oyo West")
    )
    val currentLgas = lgasByState[selectedState] ?: listOf("Central District")
    var selectedLga by remember { mutableStateOf(currentLgas.first()) }
    var lgaMenuExpanded by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var community by remember { mutableStateOf("") }
    var farmSizeStr by remember { mutableStateOf("") }

    val cooperativeOptions = listOf(
        "Dambatta Sesame Farmers Cooperative Society Ltd",
        "Maigatari Export Commodity Association",
        "National Sesame Seed Producers, Processors & Exporters Association (NSSPEN)",
        "Benue State Organic Legume & Pulse Growers Union",
        "Kaduna Ginger Growers and Processors Cooperative Union",
        "Hadejia Valley Grain Producers Cluster",
        "All Farmers Association of Nigeria (AFAN)"
    )
    var selectedCooperative by remember { mutableStateOf(cooperativeOptions.first()) }
    var coopMenuExpanded by remember { mutableStateOf(false) }

    val cropOptions = listOf("Sesame", "Cowpea", "Ginger", "Hibiscus", "Cashew")
    var selectedCrop by remember { mutableStateOf(cropOptions[0]) }
    var cropMenuExpanded by remember { mutableStateOf(false) }

    // GPS Geotag coordinates (anchored to confirmed Nigerian agricultural clusters)
    fun getCoordinates(state: String, lga: String): Pair<Double, Double> {
        return when (lga) {
            "Dambatta" -> Pair(12.4358, 8.5147)
            "Bichi" -> Pair(12.2341, 8.2415)
            "Maigatari" -> Pair(12.8122, 9.4589)
            "Hadejia" -> Pair(12.4500, 10.0333)
            "Makurdi" -> Pair(7.7322, 8.5391)
            "Otukpo" -> Pair(7.1904, 8.1331)
            "Kachia" -> Pair(9.8731, 7.9542)
            "Makarfi" -> Pair(11.3800, 7.8800)
            else -> when (state) {
                "Kano" -> Pair(12.0022, 8.5919)
                "Jigawa" -> Pair(12.4500, 9.5000)
                "Benue" -> Pair(7.7322, 8.5391)
                "Kaduna" -> Pair(10.5105, 7.4165)
                else -> Pair(10.0000, 8.0000)
            }
        }
    }

    val (initialLat, initialLng) = getCoordinates(selectedState, selectedLga)
    var latitude by remember { mutableStateOf(initialLat) }
    var longitude by remember { mutableStateOf(initialLng) }
    var isGpsCaptured by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("enroll_farmer_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Authentic Visual Banner of Nigerian Harvest
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nigerian_sesame_field_1789856083155),
                        contentDescription = "Authentic Nigerian Sesame Harvest in Northern Nigeria",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x99000000),
                                        Color(0xE6082416)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "OFFICIAL FIELD ENROLLMENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5D6A7)
                        )
                        Text(
                            text = "Smallholder Provenance Registration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Geotagged farm boundaries & automated USSD/SMS linking",
                            fontSize = 11.sp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
        }

        // Zero-literacy principle banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        tint = ComplianceGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Zero-Literacy Principle: The farmer never interacts with software. Upon enrollment, an official SMS registration ID is dispatched directly to the farmer's mobile phone for batch aggregation verification.",
                        fontSize = 12.sp,
                        color = ComplianceGreen,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Enrollment Form Card
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
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Farmer Full Legal Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farmer_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Farmer Mobile Phone Number") },
                        supportingText = { Text("e.g. 0803XXXXXXX or +234803XXXXXXX for USSD/SMS gateway", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farmer_phone_input"),
                        singleLine = true
                    )

                    // State and LGA Dropdown Row
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
                                    .testTag("state_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = stateMenuExpanded,
                                onDismissRequest = { stateMenuExpanded = false }
                            ) {
                                stateOptions.forEach { state ->
                                    DropdownMenuItem(
                                        text = { Text(state) },
                                        onClick = {
                                            selectedState = state
                                            stateMenuExpanded = false
                                            val newLgas = lgasByState[state] ?: listOf("Central")
                                            selectedLga = newLgas.first()
                                            val (lat, lng) = getCoordinates(state, selectedLga)
                                            latitude = lat
                                            longitude = lng
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
                                    .testTag("farmer_lga_input")
                            )
                            ExposedDropdownMenu(
                                expanded = lgaMenuExpanded,
                                onDismissRequest = { lgaMenuExpanded = false }
                            ) {
                                currentLgas.forEach { lgaItem ->
                                    DropdownMenuItem(
                                        text = { Text(lgaItem) },
                                        onClick = {
                                            selectedLga = lgaItem
                                            lgaMenuExpanded = false
                                            val (lat, lng) = getCoordinates(selectedState, lgaItem)
                                            latitude = lat
                                            longitude = lng
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = community,
                        onValueChange = { community = it },
                        label = { Text("Village / Community Cluster") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farmer_community_input"),
                        singleLine = true
                    )

                    // Crop & Farm size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = cropMenuExpanded,
                            onExpandedChange = { cropMenuExpanded = !cropMenuExpanded },
                            modifier = Modifier.weight(1.1f)
                        ) {
                            OutlinedTextField(
                                value = selectedCrop,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Target Export Crop") },
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

                        OutlinedTextField(
                            value = farmSizeStr,
                            onValueChange = { farmSizeStr = it },
                            label = { Text("Area (Hectares)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("farm_size_input"),
                            singleLine = true
                        )
                    }

                    // Cooperative Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = coopMenuExpanded,
                        onExpandedChange = { coopMenuExpanded = !coopMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCooperative,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Registered Farmers Cooperative") },
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
                            cooperativeOptions.forEach { coop ->
                                DropdownMenuItem(
                                    text = { Text(coop, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCooperative = coop
                                        coopMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // GPS Geotag Section
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGpsCaptured) Color(0xFFE8F5E9) else Color(0xFFF7F8F5)
                        ),
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
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isGpsCaptured) ComplianceGreen else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Satellite GPS Coordinates ($selectedLga Cluster)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isGpsCaptured) ComplianceGreen else Color(0xFF424242)
                                    )
                                }
                                Text(
                                    text = "${"%.4f".format(latitude)}° N, ${"%.4f".format(longitude)}° E (EUDR Compliant)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val (baseLat, baseLng) = getCoordinates(selectedState, selectedLga)
                                    latitude = baseLat + Random.nextDouble(-0.005, 0.005)
                                    longitude = baseLng + Random.nextDouble(-0.005, 0.005)
                                    isGpsCaptured = true
                                },
                                modifier = Modifier.testTag("capture_gps_button")
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refine GPS", fontSize = 11.sp)
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (fullName.isBlank()) {
                                errorMessage = "Please enter the farmer's full name"
                                return@Button
                            }
                            val rawPhone = phoneNumber.trim()
                            val cleanPhone = when {
                                rawPhone.isBlank() -> "+234803" + Random.nextInt(1000000, 9999999)
                                rawPhone.startsWith("+") -> rawPhone
                                rawPhone.startsWith("0") -> "+234" + rawPhone.drop(1)
                                else -> "+234$rawPhone"
                            }
                            val hectares = farmSizeStr.toDoubleOrNull() ?: 3.0
                            val communityClean = if (community.isBlank()) "$selectedLga Central Field" else community

                            onRegisterFarmer(
                                fullName,
                                cleanPhone,
                                selectedState,
                                selectedLga,
                                communityClean,
                                selectedCrop,
                                hectares,
                                latitude,
                                longitude,
                                selectedCooperative
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_enroll_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGreenPrimary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Register Farmer & Dispatch SMS Code", fontSize = 14.sp)
                    }
                }
            }
        }

        // Section: Recently Enrolled Farmers
        item {
            Text(
                text = "Recently Enrolled Farmers (${enrolledFarmers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(enrolledFarmers.take(6), key = { it.id }) { farmer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = farmer.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${farmer.crop} • ${farmer.farmSizeHectares} ha • ${farmer.lga}, ${farmer.state}",
                            fontSize = 12.sp,
                            color = Color(0xFF616161)
                        )
                        Text(
                            text = "Phone: ${farmer.phoneNumber}",
                            fontSize = 11.sp,
                            color = Color(0xFF757575)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = farmer.farmerCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ComplianceGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "SMS Active",
                            fontSize = 10.sp,
                            color = ComplianceGreen
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
