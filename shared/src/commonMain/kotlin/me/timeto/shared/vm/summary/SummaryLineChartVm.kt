package me.timeto.shared.vm.summary

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.timeto.shared.ColorRgba
import me.timeto.shared.DayBarsUi
import me.timeto.shared.UnixTime
import me.timeto.shared.db.Goal2Db
import me.timeto.shared.launchEx
import me.timeto.shared.toHms
import me.timeto.shared.vm.Vm

class SummaryLineChartVm(
    private val goalsUi: List<SummaryVm.GoalUi>,
    initialDaysBarsUi: List<DayBarsUi>,
) : Vm<SummaryLineChartVm.State>() {

    enum class TimeUnit(val title: String) {
        DAY("Day"),
        WEEK("Week"),
    }

    data class GoalSelectionUi(
        val goalDb: Goal2Db,
        val title: String,
        val color: ColorRgba,
        val isSelected: Boolean,
    )

    data class DataPoint(
        val label: String,
        val seconds: Int,
        val goalId: Int,
    )

    data class LineData(
        val goalDb: Goal2Db,
        val color: ColorRgba,
        val points: List<DataPoint>,
        val maxSeconds: Int,
    )

    data class State(
        val timeUnit: TimeUnit,
        val goalSelections: List<GoalSelectionUi>,
        val linesData: List<LineData>,
        val maxSecondsOverall: Int,
        val xLabels: List<String>,
    )

    override val state = MutableStateFlow(
        State(
            timeUnit = TimeUnit.DAY,
            goalSelections = emptyList(),
            linesData = emptyList(),
            maxSecondsOverall = 0,
            xLabels = emptyList(),
        )
    )

    private var daysBarsUi: List<DayBarsUi> = initialDaysBarsUi

    init {
        scopeVm().launchEx {
            // Initialize goal selections (all selected by default, max 5)
            val initialSelections = goalsUi.take(5).map { goalUi ->
                GoalSelectionUi(
                    goalDb = goalUi.goalDb,
                    title = goalUi.title,
                    color = goalUi.goalDb.colorRgba,
                    isSelected = true,
                )
            } + goalsUi.drop(5).map { goalUi ->
                GoalSelectionUi(
                    goalDb = goalUi.goalDb,
                    title = goalUi.title,
                    color = goalUi.goalDb.colorRgba,
                    isSelected = false,
                )
            }

            state.update {
                it.copy(goalSelections = initialSelections)
            }

            recalculateChartData()
        }
    }

    fun setTimeUnit(unit: TimeUnit) {
        state.update { it.copy(timeUnit = unit) }
        scopeVm().launchEx {
            recalculateChartData()
        }
    }

    fun toggleGoalSelection(goalId: Int) {
        val currentSelections = state.value.goalSelections
        val updatedSelections = currentSelections.map { selection ->
            if (selection.goalDb.id == goalId) {
                selection.copy(isSelected = !selection.isSelected)
            } else {
                selection
            }
        }
        state.update { it.copy(goalSelections = updatedSelections) }
        scopeVm().launchEx {
            recalculateChartData()
        }
    }

    private suspend fun recalculateChartData() {
        val currentState = state.value
        val selectedGoalIds = currentState.goalSelections
            .filter { it.isSelected }
            .map { it.goalDb.id }
            .toSet()

        if (selectedGoalIds.isEmpty() || daysBarsUi.isEmpty()) {
            state.update {
                it.copy(
                    linesData = emptyList(),
                    maxSecondsOverall = 0,
                    xLabels = emptyList(),
                )
            }
            return
        }

        val recursiveGoalsDb = Goal2Db.selectParentRecursiveMapCached()

        val groupedData: Map<Int, List<Pair<String, Map<Int, Int>>>> = when (currentState.timeUnit) {
            TimeUnit.DAY -> {
                // Group by day
                mapOf(0 to daysBarsUi.reversed().map { dayBarsUi ->
                    val unixTime = UnixTime.byLocalDay(dayBarsUi.unixDay)
                    val dayLabel = "${unixTime.dayOfMonth()}/${unixTime.month()}"
                    val goalSeconds = mutableMapOf<Int, Int>()

                    dayBarsUi.barsUi.forEach { barUi ->
                        val goalId = barUi.intervalDb?.goal_id ?: return@forEach
                        selectedGoalIds.forEach { selectedGoalId ->
                            val recursiveIds = (recursiveGoalsDb[selectedGoalId]?.map { it.id } ?: emptyList()) + selectedGoalId
                            if (goalId in recursiveIds) {
                                goalSeconds[selectedGoalId] = (goalSeconds[selectedGoalId] ?: 0) + barUi.seconds
                            }
                        }
                    }

                    dayLabel to goalSeconds
                })
            }
            TimeUnit.WEEK -> {
                // Group by week: every 7 days from the first day
                // Days that don't complete a full week are excluded
                val sortedDays = daysBarsUi.reversed() // chronological order
                if (sortedDays.isEmpty()) {
                    mapOf(0 to emptyList())
                } else {
                    val fullWeeksCount = sortedDays.size / 7
                    val weeks = (0 until fullWeeksCount).map { weekIndex ->
                        val weekDays = sortedDays.subList(weekIndex * 7, (weekIndex + 1) * 7)
                        val weekLabel = "W${weekIndex + 1}"

                        val goalSeconds = mutableMapOf<Int, Int>()
                        weekDays.forEach { dayBarsUi ->
                            dayBarsUi.barsUi.forEach { barUi ->
                                val goalId = barUi.intervalDb?.goal_id ?: return@forEach
                                selectedGoalIds.forEach { selectedGoalId ->
                                    val recursiveIds = (recursiveGoalsDb[selectedGoalId]?.map { it.id } ?: emptyList()) + selectedGoalId
                                    if (goalId in recursiveIds) {
                                        goalSeconds[selectedGoalId] = (goalSeconds[selectedGoalId] ?: 0) + barUi.seconds
                                    }
                                }
                            }
                        }

                        weekLabel to goalSeconds
                    }
                    mapOf(0 to weeks)
                }
            }
        }

        val timePoints = groupedData[0] ?: emptyList()
        val xLabels = timePoints.map { it.first }

        val selectedGoals = currentState.goalSelections.filter { it.isSelected }
        val linesData = selectedGoals.map { selection ->
            val goalId = selection.goalDb.id
            val points = timePoints.mapIndexed { index, (label, goalSeconds) ->
                DataPoint(
                    label = label,
                    seconds = goalSeconds[goalId] ?: 0,
                    goalId = goalId,
                )
            }
            val maxSeconds = points.maxOfOrNull { it.seconds } ?: 0
            LineData(
                goalDb = selection.goalDb,
                color = selection.color,
                points = points,
                maxSeconds = maxSeconds,
            )
        }

        val maxSecondsOverall = linesData.maxOfOrNull { it.maxSeconds } ?: 0

        state.update {
            it.copy(
                linesData = linesData,
                maxSecondsOverall = maxSecondsOverall,
                xLabels = xLabels,
            )
        }
    }

    companion object {
        fun formatSeconds(seconds: Int): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "0"
            }
        }

        /**
         * Calculate nice Y-axis tick values that are round numbers.
         * Returns a list of tick values from 0 to the adjusted max, in seconds.
         */
        fun calculateYAxisTicks(maxSeconds: Int, tickCount: Int = 5): List<Int> {
            if (maxSeconds <= 0) {
                // Default: 0 to 1 hour in 5 steps
                return (0..tickCount).map { it * 3600 / tickCount }
            }

            // Find a nice interval that gives round numbers
            // Prefer intervals like 15m, 30m, 1h, 2h, etc.
            val intervals = listOf(
                5 * 60,      // 5 minutes
                10 * 60,     // 10 minutes
                15 * 60,     // 15 minutes
                30 * 60,     // 30 minutes
                60 * 60,     // 1 hour
                2 * 60 * 60, // 2 hours
                3 * 60 * 60, // 3 hours
                5 * 60 * 60, // 5 hours
                10 * 60 * 60, // 10 hours
                20 * 60 * 60 // 20 hours
            )

            // Find the smallest interval that gives us <= tickCount ticks
            val targetInterval = intervals.firstOrNull { interval ->
                (maxSeconds + interval - 1) / interval <= tickCount
            } ?: (maxSeconds / tickCount).let { raw ->
                // Round up to nearest hour if very large
                ((raw + 3599) / 3600) * 3600
            }

            val adjustedMax = ((maxSeconds + targetInterval - 1) / targetInterval) * targetInterval
            val actualTickCount = adjustedMax / targetInterval

            return (0..actualTickCount).map { it * targetInterval }
        }
    }
}
