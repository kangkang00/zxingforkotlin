package com.kang.administrator.zhisuntestapplication

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.*
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList

class PreferencesFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var checkBoxPrefs: Array<CheckBoxPreference?>? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.preferences)

        val preferences = preferenceScreen
        preferences.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        checkBoxPrefs = findDecodePrefs(preferences,
                PreferencesActivity().KEY_DECODE_1D_PRODUCT,
                PreferencesActivity().KEY_DECODE_1D_INDUSTRIAL,
                PreferencesActivity().KEY_DECODE_QR,
                PreferencesActivity().KEY_DECODE_DATA_MATRIX,
                PreferencesActivity().KEY_DECODE_AZTEC,
                PreferencesActivity().KEY_DECODE_PDF417)
        disableLastCheckedPref()

        val customProductSearch = preferences.findPreference(PreferencesActivity().KEY_CUSTOM_PRODUCT_SEARCH) as EditTextPreference
        customProductSearch.onPreferenceChangeListener = CustomSearchURLValidator()
    }

    private fun findDecodePrefs(preferences: PreferenceScreen, vararg keys: String): Array<CheckBoxPreference?> {
        val prefs = arrayOfNulls<CheckBoxPreference>(keys.size)
        for (i in keys.indices) {
            prefs[i] = preferences.findPreference(keys[i]) as CheckBoxPreference
        }

        return prefs
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        disableLastCheckedPref()
    }

    private fun disableLastCheckedPref() {
        val checked = ArrayList<CheckBoxPreference>(checkBoxPrefs!!.size)
        for (pref in checkBoxPrefs!!) {
            if (pref!!.isChecked) {
                checked.add(pref!!)
            }
        }
        val disable = checked.size <= 1
        for (pref in checkBoxPrefs!!) {
            pref!!.isEnabled = !(disable && checked.contains(pref))
        }
    }

    private inner class CustomSearchURLValidator : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            if (!isValid(newValue)) {
                val builder = AlertDialog.Builder(this@PreferencesFragment.activity)
                builder.setTitle(R.string.msg_error)
                builder.setMessage(R.string.msg_invalid_value)
                builder.setCancelable(true)
                builder.show()
                return false
            }
            return true
        }

        private fun isValid(newValue: Any?): Boolean {
            // Allow empty/null value
            if (newValue == null) {
                return true
            }
            var valueString = newValue.toString()
            if (valueString.isEmpty()) {
                return true
            }
            // Before validating, remove custom placeholders, which will not
            // be considered valid parts of the URL in some locations:
            // Blank %t and %s:
            valueString = valueString.replace("%[st]".toRegex(), "")
            // Blank %f but not if followed by digit or a-f as it may be a hex sequence
            valueString = valueString.replace("%f(?![0-9a-f])".toRegex(), "")
            // Require a scheme otherwise:
            try {
                val uri = URI(valueString)
                return uri.scheme != null
            } catch (use: URISyntaxException) {
                return false
            }

        }
    }

}