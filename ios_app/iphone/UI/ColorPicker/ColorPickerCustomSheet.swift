import SwiftUI
import shared

struct ColorPickerCustomSheet: View {
    
    init(
        initColorRgba: ColorRgba,
        onDone: @escaping (ColorRgba) -> Void
    ) {
        self.onDone = onDone
        let hsl = initColorRgba.toHsl()
        _colorHsl = State(initialValue: ColorHslLocal(
            h: Double(hsl.h),
            s: Double(hsl.s),
            l: Double(hsl.l)
        ))
    }
    
    ///
    
    @State private var colorHsl: ColorHslLocal
    private let onDone: (ColorRgba) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    private var colorRgba: ColorRgba {
        ColorRgba.companion.fromHsl(h: Float(colorHsl.h), s: Float(colorHsl.s), l: Float(colorHsl.l), a: 255)
    }
    
    var body: some View {
        VStack(alignment: .center) {
            Spacer()
            ColorSliderView(
                color: Color(hue: colorHsl.h / 360.0, saturation: 1.0, brightness: 1.0),
                value: $colorHsl.h,
                range: 0...360
            )
            ColorSliderView(
                color: Color(hue: colorHsl.h / 360.0, saturation: colorHsl.s / 100.0, brightness: 0.5 + colorHsl.s / 200.0),
                value: $colorHsl.s,
                range: 0...100
            )
            ColorSliderView(
                color: Color(hue: colorHsl.h / 360.0, saturation: 1.0, brightness: colorHsl.l / 100.0),
                value: $colorHsl.l,
                range: 0...100
            )
            Spacer()
        }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    dismiss()
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button("Done") {
                    onDone(colorRgba)
                    dismiss()
                }
                .fontWeight(.semibold)
            }
            ToolbarItem(placement: .principal) {
                Text(ColorPickerVm.companion.prepCustomColorRgbaText(colorRgba: colorRgba))
                    .font(.system(size: 16))
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(colorRgba.toColor())
                    )
            }
        }
        .onChange(of: colorRgba) { old, new in
            if old != new {
                Haptic.softShot()
            }
        }
        .toolbarTitleDisplayMode(.inline)
        .presentationDetents([.height(220)])
        .interactiveDismissDisabled()
    }
}

///

private struct ColorHslLocal: Equatable {
    var h: Double
    var s: Double
    var l: Double
}

private struct ColorSliderView: View {
    
    let color: Color
    @Binding var value: Double
    var range: ClosedRange<Double>
    
    var body: some View {
        Slider(value: $value, in: range)
            .accentColor(color)
            .padding(.horizontal, H_PADDING)
            .padding(.vertical, 6)
    }
}

