package com.example.data.catalog

import java.util.Locale

enum class AgrochemicalCategory(val displayName: String) {
    PESTICIDE("Pesticide / Insecticide"),
    FERTILIZER("Fertilizer / Soil Nutrition"),
    BIO_BOTANICAL("Bio-Botanical Biopesticide"),
    SEED_TREATMENT("Seed Dressing & Fungicide"),
    STORAGE_AFLATOXIN("Hermetic Storage & Aflatoxin"),
    BANNED_SUBSTANCE("Prohibited / Banned Substance")
}

enum class NafdacExportCompliance(val label: String, val isApproved: Boolean) {
    APPROVED_EXPORT_COMPLIANT("NAFDAC Approved • EU/Codex Compliant", true),
    APPROVED_ORGANIC_PREMIUM("NAFDAC Certified Organic • Zero MRL Residue", true),
    RESTRICTED_PHI_MONITORED("NAFDAC Approved • Strict PHI Monitoring", true),
    BANNED_MRL_VIOLATION("BANNED BY NAFDAC • Fatal MRL Violation", false),
    UNREGISTERED_UNKNOWN("Unregistered • Lab Clearance Required", false)
}

data class NafdacAgrochemical(
    val nafdacRegNo: String,
    val tradeName: String,
    val category: AgrochemicalCategory,
    val activeIngredient: String,
    val manufacturer: String,
    val approvedDosage: String,
    val preHarvestIntervalDays: Int,
    val complianceStatus: NafdacExportCompliance,
    val targetCrops: List<String>,
    val phiGuidelines: String,
    val safetyWarning: String = ""
)

object NafdacCatalog {

