package com.madaraka.connect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.madaraka.connect.data.models.FaultCategory

// Icon + color + background for each fault category
private data class CategoryStyle(
    val icon: ImageVector,
    val color: Color,
    val background: Color
)

// Maps a FaultCategory to the style used on the website
private fun styleFor(category: FaultCategory): CategoryStyle = when (category) {
    FaultCategory.WATER       -> CategoryStyle(Icons.Filled.WaterDrop, Color(0xFF0EA5E9), Color(0xFFE0F2FE)) // sky
    FaultCategory.ELECTRICITY -> CategoryStyle(Icons.Filled.Bolt,      Color(0xFFF59E0B), Color(0xFFFEF3C7)) // amber
    FaultCategory.WASTE       -> CategoryStyle(Icons.Filled.Delete,    Color(0xFF10B981), Color(0xFFD1FAE5)) // emerald
    FaultCategory.UNKNOWN     -> CategoryStyle(Icons.Filled.Warning,   Color(0xFF64748B), Color(0xFFF1F5F9)) // slate
}

// Small colored circle with the category icon inside it
@Composable
fun CategoryIcon(
    category: FaultCategory,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    val style = styleFor(category)

    Box(
        modifier = modifier
            .size(size.dp)
            .background(style.background, RoundedCornerShape((size / 3).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = category.displayName,
            tint = style.color,
            modifier = Modifier.size((size * 0.55).dp)
        )
    }
}