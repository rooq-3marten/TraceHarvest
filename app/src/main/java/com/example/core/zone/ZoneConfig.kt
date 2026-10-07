package com.example.core.zone

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GeopoliticalZone(
    val code: String,
    val zoneName: String,
    val localAlias: String
) {
    NW("NW", "North West", "Arewa Maso Yamma"),
    NE("NE", "North East", "Arewa Maso Gabas"),
    NC("NC", "North Central", "Middle Belt"),
    SW("SW", "South West", "Ile Yoruba"),
    SE("SE", "South East", "Ala Igbo"),
    SS("SS", "South South", "Niger Delta Coast")
}

data class AggregationHub(
    val name: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val warehouseCapacityTons: Int
)

data class ApprovedAgroProduct(
    val tradeName: String,
    val category: String, // PESTICIDE, FERTILIZER, HERBICIDE
    val activeIngredient: String,
    val nafdacRegNo: String,
    val phiDays: Int,
    val recommendedDosage: String,
    val targetCrops: List<String>
)

data class SeasonalCalendar(
    val plantingMonths: String,
    val harvestMonths: String,
    val currentSeasonAdvice: String
)

data class ZoneProfile(
    val zone: GeopoliticalZone,
    val states: List<String>,
    val primaryCrops: List<String>,
    val primaryLanguage: String,
    val secondaryLanguages: List<String>,
    val culturalGreeting: String,
    val exportDestinations: List<String>,
    val climateSummary: String,
    val keyExportChallenges: String,
    val aggregationHubs: List<AggregationHub>,
    val seasonalCalendar: SeasonalCalendar,
    val approvedProducts: List<ApprovedAgroProduct>
)

object ZoneRegistry {

