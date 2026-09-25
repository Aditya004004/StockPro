package com.company.stockpro.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.StockProColors

/**
 * Standard StockPro text field with supporting text error formatting per rules.md §6.4.
 * Field validation appears inline via supportingText, never toasts.
 */
@Composable
fun StockProTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorMessage: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val isError = !errorMessage.isNullOrBlank()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = {
            val text = errorMessage ?: supportingText
            if (!text.isNullOrBlank()) {
                Text(
                    text = text,
                    color = if (isError) StockProColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = Radius.sm,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StockProColors.Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = StockProColors.Danger,
            focusedContainerColor = StockProColors.Surface,
            unfocusedContainerColor = StockProColors.Surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
