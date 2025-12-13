package me.timeto.app.ui.history.form

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import me.timeto.app.ui.H_PADDING_HALF
import me.timeto.app.ui.c
import me.timeto.app.ui.dpToPx
import me.timeto.app.isSdkQPlus
import me.timeto.app.ui.rememberVm
import me.timeto.app.ui.Screen
import me.timeto.app.ui.SpacerW1
import me.timeto.app.ui.footer.Footer
import me.timeto.app.ui.footer.FooterPlainButton
import me.timeto.app.ui.form.FormItemView
import me.timeto.app.ui.form.button.FormButton
import me.timeto.app.ui.form.padding.FormPaddingTop
import me.timeto.app.ui.header.Header
import me.timeto.app.ui.header.HeaderActionButton
import me.timeto.app.ui.header.HeaderCancelButton
import me.timeto.app.ui.navigation.LocalNavigationFs
import me.timeto.app.ui.navigation.LocalNavigationLayer
import me.timeto.app.ui.navigation.picker.NavigationPickerItem
import me.timeto.shared.db.Goal2Db
import me.timeto.shared.db.IntervalDb
import me.timeto.shared.vm.history.form.HistoryFormUtils
import me.timeto.shared.vm.history.form.HistoryFormVm

@Composable
fun HistoryFormFs(
    initIntervalDb: IntervalDb,
) {

    val navigationFs = LocalNavigationFs.current
    val navigationLayer = LocalNavigationLayer.current

    val (vm, state) = rememberVm {
        HistoryFormVm(
            initIntervalDb = initIntervalDb,
        )
    }

    Screen {

        val scrollState = rememberLazyListState()

        Header(
            title = state.title,
            scrollState = scrollState,
            actionButton = HeaderActionButton(
                text = state.doneText,
                isEnabled = true,
                onClick = {
                    vm.save(
                        dialogsManager = navigationFs,
                        onSuccess = {
                            navigationLayer.close()
                        },
                    )
                },
            ),
            cancelButton = HeaderCancelButton(
                text = "Cancel",
                onClick = {
                    navigationLayer.close()
                },
            ),
        )

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f),
        ) {

            item {

                FormPaddingTop()

                FormButton(
                    title = state.goalTitle,
                    isFirst = true,
                    isLast = false,
                    note = state.goalNote,
                    noteColor = c.secondaryText,
                    withArrow = true,
                    onClick = {
                        navigationFs.picker(
                            title = state.goalTitle,
                            items = buildGoalsPickerItems(
                                goalsUi = state.goalsUi,
                                selectedGoalDb = state.goalDb,
                            ),
                            onDone = { newGoal ->
                                vm.setGoal(newGoal.item)
                            },
                        )
                    },
                )

                val daysItemsUi = state.daysItemsUi
                val hoursItemsUi = state.hoursItemsUi
                val minutesItemsUi = state.minutesItemsUi

                val selectedDayIndex: MutableState<Int> = remember {
                    mutableIntStateOf(state.selectedDayIndex)
                }
                val selectedHour: MutableState<Int> = remember {
                    mutableIntStateOf(state.selectedHour)
                }
                val selectedMinute: MutableState<Int> = remember {
                    mutableIntStateOf(state.selectedMinute)
                }

                FormItemView(
                    isFirst = false,
                    isLast = false,
                    modifier = Modifier,
                    content = {
                        Text(
                            text = state.timeNote,
                            color = c.secondaryText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    },
                )

                FormItemView(
                    isFirst = false,
                    isLast = true,
                    modifier = Modifier,
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Days picker
                            AndroidView(
                                modifier = Modifier.weight(1.2f),
                                factory = { context ->
                                    NumberPicker(context).apply {
                                        setOnValueChangedListener { _, _, new ->
                                            selectedDayIndex.value = new
                                            vm.setDayIndex(new)
                                        }
                                        displayedValues = daysItemsUi.map { it.title }.toTypedArray()
                                        if (isSdkQPlus())
                                            textSize = dpToPx(16f).toFloat()
                                        wrapSelectorWheel = false
                                        minValue = 0
                                        maxValue = daysItemsUi.size - 1
                                        value = selectedDayIndex.value
                                    }
                                }
                            )

                            // Hours picker
                            AndroidView(
                                modifier = Modifier.weight(0.8f),
                                factory = { context ->
                                    NumberPicker(context).apply {
                                        setOnValueChangedListener { _, _, new ->
                                            selectedHour.value = new
                                            vm.setHour(new)
                                        }
                                        displayedValues = hoursItemsUi.map { it.title }.toTypedArray()
                                        if (isSdkQPlus())
                                            textSize = dpToPx(18f).toFloat()
                                        wrapSelectorWheel = true
                                        minValue = 0
                                        maxValue = hoursItemsUi.size - 1
                                        value = selectedHour.value
                                    }
                                }
                            )

                            Text(
                                text = ":",
                                color = c.text,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )

                            // Minutes picker
                            AndroidView(
                                modifier = Modifier.weight(0.8f),
                                factory = { context ->
                                    NumberPicker(context).apply {
                                        setOnValueChangedListener { _, _, new ->
                                            selectedMinute.value = new
                                            vm.setMinute(new)
                                        }
                                        displayedValues = minutesItemsUi.map { it.title }.toTypedArray()
                                        if (isSdkQPlus())
                                            textSize = dpToPx(18f).toFloat()
                                        wrapSelectorWheel = true
                                        minValue = 0
                                        maxValue = minutesItemsUi.size - 1
                                        value = selectedMinute.value
                                    }
                                }
                            )
                        }
                    },
                )
            }
        }

        val intervalDb: IntervalDb? = state.initIntervalDb
        if (intervalDb != null) {

            Footer(
                scrollState = scrollState,
                contentModifier = Modifier
                    .padding(horizontal = H_PADDING_HALF),
            ) {

                FooterPlainButton(
                    text = "Delete",
                    color = c.red,
                    onClick = {
                        vm.delete(
                            intervalDb = intervalDb,
                            dialogsManager = navigationFs,
                            onSuccess = {
                                navigationLayer.close()
                            },
                        )
                    },
                )

                SpacerW1()

                FooterPlainButton(
                    text = HistoryFormUtils.moveToTasksTitle,
                    color = c.orange,
                    onClick = {
                        vm.moveToTasks(
                            intervalDb = intervalDb,
                            dialogsManager = navigationFs,
                            onSuccess = {
                                navigationLayer.close()
                            },
                        )
                    },
                )
            }
        }
    }
}

private fun buildGoalsPickerItems(
    goalsUi: List<HistoryFormVm.GoalUi>,
    selectedGoalDb: Goal2Db?,
): List<NavigationPickerItem<Goal2Db>> = goalsUi.map { goalUi ->
    NavigationPickerItem(
        title = goalUi.title,
        isSelected = selectedGoalDb?.id == goalUi.goalDb.id,
        item = goalUi.goalDb,
    )
}
