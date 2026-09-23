package com.note0292.statprep.data

data class Formula(val title: String, val body: String)

/** 分野ごとの要点・公式まとめ。 */
object FormulaNotes {
    val notes: Map<Category, List<Formula>> = mapOf(
        Category.PROBABILITY to listOf(
            Formula("主な離散分布", "Bin(n,p): 平均 np, 分散 np(1−p)\nPo(λ): 平均 λ, 分散 λ\nGeo(p)（試行回数）: 平均 1/p, 分散 (1−p)/p²\nNB(r,p)（失敗回数）: 平均 r(1−p)/p, 分散 r(1−p)/p²"),
            Formula("主な連続分布", "Exp(λ): 平均 1/λ, 分散 1/λ²\nGa(α,β)（率 β）: 平均 α/β, 分散 α/β²\nBe(a,b): 平均 a/(a+b), 分散 ab/((a+b)²(a+b+1))\nU(a,b): 平均 (a+b)/2, 分散 (b−a)²/12"),
            Formula("積率母関数", "Bin: (1−p+pe^t)^n\nPo: exp(λ(e^t−1))\nN(μ,σ²): exp(μt + σ²t²/2)\nExp(λ): λ/(λ−t)\nGa(α,β): (β/(β−t))^α"),
            Formula("条件付き期待値・分散", "E[Y] = E[E[Y|X]]\nVar(Y) = E[Var(Y|X)] + Var(E[Y|X])"),
            Formula("2 変量正規分布", "Y|X=x ~ N(μy + ρ(σy/σx)(x−μx), σy²(1−ρ²))"),
            Formula("変数変換", "Y = g(X)（単調）: f_Y(y) = f_X(g⁻¹(y))·|dg⁻¹(y)/dy|\n多変量ではヤコビアンの絶対値をかける"),
            Formula("順序統計量（U(0,1), n 個）", "k 番目 ~ Be(k, n−k+1)\n最大値の期待値 n/(n+1), 最小値 1/(n+1)"),
        ),
        Category.ESTIMATION to listOf(
            Formula("フィッシャー情報量", "I(θ) = E[(∂/∂θ log f(X;θ))²] = −E[∂²/∂θ² log f(X;θ)]\nクラメール・ラオ下限: Var(θ̂) ≥ 1/(nI(θ))"),
            Formula("代表的な MLE", "N(μ,σ²): μ̂ = X̄, σ̂² = (1/n)Σ(Xᵢ−X̄)²\nPo(λ): λ̂ = X̄\nExp(λ): λ̂ = 1/X̄\nU(0,θ): θ̂ = max Xᵢ"),
            Formula("MLE の漸近正規性", "√n(θ̂ − θ) →d N(0, 1/I(θ))"),
            Formula("デルタ法", "√n(g(θ̂) − g(θ)) →d N(0, g'(θ)²/I(θ))"),
            Formula("MSE", "MSE(θ̂) = Var(θ̂) + (E[θ̂] − θ)²"),
            Formula("十分統計量", "分解定理: f(x;θ) = g(T(x);θ)·h(x)\nラオ・ブラックウェル: E[θ̃|T] は分散を改善"),
            Formula("区間推定", "平均（σ未知）: X̄ ± t_{α/2}(n−1)·s/√n\n分散: [(n−1)s²/χ²_{α/2}(n−1), (n−1)s²/χ²_{1−α/2}(n−1)]\n比率: p̂ ± z_{α/2}√(p̂(1−p̂)/n)"),
        ),
        Category.TESTING to listOf(
            Formula("過誤と検出力", "第1種の過誤 α: H₀ 真なのに棄却\n第2種の過誤 β: H₀ 偽なのに受容\n検出力 = 1 − β"),
            Formula("尤度比検定", "Λ = sup_{H₀} L / sup L\n−2 log Λ →d χ²(制約数)（ウィルクスの定理）"),
            Formula("ワルド・スコア検定", "ワルド: (θ̂ − θ₀)²·nI(θ̂) → χ²(1)\nスコア: U(θ₀)²/(nI(θ₀)) → χ²(1)"),
            Formula("2 標本 t 検定", "等分散: t = (X̄−Ȳ)/(s_p√(1/m+1/n)) ~ t(m+n−2)\ns_p² = ((m−1)s₁² + (n−1)s₂²)/(m+n−2)\n不等分散: ウェルチの近似自由度"),
            Formula("F 検定（等分散）", "F = s₁²/s₂² ~ F(m−1, n−1)"),
            Formula("多重比較", "ボンフェローニ: 各検定を α/m\nホルム: p値を昇順に並べ α/(m−k+1) と比較\nBH法: FDR を制御"),
        ),
        Category.REGRESSION to listOf(
            Formula("最小二乗推定", "β̂ = (XᵀX)⁻¹Xᵀy\nVar(β̂) = σ²(XᵀX)⁻¹\nσ̂² = RSS/(n−p−1)"),
            Formula("決定係数", "R² = 1 − RSS/TSS\nR̄² = 1 − (1−R²)(n−1)/(n−p−1)"),
            Formula("係数の検定", "t = β̂ⱼ/SE(β̂ⱼ) ~ t(n−p−1)\n全体の F = (ESS/p)/(RSS/(n−p−1)) ~ F(p, n−p−1)"),
            Formula("情報量規準", "AIC = −2log L̂ + 2k\nBIC = −2log L̂ + k log n"),
            Formula("診断", "ハット行列 H = X(XᵀX)⁻¹Xᵀ, tr(H) = p+1\nVIFⱼ = 1/(1−Rⱼ²)\nDW ≈ 2(1−ρ̂)\nクックの距離で影響の大きい観測を検出"),
            Formula("一般化線形モデル", "ロジスティック: log(p/(1−p)) = xᵀβ, オッズ比 e^β\nポアソン: log μ = xᵀβ\n逸脱度 D = 2(ℓ_飽和 − ℓ_モデル)"),
            Formula("罰則付き回帰", "リッジ: β̂ = (XᵀX + λI)⁻¹Xᵀy\nLasso: RSS + λΣ|βⱼ|（スパース解）"),
        ),
        Category.MULTIVARIATE to listOf(
            Formula("主成分分析", "Σ の固有値 λ₁≥…≥λₚ と固有ベクトル\n第k主成分の分散 = λₖ\n寄与率 = λₖ/Σλⱼ\n相関行列なら Σλⱼ = p"),
            Formula("因子分析", "x = Λf + ε\nΣ = ΛΛᵀ + Ψ\n共通性 + 独自性 = 1（標準化時）\n回転: バリマックス（直交）、プロマックス（斜交）"),
            Formula("マハラノビス距離", "D² = (x−μ)ᵀΣ⁻¹(x−μ)\n多変量正規なら D² ~ χ²(p)"),
            Formula("判別分析", "フィッシャー: w ∝ S_W⁻¹(x̄₁ − x̄₂)\n等共分散 → 線形判別、異なる共分散 → 2次判別"),
            Formula("クラスター分析", "階層的: 最短距離法・最長距離法・群平均法・ウォード法\n非階層的: k-means（初期値依存）"),
            Formula("その他", "正準相関分析: 2群の線形結合の相関を最大化\nMDS: 距離行列から座標を復元\n対応分析: 分割表の行・列を同時布置"),
        ),
        Category.ANOVA to listOf(
            Formula("一元配置", "S_T = S_A + S_E\n自由度: (N−1) = (k−1) + (N−k)\nF = V_A/V_E ~ F(k−1, N−k)"),
            Formula("二元配置（繰り返し r）", "A: a−1, B: b−1, A×B: (a−1)(b−1)\n誤差: ab(r−1), 全体: abr−1"),
            Formula("実験計画の 3 原則", "反復・無作為化・局所管理"),
            Formula("代表的な計画", "乱塊法: ブロック内で無作為化\nラテン方格法: 行・列の 2 方向でブロック化\n直交表 L₈(2⁷): 8 回で 2 水準因子を最大 7 つ\n分割法: 水準変更が難しい因子を 1 次単位に"),
            Formula("多重比較", "テューキー: 全対比較\nダネット: 対照群との比較\nシェフェ: 任意の対比"),
        ),
        Category.CATEGORICAL to listOf(
            Formula("カイ二乗検定", "χ² = Σ(O−E)²/E\n独立性（r×c）: 自由度 (r−1)(c−1)\n適合度（k 区分, 推定母数 m）: k−1−m"),
            Formula("2×2 表の指標", "オッズ比 = ad/(bc)\nSE(log OR) = √(1/a+1/b+1/c+1/d)\nリスク比 = (a/(a+b))/(c/(c+d))"),
            Formula("正確検定・対応あり", "フィッシャーの正確検定: 超幾何分布\nマクネマー検定: (b−c)²/(b+c) ~ χ²(1)"),
            Formula("交絡の調整", "シンプソンのパラドックス\nマンテル・ヘンツェル推定量で層別に統合"),
            Formula("対数線形モデル", "独立: log μᵢⱼ = λ + λᵢᴬ + λⱼᴮ\n飽和: + λᵢⱼᴬᴮ\nG² = 2ΣO log(O/E)"),
        ),
        Category.TIME_SERIES to listOf(
            Formula("弱定常", "E[Xₜ] 一定、Cov(Xₜ, Xₜ₊ₖ) = γ(k) はラグのみに依存"),
            Formula("AR(1)", "Xₜ = φXₜ₋₁ + εₜ, |φ|<1 で定常\nγ(0) = σ²/(1−φ²), ρ(k) = φᵏ"),
            Formula("MA(1)", "Xₜ = εₜ + θεₜ₋₁\nγ(0) = σ²(1+θ²), ρ(1) = θ/(1+θ²), ρ(k≥2) = 0\n反転可能: |θ|<1"),
            Formula("次数同定", "AR(p): PACF がラグ p で切断\nMA(q): ACF がラグ q で切断\nARMA: どちらも減衰"),
            Formula("非定常", "ランダムウォーク: Var(Xₜ) = tσ²\n差分で定常化 → ARIMA(p,d,q)\n単位根検定: ADF 検定"),
            Formula("AR(2) の定常条件", "φ₁ + φ₂ < 1, φ₂ − φ₁ < 1, |φ₂| < 1\n（特性方程式 1 − φ₁z − φ₂z² = 0 の根が単位円の外）"),
        ),
        Category.BAYES to listOf(
            Formula("ベイズの定理", "π(θ|x) ∝ f(x|θ)π(θ)"),
            Formula("共役事前分布", "二項 × Be(a,b) → Be(a+x, b+n−x)\nポアソン × Ga(α,β) → Ga(α+Σx, β+n)\n正規（σ²既知）× N(μ₀,τ²) → 正規\n  事後精度 = 1/τ² + n/σ²\n  事後平均 = (μ₀/τ² + nX̄/σ²)/(1/τ² + n/σ²)"),
            Formula("ベイズ推定量", "2乗損失 → 事後平均\n絶対損失 → 事後中央値\n0-1 損失 → 事後最頻値（MAP）"),
            Formula("事前分布", "ジェフリーズ: π(θ) ∝ √I(θ)（変換不変）\n非正則事前分布でも事後が正則なら使える"),
            Formula("モデル比較", "ベイズファクター B₁₀ = p(x|M₁)/p(x|M₀)\n予測分布 p(x̃|x) = ∫f(x̃|θ)π(θ|x)dθ"),
        ),
        Category.NONPARAMETRIC to listOf(
            Formula("検定の使い分け", "独立2群: ウィルコクソン順位和（マン・ホイットニー U）\n対応2群: 符号付き順位検定、符号検定\n独立k群: クラスカル・ウォリス\n対応k群: フリードマン"),
            Formula("順位相関", "スピアマン: ρ = 1 − 6Σd²/(n(n²−1))\nケンドール: τ = (一致対 − 不一致対)/(n(n−1)/2)"),
            Formula("分布の検定", "KS 検定: sup|Fₙ(x) − F₀(x)|"),
            Formula("リサンプリング", "ブートストラップ: 復元抽出で分布を近似\nジャックナイフ: 1個ずつ除く\n並べ替え検定: ラベルを並べ替えて帰無分布を作る"),
        ),
        Category.MARKOV to listOf(
            Formula("マルコフ連鎖", "定常分布: πP = π, Σπᵢ = 1\n2状態（a = P(1→2), b = P(2→1)）: π = (b/(a+b), a/(a+b))\n詳細つり合い: πᵢPᵢⱼ = πⱼPⱼᵢ"),
            Formula("ポアソン過程", "N(t) ~ Po(λt)\n到着間隔 ~ Exp(λ)\nk 回目の到着時刻 ~ Ga(k, λ)"),
            Formula("乱数生成", "逆関数法: X = F⁻¹(U)\n例: X = −log(U)/λ ~ Exp(λ)\n棄却法: f(x) ≤ Mg(x) となる g から生成し、確率 f/(Mg) で受理\nボックス・ミュラー法で正規乱数"),
            Formula("MCMC", "MH 法の受理確率: min{1, π(y)q(x|y)/(π(x)q(y|x))}\nギブス: 完全条件付き分布から順に生成\nバーンイン・自己相関・R̂ で収束診断"),
            Formula("モンテカルロ誤差", "標準誤差 σ/√n\n分散減少法: 重点サンプリング、制御変量、対称変量"),
        ),
        Category.SAMPLING to listOf(
            Formula("単純無作為抽出", "Var(ȳ) = (1 − n/N)S²/n\n(1 − n/N): 有限母集団修正"),
            Formula("層別抽出", "比例配分: nₕ ∝ Nₕ\nネイマン配分: nₕ ∝ NₕSₕ\n費用考慮: nₕ ∝ NₕSₕ/√cₕ"),
            Formula("集落抽出", "デザイン効果 ≈ 1 + (M−1)ρ（ρ: 級内相関）"),
            Formula("不等確率抽出", "ホルヴィッツ・トンプソン推定量: Ŷ = Σ yᵢ/πᵢ"),
            Formula("欠測データ", "MCAR: 完全にランダム\nMAR: 観測値のみに依存\nMNAR: 欠測値自体に依存\n多重代入: T = W + (1 + 1/m)B（ルービンのルール）"),
        ),
    )
}