    val catalog: List<NafdacAgrochemical> = listOf(
        // 1. Karate 5 EC (Requested specifically)
        NafdacAgrochemical(
            nafdacRegNo = "04-2015",
            tradeName = "Karate 5 EC",
            category = AgrochemicalCategory.PESTICIDE,
            activeIngredient = "Lambda-cyhalothrin (50 g/L EC)",
            manufacturer = "Syngenta Nigeria / Jubaili Agrotec Ltd",
            approvedDosage = "400 - 500 ml / hectare",
            preHarvestIntervalDays = 14,
            complianceStatus = NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT,
            targetCrops = listOf("Sesame", "Cowpea", "Sorghum", "Cotton"),
            phiGuidelines = "Mandatory Pre-Harvest Interval (PHI) of 14 days must elapse before harvest. Residues degrade below EU MRL threshold (0.05 mg/kg)."
        ),

        // 2. Indorama NPK 15:15:15 (Requested specifically)
        NafdacAgrochemical(
            nafdacRegNo = "04-7892",
            tradeName = "Indorama NPK 15:15:15",
            category = AgrochemicalCategory.FERTILIZER,
            activeIngredient = "Nitrogen 15% - Phosphorus 15% - Potassium 15% (Granular Compound)",
            manufacturer = "Indorama Eleme Fertilizer & Chemicals Ltd (Port Harcourt)",
            approvedDosage = "150 - 200 kg / hectare (3-4 bags)",
            preHarvestIntervalDays = 0,
            complianceStatus = NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT,
            targetCrops = listOf("Sesame", "Cowpea", "Ginger", "Maize", "Rice"),
            phiGuidelines = "Pre-Harvest Interval is 0 days. Inorganic soil nutrient, zero synthetic chemical residue risk for international export."
        ),

        // 3. Indorama Granular Urea 46% N
        NafdacAgrochemical(
            nafdacRegNo = "04-7893",
            tradeName = "Indorama Granular Urea 46% N",
            category = AgrochemicalCategory.FERTILIZER,
            activeIngredient = "Urea Nitrogen (46% N)",
            manufacturer = "Indorama Eleme Fertilizer & Chemicals Ltd",
            approvedDosage = "100 kg / hectare",
            preHarvestIntervalDays = 7,
            complianceStatus = NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT,
            targetCrops = listOf("Sesame", "Ginger", "Grains"),
            phiGuidelines = "Pre-Harvest Interval: 7 days. Nitrogen booster for vegetative vigor. Apply before flowering stage."
        ),

        // 4. BioNeem Extract 0.3%
        NafdacAgrochemical(
            nafdacRegNo = "A5-0412",
            tradeName = "BioNeem Extract 0.3% EC",
            category = AgrochemicalCategory.BIO_BOTANICAL,
            activeIngredient = "Azadirachtin (Bio-botanical cold-pressed neem)",
            manufacturer = "Biopesticides Nigeria Ltd / Tiger Brands",
            approvedDosage = "1.5 - 2.0 Litres / hectare",
            preHarvestIntervalDays = 3,
            complianceStatus = NafdacExportCompliance.APPROVED_ORGANIC_PREMIUM,
            targetCrops = listOf("Sesame", "Cowpea", "Hibiscus", "Ginger"),
            phiGuidelines = "Pre-Harvest Interval: 3 days. Exempt from EU MRL tolerance limits. Qualifies consignment for premium organic export pricing."
        ),

        // 5. Apron Star 42 WS
        NafdacAgrochemical(
            nafdacRegNo = "04-3321",
            tradeName = "Apron Star 42 WS",
            category = AgrochemicalCategory.SEED_TREATMENT,
            activeIngredient = "Thiamethoxam (20%) + Mefenoxam (20%) + Difenoconazole (2%)",
            manufacturer = "Syngenta Nigeria Ltd",
            approvedDosage = "10g sachet per 4kg certified seed",
            preHarvestIntervalDays = 45,
            complianceStatus = NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT,
            targetCrops = listOf("Cowpea", "Sesame", "Sorghum"),
            phiGuidelines = "Seed treatment dressing applied prior to planting. 45-day PHI naturally elapses well before harvest."
        ),

        // 6. Notore NPK 20:10:10
        NafdacAgrochemical(
            nafdacRegNo = "04-6511",
            tradeName = "Notore NPK 20:10:10",
            category = AgrochemicalCategory.FERTILIZER,
            activeIngredient = "NPK 20:10:10 + Trace Micronutrients",
            manufacturer = "Notore Chemical Industries Plc (Onne)",
            approvedDosage = "100 - 150 kg / hectare",
            preHarvestIntervalDays = 0,
            complianceStatus = NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT,
            targetCrops = listOf("Cowpea", "Sesame", "Ginger"),
            phiGuidelines = "Pre-Harvest Interval: 0 days. Fast-dissolving soil nutrient formulation approved for smallholder production."
        ),

        // 7. Caterpillar Force 5 SG
        NafdacAgrochemical(
            nafdacRegNo = "04-9104",
            tradeName = "Caterpillar Force 5 SG",
            category = AgrochemicalCategory.PESTICIDE,
            activeIngredient = "Emamectin Benzoate 50 g/kg",
            manufacturer = "Jubaili Agrotec Ltd",
            approvedDosage = "200 - 250 g / hectare",
            preHarvestIntervalDays = 7,
            complianceStatus = NafdacExportCompliance.APPROVED_EXPORT_COMPLIANT,
            targetCrops = listOf("Cowpea", "Legumes", "Sesame"),
            phiGuidelines = "Pre-Harvest Interval: 7 days. Selective for Maruca pod borer larvae with low non-target toxicity."
        ),

        // 8. PICS Hermetic Triple-Layer Bags
        NafdacAgrochemical(
            nafdacRegNo = "SON/CAP-2022-PICS",
            tradeName = "PICS Hermetic Storage Bags",
            category = AgrochemicalCategory.STORAGE_AFLATOXIN,
            activeIngredient = "Triple-layer High-Density Polyethylene Barrier (100% Chemical-Free)",
            manufacturer = "Lela Agro Industries / Purdue University Licensed",
            approvedDosage = "50 - 200 bags (100 kg capacity)",
            preHarvestIntervalDays = 0,
            complianceStatus = NafdacExportCompliance.APPROVED_ORGANIC_PREMIUM,
            targetCrops = listOf("Cowpea", "Sesame", "Soybeans"),
            phiGuidelines = "Zero PHI. Completely chemical-free physical barrier suffocating cowpea bruchids and preventing Aspergillus aflatoxin."
        ),

        // 9. Sniper 1000EC (DDVP) - BANNED BY NAFDAC
        NafdacAgrochemical(
            nafdacRegNo = "BANNED-NAFDAC-2019",
            tradeName = "Sniper 1000EC (DDVP)",
            category = AgrochemicalCategory.BANNED_SUBSTANCE,
            activeIngredient = "Dichlorvos 1000 g/L (DDVP)",
            manufacturer = "Illicit / Unapproved for Food Agriculture",
            approvedDosage = "PROHIBITED (ZERO TOLERANCE)",
            preHarvestIntervalDays = 90,
            complianceStatus = NafdacExportCompliance.BANNED_MRL_VIOLATION,
            targetCrops = emptyList(),
            phiGuidelines = "DO NOT USE: NAFDAC strictly banned DDVP formulations for outdoor agricultural crops. EU MRL limit of quantification (0.01 mg/kg) guarantees border rejection.",
            safetyWarning = "CRITICAL VIOLATION: Export containers containing DDVP residues are seized and incinerated at European/UK ports, incurring multimillion Naira penalties."
        ),

        // 10. Chlorpyrifos-Ethyl 480 EC - BANNED BY EU & NAFDAC
        NafdacAgrochemical(
            nafdacRegNo = "REVOKED-NAFDAC-2022",
            tradeName = "Chlorpyrifos 480 EC",
            category = AgrochemicalCategory.BANNED_SUBSTANCE,
            activeIngredient = "Chlorpyrifos 480 g/L",
            manufacturer = "Banned Formulation",
            approvedDosage = "PROHIBITED (ZERO TOLERANCE)",
            preHarvestIntervalDays = 60,
            complianceStatus = NafdacExportCompliance.BANNED_MRL_VIOLATION,
            targetCrops = emptyList(),
            phiGuidelines = "DO NOT USE: EU Regulation (EU) 2020/1085 revoked all authorisations for chlorpyrifos. MRL set to lowest detection limit.",
            safetyWarning = "CRITICAL VIOLATION: Neurotoxic organophosphate prohibited on Nigerian export crops."
        )
    )

