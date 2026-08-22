fun main() {
    val html = "data-last-price=\"25441207.60\" data-last-normal"
    val regex = """data-last-price="([0-9.]+)" """.toRegex()
    val matchResult = regex.find(html)
    println(matchResult?.groupValues?.get(1)?.toDoubleOrNull())
}
