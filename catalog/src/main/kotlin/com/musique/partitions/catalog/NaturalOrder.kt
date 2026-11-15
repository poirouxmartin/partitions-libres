package com.musique.partitions.catalog

/** Case-insensitive order where digit runs compare as numbers: "No. 2" < "No. 10". */
object NaturalOrder : Comparator<String> {
    private val chunk = Regex("\\d+|\\D+")

    override fun compare(a: String, b: String): Int {
        val xs = chunk.findAll(a.lowercase()).map { it.value }.toList()
        val ys = chunk.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(xs.size, ys.size)) {
            val x = xs[i]
            val y = ys[i]
            val c = if (x[0].isDigit() && y[0].isDigit()) {
                compareValuesBy(x, y, { it.trimStart('0').length }, { it.trimStart('0') })
            } else {
                x.compareTo(y)
            }
            if (c != 0) return c
        }
        return xs.size - ys.size
    }
}
