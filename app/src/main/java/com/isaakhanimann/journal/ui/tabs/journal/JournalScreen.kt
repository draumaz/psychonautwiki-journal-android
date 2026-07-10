/*
 * Copyright (c) 2022-2023. Isaak Hanimann.
 * This file is part of jrnl.
 *
 * jrnl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * jrnl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jrnl.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
 */

package com.isaakhanimann.journal.ui.tabs.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaakhanimann.journal.data.room.experiences.entities.AdaptiveColor
import com.isaakhanimann.journal.data.room.experiences.relations.ExperienceWithIngestionsCompanionsAndRatings
import com.isaakhanimann.journal.ui.tabs.journal.components.ExperienceRow
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.TimeDisplayOption
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.AllTimelines
import com.isaakhanimann.journal.ui.tabs.stats.EmptyScreenDisclaimer
import com.isaakhanimann.journal.ui.theme.JournalTheme
import com.isaakhanimann.journal.ui.theme.horizontalPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun JournalScreen(
    navigateToExperiencePopNothing: (experienceId: Int) -> Unit,
    navigateToAddIngestion: () -> Unit,
    navigateToCalendar: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val experiences = viewModel.experiences.collectAsState().value
    val liveUpdate by viewModel.liveUpdateFlow.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.maybeMigrate()
    }
    JournalScreen(
        navigateToExperiencePopNothing = navigateToExperiencePopNothing,
        navigateToAddIngestion = {
            viewModel.resetAddIngestionTimes()
            navigateToAddIngestion()
        },
        navigateToCalendar = navigateToCalendar,
        isFavoriteEnabled = viewModel.isFavoriteEnabledFlow.collectAsState().value,
        onChangeIsFavorite = viewModel::onChangeFavorite,
        isTimeRelativeToNow = viewModel.isTimeRelativeToNow.value,
        onChangeIsRelative = viewModel::onChangeRelative,
        searchText = viewModel.searchTextFlow.collectAsState().value,
        onChangeSearchText = viewModel::search,
        isSearchEnabled = viewModel.isSearchEnabled.value,
        onChangeIsSearchEnabled = viewModel::onChangeOfIsSearchEnabled,
        experiences = experiences,
        liveUpdate = liveUpdate
    )
}

@Preview
@Composable
fun ExperiencesScreenPreview(
    @PreviewParameter(
        JournalScreenPreviewProvider::class,
    ) experiences: List<ExperienceWithIngestionsCompanionsAndRatings>,
) {
    JournalTheme {
        JournalScreen(
            navigateToExperiencePopNothing = {},
            navigateToAddIngestion = {},
            navigateToCalendar = {},
            isFavoriteEnabled = false,
            onChangeIsFavorite = {},
            isTimeRelativeToNow = true,
            onChangeIsRelative = {},
            searchText = "",
            onChangeSearchText = {},
            isSearchEnabled = true,
            onChangeIsSearchEnabled = {},
            experiences = experiences,
            liveUpdate = null
        )
    }
}

@Composable
fun LiveUpdateCardWrapper(
    liveUpdate: LiveUpdateModel?,
    isTimeRelativeToNow: Boolean,
    navigateToExperiencePopNothing: (experienceId: Int) -> Unit
) {
    AnimatedVisibility(
        visible = liveUpdate != null && !liveUpdate.isInAfterglow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (liveUpdate != null) {
            LiveUpdateCard(
                liveUpdate = liveUpdate,
                isTimeRelativeToNow = isTimeRelativeToNow,
                onTap = {
                    navigateToExperiencePopNothing(liveUpdate.ingestionWithCompanion.ingestion.experienceId)
                }
            )
        }
    }
}

