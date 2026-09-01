package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 空状态品牌：Cu + 无点 i + 动效圆点作为 i 上方圆点。圆点直径与 LogoAnimation 默认 52.dp 双圆点一致。 */
@Composable
fun CuiBranding(modifier: Modifier = Modifier) {
    val cuiStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = 176.sp,
        lineHeight = 184.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Cu",
            style = cuiStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box(contentAlignment = Alignment.TopCenter) {
            LogoAnimation(
                modifier = Modifier
                    .offset(y = 4.dp)
                    .align(Alignment.TopCenter),
                size = 52.dp,
            )
            Text(
                text = "ı",
                style = cuiStyle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.offset(x = 2.dp),
            )
        }
    }
}