    fun findByNameOrActive(query: String): NafdacAgrochemical? {
        val clean = query.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return null
        return catalog.firstOrNull {
            it.tradeName.lowercase(Locale.ROOT).contains(clean) ||
            it.activeIngredient.lowercase(Locale.ROOT).contains(clean) ||
            clean.contains(it.tradeName.lowercase(Locale.ROOT))
        }
    }

    fun isBannedSubstance(name: String, active: String): Boolean {
        val combined = "$name $active".lowercase(Locale.ROOT)
        return combined.contains("dichlorvos") ||
               combined.contains("ddvp") ||
               combined.contains("sniper") ||
               combined.contains("chlorpyrifos") ||
               combined.contains("monocrotophos") ||
               combined.contains("endosulfan")
    }

    fun validateApplication(productName: String, activeIngredient: String): Pair<NafdacAgrochemical?, NafdacExportCompliance> {
        if (isBannedSubstance(productName, activeIngredient)) {
            val matchedBanned = catalog.firstOrNull {
                it.category == AgrochemicalCategory.BANNED_SUBSTANCE &&
                (productName.contains(it.tradeName, ignoreCase = true) || activeIngredient.contains(it.activeIngredient, ignoreCase = true))
            } ?: catalog.first { it.category == AgrochemicalCategory.BANNED_SUBSTANCE }
            return Pair(matchedBanned, NafdacExportCompliance.BANNED_MRL_VIOLATION)
        }

        val match = findByNameOrActive(productName) ?: findByNameOrActive(activeIngredient)
        return if (match != null) {
            Pair(match, match.complianceStatus)
        } else {
            Pair(null, NafdacExportCompliance.UNREGISTERED_UNKNOWN)
        }
    }
}
