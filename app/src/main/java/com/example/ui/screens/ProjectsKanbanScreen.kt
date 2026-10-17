package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RepurposeMatrixItem
import com.example.data.model.SalvageProject
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DarkBase
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ScrapMasterViewModel

@Composable
fun ProjectsKanbanScreen(
    viewModel: ScrapMasterViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val matrixItems by viewModel.filteredMatrix.collectAsState()
    val matrixFilter by viewModel.matrixCategoryFilter.collectAsState()
    val matrixSearch by viewModel.matrixSearch.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Kanban Board, 1 = 35x Matrix
    var showAddProjectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Main Tab Switcher
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonCyan
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "DÍLNA KANBAN (${projects.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "35-BODOVÁ MATICE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            )
        }

        if (selectedTab == 0) {
            // Kanban View
            KanbanBoardView(
                projects = projects,
                onStatusChange = { id, status -> viewModel.updateProjectStatus(id, status) },
                onDelete = { id -> viewModel.deleteProject(id) },
                onAddClick = { showAddProjectDialog = true }
            )
        } else {
            // 35-Point Repurpose Matrix View
            RepurposeMatrixView(
                matrixItems = matrixItems,
                currentFilter = matrixFilter,
                searchQuery = matrixSearch,
                onSearchChange = { viewModel.matrixSearch.value = it },
                onFilterChange = { viewModel.matrixCategoryFilter.value = it },
                onImportToKanban = { matrixItem ->
                    viewModel.addProject(
                        SalvageProject(
                            title = matrixItem.title,
                            status = "IN_PROGRESS",
                            category = if (matrixItem.category.contains("IoT")) "HARDCORE_IOT_RF" else "EDC_POCKET_MODULE",
                            description = matrixItem.description,
                            requiredParts = matrixItem.keySalvagedChips,
                            difficulty = matrixItem.difficulty,
                            stepByStep = "1. Vyhledej díly v e-waste: ${matrixItem.salvageSources}.\n2. Zkontroluj zapojení: ${matrixItem.pinoutHint}.\n3. Otestuj na nepájivém poli.",
                            matrixIndex = matrixItem.index
                        )
                    )
                    selectedTab = 0 // Switch to Kanban to see added project
                }
            )
        }
    }

    if (showAddProjectDialog) {
        AddProjectDialog(
            onDismiss = { showAddProjectDialog = false },
            onAdd = { newProject ->
                viewModel.addProject(newProject)
                showAddProjectDialog = false
            }
        )
    }
}

