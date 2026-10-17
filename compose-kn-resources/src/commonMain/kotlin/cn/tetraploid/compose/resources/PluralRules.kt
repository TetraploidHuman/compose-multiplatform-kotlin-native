package cn.tetraploid.compose.resources

/**
 * CLDR 基数复数，只覆盖整数 n（API 是 `Int`，所以 v = f = t = 0，i = |n|）。
 * 规则按 Unicode CLDR 语言分组；未列出的语言用英语式 `one`（n = 1）/ `other`。
 */
internal fun selectCardinalPlural(language: String, count: Int): String {
    val n = if (count < 0) -count else count
    val mod10 = n % 10
    val mod100 = n % 100
    return when (language.lowercase().substringBefore('-').substringBefore('_')) {
        // 无复数范畴
        "zh", "ja", "ko", "vi", "th", "id", "ms", "my", "km", "lo", "jv", "wuu", "yue" -> "other"

        // one = 0 或 1
        "fr", "ff", "hy", "kab", "pt" -> if (n == 0 || n == 1) "one" else "other"

        // 阿拉伯语
        "ar" -> when {
            n == 0 -> "zero"
            n == 1 -> "one"
            n == 2 -> "two"
            mod100 in 3..10 -> "few"
            mod100 in 11..99 -> "many"
            else -> "other"
        }

        // 东斯拉夫：其余整数是 many，不是 other
        "ru", "uk", "be" -> when {
            mod10 == 1 && mod100 != 11 -> "one"
            mod10 in 2..4 && mod100 !in 12..14 -> "few"
            else -> "many"
        }

        // 南斯拉夫：其余是 other
        "sr", "hr", "bs", "sh" -> when {
            mod10 == 1 && mod100 != 11 -> "one"
            mod10 in 2..4 && mod100 !in 12..14 -> "few"
            else -> "other"
        }

        "pl" -> when {
            n == 1 -> "one"
            mod10 in 2..4 && mod100 !in 12..14 -> "few"
            else -> "many"
        }

        "cs", "sk" -> when {
            n == 1 -> "one"
            n in 2..4 -> "few"
            else -> "other"
        }

        "sl" -> when (mod100) {
            1 -> "one"
            2 -> "two"
            3, 4 -> "few"
            else -> "other"
        }

        "ga" -> when (n) {
            1 -> "one"
            2 -> "two"
            in 3..6 -> "few"
            in 7..10 -> "many"
            else -> "other"
        }

        "gd" -> when {
            n == 1 || n == 11 -> "one"
            n == 2 || n == 12 -> "two"
            n in 3..10 || n in 13..19 -> "few"
            else -> "other"
        }

        "cy" -> when (n) {
            0 -> "zero"
            1 -> "one"
            2 -> "two"
            3 -> "few"
            6 -> "many"
            else -> "other"
        }

        "lt" -> when {
            mod10 == 1 && mod100 != 11 -> "one"
            mod10 in 2..9 && mod100 !in 11..19 -> "few"
            else -> "other"
        }

        "lv" -> when {
            mod10 == 0 || mod100 in 11..19 -> "zero"
            mod10 == 1 && mod100 != 11 -> "one"
            else -> "other"
        }

        "ro", "mo" -> when {
            n == 1 -> "one"
            n == 0 || mod100 in 2..19 -> "few"
            else -> "other"
        }

        "he", "iw" -> when {
            n == 1 -> "one"
            n == 2 -> "two"
            n > 10 && mod10 == 0 -> "many"
            else -> "other"
        }

        "mt" -> when {
            n == 1 -> "one"
            n == 0 || mod100 in 2..10 -> "few"
            mod100 in 11..19 -> "many"
            else -> "other"
        }

        "is", "mk" -> if (mod10 == 1 && mod100 != 11) "one" else "other"

        else -> if (n == 1) "one" else "other"
    }
}
