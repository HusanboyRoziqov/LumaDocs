package app.lumadocs.kmp.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val VISIBLE_SECONDS = 2.0
private const val FADE_SECONDS = 0.3

/**
 * iOS has no system toast, so this mimics Android's: a short, non-blocking message that
 * fades out on its own and never intercepts touches.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun showToast(message: String) {
    dispatch_async(dispatch_get_main_queue()) {
        val window = UIApplication.sharedApplication.keyWindow ?: return@dispatch_async

        val label = UILabel().apply {
            text = message
            textColor = UIColor.whiteColor
            backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.8)
            textAlignment = NSTextAlignmentCenter
            font = UIFont.systemFontOfSize(14.0)
            numberOfLines = 0
            alpha = 0.0
            layer.cornerRadius = 14.0
            layer.masksToBounds = true
            // Purely informational: never swallow taps meant for the UI underneath.
            userInteractionEnabled = false
        }

        val (windowWidth, windowHeight) = window.bounds.useContents { size.width to size.height }
        val maxWidth = windowWidth - 80.0
        val textSize = label.sizeThatFits(CGSizeMake(maxWidth, Double.MAX_VALUE))
        val width = textSize.useContents { width } + 32.0
        val height = textSize.useContents { height } + 20.0

        label.setFrame(
            CGRectMake(
                (windowWidth - width) / 2.0,
                windowHeight - height - 96.0,
                width,
                height,
            ),
        )
        window.addSubview(label)

        UIView.animateWithDuration(
            duration = FADE_SECONDS,
            animations = { label.alpha = 1.0 },
            completion = {
                UIView.animateWithDuration(
                    duration = FADE_SECONDS,
                    delay = VISIBLE_SECONDS,
                    options = 0u,
                    animations = { label.alpha = 0.0 },
                    completion = { label.removeFromSuperview() },
                )
            },
        )
    }
}
