package com.company.stockpro.core.common.result

import android.content.Context
import com.company.stockpro.core.domain.util.UiText

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.StringResource -> context.getString(id, *args.toTypedArray())
}