@Composable
fun LiveUpdateCard(
    liveUpdate: LiveUpdateModel,
    isTimeRelativeToNow: Boolean,
    onTap: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val zoneId = remember { ZoneId.systemDefault() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = liveUpdate.ingestionWithCompanion.ingestion.substanceName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val startTime = liveUpdate.ingestionWithCompanion.ingestion.time
                    if (isTimeRelativeToNow) {
                        com.isaakhanimann.journal.ui.tabs.journal.experience.components.TimeRelativeToNowText(
                            time = startTime,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    } else {
                        Text(
                            text = "Started at ${startTime.atZone(zoneId).format(timeFormatter)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val displayOption = if (isTimeRelativeToNow) {
                TimeDisplayOption.RELATIVE_TO_NOW
            } else {
                TimeDisplayOption.REGULAR
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // Slightly taller to accommodate AllTimelines axis
            ) {
                AllTimelines(
                    model = liveUpdate.timelineModel,
                    isShowingCurrentTime = true,
                    timeDisplayOption = displayOption,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    navigateToExperiencePopNothing: (experienceId: Int) -> Unit,
    navigateToAddIngestion: () -> Unit,
    navigateToCalendar: () -> Unit,
    isFavoriteEnabled: Boolean,
    onChangeIsFavorite: (Boolean) -> Unit,
    isTimeRelativeToNow: Boolean,
    onChangeIsRelative: (Boolean) -> Unit,
    searchText: String,
    onChangeSearchText: (String) -> Unit,
    isSearchEnabled: Boolean,
    onChangeIsSearchEnabled: (Boolean) -> Unit,
    experiences: List<ExperienceWithIngestionsCompanionsAndRatings>,
    liveUpdate: LiveUpdateModel?,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("jrnl") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                actions = {
                    IconToggleButton(
                        checked = isTimeRelativeToNow,
                        onCheckedChange = onChangeIsRelative
                    ) {
                        if (isTimeRelativeToNow) {
                            Icon(Icons.Filled.Timer, contentDescription = "Regular time")
                        } else {
                            Icon(Icons.Outlined.Timer, contentDescription = "Time relative to now")
                        }
                    }
                    IconToggleButton(
                        checked = isFavoriteEnabled,
                        onCheckedChange = onChangeIsFavorite
                    ) {
                        if (isFavoriteEnabled) {
                            Icon(Icons.Filled.Star, contentDescription = "Is favorite")
                        } else {
                            Icon(Icons.Outlined.StarOutline, contentDescription = "Is not favorite")
                        }
                    }
                    IconToggleButton(
                        checked = isSearchEnabled,
                        onCheckedChange = onChangeIsSearchEnabled
                    ) {
                        if (isSearchEnabled) {
                            Icon(Icons.Outlined.SearchOff, contentDescription = "Search off")
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = navigateToCalendar) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Navigate to calendar"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSearchEnabled) {
                ExtendedFloatingActionButton(
                    onClick = navigateToAddIngestion,
                    modifier = Modifier.padding(bottom = 90.dp),
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add"
                        )
                    },
                    text = { Text("Ingestion") },
                )
            }
        },
    ) { padding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                AnimatedVisibility(visible = isSearchEnabled) {
                    Column {
                        val focusManager = LocalFocusManager.current
                        TextField(
                            value = searchText,
                            onValueChange = onChangeSearchText,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                )
                            },
                            trailingIcon = {
                                if (searchText != "") {
                                    IconButton(
                                        onClick = {
                                            onChangeSearchText("")
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close",
                                        )
                                    }
                                }
                            },
                            label = { Text(text = "Search experiences") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            singleLine = true
                        )
                        if (experiences.isEmpty() && isSearchEnabled && searchText.isNotEmpty()) {
                            if (isFavoriteEnabled) {
                                Column(modifier = Modifier.padding(horizontalPadding)) {
                                    Text(
                                        text = "No results",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "No favorite experience titles match your search.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.padding(horizontalPadding)) {
                                    Text(
                                        text = "No results",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "No experience titles match your search.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                val listState = rememberLazyListState()
                val isScrollUpButtonShown by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0
                    }
                }
                Box(contentAlignment = Alignment.TopEnd) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        if (experiences.isNotEmpty()) {
                            item {
                                HorizontalDivider()
                            }
                        }
                        item {
                            Column {
                                AnimatedVisibility(
                                    visible = liveUpdate == null || liveUpdate.isInAfterglow,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(32.dp))
                                        Text(
                                            text = "Past experiences",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = horizontalPadding)
                                        )
                                        Spacer(modifier = Modifier.height(32.dp))
                                    }
                                }
                                LiveUpdateCardWrapper(
                                    liveUpdate = liveUpdate,
                                    isTimeRelativeToNow = isTimeRelativeToNow,
                                    navigateToExperiencePopNothing = navigateToExperiencePopNothing
                                )
                            }
                        }

                        items(experiences) { experienceWithIngestions ->
                            ExperienceRow(
                                experienceWithIngestions,
                                navigateToExperienceScreen = {
                                    navigateToExperiencePopNothing(experienceWithIngestions.experience.id)
                                },
                                isTimeRelativeToNow = isTimeRelativeToNow
                            )
                        }
                    }
                    this@Column.AnimatedVisibility(visible = isScrollUpButtonShown) {
                        val scope = rememberCoroutineScope()
                        ElevatedButton(
                            modifier = Modifier.padding(all = horizontalPadding),
                            onClick = {
                                scope.launch {
                                    listState.scrollToItem(index = 0)
                                }
                            }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll to top")
                        }
                    }
                }
            }
            if (experiences.isEmpty() && !isSearchEnabled) {
                if (isFavoriteEnabled) {
                    EmptyScreenDisclaimer(
                        title = "No favorites",
                        description = "Mark experiences as favorites to find them quickly."
                    )
                } else {
                    EmptyScreenDisclaimer(
                        title = "No experiences yet",
                        description = "Add your first ingestion."
                    )
                }
            }
        }
    }
}
