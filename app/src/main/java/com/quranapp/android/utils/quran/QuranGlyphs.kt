package com.quranapp.android.utils.quran

object QuranGlyphs {
    object Special {
        const val BISMILLAH = "\uFDFD"
        const val MECCAN = "\uE073"
        const val MEDINAN = "\uE075"
        const val SEJDA = "۩"
        const val TITLE_FRAME = "\uE000"
    }

    object Chapter {
        private val icons = arrayOf(
            "\uE903", // prefix
            "\uE904", // 1
            "\uE905", // 2
            "\uE906", // 3
            "\uE907", // 4
            "\uE908", // 5
            "\uE90B", // 6
            "\uE90C", // 7
            "\uE90D", // 8
            "\uE90E", // 9
            "\uE90F", // 10
            "\uE910", // 11
            "\uE911", // 12
            "\uE912", // 13
            "\uE913", // 14
            "\uE914", // 15
            "\uE915", // 16
            "\uE916", // 17
            "\uE917", // 18
            "\uE918", // 19
            "\uE919", // 20
            "\uE91A", // 21
            "\uE91B", // 22
            "\uE91C", // 23
            "\uE91D", // 24
            "\uE91E", // 25
            "\uE91F", // 26
            "\uE920", // 27
            "\uE921", // 28
            "\uE922", // 29
            "\uE923", // 30
            "\uE924", // 31
            "\uE925", // 32
            "\uE926", // 33
            "\uE92E", // 34
            "\uE92F", // 35
            "\uE930", // 36
            "\uE931", // 37
            "\uE909", // 38
            "\uE90A", // 39
            "\uE927", // 40
            "\uE928", // 41
            "\uE929", // 42
            "\uE92A", // 43
            "\uE92B", // 44
            "\uE92C", // 45
            "\uE92D", // 46
            "\uE932", // 47
            "\uE902", // 48
            "\uE933", // 49
            "\uE934", // 50
            "\uE935", // 51
            "\uE936", // 52
            "\uE937", // 53
            "\uE938", // 54
            "\uE939", // 55
            "\uE93A", // 56
            "\uE93B", // 57
            "\uE93C", // 58
            "\uE900", // 59
            "\uE901", // 60
            "\uE941", // 61
            "\uE942", // 62
            "\uE943", // 63
            "\uE944", // 64
            "\uE945", // 65
            "\uE946", // 66
            "\uE947", // 67
            "\uE948", // 68
            "\uE949", // 69
            "\uE94A", // 70
            "\uE94B", // 71
            "\uE94C", // 72
            "\uE94D", // 73
            "\uE94E", // 74
            "\uE94F", // 75
            "\uE950", // 76
            "\uE951", // 77
            "\uE952", // 78
            "\uE93D", // 79
            "\uE93E", // 80
            "\uE93F", // 81
            "\uE940", // 82
            "\uE953", // 83
            "\uE954", // 84
            "\uE955", // 85
            "\uE956", // 86
            "\uE957", // 87
            "\uE958", // 88
            "\uE959", // 89
            "\uE95A", // 90
            "\uE95B", // 91
            "\uE95C", // 92
            "\uE95D", // 93
            "\uE95E", // 94
            "\uE95F", // 95
            "\uE960", // 96
            "\uE961", // 97
            "\uE962", // 98
            "\uE963", // 99
            "\uE964", // 100
            "\uE965", // 101
            "\uE966", // 102
            "\uE967", // 103
            "\uE968", // 104
            "\uE969", // 105
            "\uE96A", // 106
            "\uE96B", // 107
            "\uE96C", // 108
            "\uE96D", // 109
            "\uE96E", // 110
            "\uE96F", // 111
            "\uE970", // 112
            "\uE971", // 113
            "\uE972"  // 114
        )

        fun getPrefix() = icons.getOrNull(0) ?: ""

        fun get(number: Int) = icons.getOrNull(number) ?: ""
    }

    object Juz {
        private val icons = arrayOf(
            "\uE900",
            "\uE901",
            "\uE902",
            "\uE903",
            "\uE904",
            "\uE905",
            "\uE906",
            "\uE907",
            "\uE908",
            "\uE909",
            "\uE90A",
            "\uE90B",
            "\uE90C",
            "\uE90D",
            "\uE90E",
            "\uE90F",
            "\uE910",
            "\uE911",
            "\uE912",
            "\uE913",
            "\uE914",
            "\uE915",
            "\uE916",
            "\uE917",
            "\uE918",
            "\uE919",
            "\uE91A",
            "\uE91B",
            "\uE91C",
            "\uE91D"
        )

        fun get(number: Int) = icons.getOrNull(number) ?: ""
    }
}