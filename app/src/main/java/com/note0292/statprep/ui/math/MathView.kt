package com.note0292.statprep.ui.math

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** 描画後の高さを JavaScript から受け取るためのブリッジ。 */
private class HeightBridge(private val onHeight: (Int) -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onHeight(px: Int) {
        main.post { onHeight(px) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(context: android.content.Context): WebView = WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.allowFileAccess = true
    settings.builtInZoomControls = false
    setBackgroundColor(Color.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
}

/**
 * TeX を含む短いテキスト（問題文・選択肢・解説など）を描画する。
 * 周囲の Compose レイアウトに合わせて、内容の高さに自動で伸縮する。
 */
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    fontSizePx: Int = 16,
) {
    MathBlock(MathHtml.escape(text), modifier, fontSizePx)
}

/** エスケープ済みの HTML 断片を、内容の高さに合わせて描画する。 */
@Composable
fun MathBlock(
    bodyHtml: String,
    modifier: Modifier = Modifier,
    fontSizePx: Int = 16,
) {
    val colors = MaterialTheme.colorScheme
    var heightDp by remember { mutableIntStateOf(0) }
    val html = remember(bodyHtml, colors, fontSizePx) { MathHtml.page(bodyHtml, colors, fontSizePx) }
    AndroidView(
        factory = { context ->
            createWebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addJavascriptInterface(HeightBridge { heightDp = it }, "MathBridge")
            }
        },
        update = { view ->
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(MathHtml.BASE_URL, html, "text/html", "utf-8", null)
            }
        },
        // 描画完了までは 1 行分の高さを確保してちらつきを抑える
        modifier = modifier.fillMaxWidth().height(if (heightDp > 0) heightDp.dp else (fontSizePx * 1.7f).dp),
    )
}

/** レッスン本文のように、画面全体をスクロールする長い HTML を描画する。 */
@Composable
fun MathPage(
    bodyHtml: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val html = remember(bodyHtml, colors) { MathHtml.page(bodyHtml, colors) }
    AndroidView(
        factory = { context -> createWebView(context).apply { isVerticalScrollBarEnabled = true } },
        update = { view ->
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(MathHtml.BASE_URL, html, "text/html", "utf-8", null)
            }
        },
        modifier = modifier,
    )
}
