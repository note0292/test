// 学習データ中の TeX 数式を KaTeX で実際に解析し、書き間違いを検出する。
// あわせて、$...$ の外に数式用の文字（^ _ ² Σ α など）が残っていないかを調べる。
// 使い方: npm install --no-save katex && node tools/check-tex.js
const fs = require('fs');
const path = require('path');
const katex = require('katex');

const ASSETS = process.env.ASSETS_DIR || path.join(__dirname, '..', 'app', 'src', 'main', 'assets');
// 数式の外に書いてはいけない文字（上付き・下付き文字、演算子、ギリシャ文字など）
const MATH_CHARS = /[\^_²³¹⁰⁴⁵⁶⁷⁸⁹⁻⁺ⁿᵀᵢⱼₖₙₘₜ₀₁₂₃₄₅₆₇₈₉√Σ∑∫Π∏≤≥≠≈∞→⇒⇔∈∉⊂⊥‖αβγδεζηθικλμνξπρσςτυφχψωΓΔΘΛΞΠΣΦΨΩ̂̄̃]/;

let errors = 0;
let formulas = 0;

function report(where, message) {
  errors++;
  console.error(`${where}: ${message}`);
}

/** 文字列を数式部分とそれ以外に分ける。数式は 1 行の中で閉じていなければならない。 */
function check(where, text, { allowMath = true } = {}) {
  text.split('\n').forEach((line, lineNo) => {
    const at = `${where} (${lineNo + 1} 行目)`;
    let rest = line;
    let plain = '';
    while (rest.length > 0) {
      const i = rest.indexOf('$');
      if (i < 0) { plain += rest; break; }
      plain += rest.slice(0, i);
      const display = rest.startsWith('$$', i);
      const open = display ? '$$' : '$';
      const j = rest.indexOf(open, i + open.length);
      if (j < 0) { report(at, `閉じていない ${open}: ${line}`); return; }
      const tex = rest.slice(i + open.length, j);
      if (!allowMath) report(at, `ここには数式を書けません: ${line}`);
      if (tex.trim() === '') report(at, `空の数式: ${line}`);
      try {
        katex.renderToString(tex, { displayMode: display, throwOnError: true, strict: 'error' });
        formulas++;
      } catch (e) {
        report(at, `${e.message}\n    数式: ${tex}`);
      }
      rest = rest.slice(j + open.length);
    }
    const m = plain.match(MATH_CHARS);
    if (m) report(at, `数式の外に数式用の文字「${m[0]}」があります: ${line}`);
  });
}

const questions = JSON.parse(fs.readFileSync(path.join(ASSETS, 'questions.json'), 'utf8')).questions;
for (const q of questions) {
  check(`${q.id} 問題文`, q.question);
  q.choices.forEach((c, i) => check(`${q.id} 選択肢${i + 1}`, c));
  check(`${q.id} 解説`, q.explanation);
}

const dir = path.join(ASSETS, 'textbook');
for (const file of fs.readdirSync(dir).filter((f) => f.endsWith('.json')).sort()) {
  const ch = JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8'));
  // 章の導入とレッスン名は画面上で数式として描画しない場所に表示される
  check(`${file} 導入`, ch.intro, { allowMath: false });
  for (const l of ch.lessons) {
    check(`${l.id} タイトル`, l.title, { allowMath: false });
    check(`${l.id} 本文`, l.body);
    for (const t of l.theorems || []) {
      check(`${l.id} 定理「${t.title}」タイトル`, t.title);
      check(`${l.id} 定理「${t.title}」`, t.statement);
      check(`${l.id} 定理「${t.title}」証明`, t.proof);
    }
    (l.examples || []).forEach((e, i) => {
      check(`${l.id} 例題${i + 1}`, e.question);
      check(`${l.id} 例題${i + 1} 解答`, e.solution);
    });
  }
  for (const f of ch.formulas || []) {
    check(`${file} 公式「${f.title}」タイトル`, f.title);
    check(`${file} 公式「${f.title}」`, f.body);
  }
}

console.log(`数式 ${formulas} 個を確認しました。エラー ${errors} 件。`);
process.exit(errors > 0 ? 1 : 0);
