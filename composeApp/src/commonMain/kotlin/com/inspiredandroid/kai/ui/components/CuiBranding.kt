package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 空状态品牌：Cu + 动效圆点叠在 i 上方 */
@Composable
fun CuiBranding(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Cu",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box(contentAlignment = Alignment.TopCenter) {
            LogoAnimation(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .align(Alignment.TopCenter),
                size = 18.dp,
            )
            Text(
                text = "i",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.offset(x = 2.dp),
            )
        }
    }
}
