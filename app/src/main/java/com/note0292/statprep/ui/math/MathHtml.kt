package com.note0292.statprep.ui.math

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * TeX 記法（インライン `$...$`、ディスプレイ `$$...$$`）を含むテキストを
 * 同梱の KaTeX で描画する HTML を組み立てる。
 */
object MathHtml {
    const val BASE_URL = "file:///android_asset/"

    /** 本文用のエスケープ。改行は <br> にする（数式は 1 行の中で閉じている前提）。 */
    fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br>")

    private fun css(color: Color): String {
        val argb = color.toArgb()
        return "rgba(${(argb shr 16) and 0xFF},${(argb shr 8) and 0xFF},${argb and 0xFF},${((argb ushr 24) / 255f)})"
    }

    private fun style(colors: ColorScheme, fontSizePx: Int): String = """
        :root {
          --fg: ${css(colors.onSurface)};
          --muted: ${css(colors.onSurfaceVariant)};
          --primary: ${css(colors.primary)};
          --thm-bg: ${css(colors.primaryContainer)};
          --thm-fg: ${css(colors.onPrimaryContainer)};
          --ex-bg: ${css(colors.tertiaryContainer)};
          --ex-fg: ${css(colors.onTertiaryContainer)};
          --line: ${css(colors.outlineVariant)};
        }
        html, body { margin: 0; padding: 0; background: transparent; }
        body {
          color: var(--fg); font-size: ${fontSizePx}px; line-height: 1.7;
          font-family: sans-serif; overflow-wrap: anywhere; -webkit-text-size-adjust: 100%;
        }
        .katex { font-size: 1.08em; }
        .katex-display { margin: 0.5em 0; overflow-x: auto; overflow-y: hidden; padding: 2px 0; }
        h1 { font-size: 1.3em; margin: 0 0 0.6em; }
        .card { border-radius: 12px; padding: 12px 14px; margin: 14px 0; }
        .thm { background: var(--thm-bg); color: var(--thm-fg); }
        .ex { background: var(--ex-bg); color: var(--ex-fg); }
        .label { font-weight: bold; margin-bottom: 6px; }
        details { margin-top: 8px; }
        summary { color: var(--primary); font-weight: bold; cursor: pointer; padding: 6px 0; }
        details > div { border-top: 1px solid var(--line); padding-top: 8px; }
        .qed { float: right; }
        .formula-title { color: var(--primary); font-weight: bold; margin-top: 10px; }
        hr { border: none; border-top: 1px solid var(--line); }
    """.trimIndent()

    private const val RENDER_SCRIPT = """
        renderMathInElement(document.body, {
          delimiters: [
            {left: '$$', right: '$$', display: true},
            {left: '$', right: '$', display: false}
          ],
          throwOnError: false
        });
        function reportHeight() {
          if (window.MathBridge) MathBridge.onHeight(Math.ceil(document.documentElement.getBoundingClientRect().height));
        }
        reportHeight();
        window.addEventListener('load', reportHeight);
        if (window.ResizeObserver) new ResizeObserver(reportHeight).observe(document.body);
        document.addEventListener('toggle', reportHeight, true);
    """

    /** [bodyHtml] はエスケープ済みの HTML 断片。 */
    fun page(bodyHtml: String, colors: ColorScheme, fontSizePx: Int = 16): String = """
        <!DOCTYPE html>
        <html><head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="katex/katex.min.css">
        <script src="katex/katex.min.js"></script>
        <script src="katex/auto-render.min.js"></script>
        <style>${style(colors, fontSizePx)}</style>
        </head><body>$bodyHtml
        <script>$RENDER_SCRIPT</script>
        </body></html>
    """.trimIndent()
}
