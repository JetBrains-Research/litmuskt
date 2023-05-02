// returns true if samples have the same distribution with 0.05 significance level
// written almost completely by ChatGPT :)
fun chiSquaredTest(sample1: List<Int>, sample2: List<Int>): Boolean {
    require(sample1.size == sample2.size) { "incomparable samples" }
    val n = sample1.size // number of categories
    val dof = n - 1 // degrees of freedom
    val obs = Array(2) { IntArray(n) } // observed frequencies

    // fill in observed frequencies
    for (i in 0 until n) {
        obs[0][i] = sample1[i]
        obs[1][i] = sample2[i]
    }

    // calculate expected frequencies
    val total1 = sample1.sum()
    val total2 = sample2.sum()
    val exp = Array(2) { DoubleArray(n) }
    for (i in 0 until n) {
        exp[0][i] = total1.toDouble() * (sample1[i] + sample2[i]) / (total1 + total2)
        exp[1][i] = total2.toDouble() * (sample1[i] + sample2[i]) / (total1 + total2)
    }

    // calculate chi-squared test statistic
    var chiSq = 0.0
    for (i in 0 until n) {
        for (j in 0 until 2) {
            val o = obs[j][i].toDouble()
            val e = exp[j][i]
            chiSq += (o - e) * (o - e) / e
        }
    }

    val targetChiSq = chiSquaredDist[dof]
    return chiSq < targetChiSq
}

fun chiSquaredTest(samples: List<List<Int>>): Boolean {
    val k = samples.size // number of samples
    val n = samples[0].size // number of categories
    require(samples.all { it.size == n }) { "incomparable samples" }
    val dof = (k - 1) * (n - 1) // degrees of freedom

    // calculate expected frequencies
    val totalTotal = samples.flatten().sum()
    val totalCol = List(k) { samples[it].sum() }
    val totalRow = List(n) { i -> samples.sumOf { it[i] } }
    val exp = Array(k) { DoubleArray(n) }
    for (i in 0 until k) {
        for (j in 0 until n) {
            exp[i][j] = 1.0 * totalCol[i] * totalRow[j] / totalTotal
        }
    }

    // calculate chi-squared test statistic
    var chiSq = 0.0
    for (i in 0 until k) {
        for (j in 0 until n) {
            val o = samples[i][j].toDouble()
            val e = exp[i][j]
            chiSq += (o - e) * (o - e) / e
        }
    }

    val targetChiSq = chiSquaredDist[dof]
    return chiSq < targetChiSq
}

// chi-squared cdf with significance = 0.05
private val chiSquaredDist: List<Double> = listOf(
        0.0,
        3.841,
        5.991,
        7.815,
        9.488,
        11.07,
        12.59,
        14.07,
        15.51,
        16.92,
        18.31,
        19.68,
        21.03,
        22.36,
        23.68,
        25.0,
        26.3,
        27.59,
        28.87,
        30.14,
        31.41,
        32.67,
        33.92,
        35.17,
        36.42,
        37.65,
        38.89,
        40.11,
        41.34,
        42.56,
        43.77,
        44.99,
        46.19,
        47.4,
        48.6,
        49.8,
        51.0,
        52.19,
        53.38,
        54.57,
)
