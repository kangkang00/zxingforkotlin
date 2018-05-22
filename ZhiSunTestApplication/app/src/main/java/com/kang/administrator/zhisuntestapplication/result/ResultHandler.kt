package com.kang.administrator.zhisuntestapplication.result

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import com.kang.administrator.zhisuntestapplication.*
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

abstract class ResultHandler @JvmOverloads internal constructor(internal val activity: Activity, val result: ParsedResult, private val rawResult: Result? = null) {
    private val customProductSearch: String?

    /**
     * Indicates how many buttons the derived class wants shown.
     *
     * @return The integer button count.
     */
    abstract val buttonCount: Int

    val defaultButtonID: Int?
        get() = null

    /**
     * Create a possibly styled string for the contents of the current barcode.
     *
     * @return The text to be displayed.
     */
    val displayContents: CharSequence
        get() {
            val contents = result.displayResult
            return contents.replace("\r", "")
        }

    /**
     * A string describing the kind of barcode that was found, e.g. "Found contact info".
     *
     * @return The resource ID of the string.
     */
    abstract val displayTitle: Int

    /**
     * A convenience method to get the parsed type. Should not be overridden.
     *
     * @return The parsed type, e.g. URI or ISBN
     */
    val type: ParsedResultType
        get() = result.type

    init {
        this.customProductSearch = parseCustomSearchURL()
    }

    internal fun hasCustomProductSearch(): Boolean {
        return customProductSearch != null
    }

    /**
     * The text of the nth action button.
     *
     * @param index From 0 to getButtonCount() - 1
     * @return The button text as a resource ID
     */
    abstract fun getButtonText(index: Int): Int

    /**
     * Execute the action which corresponds to the nth button.
     *
     * @param index The button that was clicked.
     */
    abstract fun handleButtonPress(index: Int)

    /**
     * Some barcode contents are considered secure, and should not be saved to history, copied to
     * the clipboard, or otherwise persisted.
     *
     * @return If true, do not create any permanent record of these contents.
     */
    fun areContentsSecure(): Boolean {
        return false
    }

    internal fun addPhoneOnlyContact(phoneNumbers: Array<String>, phoneTypes: Array<String>) {
        addContact(null, null, null, phoneNumbers, phoneTypes, null, null, null, null, null, null, null, null, null, null, null)
    }

    internal fun addEmailOnlyContact(emails: Array<String>, emailTypes: Array<String>) {
        addContact(null, null, null, null, null, emails, emailTypes, null, null, null, null, null, null, null, null, null)
    }

    internal fun addContact(names: Array<String>?,
                            nicknames: Array<String>?,
                            pronunciation: String?,
                            phoneNumbers: Array<String>?,
                            phoneTypes: Array<String>?,
                            emails: Array<String>?,
                            emailTypes: Array<String>?,
                            note: String?,
                            instantMessenger: String?,
                            address: String?,
                            addressType: String?,
                            org: String?,
                            title: String?,
                            urls: Array<String>?,
                            birthday: String?,
                            geo: Array<String>?) {

        // Only use the first name in the array, if present.
        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT, ContactsContract.Contacts.CONTENT_URI)
        intent.type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
        putExtra(intent, ContactsContract.Intents.Insert.NAME, if (names != null && names.size > 0) names[0] else null)

        putExtra(intent, ContactsContract.Intents.Insert.PHONETIC_NAME, pronunciation)

        val phoneCount = Math.min(phoneNumbers?.size ?: 0, Contents.PHONE_KEYS.size)
        for (x in 0 until phoneCount) {
            putExtra(intent, Contents.PHONE_KEYS[x], phoneNumbers!![x])
            if (phoneTypes != null && x < phoneTypes.size) {
                val type = toPhoneContractType(phoneTypes[x])
                if (type >= 0) {
                    intent.putExtra(Contents.PHONE_TYPE_KEYS[x], type)
                }
            }
        }

        val emailCount = Math.min(emails?.size ?: 0, Contents.EMAIL_KEYS.size)
        for (x in 0 until emailCount) {
            putExtra(intent, Contents.EMAIL_KEYS[x], emails!![x])
            if (emailTypes != null && x < emailTypes.size) {
                val type = toEmailContractType(emailTypes[x])
                if (type >= 0) {
                    intent.putExtra(Contents.EMAIL_TYPE_KEYS[x], type)
                }
            }
        }

        val data = ArrayList<ContentValues>()
        if (urls != null) {
            for (url in urls) {
                if (url != null && !url.isEmpty()) {
                    val row = ContentValues(2)
                    row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    row.put(ContactsContract.CommonDataKinds.Website.URL, url)
                    data.add(row)
                    break
                }
            }
        }

        if (birthday != null) {
            val row = ContentValues(3)
            row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
            row.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
            row.put(ContactsContract.CommonDataKinds.Event.START_DATE, birthday)
            data.add(row)
        }

