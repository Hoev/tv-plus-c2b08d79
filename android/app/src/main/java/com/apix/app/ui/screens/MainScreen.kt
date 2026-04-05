package com.apix.app.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apix.app.data.Category
import com.apix.app.data.Channel
import com.apix.app.ui.components.*
import com.apix.app.ui.theme.Gold
import com.apix.app.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    uiState: UiState,
    onCategorySelected: (Category) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onSearchClick: () -> Unit,
    channels: List<Channel>,
    modifier: Modifier = Modifier
) {
    val displayCategories = remember(uiState.categories, uiState.showSettingsSection) {
        buildList {
            addAll(uiState.categories)
            if (uiState.showSettingsSection) {
                add(Category(id = "__settings", name = "الإعدادات", sortOrder = Int.MAX_VALUE))
            }
        }
    }

    // Force RTL
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (uiState.isLoading) {
            FullScreenLoader()
            return@CompositionLocalProvider
        }

        if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error, color = Color.Red, fontSize = 18.sp)
            }
            return@CompositionLocalProvider
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeLayout(
                uiState = uiState,
                categories = displayCategories,
                channels = channels,
                onCategorySelected = onCategorySelected,
                onChannelClick = onChannelClick,
                onSearchClick = onSearchClick
            )
        } else {
            PortraitLayout(
                uiState = uiState,
                categories = displayCategories,
                channels = channels,
                onCategorySelected = onCategorySelected,
                onChannelClick = onChannelClick,
                onSearchClick = onSearchClick
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: UiState,
    categories: List<Category>,
    channels: List<Channel>,
    onCategorySelected: (Category) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onSearchClick: () -> Unit
) {
    val selectedIndex = categories.indexOfFirst { it.id == uiState.selectedCategory?.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar: APiX logo + search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ApixLogo(fontSize = 32)
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "Search", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        // Category title
        uiState.selectedCategory?.let {
            Text(
                text = it.name.uppercase(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Channels grid - 2 columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }

        // Bottom navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.forEachIndexed { index, cat ->
                BottomNavCategoryItem(
                    category = cat,
                    isSelected = index == selectedIndex,
                    onClick = { onCategorySelected(cat) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LandscapeLayout(
    uiState: UiState,
    categories: List<Category>,
    channels: List<Channel>,
    onCategorySelected: (Category) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onSearchClick: () -> Unit
) {
    val selectedIndex = categories.indexOfFirst { it.id == uiState.selectedCategory?.id }

    // Force LTR for layout positioning, then RTL content inside
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Main content area (left)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top bar with category name + clock + search
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category title (left)
                        uiState.selectedCategory?.let {
                            Text(
                                text = it.name.uppercase(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Clock
                            val time = remember { mutableStateOf("") }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    time.value = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        .format(Date())
                                    kotlinx.coroutines.delay(30000)
                                }
                            }
                            Text(
                                text = time.value,
                                color = Gold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .border(1.dp, Gold, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            // Search
                            IconButton(onClick = onSearchClick) {
                                Icon(Icons.Default.Search, "Search", tint = Color.White)
                            }
                        }
                    }
                }

                // Channel grid - 4 columns for TV (2 for small landscape)
                val config = LocalConfiguration.current
                val cols = if (config.screenWidthDp > 900) 4 else 2

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(channels, key = { it.id }) { channel ->
                            ChannelCard(
                                channel = channel,
                                onClick = { onChannelClick(channel) }
                            )
                        }
                    }
                }
            }

            // Right sidebar
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF111111))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gold bar + APiX
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .background(Gold, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(4.dp))
                ApixLogo(fontSize = 24)

                Spacer(Modifier.height(24.dp))

                // Category list
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val idx = categories.indexOf(cat)
                            SidebarCategoryItem(
                                category = cat,
                                isSelected = idx == selectedIndex,
                                onClick = { onCategorySelected(cat) }
                            )
                        }
                    }
                }
            }
        }
    }
}
