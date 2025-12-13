import SwiftUI
import shared

struct SummaryLineChartView: View {
    
    let goalsUi: [SummaryVm.GoalUi]
    let daysBarsUi: [DayBarsUi]
    
    var body: some View {
        VmView({
            SummaryLineChartVm(
                goalsUi: goalsUi,
                initialDaysBarsUi: daysBarsUi
            )
        }) { vm, state in
            SummaryLineChartViewInner(
                vm: vm,
                state: state
            )
        }
    }
}

private struct SummaryLineChartViewInner: View {
    
    let vm: SummaryLineChartVm
    let state: SummaryLineChartVm.State
    
    var body: some View {
        
        VStack {
            
            // Time unit selector
            TimeUnitSelector(
                currentUnit: state.timeUnit,
                onUnitSelected: { unit in
                    vm.setTimeUnit(unit: unit)
                }
            )
            
            // Line Chart
            if !state.linesData.isEmpty {
                LineChartView(
                    linesData: state.linesData,
                    maxSeconds: state.maxSecondsOverall,
                    xLabels: state.xLabels
                )
                .frame(height: 220)
                .padding(.horizontal, 8)
                .padding(.vertical, 16)
            } else {
                VStack {
                    Text("Select activities to display")
                        .foregroundColor(.secondary)
                        .font(.system(size: 14))
                }
                .frame(height: 200)
            }
            
            // Goal selection
            Text("Select Activities")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(Color(.label))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            
            GoalSelectionList(
                selections: state.goalSelections,
                onToggle: { goalId in
                    vm.toggleGoalSelection(goalId: goalId)
                }
            )
            
            Spacer()
                .frame(height: 20)
        }
        .background(.background)
    }
}

private struct TimeUnitSelector: View {
    
    let currentUnit: SummaryLineChartVm.TimeUnit
    let onUnitSelected: (SummaryLineChartVm.TimeUnit) -> Void
    
