package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.R

/**
 * The launcher icon, drawn from the same two adaptive icon layers so the header always matches it.
 * Launchers show the middle 72 of the layers' 108 units, so the layers are drawn 1.5x larger than
 * the badge and cropped to it.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        val layerSize = size * 1.5f
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.requiredSize(layerSize),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(layerSize),
        )
    }
}
