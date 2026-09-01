package com.inspiredandroid.kai.data

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

@Immutable
data class ServiceEntry(
    val instanceId: String,
    val serviceId: String,
    val serviceName: String,
    val modelId: String,
    val icon: DrawableResource,
    /** Models available under this service (the "分支" of the 总类). Empty for services
     *  without a model list (e.g. Free modes). */
    val modelOptions: List<ServiceModelOption> = emptyList(),
)

/**
 * A single selectable model branch under a [ServiceEntry] 总类, used by the
 * two-level chat model dropdown.
 */
@Immutable
data class ServiceModelOption(
    val id: String,
    val label: String,
    val isFreeTier: Boolean = false,
)