    var body: some View {
        HStack {
            ForEach(SummaryLineChartVm.TimeUnit.entries, id: \.self) { unit in
                let isSelected = unit == currentUnit
                
                Button(action: {
                    onUnitSelected(unit)
                }) {
                    Text(unit.title)
                        .font(.system(size: 14, weight: isSelected ? .medium : .regular))
                        .foregroundColor(isSelected ? .white : Color(.label))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(isSelected ? Color.blue : Color.clear)
                        )
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

private struct LineChartView: View {
    
    let linesData: [SummaryLineChartVm.LineData]
    let maxSeconds: Int32
    let xLabels: [String]
    
    private let pointSpacing: CGFloat = 50
    private let labelWidth: CGFloat = 40
    
    var body: some View {
        // Calculate nice Y-axis ticks (round numbers)
        let yTicks = SummaryLineChartVm.companion.calculateYAxisTicks(maxSeconds: maxSeconds, tickCount: 5)
        let adjustedMaxSeconds = yTicks.last?.int32Value ?? 3600
        
        let pointCount = xLabels.count
        let totalWidth = pointCount > 1 ? labelWidth + pointSpacing * CGFloat(pointCount - 1) : labelWidth
        
        VStack {
            // Y-axis labels and chart area
            HStack(alignment: .top, spacing: 0) {
                // Y-axis labels - use GeometryReader for precise positioning
                GeometryReader { geo in
                    let height = geo.size.height
                    ZStack(alignment: .topTrailing) {
                        ForEach(yTicks, id: \.self) { tickValue in
                            let fraction = CGFloat(tickValue.int32Value) / CGFloat(adjustedMaxSeconds)
                            // Position from bottom: fraction=0 at bottom, fraction=1 at top
                            let offsetFromTop = height * (1 - fraction)
                            Text(SummaryLineChartVm.companion.formatSeconds(seconds: tickValue.int32Value))
                                .foregroundColor(.secondary)
                                .font(.system(size: 10, weight: .light))
                                .position(x: 20, y: offsetFromTop)
                        }
                    }
                }
                .frame(width: 45)
                
                // Chart canvas
                GeometryReader { geometry in
                    ScrollViewReader { scrollProxy in
                        ScrollView(.horizontal, showsIndicators: false) {
                            VStack(spacing: 4) {
                                // Chart canvas
                                Canvas { context, size in
                                    let canvasHeight = size.height
                                    let canvasWidth = size.width
                                    
                                    guard pointCount > 0 else { return }
                                    
                                    // Draw horizontal grid lines at each Y-axis tick
                                    let gridColor = Color.gray.opacity(0.3)
                                    for tickValue in yTicks {
                                        let y = canvasHeight - (CGFloat(tickValue.int32Value) / CGFloat(adjustedMaxSeconds) * canvasHeight)
                                        var path = Path()
                                        path.move(to: CGPoint(x: 0, y: y))
                                        path.addLine(to: CGPoint(x: canvasWidth, y: y))
                                        context.stroke(path, with: .color(gridColor), lineWidth: 1)
                                    }
                                    
                                    // Draw lines for each goal
                                    for lineData in linesData {
                                        guard !lineData.points.isEmpty else { continue }
                                        
                                        let color = lineData.color.toColor()
                                        var linePath = Path()
                                        
                                        for (index, point) in lineData.points.enumerated() {
                                            let x = labelWidth / 2 + pointSpacing * CGFloat(index)
                                            let y = canvasHeight - (CGFloat(point.seconds) / CGFloat(adjustedMaxSeconds) * canvasHeight)
                                            
                                            if index == 0 {
                                                linePath.move(to: CGPoint(x: x, y: y))
                                            } else {
                                                linePath.addLine(to: CGPoint(x: x, y: y))
                                            }
                                        }
                                        
                                        // Draw the line
                                        context.stroke(linePath, with: .color(color), style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
                                        
                                        // Draw points
                                        for (index, point) in lineData.points.enumerated() {
                                            let x = labelWidth / 2 + pointSpacing * CGFloat(index)
                                            let y = canvasHeight - (CGFloat(point.seconds) / CGFloat(adjustedMaxSeconds) * canvasHeight)
                                            
                                            let pointPath = Path(ellipseIn: CGRect(x: x - 4, y: y - 4, width: 8, height: 8))
                                            context.fill(pointPath, with: .color(color))
                                        }
                                    }
                                }
                                .frame(width: totalWidth, height: geometry.size.height - 20)
                                
                                // X-axis labels (inside same ScrollView for sync)
                                HStack(spacing: 0) {
                                    ForEach(Array(xLabels.enumerated()), id: \.offset) { index, label in
                                        Text(label)
                                            .font(.system(size: 9, weight: .light))
                                            .foregroundColor(.secondary)
                                            .frame(width: labelWidth)
                                        
                                        if index < xLabels.count - 1 {
                                            Spacer()
                                                .frame(width: pointSpacing - labelWidth)
                                        }
                                    }
                                }
                                .frame(width: totalWidth)
                            }
                        }
                    }
                }
            }
        }
    }
}

private struct GoalSelectionList: View {
    
    let selections: [SummaryLineChartVm.GoalSelectionUi]
    let onToggle: (Int32) -> Void
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(selections, id: \.goalDb.id) { selection in
                    GoalSelectionItem(
                        selection: selection,
                        onToggle: { onToggle(selection.goalDb.id) }
                    )
                }
            }
            .padding(.horizontal, 8)
        }
    }
}

private struct GoalSelectionItem: View {
    
    let selection: SummaryLineChartVm.GoalSelectionUi
    let onToggle: () -> Void
    
    var body: some View {
        let backgroundColor = selection.isSelected
            ? selection.color.toColor().opacity(0.3)
            : Color(.systemGray5)
        
        Button(action: onToggle) {
            HStack {
                Circle()
                    .fill(selection.color.toColor())
                    .frame(width: 12, height: 12)
                
                Text(selection.title)
                    .font(.system(size: 13, weight: selection.isSelected ? .medium : .regular))
                    .foregroundColor(Color(.label))
                    .lineLimit(1)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(backgroundColor)
            )
        }
    }
}