    private val profiles = mapOf(
        GeopoliticalZone.NW to ZoneProfile(
            zone = GeopoliticalZone.NW,
            states = listOf("Kano", "Kaduna", "Katsina", "Kebbi", "Sokoto", "Zamfara", "Jigawa"),
            primaryCrops = listOf("Sesame", "Cowpea", "Groundnut", "Cotton", "Rice"),
            primaryLanguage = "Hausa",
            secondaryLanguages = listOf("Fulfulde", "English"),
            culturalGreeting = "Sannu da aiki / Barka da zuwa",
            exportDestinations = listOf("Japan", "China", "Türkiye", "European Union"),
            climateSummary = "Semi-arid savanna, short rainy season (June–September)",
            keyExportChallenges = "Aflatoxin risk in groundnut; chemical residues in sesame",
            aggregationHubs = listOf(
                AggregationHub("Kano Dawanau Central Hub", "Kano", 11.9821, 8.5167, 15000),
                AggregationHub("Kaduna Kakuri Export Silo", "Kaduna", 10.5105, 7.4165, 8500),
                AggregationHub("Sokoto Grain Depot", "Sokoto", 13.0609, 5.2390, 6000)
            ),
            seasonalCalendar = SeasonalCalendar(
                plantingMonths = "June – July",
                harvestMonths = "October – November",
                currentSeasonAdvice = "Sesame moisture must remain strictly below 8.0% prior to bagging to avoid aflatoxin."
            ),
            approvedProducts = listOf(
                ApprovedAgroProduct("Karate 5 EC", "PESTICIDE", "Lambda-cyhalothrin (50 g/L)", "04-2015", 14, "500 ml / ha", listOf("Sesame", "Cowpea")),
                ApprovedAgroProduct("Belt 480 SC", "PESTICIDE", "Flubendiamide (480 g/L)", "04-7712", 7, "100 ml / ha", listOf("Cowpea", "Cotton")),
                ApprovedAgroProduct("Apron Star 42 WS", "SEED TREATMENT", "Thiamethoxam + Mefenoxam", "04-1021", 0, "10 g / 4 kg seed", listOf("Sesame", "Groundnut")),
                ApprovedAgroProduct("Golden NPK 15:15:15", "FERTILIZER", "Nitrogen, Phosphorus, Potassium", "04-9821", 0, "200 kg / ha", listOf("Rice", "Cotton"))
            )
        ),

        GeopoliticalZone.NE to ZoneProfile(
            zone = GeopoliticalZone.NE,
            states = listOf("Borno", "Yobe", "Adamawa", "Bauchi", "Gombe", "Taraba"),
            primaryCrops = listOf("Cowpea", "Groundnut", "Sorghum", "Millet", "Sesame"),
            primaryLanguage = "Hausa",
            secondaryLanguages = listOf("Kanuri", "Fulfulde", "Marghi", "English"),
            culturalGreeting = "Barka da yini / Ushirro",
            exportDestinations = listOf("West Africa (ECOWAS)", "Asia", "Middle East"),
            climateSummary = "Semi-arid to Sahel savanna, erratic rainfall",
            keyExportChallenges = "Post-harvest moisture preservation; security routing",
            aggregationHubs = listOf(
                AggregationHub("Maiduguri Grain Complex", "Borno", 11.8311, 13.1510, 8000),
                AggregationHub("Gombe International Commodity Depot", "Gombe", 10.2897, 11.1673, 10000),
                AggregationHub("Yola River Port Aggregation Point", "Adamawa", 9.2094, 12.4818, 5000)
            ),
            seasonalCalendar = SeasonalCalendar(
                plantingMonths = "June – July",
                harvestMonths = "October",
                currentSeasonAdvice = "Inspect cowpea lots for bruchid beetle holes before sealing hermetic bags."
            ),
            approvedProducts = listOf(
                ApprovedAgroProduct("Super Guard Dust", "GRAIN PROTECTANT", "Pirimiphos-methyl + Deltamethrin", "04-3329", 90, "50 g / 100 kg bag", listOf("Cowpea", "Sorghum")),
                ApprovedAgroProduct("Best Action", "PESTICIDE", "Cypermethrin + Dimethoate", "04-5512", 21, "1.0 L / ha", listOf("Cowpea", "Sesame")),
                ApprovedAgroProduct("Urea 46% N", "FERTILIZER", "Nitrogen", "04-0012", 0, "100 kg / ha", listOf("Millet", "Sorghum"))
            )
        ),

        GeopoliticalZone.NC to ZoneProfile(
            zone = GeopoliticalZone.NC,
            states = listOf("Benue", "Kogi", "Kwara", "Nasarawa", "Niger", "Plateau", "FCT"),
            primaryCrops = listOf("Yam", "Cassava", "Soybean", "Sesame", "Ginger", "Rice"),
            primaryLanguage = "English",
            secondaryLanguages = listOf("Hausa", "Tiv", "Idoma", "Nupe", "Igala", "Berom"),
            culturalGreeting = "Msugh / Good day / Barka",
            exportDestinations = listOf("Europe", "Asia", "Regional West Africa"),
            climateSummary = "Guinea savanna, fertile river basins, moderate rainfall",
            keyExportChallenges = "Ginger fungal blight control; EU MRL chemical thresholds",
            aggregationHubs = listOf(
                AggregationHub("Makurdi Benue Valley Grain Silo", "Benue", 7.7322, 8.5391, 12000),
                AggregationHub("Jos Ginger & Potato Cold Hub", "Plateau", 9.8965, 8.8583, 6000),
                AggregationHub("Lokoja Confluence Terminal", "Kogi", 7.7969, 6.7405, 7500),
                AggregationHub("Abuja Idu Export Staging Center", "FCT", 9.0493, 7.3328, 14000)
            ),
            seasonalCalendar = SeasonalCalendar(
                plantingMonths = "April – June",
                harvestMonths = "September – November",
                currentSeasonAdvice = "Ginger drying beds must be raised off bare soil to maintain European organic compliance."
            ),
            approvedProducts = listOf(
                ApprovedAgroProduct("Ridomil Gold MZ 68 WG", "FUNGICIDE", "Metalaxyl-M + Mancozeb", "04-1188", 21, "2.5 kg / ha", listOf("Ginger", "Yam")),
                ApprovedAgroProduct("Caterpillar Force", "PESTICIDE", "Emamectin benzoate", "04-8841", 7, "250 g / ha", listOf("Soybean", "Sesame")),
                ApprovedAgroProduct("YaraVila Complex", "FERTILIZER", "NPK + Trace micronutrients", "04-6291", 0, "150 kg / ha", listOf("Ginger", "Rice"))
            )
        ),

        GeopoliticalZone.SW to ZoneProfile(
            zone = GeopoliticalZone.SW,
            states = listOf("Lagos", "Ogun", "Oyo", "Osun", "Ondo", "Ekiti"),
            primaryCrops = listOf("Cocoa", "Cassava", "Oil Palm", "Maize", "Plantain", "Rubber"),
            primaryLanguage = "Yoruba",
            secondaryLanguages = listOf("English", "Pidgin"),
            culturalGreeting = "E kaaro / E ku ise o",
            exportDestinations = listOf("Europe (Netherlands, Germany)", "United Kingdom", "USA"),
            climateSummary = "Humid tropical rainforest, bi-modal rainfall seasons",
            keyExportChallenges = "Strict EU cocoa MRLs; bans on chlorpyrifos and endosulfan",
            aggregationHubs = listOf(
                AggregationHub("Akure Cocoa Export Terminal", "Ondo", 7.2571, 5.2058, 20000),
                AggregationHub("Ibadan Bodija Commodity Depot", "Oyo", 7.3775, 3.9470, 15000),
                AggregationHub("Abeokuta Gateway Agro Hub", "Ogun", 7.1475, 3.3619, 8000),
                AggregationHub("Lagos Apapa Port Staging Buffer", "Lagos", 6.4474, 3.3653, 35000)
            ),
            seasonalCalendar = SeasonalCalendar(
                plantingMonths = "March – April and September",
                harvestMonths = "July – August and October – December",
                currentSeasonAdvice = "Ferment cocoa beans for 5–7 days using plantain leaves before drying to develop export chocolate precursors."
            ),
            approvedProducts = listOf(
                ApprovedAgroProduct("Nordox 75 WG", "FUNGICIDE", "Cuprous oxide (750 g/kg)", "04-0342", 14, "100 g / 20 L water", listOf("Cocoa")),
                ApprovedAgroProduct("Actara 25 WG", "INSECTICIDE", "Thiamethoxam", "04-2291", 28, "8 g / knapsack", listOf("Cocoa", "Oil Palm")),
                ApprovedAgroProduct("Touchdown Forte", "HERBICIDE", "Glyphosate potassium salt", "04-1941", 30, "2.0 L / ha", listOf("Rubber", "Oil Palm"))
            )
        ),

        GeopoliticalZone.SE to ZoneProfile(
            zone = GeopoliticalZone.SE,
            states = listOf("Abia", "Anambra", "Ebonyi", "Enugu", "Imo"),
            primaryCrops = listOf("Cashew", "Cassava", "Yam", "Rice", "Oil Palm", "Cocoa"),
            primaryLanguage = "Igbo",
            secondaryLanguages = listOf("English", "Pidgin"),
            culturalGreeting = "Ndewo / Ututu oma",
            exportDestinations = listOf("India", "Vietnam", "Europe", "Middle East"),
            climateSummary = "Tropical rainforest, heavy rainfall, high humidity",
            keyExportChallenges = "Raw cashew nut moisture and out-turn rate (KOR); soil erosion control",
            aggregationHubs = listOf(
                AggregationHub("Enugu 9th Mile Agro Logistics Hub", "Enugu", 6.4483, 7.4116, 9000),
                AggregationHub("Onitsha Niger Bridgehead Commodity Center", "Anambra", 6.1518, 6.7864, 12000),
                AggregationHub("Abakaliki Rice & Grain Exchange", "Ebonyi", 6.3249, 8.1137, 14000),
                AggregationHub("Owerri Central Produce Depot", "Imo", 5.4852, 7.0357, 5000)
            ),
            seasonalCalendar = SeasonalCalendar(
                plantingMonths = "March – May",
                harvestMonths = "September – November (Cashew: Feb–May)",
                currentSeasonAdvice = "Dry cashew nuts on clean tarpaulins until moisture reaches 8–10% to preserve Kernel Out-Turn Ratio (KOR)."
            ),
            approvedProducts = listOf(
                ApprovedAgroProduct("Funguran-OH 50 WP", "FUNGICIDE", "Copper hydroxide (50%)", "04-0812", 14, "150 g / 20 L", listOf("Cashew", "Cocoa")),
                ApprovedAgroProduct("Ampligo 150 ZC", "PESTICIDE", "Chlorantraniliprole + Lambda-cyhalothrin", "04-7121", 14, "200 ml / ha", listOf("Rice", "Cashew")),
                ApprovedAgroProduct("Agrolyser Micronutrient", "BIO-FERTILIZER", "Chelated Zinc, Boron, Copper", "04-9901", 0, "1.0 L / ha", listOf("Cashew", "Oil Palm"))
            )
        ),

        GeopoliticalZone.SS to ZoneProfile(
            zone = GeopoliticalZone.SS,
            states = listOf("Akwa Ibom", "Bayelsa", "Cross River", "Delta", "Edo", "Rivers"),
            primaryCrops = listOf("Oil Palm", "Rubber", "Cocoa", "Cassava", "Plantain", "Timber"),
            primaryLanguage = "English",
            secondaryLanguages = listOf("Pidgin", "Efik", "Ibibio", "Ijaw", "Urhobo", "Edo"),
            culturalGreeting = "Mesiere / Weldom / Good day",
            exportDestinations = listOf("Regional African Markets", "Europe", "Asia"),
            climateSummary = "Tropical coastal rainforest, intense year-round precipitation",
            keyExportChallenges = "Crude palm oil Free Fatty Acid (FFA < 5%) control; deforestation compliance",
            aggregationHubs = listOf(
                AggregationHub("Port Harcourt Onne Port Staging Terminal", "Rivers", 4.7214, 7.1524, 25000),
                AggregationHub("Calabar Free Trade Agro Depot", "Cross River", 4.9757, 8.3417, 11000),
                AggregationHub("Benin City Oredo Rubber & Palm Hub", "Edo", 6.3350, 5.6037, 16000),
                AggregationHub("Warri Delta River Basin Depot", "Delta", 5.5174, 5.7501, 7000)
            ),
            seasonalCalendar = SeasonalCalendar(
                plantingMonths = "March – May",
                harvestMonths = "March – December (Continuous Oil Palm)",
                currentSeasonAdvice = "Sterilize and mill fresh fruit bunches within 24 hours of harvest to keep Free Fatty Acid (FFA) under 3.5%."
            ),
            approvedProducts = listOf(
                ApprovedAgroProduct("Kocide 2000", "FUNGICIDE", "Copper hydroxide (53.8%)", "04-1209", 14, "100 g / 20 L", listOf("Cocoa", "Rubber")),
                ApprovedAgroProduct("Gramoxone Extra", "HERBICIDE", "Paraquat alternative glufosinate", "04-4412", 21, "2.5 L / ha", listOf("Oil Palm", "Rubber")),
                ApprovedAgroProduct("Phostrogen Palm Feed", "FERTILIZER", "High Potassium (K) Formulation", "04-7622", 0, "300 kg / ha", listOf("Oil Palm"))
            )
        )
    )

