package com.kang.administrator.zhisuntestapplication

import android.content.Context
import android.preference.PreferenceManager
import java.util.*

object LocaleManager {

    private val DEFAULT_TLD = "com"
    private val DEFAULT_COUNTRY = "US"
    private val DEFAULT_LANGUAGE = "en"

    /**
     * Locales (well, countries) where Google web search is available.
     * These should be kept in sync with our translations.
     */
    private val GOOGLE_COUNTRY_TLD: MutableMap<String, String>

    /**
     * Google Product Search for mobile is available in fewer countries than web search. See here:
     * http://support.google.com/merchants/bin/answer.py?hl=en-GB&answer=160619
     */
    private val GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD: MutableMap<String, String>


    private val TRANSLATED_HELP_ASSET_LANGUAGES = Arrays.asList("de", "en", "es", "fa", "fr", "it", "ja", "ko", "nl", "pt", "ru", "uk", "zh-rCN", "zh")

    private val systemCountry: String
        get() {
            val locale = Locale.getDefault()
            return if (locale == null) DEFAULT_COUNTRY else locale.country
        }

    private// Special case Chinese
    val systemLanguage: String
        get() {
            val locale = Locale.getDefault() ?: return DEFAULT_LANGUAGE
            return if (Locale.SIMPLIFIED_CHINESE == locale) {
                "zh-rCN"
            } else locale.language
        }

    internal val translatedAssetLanguage: String
        get() {
            val language = systemLanguage
            return if (TRANSLATED_HELP_ASSET_LANGUAGES.contains(language)) language else DEFAULT_LANGUAGE
        }

    init {
        GOOGLE_COUNTRY_TLD = HashMap()
        GOOGLE_COUNTRY_TLD["AR"] = "com.ar" // ARGENTINA
        GOOGLE_COUNTRY_TLD["AU"] = "com.au" // AUSTRALIA
        GOOGLE_COUNTRY_TLD["BR"] = "com.br" // BRAZIL
        GOOGLE_COUNTRY_TLD["BG"] = "bg" // BULGARIA
        GOOGLE_COUNTRY_TLD[Locale.CANADA.country] = "ca"
        GOOGLE_COUNTRY_TLD[Locale.CHINA.country] = "cn"
        GOOGLE_COUNTRY_TLD["CZ"] = "cz" // CZECH REPUBLIC
        GOOGLE_COUNTRY_TLD["DK"] = "dk" // DENMARK
        GOOGLE_COUNTRY_TLD["FI"] = "fi" // FINLAND
        GOOGLE_COUNTRY_TLD[Locale.FRANCE.country] = "fr"
        GOOGLE_COUNTRY_TLD[Locale.GERMANY.country] = "de"
        GOOGLE_COUNTRY_TLD["GR"] = "gr" // GREECE
        GOOGLE_COUNTRY_TLD["HU"] = "hu" // HUNGARY
        GOOGLE_COUNTRY_TLD["ID"] = "co.id" // INDONESIA
        GOOGLE_COUNTRY_TLD["IL"] = "co.il" // ISRAEL
        GOOGLE_COUNTRY_TLD[Locale.ITALY.country] = "it"
        GOOGLE_COUNTRY_TLD[Locale.JAPAN.country] = "co.jp"
        GOOGLE_COUNTRY_TLD[Locale.KOREA.country] = "co.kr"
        GOOGLE_COUNTRY_TLD["NL"] = "nl" // NETHERLANDS
        GOOGLE_COUNTRY_TLD["PL"] = "pl" // POLAND
        GOOGLE_COUNTRY_TLD["PT"] = "pt" // PORTUGAL
        GOOGLE_COUNTRY_TLD["RO"] = "ro" // ROMANIA
        GOOGLE_COUNTRY_TLD["RU"] = "ru" // RUSSIA
        GOOGLE_COUNTRY_TLD["SK"] = "sk" // SLOVAK REPUBLIC
        GOOGLE_COUNTRY_TLD["SI"] = "si" // SLOVENIA
        GOOGLE_COUNTRY_TLD["ES"] = "es" // SPAIN
        GOOGLE_COUNTRY_TLD["SE"] = "se" // SWEDEN
        GOOGLE_COUNTRY_TLD["CH"] = "ch" // SWITZERLAND
        GOOGLE_COUNTRY_TLD[Locale.TAIWAN.country] = "tw"
        GOOGLE_COUNTRY_TLD["TR"] = "com.tr" // TURKEY
        GOOGLE_COUNTRY_TLD["UA"] = "com.ua" // UKRAINE
        GOOGLE_COUNTRY_TLD[Locale.UK.country] = "co.uk"
        GOOGLE_COUNTRY_TLD[Locale.US.country] = "com"
    }

    init {
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD = HashMap()
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD["AU"] = "com.au" // AUSTRALIA
        //GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.CHINA.getCountry(), "cn");
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD[Locale.FRANCE.country] = "fr"
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD[Locale.GERMANY.country] = "de"
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD[Locale.ITALY.country] = "it"
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD[Locale.JAPAN.country] = "co.jp"
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD["NL"] = "nl" // NETHERLANDS
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD["ES"] = "es" // SPAIN
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD["CH"] = "ch" // SWITZERLAND
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD[Locale.UK.country] = "co.uk"
        GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD[Locale.US.country] = "com"
    }

    /**
     * Book search is offered everywhere that web search is available.
     */
    private val GOOGLE_BOOK_SEARCH_COUNTRY_TLD = GOOGLE_COUNTRY_TLD

    /**
     * @param context application's [Context]
     * @return country-specific TLD suffix appropriate for the current default locale
     * (e.g. "co.uk" for the United Kingdom)
     */
    fun getCountryTLD(context: Context): String {
        return doGetTLD(GOOGLE_COUNTRY_TLD, context)
    }

    /**
     * The same as above, but specifically for Google Product Search.
     *
     * @param context application's [Context]
     * @return The top-level domain to use.
     */
    fun getProductSearchCountryTLD(context: Context): String {
        return doGetTLD(GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD, context)
    }

    /**
     * The same as above, but specifically for Google Book Search.
     *
     * @param context application's [Context]
     * @return The top-level domain to use.
     */
    fun getBookSearchCountryTLD(context: Context): String {
        return doGetTLD(GOOGLE_BOOK_SEARCH_COUNTRY_TLD, context)
    }

    /**
     * Does a given URL point to Google Book Search, regardless of domain.
     *
     * @param url The address to check.
     * @return True if this is a Book Search URL.
     */
    fun isBookSearchUrl(url: String): Boolean {
        return url.startsWith("http://google.com/books") || url.startsWith("http://books.google.")
    }

    private fun doGetTLD(map: Map<String, String>, context: Context): String {
        val tld = map[getCountry(context)]
        return tld ?: DEFAULT_TLD
    }

    private fun getCountry(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val countryOverride = prefs.getString(PreferencesActivity().KEY_SEARCH_COUNTRY, "-")
        return if (countryOverride != null && !countryOverride.isEmpty() && "-" != countryOverride) {
            countryOverride
        } else systemCountry
    }

}