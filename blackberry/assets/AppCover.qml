import bb.cascades 1.0

SceneCover {
    // The content property must be explicitly specified
    content: Container {
        layout: DockLayout {
        }
        background: Color.Black
        ImageView {
            imageSource: "asset:///images/h4h-logo.png"
            scalingMethod: ScalingMethod.AspectFit
        }
    }
}