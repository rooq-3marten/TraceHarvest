package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Science
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.viewmodel.AppTab

/**
 * Streamlined Field Agent workflow destinations:
 * 1. Register New Farmer (Offline SQLite -> pending_sync -> Backend Sync -> Unique Farmer ID)
 * 2. Stage 2: Practice Logging (🌱 Planting, 💧 Irrigation, 🧪 Pesticide, 🌿 Fertilizer, ✂️ Harvest)
 * 3. Sync & Records (Offline queue and ledger verification)
 */
data class NavigationItem(
    val tab: AppTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

object NavigationDestinations {
    val items = listOf(
        NavigationItem(
            tab = AppTab.REGISTER_FARMER,
            label = "Register Farmer",
            selectedIcon = Icons.Filled.PersonAdd,
            unselectedIcon = Icons.Outlined.PersonAdd,
            testTag = "nav_tab_register_farmer"
        ),
        NavigationItem(
            tab = AppTab.LOG_PRACTICE,
            label = "Log Practice",
            selectedIcon = Icons.Filled.Science,
            unselectedIcon = Icons.Outlined.Science,
            testTag = "nav_tab_log_practice"
        ),
        NavigationItem(
            tab = AppTab.SYNC_RECORDS,
            label = "Sync & Records",
            selectedIcon = Icons.Filled.CloudSync,
            unselectedIcon = Icons.Outlined.CloudSync,
            testTag = "nav_tab_sync_records"
        )
    )
}
