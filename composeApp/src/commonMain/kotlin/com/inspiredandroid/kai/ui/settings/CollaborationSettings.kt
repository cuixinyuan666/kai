package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry

/**
 * 协作模式设置页：说明参与规则。运行参数在聊天界面的协作向导中配置。
 */
@Composable
internal fun CollaborationSettings(
    dataRepository: DataRepository,
    services: List<ServiceEntry>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("参与规则", style = MaterialTheme.typography.titleMedium)
                Text(
                    "在聊天界面点击「协作模式」打开向导：输入问题 → 选择分数门槛 → 设置运行参数 → 开始。每个达标模型以单一模式完整流水线独立作答，结果保存在聊天记录的「协作模式」文件夹中。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "仅模型测试总分严格大于门槛的模型会参与。请先运行模型测试。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