        if (nicknames != null) {
            for (nickname in nicknames) {
                if (nickname != null && !nickname.isEmpty()) {
                    val row = ContentValues(3)
                    row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                    row.put(ContactsContract.CommonDataKinds.Nickname.TYPE,
                            ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                    row.put(ContactsContract.CommonDataKinds.Nickname.NAME, nickname)
                    data.add(row)
                    break
                }
            }
        }

        if (!data.isEmpty()) {
            intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data)
        }

        val aggregatedNotes = StringBuilder()
        if (note != null) {
            aggregatedNotes.append('\n').append(note)
        }
        if (geo != null && geo.size >= 2) {
            aggregatedNotes.append('\n').append(geo[0]).append(',').append(geo[1])
        }

        if (aggregatedNotes.length > 0) {
            // Remove extra leading '\n'
            putExtra(intent, ContactsContract.Intents.Insert.NOTES, aggregatedNotes.substring(1))
        }

        putExtra(intent, ContactsContract.Intents.Insert.IM_HANDLE, instantMessenger)
        putExtra(intent, ContactsContract.Intents.Insert.POSTAL, address)
        if (addressType != null) {
            val type = toAddressContractType(addressType)
            if (type >= 0) {
                intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE, type)
            }
        }
        putExtra(intent, ContactsContract.Intents.Insert.COMPANY, org)
        putExtra(intent, ContactsContract.Intents.Insert.JOB_TITLE, title)
        launchIntent(intent)
    }

    internal fun shareByEmail(contents: String) {
        sendEmail(null, null, null, null, contents)
    }

    internal fun sendEmail(to: Array<String>?,
                           cc: Array<String>?,
                           bcc: Array<String>?,
                           subject: String?,
                           body: String) {
        val intent = Intent(Intent.ACTION_SEND, Uri.parse("mailto:"))
        if (to != null && to.size != 0) {
            intent.putExtra(Intent.EXTRA_EMAIL, to)
        }
        if (cc != null && cc.size != 0) {
            intent.putExtra(Intent.EXTRA_CC, cc)
        }
        if (bcc != null && bcc.size != 0) {
            intent.putExtra(Intent.EXTRA_BCC, bcc)
        }
        putExtra(intent, Intent.EXTRA_SUBJECT, subject)
        putExtra(intent, Intent.EXTRA_TEXT, body)
        intent.type = "text/plain"
        launchIntent(intent)
    }

    internal fun shareBySMS(contents: String) {
        sendSMSFromUri("smsto:", contents)
    }

    internal fun sendSMS(phoneNumber: String, body: String) {
        sendSMSFromUri("smsto:$phoneNumber", body)
    }

    private fun sendSMSFromUri(uri: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uri))
        putExtra(intent, "sms_body", body)
        // Exit the app once the SMS is sent
        intent.putExtra("compose_mode", true)
        launchIntent(intent)
    }

    internal fun sendMMS(phoneNumber: String, subject: String, body: String) {
        sendMMSFromUri("mmsto:$phoneNumber", subject, body)
    }

    private fun sendMMSFromUri(uri: String, subject: String?, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uri))
        // The Messaging app needs to see a valid subject or else it will treat this an an SMS.
        if (subject == null || subject.isEmpty()) {
            putExtra(intent, "subject", activity.getString(R.string.msg_default_mms_subject))
        } else {
            putExtra(intent, "subject", subject)
        }
        putExtra(intent, "sms_body", body)
        intent.putExtra("compose_mode", true)
        launchIntent(intent)
    }

    internal fun dialPhone(phoneNumber: String) {
        launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
    }

    internal fun dialPhoneFromUri(uri: String) {
        launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse(uri)))
    }

    internal fun openMap(geoURI: String) {
        launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse(geoURI)))
    }

    /**
     * Do a geo search using the address as the query.
     *
     * @param address The address to find
     */
    internal fun searchMap(address: String) {
        launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address))))
    }

    internal fun getDirections(latitude: Double, longitude: Double) {
        launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google." +
                LocaleManager.getCountryTLD(activity) + "/maps?f=d&daddr=" + latitude + ',' + longitude)))
    }

    // Uses the mobile-specific version of Product Search, which is formatted for small screens.
    internal fun openProductSearch(upc: String) {
        val uri = Uri.parse("http://www.google." + LocaleManager.getProductSearchCountryTLD(activity) +
                "/m/products?q=" + upc + "&source=zxing")
        launchIntent(Intent(Intent.ACTION_VIEW, uri))
    }

    internal fun openBookSearch(isbn: String) {
        val uri = Uri.parse("http://books.google." + LocaleManager.getBookSearchCountryTLD(activity) +
                "/books?vid=isbn" + isbn)
        launchIntent(Intent(Intent.ACTION_VIEW, uri))
    }

