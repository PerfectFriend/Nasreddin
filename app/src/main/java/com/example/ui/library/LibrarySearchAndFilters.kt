package com.example.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

@Composable
internal fun LibrarySearchAndFiltersSection(
    currentLanguage: AppLanguage,
    searchQuery: String,
    selectedFilter: LibraryFilter,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("library_search_input"),
            singleLine = true,
            placeholder = {
                Text(
                    text = AppStrings.searchStoriesPlaceholder(currentLanguage),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = CyberNeonCyan
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("library_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = AppStrings.clearSearchTooltip(currentLanguage),
                            tint = VanGoghStarGold
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberSurfaceDark,
                unfocusedContainerColor = CyberSurfaceDark,
                focusedBorderColor = CyberNeonCyan,
                unfocusedBorderColor = CyberSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = VanGoghStarGold
            )
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryFilter.entries) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFilter(filter) },
                    modifier = Modifier.testTag("library_filter_${filter.name.lowercase()}"),
                    label = {
                        Text(
                            text = AppStrings.libraryFilterLabel(currentLanguage, filter),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CyberSurfaceDark,
                        labelColor = Color.White.copy(alpha = 0.8f),
                        selectedContainerColor = VanGoghStarGold,
                        selectedLabelColor = Color(0xFF281500)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = CyberSurfaceElevated,
                        selectedBorderColor = VanGoghStarGold
                    )
                )
            }
        }
    }
}

@Composable
internal fun EmptyFilterCard(
    currentLanguage: AppLanguage,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("library_empty_filter_card"),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CyberSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = VanGoghStarGold.copy(alpha = 0.7f),
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = AppStrings.emptySearchTitle(currentLanguage),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppStrings.emptySearchSub(currentLanguage),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("library_reset_filters_btn"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CyberNeonCyan.copy(alpha = 0.16f),
                    contentColor = CyberNeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = AppStrings.resetFiltersAction(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
