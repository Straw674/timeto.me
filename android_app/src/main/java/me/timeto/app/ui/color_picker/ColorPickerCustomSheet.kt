package me.timeto.app.ui.color_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.timeto.app.ui.H_PADDING
import me.timeto.app.ui.H_PADDING_HALF
import me.timeto.app.ui.VStack
import me.timeto.app.ui.ZStack
import me.timeto.app.ui.c
import me.timeto.app.ui.squircleShape
import me.timeto.app.toColor
import me.timeto.app.ui.Screen
import me.timeto.app.ui.navigation.LocalNavigationLayer
import me.timeto.shared.ColorRgba
import me.timeto.shared.vm.color_picker.ColorPickerVm

// todo prevent navigation back

@Composable
fun ColorPickerCustomSheet(
    initColorRgba: ColorRgba,
    onDone: (ColorRgba) -> Unit,
) {

    val navigationLayer = LocalNavigationLayer.current

    val (initH, initS, initL) = initColorRgba.toHsl()
    val hue = remember { mutableFloatStateOf(initH) }
    val saturation = remember { mutableFloatStateOf(initS) }
    val lightness = remember { mutableFloatStateOf(initL) }

    val colorRgba: ColorRgba =
        ColorRgba.fromHsl(hue.floatValue, saturation.floatValue, lightness.floatValue, initColorRgba.a)

    VStack {

        ZStack(Modifier.weight(1f))

        // todo divider or sheet background color

        Screen(
            modifier = Modifier
                .height(300.dp),
        ) {

            ZStack(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .padding(horizontal = H_PADDING_HALF),
            ) {

                Text(
                    text = "Cancel",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(squircleShape)
                        .clickable {
                            navigationLayer.close()
                        }
                        .padding(
                            horizontal = H_PADDING_HALF,
                            vertical = 4.dp,
                        ),
                    color = c.blue,
                )

                Text(
                    text = ColorPickerVm.prepCustomColorRgbaText(
                        colorRgba = colorRgba,
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(squircleShape)
                        .background(colorRgba.toColor())
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                    color = c.white,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                )

                Text(
                    text = "Done",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(squircleShape)
                        .clickable {
                            onDone(colorRgba)
                            navigationLayer.close()
                        }
                        .padding(
                            horizontal = H_PADDING_HALF,
                            vertical = 4.dp,
                        ),
                    color = c.blue,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HslSliderView(
                value = hue,
                valueRange = 0f..360f,
                color = Color.hsl(hue.floatValue, 1f, 0.5f),
            )

            HslSliderView(
                value = saturation,
                valueRange = 0f..100f,
                color = Color.hsl(hue.floatValue, saturation.floatValue / 100f, 0.5f),
            )

            HslSliderView(
                value = lightness,
                valueRange = 0f..100f,
                color = Color.hsl(hue.floatValue, 1f, lightness.floatValue / 100f),
            )

            ZStack(Modifier.navigationBarsPadding())
        }
    }
}

///

@Composable
private fun HslSliderView(
    value: MutableFloatState,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color,
) {
    Slider(
        value = value.floatValue,
        onValueChange = { value.floatValue = it },
        modifier = Modifier
            .padding(horizontal = H_PADDING - 6.dp),
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color,
        ),
    )
}

