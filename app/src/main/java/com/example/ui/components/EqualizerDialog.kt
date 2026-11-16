package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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

    val glassBackground = Color(0xFF07110A)
    val glassCard = Color(0xFF111F16)
    val glassLime = Color(0xFFC8FF55)
    val glassText = Color(0xFFEAF2E6)
    val glassMuted = Color(0xFFA5B2A2)

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = glassLime,
            onPrimary = Color(0xFF17210B),
            surface = glassBackground,
            onSurface = glassText,
            surfaceVariant = glassCard,
            onSurfaceVariant = glassMuted,
            outlineVariant = Color(0xFF354638)
        )
    ) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = androidx.compose.ui.graphics.RectangleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("equalizer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Rounded segmented tabs matching the dark lime reference design.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabItems = listOf(
                        Triple("Эквалайзер", Icons.Default.Tune, 0),
                        Triple("Эффекты", Icons.Default.Waves, 1),
                        Triple("Скорость", Icons.Default.Speed, 2)
                    )
                    tabItems.forEach { (label, icon, index) ->
                        val selected = selectedTabIndex == index
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) glassLime else Color.Transparent)
                                .clickable { selectedTabIndex = index }
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) Color(0xFF17210B) else glassMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = if (selected) Color(0xFF17210B) else glassText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Keep the complete equalizer tab visible; secondary tabs remain scrollable.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (selectedTabIndex == 0 || selectedTabIndex == 1) Modifier else Modifier.verticalScroll(rememberScrollState()))
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

                Spacer(modifier = Modifier.height(8.dp))

                // Fixed bottom actions, matching the reference layout.
                if (selectedTabIndex == 0 || selectedTabIndex == 1 || selectedTabIndex == 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                when (selectedTabIndex) {
                                    0 -> viewModel?.resetEqualizer()
                                    1 -> viewModel?.resetAudioEffects()
                                    2 -> viewModel?.resetPlaybackSpeedAndPitch()
                                }
                            },
                            enabled = selectedTabIndex != 0 || isEnabled,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сбросить всё", maxLines = 1, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Применить", maxLines = 1, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.testTag("equalizer_close_button")
                        ) {
                            Text("Готово", fontWeight = FontWeight.SemiBold)
                        }
                    }
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "Пресеты",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            EqualizerPreset.entries.forEach { preset ->
                FilterChip(
                    selected = currentPreset == preset,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    enabled = isEnabled,
                    modifier = Modifier.testTag("preset_${preset.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        if (bands.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("АЧХ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Уровень, дБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FrequencyResponseCurveView(
                        bands = bands,
                        isEnabled = isEnabled,
                        modifier = Modifier.fillMaxWidth().height(78.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bands.forEach { band ->
                            val frequencyLabel = if (band.centerFreqHz >= 1000) {
                                "${band.centerFreqHz / 1000} кГц"
                            } else {
                                "${band.centerFreqHz} Гц"
                            }
                            Text(
                                frequencyLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Полосы частот", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("−12 … +12 дБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        bands.forEach { band ->
                            val freqText = if (band.centerFreqHz >= 1000) {
                                "${band.centerFreqHz / 1000} кГц"
                            } else {
                                "${band.centerFreqHz} Гц"
                            }
                            val dbValue = band.levelMilliBels.toFloat() / 100f
                            val dbText = if (dbValue > 0) "+%.1f".format(dbValue) else "%.1f".format(dbValue)

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.width(42.dp).height(190.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VerticalBandSlider(
                                        value = band.levelMilliBels.toFloat(),
                                        enabled = isEnabled,
                                        onValueChange = { newMilliBels ->
                                            val quantized = ((newMilliBels / 25f).roundToInt() * 25)
                                                .coerceIn(-1200, 1200).toShort()
                                            onBandLevelChanged(band.bandIndex, quantized)
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("band_slider_${band.bandIndex}")
                                    )
                                }
                                Text(
                                    text = freqText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = dbText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (band.levelMilliBels != 0.toShort()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Эквалайзер недоступен на данном устройстве",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
            modifier = Modifier.fillMaxWidth().offset(y = (-8).dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Предусиление", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Общий уровень громкости", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val preampDb = eqPreamp / 100f
                    val preampFormatted = if (preampDb > 0) "+%.1f дБ".format(preampDb) else "%.1f дБ".format(preampDb)
                    Text(preampFormatted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                DotSlider(
                    value = eqPreamp.toFloat(),
                    onValueChange = {
                        val quantized = ((it / 50f).roundToInt() * 50).coerceIn(-1200, 600)
                        onPreampChanged(quantized)
                    },
                    valueRange = -1200f..600f,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                )
            }
        }
    }
}

// ==========================================
@Composable
private fun VerticalBandSlider(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = Color(0xFF26362A)
    fun updateFromY(y: Float, height: Float) {
        if (!enabled || height <= 0f) return
        val fraction = (1f - (y / height)).coerceIn(0f, 1f)
        onValueChange(-1200f + fraction * 2400f)
    }

    Canvas(
        modifier = modifier
            .pointerInput(enabled) {
                detectTapGestures { position -> updateFromY(position.y, size.height.toFloat()) }
            }
            .pointerInput(enabled) {
                detectDragGestures(
                    onDragStart = { position -> updateFromY(position.y, size.height.toFloat()) },
                    onDrag = { change, _ ->
                        updateFromY(change.position.y, size.height.toFloat())
                        change.consume()
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val fraction = ((value + 1200f) / 2400f).coerceIn(0f, 1f)
        val thumbY = bottom - fraction * (bottom - top)
        val railWidth = 5.dp.toPx()

        drawLine(inactiveColor, Offset(centerX, top), Offset(centerX, bottom), railWidth, cap = StrokeCap.Round)
        drawLine(
            if (enabled) activeColor else inactiveColor,
            Offset(centerX, thumbY),
            Offset(centerX, bottom),
            railWidth,
            cap = StrokeCap.Round
        )
        drawCircle(Color(0xFF17251A), 12.dp.toPx(), Offset(centerX, thumbY))
        drawCircle(if (enabled) activeColor else Color(0xFF657064), 9.dp.toPx(), Offset(centerX, thumbY))
    }
}

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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Пространственные и звуковые эффекты",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

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
            valueRange = 0f..100f,
            compact = true
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
            valueRange = 0f..100f,
            compact = true
        )

        // 3. Preset Reverb Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
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
                        colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

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
            onSliderChange = { onLoudnessGainChanged(((it / 25f).roundToInt() * 25).coerceIn(0, 800)) },
            valueRange = 0f..800f,
            compact = true
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

                DotSlider(
                    value = speed,
                    onValueChange = onSpeedChanged,
                    valueRange = 0.5f..2.0f,
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

                DotSlider(
                    value = pitch,
                    onValueChange = onPitchChanged,
                    valueRange = 0.5f..2.0f,
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

@Composable
private fun DotSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val active = MaterialTheme.colorScheme.primary
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = active,
            activeTrackColor = active,
            inactiveTrackColor = Color(0xFF26362A),
            disabledThumbColor = Color(0xFF657064),
            disabledActiveTrackColor = Color(0xFF657064)
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color(0xFF17251A), CircleShape)
                    .padding(3.dp)
                    .background(if (enabled) active else Color(0xFF657064), CircleShape)
            )
        }
    )
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
    valueRange: ClosedFloatingPointRange<Float>,
    compact: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(if (compact) 9.dp else 14.dp)) {
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
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotSlider(
                    value = sliderValue.toFloat(),
                    onValueChange = onSliderChange,
                    valueRange = valueRange,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f).then(if (compact) Modifier.height(28.dp) else Modifier)
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

    // This canvas redraws on every band-slider movement (~60 times a second while
    // dragging). Paths, the point list, the gradient colour list and the stroke used to be
    // allocated per frame; keep them for the lifetime of the composable instead.
    val strokePath = remember { Path() }
    val fillPath = remember { Path() }
    val points = remember { ArrayList<Offset>() }
    val activeColor = if (isEnabled) primaryColor else outlineColor
    val gradientColors = remember(activeColor, isEnabled) {
        listOf(
            activeColor.copy(alpha = if (isEnabled) 0.35f else 0.15f),
            activeColor.copy(alpha = 0.02f)
        )
    }
    val curveStrokeWidth = with(LocalDensity.current) { 2.5.dp.toPx() }
    val curveStroke = remember(curveStrokeWidth) {
        Stroke(width = curveStrokeWidth, cap = StrokeCap.Round)
    }

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
        points.clear()

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
        strokePath.reset()
        fillPath.reset()

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

        // Draw gradient fill under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = 0f,
                endY = height
            )
        )

        // Draw curve outline
        drawPath(
            path = strokePath,
            color = activeColor,
            style = curveStroke
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
