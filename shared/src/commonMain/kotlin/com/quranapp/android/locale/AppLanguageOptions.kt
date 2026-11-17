package com.quranapp.android.locale

data class AppLanguageOption(
    val tag: String,
    val nativeName: String,
    val systemDefault: Boolean = false,
)

val appLanguageOptions: List<AppLanguageOption> = listOf(
    AppLanguageOption("default", "", systemDefault = true),
    AppLanguageOption("en", "English"),
    AppLanguageOption("ar", "العربية"),
    AppLanguageOption("bn", "বাংলা"),
    AppLanguageOption("ckb", "کوردی"),
    AppLanguageOption("de", "Deutsch"),
    AppLanguageOption("es", "Español"),
    AppLanguageOption("fa", "فارسی"),
    AppLanguageOption("fil", "Filipino"),
    AppLanguageOption("fr", "Français"),
    AppLanguageOption("gu", "ગુજરાતી"),
    AppLanguageOption("hi", "हिन्दी"),
    AppLanguageOption("id", "Indonesian"),
    AppLanguageOption("it", "Italiano"),
    AppLanguageOption("ky", "Кыргызча"),
    AppLanguageOption("ml", "മലയാളം"),
    AppLanguageOption("pt", "Português"),
    AppLanguageOption("ru", "Русский"),
    AppLanguageOption("sd", "سنڌي"),
    AppLanguageOption("ta", "தமிழ்"),
    AppLanguageOption("tr", "Türkçe"),
    AppLanguageOption("ur", "اردو"),
    AppLanguageOption("zh-CN", "简体中文"),
    AppLanguageOption("zh-TW", "繁體中文"),
)