    fun getProfile(zone: GeopoliticalZone): ZoneProfile {
        return profiles[zone] ?: profiles[GeopoliticalZone.NW]!!
    }

    fun getAllZones(): List<ZoneProfile> = profiles.values.toList()
}

/**
 * Persists and provides reactive access to the agent's active operational geopolitical zone.
 */
class ZonePreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences("traceharvest_zone_prefs", Context.MODE_PRIVATE)
    private val KEY_SELECTED_ZONE = "key_selected_geopolitical_zone"
    private val KEY_ZONE_INITIALIZED = "key_zone_initialized"

    private val _currentZone = MutableStateFlow(loadInitialZone())
    val currentZone: StateFlow<GeopoliticalZone> = _currentZone.asStateFlow()

    private fun loadInitialZone(): GeopoliticalZone {
        val code = prefs.getString(KEY_SELECTED_ZONE, GeopoliticalZone.NW.code) ?: GeopoliticalZone.NW.code
        return GeopoliticalZone.values().firstOrNull { it.code == code } ?: GeopoliticalZone.NW
    }

    fun isZoneConfigured(): Boolean {
        return prefs.getBoolean(KEY_ZONE_INITIALIZED, false)
    }

    fun setZone(zone: GeopoliticalZone) {
        prefs.edit()
            .putString(KEY_SELECTED_ZONE, zone.code)
            .putBoolean(KEY_ZONE_INITIALIZED, true)
            .apply()
        _currentZone.value = zone
    }

    fun getProfile(): ZoneProfile {
        return ZoneRegistry.getProfile(_currentZone.value)
    }
}
