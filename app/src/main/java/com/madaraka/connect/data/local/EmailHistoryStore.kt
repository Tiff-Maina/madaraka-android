package com.madaraka.connect.data.local

import android.content.Context
import android.content.SharedPreferences

// Remembers the last few emails the user signed in with, on this device only
class EmailHistoryStore(context: Context) {

    // The SharedPreferences file used to persist the email list
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("auth_email_history", Context.MODE_PRIVATE)

    companion object {
        // Key under which the joined emails are stored
        private const val KEY_EMAILS = "saved_emails"
        // How many recent emails we keep
        private const val MAX_EMAILS = 5
    }

    // Returns the list of saved emails, most recent first
    fun getEmails(): List<String> {
        val raw = prefs.getString(KEY_EMAILS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    // Adds an email to the front of the list. Deduplicates and caps at MAX_EMAILS.
    fun addEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return
        val current = getEmails().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val capped = current.take(MAX_EMAILS)
        prefs.edit().putString(KEY_EMAILS, capped.joinToString("|")).apply()
    }

    // Clears all saved emails
    fun clear() {
        prefs.edit().remove(KEY_EMAILS).apply()
    }
}