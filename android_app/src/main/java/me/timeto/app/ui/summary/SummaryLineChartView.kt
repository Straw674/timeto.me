package me.timeto.app.ui.summary

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.timeto.app.toColor
import me.timeto.app.ui.HStack
import me.timeto.app.ui.Screen
import me.timeto.app.ui.VStack
import me.timeto.app.ui.ZStack
import me.timeto.app.ui.c
import me.timeto.app.ui.rememberVm
import me.timeto.app.ui.roundedShape
import me.timeto.app.ui.squircleShape
import me.timeto.shared.DayBarsUi
import me.timeto.shared.vm.summary.SummaryLineChartVm
import me.timeto.shared.vm.summary.SummaryVm

@Composable
fun SummaryLineChartView(
    goalsUi: List<SummaryVm.GoalUi>,
    daysBarsUi: List<DayBarsUi>,
) {
    val (vm, state) = rememberVm(goalsUi, daysBarsUi) {
        SummaryLineChartVm(goalsUi, daysBarsUi)
    }

    Screen {
        VStack(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .verticalScroll(rememberScrollState()),
        ) {
            // Time unit selector
            TimeUnitSelector(
                currentUnit = state.timeUnit,
                onUnitSelected = { vm.setTimeUnit(it) }
            )

            // Line Chart
            if (state.linesData.isNotEmpty()) {
                LineChart(
                    linesData = state.linesData,
                    maxSeconds = state.maxSecondsOverall,
                    xLabels = state.xLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                )
            } else {
                ZStack(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Select activities to display",
                        color = c.secondaryText,
                        fontSize = 14.sp,
                    )
                }
            }

            // Goal selection
            Text(
                text = "Select Activities",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = c.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            GoalSelectionList(
                selections = state.goalSelections,
                onToggle = { goalId -> vm.toggleGoalSelection(goalId) }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TimeUnitSelector(
    currentUnit: SummaryLineChartVm.TimeUnit,
    onUnitSelected: (SummaryLineChartVm.TimeUnit) -> Unit,
) {
    HStack(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        SummaryLineChartVm.TimeUnit.entries.forEach { unit ->
            val isSelected = unit == currentUnit
            Text(
                text = unit.title,
                modifier = Modifier
                    .clip(squircleShape)
                    .background(if (isSelected) c.blue else c.transparent)
                    .clickable { onUnitSelected(unit) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (isSelected) c.white else c.text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun LineChart(
    linesData: List<SummaryLineChartVm.LineData>,
    maxSeconds: Int,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    // Calculate nice Y-axis ticks (round numbers)
    val yTicks = SummaryLineChartVm.calculateYAxisTicks(maxSeconds)
    val adjustedMaxSeconds = yTicks.lastOrNull() ?: 3600
    
    val scrollState = rememberScrollState()
    val pointSpacingDp = 50.dp
    val labelWidthDp = 40.dp
    val pointCount = xLabels.size
    
    // Calculate total width: first point at labelWidth/2, then spacing between points
    val totalWidthDp = if (pointCount > 1) {
        labelWidthDp + pointSpacingDp * (pointCount - 1)
    } else {
        labelWidthDp
    }

    Column(modifier = modifier) {
        // Y-axis labels and chart area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Y-axis labels - use BoxWithConstraints to position labels precisely
            BoxWithConstraints(
                modifier = Modifier
                    .width(45.dp)
                    .fillMaxHeight()
            ) {
                val boxHeight = maxHeight
                yTicks.forEach { tickValue ->
                    val fraction = tickValue.toFloat() / adjustedMaxSeconds
                    // Position from bottom: fraction=0 at bottom, fraction=1 at top
                    val offsetFromBottom = boxHeight * fraction
                    Text(
                        text = SummaryLineChartVm.formatSeconds(tickValue),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = -offsetFromBottom + 5.dp) // +5.dp to center text on line
                            .padding(end = 4.dp),
                        color = c.secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                    )
                }
            }

            // Chart canvas with horizontal scroll
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidthDp)
                        .fillMaxHeight()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val labelWidth = labelWidthDp.toPx()
                    val pointSpacing = pointSpacingDp.toPx()

                    if (pointCount == 0) return@Canvas

                    // Draw horizontal grid lines at each Y-axis tick
                    val gridColor = Color.Gray.copy(alpha = 0.5f)
                    yTicks.forEach { tickValue ->
                        val y = canvasHeight - (tickValue.toFloat() / adjustedMaxSeconds * canvasHeight)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f,
                        )
                    }

                    // Draw lines for each goal
                    linesData.forEach { lineData ->
                        if (lineData.points.isEmpty()) return@forEach

                        val path = Path()
                        val color = lineData.color.toColor()

                        lineData.points.forEachIndexed { index, point ->
                            // First point at labelWidth/2, then spaced by pointSpacing
                            val x = labelWidth / 2 + pointSpacing * index
                            val y = canvasHeight - (point.seconds.toFloat() / adjustedMaxSeconds * canvasHeight)

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // Draw the line
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            )
                        )

                        // Draw points
                        lineData.points.forEachIndexed { index, point ->
                            val x = labelWidth / 2 + pointSpacing * index
                            val y = canvasHeight - (point.seconds.toFloat() / adjustedMaxSeconds * canvasHeight)

                            drawCircle(
                                color = color,
                                radius = 5f,
                                center = Offset(x, y),
                            )
                        }
                    }
                }
            }
        }

        // X-axis labels (shared scrollState for sync)
        if (xLabels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(start = 45.dp)
                    .horizontalScroll(scrollState)
                    .padding(top = 4.dp),
            ) {
                xLabels.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        modifier = Modifier.width(labelWidthDp),
                        color = c.secondaryText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    if (index < xLabels.size - 1) {
                        Spacer(modifier = Modifier.width(pointSpacingDp - labelWidthDp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalSelectionList(
    selections: List<SummaryLineChartVm.GoalSelectionUi>,
    onToggle: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(selections, key = { it.goalDb.id }) { selection ->
            GoalSelectionItem(
                selection = selection,
                onToggle = { onToggle(selection.goalDb.id) }
            )
        }
    }
}

@Composable
private fun GoalSelectionItem(
    selection: SummaryLineChartVm.GoalSelectionUi,
    onToggle: () -> Unit,
) {
    val backgroundColor = if (selection.isSelected)
        selection.color.toColor().copy(alpha = 0.3f)
    else
        c.gray5

    val borderWidth = animateDpAsState(
        targetValue = if (selection.isSelected) 2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "borderWidth"
    )

    HStack(
        modifier = Modifier
            .clip(roundedShape)
            .background(backgroundColor)
            .then(
                if (selection.isSelected) {
                    Modifier.background(
                        color = selection.color.toColor().copy(alpha = 0.15f),
                        shape = roundedShape
                    )
                } else Modifier
            )
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(selection.color.toColor())
        )

        Text(
            text = selection.title,
            modifier = Modifier
                .padding(start = 8.dp),
            color = c.text,
            fontSize = 13.sp,
            fontWeight = if (selection.isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
