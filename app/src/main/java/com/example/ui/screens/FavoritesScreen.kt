package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FavoritePlaceEntity
import com.example.data.model.LocationPoint
import com.example.ui.NavScreen
import com.example.ui.NavViewModel
import com.example.ui.i18n.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val strings = AppStrings.get(lang)
    val favorites by viewModel.favorites.collectAsState()
    val curLoc by viewModel.currentLocation.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFavorite by remember { mutableStateOf<FavoritePlaceEntity?>(null) }

    var inputName by remember { mutableStateOf("") }
    var inputAddress by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("HOME") }
    var inputNotes by remember { mutableStateOf("") }

    val categories = listOf("ALL", "HOME", "WORK", "FAVORITE", "CUSTOM")

    val filteredList = if (selectedCategoryFilter == "ALL") {
        favorites
    } else {
        favorites.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    inputName = ""
                    inputAddress = ""
                    inputCategory = "HOME"
                    inputNotes = ""
                    editingFavorite = null
                    showAddDialog = true
                },
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_favorite_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addPlace)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateTo(NavScreen.MAP) },
                            modifier = Modifier.testTag("favorites_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.favoritesTab,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Pills
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Surface(
                                onClick = { selectedCategoryFilter = cat },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Favorites List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.noFavoritesFound,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { fav ->
                        FavoriteItemCard(
                            favorite = fav,
                            strings = strings,
                            onNavigate = {
                                val loc = LocationPoint(fav.name, fav.address, fav.latitude, fav.longitude)
                                viewModel.setDestination(loc)
                                viewModel.navigateTo(NavScreen.MAP)
                            },
                            onEdit = {
                                editingFavorite = fav
                                inputName = fav.name
                                inputAddress = fav.address
                                inputCategory = fav.category
                                inputNotes = fav.notes
                                showAddDialog = true
                            },
                            onDelete = {
                                viewModel.deleteFavorite(fav)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Favorite Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingFavorite != null) strings.editPlace else strings.addPlace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Place Name") },
                        placeholder = { Text("e.g. My Apartment, Gym") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputAddress,
                        onValueChange = { inputAddress = it },
                        label = { Text("Address / City") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputNotes,
                        onValueChange = { inputNotes = it },
                        label = { Text("Notes (optional)") },
                        singleLine = true
                    )

                    Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("HOME", "WORK", "FAVORITE", "CUSTOM").forEach { cat ->
                            val isSel = inputCategory == cat
                            Surface(
                                onClick = { inputCategory = cat },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(30.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            if (editingFavorite != null) {
                                val updated = editingFavorite!!.copy(
                                    name = inputName,
                                    address = inputAddress,
                                    category = inputCategory,
                                    notes = inputNotes,
                                    updatedAt = System.currentTimeMillis()
                                )
                                viewModel.updateFavorite(updated)
                            } else {
                                val lat = curLoc.latitude + 0.005
                                val lng = curLoc.longitude + 0.005
                                viewModel.saveFavorite(inputName, inputAddress, inputCategory, lat, lng, inputNotes)
                            }
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FavoriteItemCard(
    favorite: FavoritePlaceEntity,
    strings: com.example.ui.i18n.TranslationDictionary,
    onNavigate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            val (icon, color) = when (favorite.category.uppercase()) {
                "HOME" -> Icons.Default.Home to Color(0xFF10B981)
                "WORK" -> Icons.Default.Work to Color(0xFF3B82F6)
                "FAVORITE" -> Icons.Default.Star to Color(0xFFF59E0B)
                else -> Icons.Default.Place to Color(0xFF8B5CF6)
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(favorite.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(favorite.address, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                if (favorite.notes.isNotBlank()) {
                    Text(favorite.notes, color = MaterialTheme.colorScheme.outlineVariant, fontSize = 11.sp)
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onNavigate, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Navigation, contentDescription = strings.navigateNow, tint = Color(0xFF0284C7))
            }
        }
    }
}
