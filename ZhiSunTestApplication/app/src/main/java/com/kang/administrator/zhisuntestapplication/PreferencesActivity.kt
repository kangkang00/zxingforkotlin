package com.kang.administrator.zhisuntestapplication

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class PreferencesActivity : AppCompatActivity() {

    val KEY_DECODE_1D_PRODUCT = "preferences_decode_1D_product"
    val KEY_DECODE_1D_INDUSTRIAL = "preferences_decode_1D_industrial"
    val KEY_DECODE_QR = "preferences_decode_QR"
    val KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix"
    val KEY_DECODE_AZTEC = "preferences_decode_Aztec"
    val KEY_DECODE_PDF417 = "preferences_decode_PDF417"

    val KEY_CUSTOM_PRODUCT_SEARCH = "preferences_custom_product_search"

    val KEY_PLAY_BEEP = "preferences_play_beep"
    val KEY_VIBRATE = "preferences_vibrate"
    val KEY_COPY_TO_CLIPBOARD = "preferences_copy_to_clipboard"
    val KEY_FRONT_LIGHT_MODE = "preferences_front_light_mode"
    val KEY_BULK_MODE = "preferences_bulk_mode"
    val KEY_REMEMBER_DUPLICATES = "preferences_remember_duplicates"
    val KEY_ENABLE_HISTORY = "preferences_history"
    val KEY_SUPPLEMENTAL = "preferences_supplemental"
    val KEY_AUTO_FOCUS = "preferences_auto_focus"
    val KEY_INVERT_SCAN = "preferences_invert_scan"
    val KEY_SEARCH_COUNTRY = "preferences_search_country"
    val KEY_DISABLE_AUTO_ORIENTATION = "preferences_orientation"

    val KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus"
    val KEY_DISABLE_EXPOSURE = "preferences_disable_exposure"
    val KEY_DISABLE_METERING = "preferences_disable_metering"
    val KEY_DISABLE_BARCODE_SCENE_MODE = "preferences_disable_barcode_scene_mode"
    val KEY_AUTO_OPEN_WEB = "preferences_auto_open_web"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(R.id.preferencesactivity, PreferencesFragment()).commit()
    }
}
