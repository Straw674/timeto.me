package me.timeto.shared.vm.history.form

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.timeto.shared.Cache
import me.timeto.shared.UnixTime
import me.timeto.shared.db.IntervalDb
import me.timeto.shared.launchExIo
import me.timeto.shared.time
import me.timeto.shared.textFeatures
import me.timeto.shared.DialogsManager
import me.timeto.shared.UiException
import me.timeto.shared.db.Goal2Db
import me.timeto.shared.vm.Vm

class HistoryFormVm(
    initIntervalDb: IntervalDb,
) : Vm<HistoryFormVm.State>() {

    private val initTime: Int = initIntervalDb.id

    data class State(
        val initIntervalDb: IntervalDb,
        val goalDb: Goal2Db,
        val time: Int,
        val goalsUi: List<GoalUi>,
        val daysItemsUi: List<DayItemUi>,
        val hoursItemsUi: List<HourItemUi>,
        val minutesItemsUi: List<MinuteItemUi>,
        val selectedDayIndex: Int,
        val selectedHour: Int,
        val selectedMinute: Int,
    ) {

        val title: String = run {
            val note: String? = initIntervalDb.note
                ?.trim()
                ?.textFeatures()
                ?.textNoFeatures
                ?.takeIf { it.isNotBlank() }
            note ?: initIntervalDb.selectGoalDbCached().name.textFeatures().textNoFeatures
        }
        val doneText = "Save"

        val goalTitle = "Goal"
        val goalNote: String =
            goalDb.name.textFeatures().textNoFeatures

        val timeNote: String =
            HistoryFormUtils.makeTimeNote(time, withToday = true)
    }

    override val state = MutableStateFlow(
        run {
            val unixTime = UnixTime(initTime)
            val dayTime = unixTime.utcTime() % 86_400
            val hour = dayTime / 3600
            val minute = (dayTime % 3600) / 60
            val daysItemsUi = buildDaysItems()
            val selectedDayIndex = daysItemsUi.indexOfFirst { it.localDay == unixTime.localDay }
                .let { if (it < 0) daysItemsUi.size - 1 else it }
            State(
                initIntervalDb = initIntervalDb,
                goalDb = initIntervalDb.selectGoalDbCached(),
                time = initTime,
                goalsUi = Cache.goals2Db.map { GoalUi(it) },
                daysItemsUi = daysItemsUi,
                hoursItemsUi = buildHoursItems(),
                minutesItemsUi = buildMinutesItems(),
                selectedDayIndex = selectedDayIndex,
                selectedHour = hour,
                selectedMinute = minute,
            )
        }
    )

    fun setGoal(newGoalDb: Goal2Db) {
        state.update { it.copy(goalDb = newGoalDb) }
    }

    fun setDayIndex(newDayIndex: Int) {
        state.update { currentState ->
            val localDay = currentState.daysItemsUi[newDayIndex].localDay
            val newTime = calculateTime(localDay, currentState.selectedHour, currentState.selectedMinute)
            currentState.copy(
                selectedDayIndex = newDayIndex,
                time = newTime,
            )
        }
    }

    fun setHour(newHour: Int) {
        state.update { currentState ->
            val localDay = currentState.daysItemsUi[currentState.selectedDayIndex].localDay
            val newTime = calculateTime(localDay, newHour, currentState.selectedMinute)
            currentState.copy(
                selectedHour = newHour,
                time = newTime,
            )
        }
    }

    fun setMinute(newMinute: Int) {
        state.update { currentState ->
            val localDay = currentState.daysItemsUi[currentState.selectedDayIndex].localDay
            val newTime = calculateTime(localDay, currentState.selectedHour, newMinute)
            currentState.copy(
                selectedMinute = newMinute,
                time = newTime,
            )
        }
    }

    private fun calculateTime(localDay: Int, hour: Int, minute: Int): Int {
        val newUnixTime = UnixTime.byLocalDay(localDay)
            .inSeconds(hour * 3600 + minute * 60)
        // Ensure time doesn't exceed current time
        val now = time()
        return if (newUnixTime.time > now) now else newUnixTime.time
    }

    fun save(
        dialogsManager: DialogsManager,
        onSuccess: () -> Unit,
    ) {
        val state = state.value
        val time: Int = state.time
        launchExIo {
            try {
                val intervalDb = state.initIntervalDb
                intervalDb.updateEx(
                    newId = time,
                    newTimer = intervalDb.timer,
                    newGoalDb = state.goalDb,
                    newNote = intervalDb.note,
                )
                onUi { onSuccess() }
            } catch (e: UiException) {
                dialogsManager.alert(e.uiMessage)
            }
        }
    }

    fun moveToTasks(
        intervalDb: IntervalDb,
        dialogsManager: DialogsManager,
        onSuccess: () -> Unit,
    ) {
        HistoryFormUtils.moveToTasksUi(
            intervalDb = intervalDb,
            dialogsManager = dialogsManager,
            onSuccess = {
                onUi { onSuccess() }
            },
        )
    }

    fun delete(
        intervalDb: IntervalDb,
        dialogsManager: DialogsManager,
        onSuccess: () -> Unit,
    ) {
        HistoryFormUtils.deleteIntervalUi(
            intervalDb = intervalDb,
            dialogsManager = dialogsManager,
            onSuccess = {
                onUi { onSuccess() }
            },
        )
    }

    ///

    data class GoalUi(
        val goalDb: Goal2Db,
    ) {
        val title: String =
            goalDb.name.textFeatures().textNoFeatures
    }

    data class DayItemUi(
        val localDay: Int,
    ) {
        val title: String = run {
            val unixTime = UnixTime.byLocalDay(localDay)
            val today = UnixTime().localDay
            when (localDay) {
                today -> "Today"
                today - 1 -> "Yesterday"
                else -> unixTime.getStringByComponents(
                    UnixTime.StringComponent.dayOfMonth,
                    UnixTime.StringComponent.space,
                    UnixTime.StringComponent.month3,
                )
            }
        }
    }

    data class HourItemUi(
        val hour: Int,
    ) {
        val title: String = "$hour".padStart(2, '0')
    }

    data class MinuteItemUi(
        val minute: Int,
    ) {
        val title: String = "$minute".padStart(2, '0')
    }
}

// Build last 30 days
private fun buildDaysItems(): List<HistoryFormVm.DayItemUi> {
    val today = UnixTime().localDay
    return (0 until 30).map { daysAgo ->
        HistoryFormVm.DayItemUi(today - daysAgo)
    }.reversed()
}

private fun buildHoursItems(): List<HistoryFormVm.HourItemUi> =
    (0..23).map { HistoryFormVm.HourItemUi(it) }

private fun buildMinutesItems(): List<HistoryFormVm.MinuteItemUi> =
    (0..59).map { HistoryFormVm.MinuteItemUi(it) }
