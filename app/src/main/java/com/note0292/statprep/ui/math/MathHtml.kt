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
        #content { display: flow-root; padding: 4px 0; }
        .katex-display { margin: 0.5em 0; overflow-x: auto; overflow-y: hidden; padding: 0.25em 0; }
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
        renderMathInElement(document.getElementById('content'), {
          delimiters: [
            {left: '$$', right: '$$', display: true},
            {left: '$', right: '$', display: false}
          ],
          throwOnError: false
        });
        var content = document.getElementById('content');
        // 画面幅に収まらない数式は、はみ出した分だけ文字を小さくして全体を表示する
        // （選択肢はタップ用の層が重なっていて横スクロールできないため）
        function fitMath() {
          var nodes = content.querySelectorAll('.katex');
          for (var i = 0; i < nodes.length; i++) nodes[i].style.fontSize = '';
          for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            var box = el.parentElement.classList.contains('katex-display') ? el.parentElement : content;
            var avail = box.clientWidth;
            var width = el.getBoundingClientRect().width;
            if (avail > 0 && width > avail) {
              el.style.fontSize = (1.08 * Math.max(0.5, (avail - 2) / width)) + 'em';
            }
          }
        }
        var lastHeight = -1;
        // 分数などは行の枠からはみ出して描かれることがあるので、実際に描かれる文字・線の上下端まで測る
        // （KaTeX の位置合わせ用の透明な要素は大きくはみ出すので対象外）
        function inkBounds() {
          var box = content.getBoundingClientRect();
          var top = box.top, bottom = box.bottom;
          function add(r) {
            if (r.width > 0 && r.height > 0) { top = Math.min(top, r.top); bottom = Math.max(bottom, r.bottom); }
          }
          var walker = document.createTreeWalker(content, NodeFilter.SHOW_TEXT);
          var range = document.createRange();
          while (walker.nextNode()) {
            var node = walker.currentNode;
            if (!node.nodeValue.trim() || node.parentElement.closest('.katex-mathml')) continue;
            range.selectNodeContents(node);
            var rects = range.getClientRects();
            for (var i = 0; i < rects.length; i++) add(rects[i]);
          }
          var lines = content.querySelectorAll('.katex-html svg, .katex-html .frac-line, .katex-html .hline, .katex-html .overline-line, .katex-html .underline-line');
          for (var i = 0; i < lines.length; i++) add(lines[i].getBoundingClientRect());
          return { top: top + window.scrollY, bottom: bottom + window.scrollY };
        }
        function reportHeight() {
          if (!window.MathBridge) return;
          content.style.paddingTop = '';
          var ink = inkBounds();
          // 1 行目の分数の分子が上に出る場合は、その分だけ上に余白を足す
          if (ink.top < 0) {
            content.style.paddingTop = Math.ceil(4 - ink.top) + 'px';
            ink = inkBounds();
          }
          var h = Math.ceil(ink.bottom) + 4;
          if (h !== lastHeight) { lastHeight = h; MathBridge.onHeight(h); }
        }
        function update() { fitMath(); reportHeight(); }
        update();
        window.addEventListener('load', update);
        window.addEventListener('resize', update);
        if (document.fonts && document.fonts.ready) document.fonts.ready.then(update);
        [100, 400, 1000, 2000].forEach(function (t) { setTimeout(update, t); });
        if (window.ResizeObserver) new ResizeObserver(reportHeight).observe(content);
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
        </head><body><div id="content">$bodyHtml</div>
        <script>$RENDER_SCRIPT</script>
        </body></html>
    """.trimIndent()
}
