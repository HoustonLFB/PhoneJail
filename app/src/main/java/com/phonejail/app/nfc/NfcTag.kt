package com.phonejail.app.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/**
 * Helpers for the PhoneJail "jail" tag. We store one external-type NDEF record;
 * the manifest's NDEF_DISCOVERED filter matches it so a tap auto-launches the app.
 */
object NfcTag {

    // Must line up with the <data> entry in AndroidManifest for auto-launch.
    private const val DOMAIN = "phonejail.com"
    private const val TYPE = "session"

    private fun launchMessage(): NdefMessage =
        NdefMessage(arrayOf(NdefRecord.createExternal(DOMAIN, TYPE, ByteArray(0))))

    /**
     * Write the launch record onto a blank/formatable tag.
     * @return true on success.
     */
    fun program(tag: Tag): Boolean {
        val message = launchMessage()

        Ndef.get(tag)?.let { ndef ->
            return try {
                ndef.connect()
                if (!ndef.isWritable) return false
                if (ndef.maxSize < message.toByteArray().size) return false
                ndef.writeNdefMessage(message)
                true
            } catch (e: Exception) {
                false
            } finally {
                runCatching { ndef.close() }
            }
        }

        NdefFormatable.get(tag)?.let { formatable ->
            return try {
                formatable.connect()
                formatable.format(message)
                true
            } catch (e: Exception) {
                false
            } finally {
                runCatching { formatable.close() }
            }
        }

        return false
    }
}
