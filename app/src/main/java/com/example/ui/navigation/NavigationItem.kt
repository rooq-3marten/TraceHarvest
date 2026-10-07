package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.viewmodel.AppTab

/**
 * Human-centered Navigation Destinations for Nigerian Agricultural Field Agents:
 * 1. Home (Regional Overview & Quick Actions)
 * 2. Register Farmer (5-Step Satellite Boundary & Ethical Avatar Onboarding)
 * 3. Log Practice (NAFDAC Chemical & Organic Cultural Auditing)
 * 4. Batch Creation (Export Consignment Lot Building)
 * 5. Sync & Records (Offline Store & Forward Engine)
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
            tab = AppTab.HOME_DASHBOARD,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_tab_home"
        ),
        NavigationItem(
            tab = AppTab.REGISTER_FARMER,
            label = "Enroll",
            selectedIcon = Icons.Filled.PersonAdd,
            unselectedIcon = Icons.Outlined.PersonAdd,
            testTag = "nav_tab_register_farmer"
        ),
        NavigationItem(
            tab = AppTab.LOG_PRACTICE,
            label = "Practice",
            selectedIcon = Icons.Filled.Agriculture,
            unselectedIcon = Icons.Outlined.Agriculture,
            testTag = "nav_tab_log_practice"
        ),
        NavigationItem(
            tab = AppTab.BATCH_CREATION,
            label = "Batch",
            selectedIcon = Icons.Filled.Inventory2,
            unselectedIcon = Icons.Outlined.Inventory2,
            testTag = "nav_tab_batch_creation"
        ),
        NavigationItem(
            tab = AppTab.SYNC_RECORDS,
            label = "Sync",
            selectedIcon = Icons.Filled.CloudSync,
            unselectedIcon = Icons.Outlined.CloudSync,
            testTag = "nav_tab_sync_records"
        )
    )
}