//    internal fun searchBookContents(isbnOrUrl: String) {
//        val intent = Intent(Intents.SearchBookContents.ACTION)
//        intent.setClassName(activity, SearchBookContentsActivity::class.java!!.getName())
//        putExtra(intent, Intents.SearchBookContents.ISBN, isbnOrUrl)
//        launchIntent(intent)
//    }

    internal fun openURL(url: String) {
        var url = url
        // Strangely, some Android browsers don't seem to register to handle HTTP:// or HTTPS://.
        // Lower-case these as it should always be OK to lower-case these schemes.
        if (url.startsWith("HTTP://")) {
            url = "http" + url.substring(4)
        } else if (url.startsWith("HTTPS://")) {
            url = "https" + url.substring(5)
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            launchIntent(intent)
        } catch (ignored: ActivityNotFoundException) {
            Log.w(TAG, "Nothing available to handle $intent")
        }

    }

    internal fun webSearch(query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra("query", query)
        launchIntent(intent)
    }

    /**
     * Like [.launchIntent] but will tell you if it is not handle-able
     * via [ActivityNotFoundException].
     *
     * @throws ActivityNotFoundException if Intent can't be handled
     */
    internal fun rawLaunchIntent(intent: Intent?) {
        if (intent != null) {
            intent.addFlags(Intents.FLAG_NEW_DOC)
            Log.d(TAG, "Launching intent: " + intent + " with extras: " + intent.extras)
            activity.startActivity(intent)
        }
    }

    /**
     * Like [.rawLaunchIntent] but will show a user dialog if nothing is available to handle.
     */
    internal fun launchIntent(intent: Intent) {
        try {
            rawLaunchIntent(intent)
        } catch (ignored: ActivityNotFoundException) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.app_name)
            builder.setMessage(R.string.msg_intent_failed)
            builder.setPositiveButton(R.string.button_ok, null)
            builder.show()
        }

    }

    private fun parseCustomSearchURL(): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val customProductSearch = prefs.getString(PreferencesActivity().KEY_CUSTOM_PRODUCT_SEARCH, null)
        return if (customProductSearch != null && customProductSearch.trim { it <= ' ' }.isEmpty()) {
            null
        } else customProductSearch
    }

    internal fun fillInCustomSearchURL(text: String): String {
        var text = text
        if (customProductSearch == null) {
            return text // ?
        }
        try {
            text = URLEncoder.encode(text, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            // can't happen; UTF-8 is always supported. Continue, I guess, without encoding
        }

        var url: String = customProductSearch
        if (rawResult != null) {
            // Replace %f but only if it doesn't seem to be a hex escape sequence. This remains
            // problematic but avoids the more surprising problem of breaking escapes
            url = url.replaceFirst("%f(?![0-9a-f])".toRegex(), rawResult.barcodeFormat.toString())
            if (url.contains("%t")) {
                val parsedResultAgain = ResultParser.parseResult(rawResult)
                url = url.replace("%t", parsedResultAgain.type.toString())
            }
        }
        // Replace %s last as it might contain itself %f or %t
        return url.replace("%s", text)
    }

    companion object {

        private val TAG = ResultHandler::class.java.simpleName

        private val EMAIL_TYPE_STRINGS = arrayOf("home", "work", "mobile")
        private val PHONE_TYPE_STRINGS = arrayOf("home", "work", "mobile", "fax", "pager", "main")
        private val ADDRESS_TYPE_STRINGS = arrayOf("home", "work")
        private val EMAIL_TYPE_VALUES = intArrayOf(ContactsContract.CommonDataKinds.Email.TYPE_HOME, ContactsContract.CommonDataKinds.Email.TYPE_WORK, ContactsContract.CommonDataKinds.Email.TYPE_MOBILE)
        private val PHONE_TYPE_VALUES = intArrayOf(ContactsContract.CommonDataKinds.Phone.TYPE_HOME, ContactsContract.CommonDataKinds.Phone.TYPE_WORK, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK, ContactsContract.CommonDataKinds.Phone.TYPE_PAGER, ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)
        private val ADDRESS_TYPE_VALUES = intArrayOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
        private val NO_TYPE = -1

        val MAX_BUTTON_COUNT = 4

        private fun toEmailContractType(typeString: String): Int {
            return doToContractType(typeString, EMAIL_TYPE_STRINGS, EMAIL_TYPE_VALUES)
        }

        private fun toPhoneContractType(typeString: String): Int {
            return doToContractType(typeString, PHONE_TYPE_STRINGS, PHONE_TYPE_VALUES)
        }

        private fun toAddressContractType(typeString: String): Int {
            return doToContractType(typeString, ADDRESS_TYPE_STRINGS, ADDRESS_TYPE_VALUES)
        }

        private fun doToContractType(typeString: String?, types: Array<String>, values: IntArray): Int {
            if (typeString == null) {
                return NO_TYPE
            }
            for (i in types.indices) {
                val type = types[i]
                if (typeString.startsWith(type) || typeString.startsWith(type.toUpperCase(Locale.ENGLISH))) {
                    return values[i]
                }
            }
            return NO_TYPE
        }

        private fun putExtra(intent: Intent, key: String, value: String?) {
            if (value != null && !value.isEmpty()) {
                intent.putExtra(key, value)
            }
        }

        internal fun formatPhone(phoneData: String): String {
            // Just collect the call to a deprecated method in one place
            return PhoneNumberUtils.formatNumber(phoneData)
        }
    }

}