@Composable
private fun KanbanBoardView(
    projects: List<SalvageProject>,
    onStatusChange: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onAddClick: () -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    val statuses = listOf("ALL", "IDEA", "IN_PROGRESS", "TESTING", "DONE")

    val filtered = if (selectedStatusFilter == "ALL") projects
    else projects.filter { it.status == selectedStatusFilter }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Status Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statuses) { status ->
                    val isSelected = status == selectedStatusFilter
                    val label = when (status) {
                        "ALL" -> "Vše (${projects.size})"
                        "IDEA" -> "Nápad (${projects.count { it.status == "IDEA" }})"
                        "IN_PROGRESS" -> "V řešení (${projects.count { it.status == "IN_PROGRESS" }})"
                        "TESTING" -> "Testování (${projects.count { it.status == "TESTING" }})"
                        "DONE" -> "Hotovo (${projects.count { it.status == "DONE" }})"
                        else -> status
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface)
                            .border(1.dp, if (isSelected) NeonCyan else DarkCardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedStatusFilter = status }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DÍLENSKÉ KANBAN PROJEKTY (${filtered.size})",
                    color = NeonEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase),
                    modifier = Modifier.testTag("add_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nový projekt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Žádné projekty v tomto sloupci", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        items(filtered, key = { it.id }) { project ->
            KanbanProjectCard(
                project = project,
                onStatusChange = { newStatus -> onStatusChange(project.id, newStatus) },
                onDelete = { onDelete(project.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun KanbanProjectCard(
    project: SalvageProject,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expandedSteps by remember { mutableStateOf(false) }

    val statusColor = when (project.status) {
        "IDEA" -> NeonAmber
        "IN_PROGRESS" -> NeonCyan
        "TESTING" -> NeonPurple
        "DONE" -> NeonEmerald
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Obtížnost: ${project.difficulty} | ${project.category}",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                StatusBadge(text = project.status, color = statusColor)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = project.description,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            // BOM parts required
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "SALVAGED BOM DÍLY:",
                        color = NeonAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = project.requiredParts,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (project.stepByStep.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedSteps = !expandedSteps },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expandedSteps) "Skrýt montážní postup ▲" else "Zobrazit montážní postup ▼",
                        color = NeonEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                AnimatedVisibility(visible = expandedSteps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkBase)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = project.stepByStep,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status transition buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Smazat",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDelete() }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (project.status != "IDEA") {
                        MiniStatusButton("NÁPAD") { onStatusChange("IDEA") }
                    }
                    if (project.status != "IN_PROGRESS") {
                        MiniStatusButton("V ŘEŠENÍ") { onStatusChange("IN_PROGRESS") }
                    }
                    if (project.status != "TESTING") {
                        MiniStatusButton("TESTOVAT") { onStatusChange("TESTING") }
                    }
                    if (project.status != "DONE") {
                        MiniStatusButton("HOTOVO", NeonEmerald) { onStatusChange("DONE") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatusButton(label: String, color: Color = NeonCyan, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RepurposeMatrixView(
    matrixItems: List<RepurposeMatrixItem>,
    currentFilter: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String?) -> Unit,
    onImportToKanban: (RepurposeMatrixItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Hledat v 35 projektech (LoRa, BadUSB, powerbanka, sniffer...)", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        item {
            // Filter Pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Pair("Vše (35)", null),
                    Pair("Hardcore IoT/RF (20)", "Hardcore IoT/RF"),
                    Pair("EDC Pocket Module (15)", "EDC Pocket Module")
                ).forEach { (label, cat) ->
                    val isSelected = currentFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonEmerald.copy(alpha = 0.2f) else DarkSurface)
                            .border(1.dp, if (isSelected) NeonEmerald else DarkCardBorder, RoundedCornerShape(8.dp))
                            .clickable { onFilterChange(cat) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) NeonEmerald else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        items(matrixItems, key = { it.index }) { item ->
            MatrixItemCard(item = item, onImport = { onImportToKanban(item) })
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun MatrixItemCard(
    item: RepurposeMatrixItem,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${item.index} ",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(text = item.difficulty, color = NeonAmber)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Zdroje e-waste: ${item.salvageSources}",
                color = TextMuted,
                fontSize = 11.sp
            )
            Text(
                text = "Klíčové čipy: ${item.keySalvagedChips}",
                color = NeonCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "💡 Pinout tip: ${item.pinoutHint}",
                color = NeonEmerald,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f), contentColor = NeonCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Převést do aktivních Kanban projektů dílny", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddProjectDialog(
    onDismiss: () -> Unit,
    onAdd: (SalvageProject) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("INTERMEDIATE") }
    var steps by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "NOVÝ PROJEKT ZE ŠROTU",
                color = NeonCyan,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název projektu (např. BadUSB Dongle)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = parts,
                    onValueChange = { parts = it },
                    label = { Text("Potřebné součástky (ESP32, OLED, BMS...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Stručný popis a cíl projektu") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = { Text("Postup montáže (kroky)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            SalvageProject(
                                title = title,
                                status = "IDEA",
                                category = "HARDCORE_IOT_RF",
                                description = description,
                                requiredParts = parts,
                                difficulty = difficulty,
                                stepByStep = steps
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase)
            ) {
                Text("Vytvořit projekt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
