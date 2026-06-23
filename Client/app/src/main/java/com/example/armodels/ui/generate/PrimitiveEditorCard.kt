package com.example.armodels.ui.generate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.armodels.geometry.PrimitiveObject
import com.example.armodels.geometry.PrimitiveType
import java.util.Locale

@Composable
fun PrimitiveEditorCard(
    index: Int,
    primitive: PrimitiveObject,
    onUpdate: (PrimitiveObject) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val color = Color(
        red = primitive.color.getOrElse(0) { 0.7f },
        green = primitive.color.getOrElse(1) { 0.7f },
        blue = primitive.color.getOrElse(2) { 0.7f },
        alpha = primitive.color.getOrElse(3) { 1.0f }
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                )
                Text(
                    text = "#${index + 1}  ${primitive.type.label}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Type selector
                TypeSelector(
                    selected = primitive.type,
                    onSelect = { newType ->
                        onUpdate(primitive.copy(type = newType, dimensions = newType.defaultDimensions.toList()))
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Dimensions
                SectionLabel("Размеры")
                primitive.type.dimensionLabels.forEachIndexed { i, label ->
                    FloatField(
                        label = label,
                        value = primitive.dimensions.getOrElse(i) { 1f },
                        onValueChange = { v ->
                            val dims = primitive.dimensions.toMutableList()
                            while (dims.size <= i) dims.add(1f)
                            dims[i] = v
                            onUpdate(primitive.copy(dimensions = dims))
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Offset
                SectionLabel("Смещение (X, Y, Z)")
                Vec3Fields(
                    labels = listOf("X", "Y", "Z"),
                    values = primitive.offset,
                    onUpdate = { onUpdate(primitive.copy(offset = it)) }
                )

                Spacer(Modifier.height(8.dp))

                // Rotation
                SectionLabel("Поворот (°X, °Y, °Z)")
                Vec3Fields(
                    labels = listOf("Rx", "Ry", "Rz"),
                    values = primitive.rotation,
                    onUpdate = { onUpdate(primitive.copy(rotation = it)) }
                )

                Spacer(Modifier.height(8.dp))

                // Color
                SectionLabel("Цвет (R, G, B, A)")
                ColorSliders(
                    color = primitive.color,
                    onUpdate = { onUpdate(primitive.copy(color = it)) }
                )
            }
        }
    }
}

@Composable
private fun TypeSelector(selected: PrimitiveType, onSelect: (PrimitiveType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Тип: ${selected.label}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PrimitiveType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = { onSelect(type); expanded = false },
                    leadingIcon = if (type == selected) ({ Text("✓") }) else null
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun FloatField(label: String, value: Float, onValueChange: (Float) -> Unit) {
    var text by remember(value) { mutableStateOf(value.fmt()) }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            text = s
            s.toFloatOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    )
}

@Composable
private fun Vec3Fields(labels: List<String>, values: List<Float>, onUpdate: (List<Float>) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            var text by remember(values.getOrElse(i) { 0f }) { mutableStateOf(values.getOrElse(i) { 0f }.fmt()) }
            OutlinedTextField(
                value = text,
                onValueChange = { s ->
                    text = s
                    s.toFloatOrNull()?.let { v ->
                        val updated = values.toMutableList()
                        while (updated.size <= i) updated.add(0f)
                        updated[i] = v
                        onUpdate(updated)
                    }
                },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ColorSliders(color: List<Float>, onUpdate: (List<Float>) -> Unit) {
    val r = color.getOrElse(0) { 0.7f }
    val g = color.getOrElse(1) { 0.7f }
    val b = color.getOrElse(2) { 0.7f }
    val a = color.getOrElse(3) { 1.0f }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(r, g, b, a))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            listOf("R" to r, "G" to g, "B" to b, "A" to a).forEachIndexed { i, (label, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.width(16.dp), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = v,
                        onValueChange = { nv ->
                            val updated = color.toMutableList()
                            while (updated.size <= i) updated.add(1f)
                            updated[i] = nv
                            onUpdate(updated)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${"%.2f".format(v)}", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun Float.fmt(): String {
    return if (this == kotlin.math.floor(this).toFloat() && this >= -1e6f && this <= 1e6f)
        this.toInt().toString()
    else
        String.format(Locale.US, "%.3f", this).trimEnd('0').trimEnd('.')
}
