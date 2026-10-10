package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.ReverbPreset
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun EqualizerDialog(
    isEnabled: Boolean,
    bands: List<EqualizerBand>,
    currentPreset: EqualizerPreset,
    onEnableChanged: (Boolean) -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onBandLevelChanged: (Short, Short) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MainViewModel? = null
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Audio effects states from ViewModel (if available)
    val isBassBoostEnabled = viewModel?.isBassBoostEnabled?.collectAsStateWithLifecycle()?.value ?: false
    val bassBoostStrength = viewModel?.bassBoostStrength?.collectAsStateWithLifecycle()?.value ?: 0
    val isVirtualizerEnabled = viewModel?.isVirtualizerEnabled?.collectAsStateWithLifecycle()?.value ?: false
    val virtualizerStrength = viewModel?.virtualizerStrength?.collectAsStateWithLifecycle()?.value ?: 0
    val isReverbEnabled = viewModel?.isReverbEnabled?.collectAsStateWithLifecycle()?.value ?: false
    val reverbPreset = viewModel?.reverbPreset?.collectAsStateWithLifecycle()?.value ?: ReverbPreset.NONE
    val isLoudnessEnabled = viewModel?.isLoudnessEnabled?.collectAsStateWithLifecycle()?.value ?: false
    val loudnessGain = viewModel?.loudnessGain?.collectAsStateWithLifecycle()?.value ?: 0
    val eqPreamp = viewModel?.eqPreamp?.collectAsStateWithLifecycle()?.value ?: 0
    val playbackSpeed = viewModel?.playbackSpeed?.collectAsStateWithLifecycle()?.value ?: 1.0f
    val playbackPitch = viewModel?.playbackPitch?.collectAsStateWithLifecycle()?.value ?: 1.0f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 580.dp)
                .padding(vertical = 16.dp)
                .testTag("equalizer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header: Title & Master Power Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Эквалайзер и эффекты",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isEnabled) "Обработка активна" else "Эквалайзер отключен",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onEnableChanged,
                        modifier = Modifier.testTag("equalizer_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Tabs: Эквалайзер, Аудиоэффекты, Скорость и тон
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = "Эквалайзер",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = "Эффекты",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Waves, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = {
                            Text(
                                text = "Скорость",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content per Tab
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTabIndex) {
                        0 -> EqualizerTabContent(
                            isEnabled = isEnabled,
                            bands = bands,
                            currentPreset = currentPreset,
                            eqPreamp = eqPreamp,
                            onPresetSelected = onPresetSelected,
                            onBandLevelChanged = onBandLevelChanged,
                            onPreampChanged = { viewModel?.setEqPreamp(it) },
                            onReset = { viewModel?.resetEqualizer() }
                        )
                        1 -> AudioEffectsTabContent(
                            isBassBoostEnabled = isBassBoostEnabled,
                            bassBoostStrength = bassBoostStrength,
                            isVirtualizerEnabled = isVirtualizerEnabled,
                            virtualizerStrength = virtualizerStrength,
                            isReverbEnabled = isReverbEnabled,
                            reverbPreset = reverbPreset,
                            isLoudnessEnabled = isLoudnessEnabled,
                            loudnessGain = loudnessGain,
                            onBassBoostEnabledChanged = { viewModel?.setBassBoostEnabled(it) },
                            onBassBoostStrengthChanged = { viewModel?.setBassBoostStrength(it) },
                            onVirtualizerEnabledChanged = { viewModel?.setVirtualizerEnabled(it) },
                            onVirtualizerStrengthChanged = { viewModel?.setVirtualizerStrength(it) },
                            onReverbEnabledChanged = { viewModel?.setReverbEnabled(it) },
                            onReverbPresetSelected = { viewModel?.setReverbPreset(it) },
                            onLoudnessEnabledChanged = { viewModel?.setLoudnessEnabled(it) },
                            onLoudnessGainChanged = { viewModel?.setLoudnessGain(it) },
                            onResetEffects = { viewModel?.resetAudioEffects() }
                        )
                        2 -> PlaybackSpeedPitchTabContent(
                            speed = playbackSpeed,
                            pitch = playbackPitch,
                            onSpeedChanged = { viewModel?.setPlaybackSpeed(it) },
                            onPitchChanged = { viewModel?.setPlaybackPitch(it) },
                            onReset = { viewModel?.resetPlaybackSpeedAndPitch() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dialog Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("equalizer_close_button")
                    ) {
                        Text("Готово", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 1: EQUALIZER CONTENT
// ==========================================
@Composable
private fun EqualizerTabContent(
    isEnabled: Boolean,
    bands: List<EqualizerBand>,
    currentPreset: EqualizerPreset,
    eqPreamp: Int,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onBandLevelChanged: (Short, Short) -> Unit,
    onPreampChanged: (Int) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Presets Chips
        Text(
            text = "Пресеты",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqualizerPreset.entries.forEach { preset ->
                FilterChip(
                    selected = currentPreset == preset,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.displayName, style = MaterialTheme.typography.bodySmall) },
                    enabled = isEnabled,
                    modifier = Modifier.testTag("preset_${preset.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Frequency Response Curve
        if (bands.isNotEmpty()) {
            Text(
                text = "Амплитудно-частотная характеристика (АЧХ)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FrequencyResponseCurveView(
                bands = bands,
                isEnabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Headroom / Preamp protection slider
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Предусиление (Preamp Headroom)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Защита от перегрузки при усилении частот",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    val preampDb = eqPreamp / 100f
                    val preampFormatted = if (preampDb > 0) "+%.1f дБ".format(preampDb) else "%.1f дБ".format(preampDb)
                    Text(
                        text = preampFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = eqPreamp.toFloat(),
                    onValueChange = { onPreampChanged(it.roundToInt()) },
                    valueRange = -1200f..600f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Frequency Bands Header with Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Полосы частот",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            TextButton(
                onClick = onReset,
                enabled = isEnabled,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Сбросить в 0 дБ", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (bands.isEmpty()) {
            Text(
                text = "Эквалайзер недоступен на данном устройстве",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            bands.forEach { band ->
                val freqText = if (band.centerFreqHz >= 1000) {
                    "${band.centerFreqHz / 1000} кГц"
                } else {
                    "${band.centerFreqHz} Гц"
                }

                val dbValue = band.levelMilliBels.toFloat() / 100f
                val dbText = if (dbValue > 0) "+%.1f дБ".format(dbValue) else "%.1f дБ".format(dbValue)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Frequency label
                        Text(
                            text = freqText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(62.dp)
                        )

                        // Slider with step buttons
                        FilledTonalIconButton(
                            onClick = {
                                val next = (band.levelMilliBels - 100).coerceAtLeast(-1200)
                                onBandLevelChanged(band.bandIndex, next.toShort())
                            },
                            enabled = isEnabled,
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Меньше", modifier = Modifier.size(16.dp))
                        }

                        Slider(
                            value = band.levelMilliBels.toFloat(),
                            onValueChange = { newMilliBels ->
                                onBandLevelChanged(band.bandIndex, newMilliBels.toInt().toShort())
                            },
                            valueRange = -1200f..1200f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("band_slider_${band.bandIndex}")
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val next = (band.levelMilliBels + 100).coerceAtMost(1200)
                                onBandLevelChanged(band.bandIndex, next.toShort())
                            },
                            enabled = isEnabled,
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Больше", modifier = Modifier.size(16.dp))
                        }

                        // Current dB value
                        Text(
                            text = dbText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: AUDIO EFFECTS CONTENT
// ==========================================
@Composable
private fun AudioEffectsTabContent(
    isBassBoostEnabled: Boolean,
    bassBoostStrength: Int,
    isVirtualizerEnabled: Boolean,
    virtualizerStrength: Int,
    isReverbEnabled: Boolean,
    reverbPreset: ReverbPreset,
    isLoudnessEnabled: Boolean,
    loudnessGain: Int,
    onBassBoostEnabledChanged: (Boolean) -> Unit,
    onBassBoostStrengthChanged: (Int) -> Unit,
    onVirtualizerEnabledChanged: (Boolean) -> Unit,
    onVirtualizerStrengthChanged: (Int) -> Unit,
    onReverbEnabledChanged: (Boolean) -> Unit,
    onReverbPresetSelected: (ReverbPreset) -> Unit,
    onLoudnessEnabledChanged: (Boolean) -> Unit,
    onLoudnessGainChanged: (Int) -> Unit,
    onResetEffects: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Reset Header Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Пространственные и звуковые эффекты",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onResetEffects,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Сбросить всё", style = MaterialTheme.typography.labelSmall)
            }
        }

        // 1. Bass Boost Card
        AudioEffectCard(
            title = "Усиление басов (Bass Boost)",
            subtitle = "Глубокие и насыщенные низкие частоты",
            icon = Icons.Default.Hearing,
            isEnabled = isBassBoostEnabled,
            onEnabledChange = onBassBoostEnabledChanged,
            sliderValue = (bassBoostStrength / 10f).roundToInt(),
            sliderValueText = "${(bassBoostStrength / 10f).roundToInt()}%",
            onSliderChange = { onBassBoostStrengthChanged((it * 10).roundToInt()) },
            valueRange = 0f..100f
        )

        // 2. Virtualizer Card
        AudioEffectCard(
            title = "Виртуализатор (3D Virtualizer)",
            subtitle = "Объёмное пространственное звучание (для наушников)",
            icon = Icons.Default.Headphones,
            isEnabled = isVirtualizerEnabled,
            onEnabledChange = onVirtualizerEnabledChanged,
            sliderValue = (virtualizerStrength / 10f).roundToInt(),
            sliderValueText = "${(virtualizerStrength / 10f).roundToInt()}%",
            onSliderChange = { onVirtualizerStrengthChanged((it * 10).roundToInt()) },
            valueRange = 0f..100f
        )

        // 3. Preset Reverb Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Реверберация (Spatial Reverb)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Акустическое моделирование помещений",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isReverbEnabled,
                        onCheckedChange = onReverbEnabledChanged,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReverbPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = reverbPreset == preset,
                            onClick = {
                                onReverbPresetSelected(preset)
                                if (!isReverbEnabled && preset != ReverbPreset.NONE) {
                                    onReverbEnabledChanged(true)
                                }
                            },
                            label = { Text(preset.displayName, style = MaterialTheme.typography.bodySmall) },
                            enabled = isReverbEnabled,
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // 4. Loudness Enhancer Card
        val loudnessDb = loudnessGain / 100f
        AudioEffectCard(
            title = "Усилитель громкости (Loudness Gain)",
            subtitle = "Чистый аппаратный запас громкости",
            icon = Icons.Default.VolumeUp,
            isEnabled = isLoudnessEnabled,
            onEnabledChange = onLoudnessEnabledChanged,
            sliderValue = loudnessGain,
            sliderValueText = "+%.1f дБ".format(loudnessDb),
            onSliderChange = { onLoudnessGainChanged(it.roundToInt()) },
            valueRange = 0f..800f
        )
    }
}

// ==========================================
// TAB 3: PLAYBACK SPEED & PITCH CONTENT
// ==========================================
@Composable
private fun PlaybackSpeedPitchTabContent(
    speed: Float,
    pitch: Float,
    onSpeedChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Скорость и тональность треков",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Сбросить 1.0x", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Speed Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скорость воспроизведения", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("%.2fx".format(speed), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Slider(
                    value = speed,
                    onValueChange = onSpeedChanged,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick speed chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { s ->
                        FilledTonalButton(
                            onClick = { onSpeedChanged(s) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = if ((speed - s).let { kotlin.math.abs(it) < 0.05f }) {
                                ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            }
                        ) {
                            Text("${s}x", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Pitch Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Тональность (Pitch)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("%.2fx".format(pitch), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Slider(
                    value = pitch,
                    onValueChange = onPitchChanged,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick pitch chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f).forEach { p ->
                        FilledTonalButton(
                            onClick = { onPitchChanged(p) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = if ((pitch - p).let { kotlin.math.abs(it) < 0.05f }) {
                                ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            }
                        ) {
                            Text("${p}x", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// REUSABLE AUDIO EFFECT CARD
// ==========================================
@Composable
private fun AudioEffectCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    sliderValue: Int,
    sliderValueText: String,
    onSliderChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = onSliderChange,
                    valueRange = valueRange,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = sliderValueText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ==========================================
// VISUAL FREQUENCY RESPONSE CURVE CANVAS
// ==========================================
@Composable
private fun FrequencyResponseCurveView(
    bands: List<EqualizerBand>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceVariantColor.copy(alpha = 0.4f))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Draw 0 dB dashed line
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.5.dp.toPx()
        )

        if (bands.isEmpty()) return@Canvas

        val stepX = width / (bands.size + 1).coerceAtLeast(2)
        val points = mutableListOf<Offset>()

        // Leftmost point at 0 dB
        points.add(Offset(0f, centerY))

        bands.forEachIndexed { index, band ->
            val x = stepX * (index + 1)
            // Range is -1200 to +1200 mB (-12 to +12 dB)
            val normalized = (band.levelMilliBels.toFloat() / 1200f).coerceIn(-1f, 1f)
            // Inverted for canvas Y (up is negative Y)
            val y = centerY - (normalized * (height * 0.42f))
            points.add(Offset(x, y))
        }

        // Rightmost point at 0 dB
        points.add(Offset(width, centerY))

        // Create smooth cubic Bezier path
        val strokePath = Path()
        val fillPath = Path()

        strokePath.moveTo(points[0].x, points[0].y)
        fillPath.moveTo(0f, height)
        fillPath.lineTo(points[0].x, points[0].y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            strokePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        val activeColor = if (isEnabled) primaryColor else outlineColor

        // Draw gradient fill under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    activeColor.copy(alpha = if (isEnabled) 0.35f else 0.15f),
                    activeColor.copy(alpha = 0.02f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw curve outline
        drawPath(
            path = strokePath,
            color = activeColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw control dots
        for (i in 1 until points.size - 1) {
            val p = points[i]
            drawCircle(
                color = surfaceColor,
                radius = 5.dp.toPx(),
                center = p
            )
            drawCircle(
                color = activeColor,
                radius = 3.dp.toPx(),
                center = p
            )
        }
    }
}
