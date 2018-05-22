package com.kang.administrator.zhisuntestapplication

import android.provider.ContactsContract

object Contents {

    val URL_KEY = "URL_KEY"

    val NOTE_KEY = "NOTE_KEY"

    /**
     * When using Type.CONTACT, these arrays provide the keys for adding or retrieving multiple
     * phone numbers and addresses.
     */
    val PHONE_KEYS = arrayOf(ContactsContract.Intents.Insert.PHONE, ContactsContract.Intents.Insert.SECONDARY_PHONE, ContactsContract.Intents.Insert.TERTIARY_PHONE)

    val PHONE_TYPE_KEYS = arrayOf(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE, ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE)

    val EMAIL_KEYS = arrayOf(ContactsContract.Intents.Insert.EMAIL, ContactsContract.Intents.Insert.SECONDARY_EMAIL, ContactsContract.Intents.Insert.TERTIARY_EMAIL)

    val EMAIL_TYPE_KEYS = arrayOf(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE, ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE)

    /**
     * Contains type constants used when sending Intents.
     */
    object Type {
        /**
         * Plain text. Use Intent.putExtra(DATA, string). This can be used for URLs too, but string
         * must include "http://" or "https://".
         */
        val TEXT = "TEXT_TYPE"

        /**
         * An email type. Use Intent.putExtra(DATA, string) where string is the email address.
         */
        val EMAIL = "EMAIL_TYPE"

        /**
         * Use Intent.putExtra(DATA, string) where string is the phone number to call.
         */
        val PHONE = "PHONE_TYPE"

        /**
         * An SMS type. Use Intent.putExtra(DATA, string) where string is the number to SMS.
         */
        val SMS = "SMS_TYPE"

        /**
         * A contact. Send a request to encode it as follows:
         * `import android.provider.Contacts;
         *
         * Intent intent = new Intent(Intents.Encode.ACTION);
         * intent.putExtra(Intents.Encode.TYPE, CONTACT);
         * Bundle bundle = new Bundle();
         * bundle.putString(ContactsContract.Intents.Insert.NAME, "Jenny");
         * bundle.putString(ContactsContract.Intents.Insert.PHONE, "8675309");
         * bundle.putString(ContactsContract.Intents.Insert.EMAIL, "jenny@the80s.com");
         * bundle.putString(ContactsContract.Intents.Insert.POSTAL, "123 Fake St. San Francisco, CA 94102");
         * intent.putExtra(Intents.Encode.DATA, bundle);
        ` *
         */
        val CONTACT = "CONTACT_TYPE"

        /**
         * A geographic location. Use as follows:
         * Bundle bundle = new Bundle();
         * bundle.putFloat("LAT", latitude);
         * bundle.putFloat("LONG", longitude);
         * intent.putExtra(Intents.Encode.DATA, bundle);
         */
        val LOCATION = "LOCATION_TYPE"
    }

}