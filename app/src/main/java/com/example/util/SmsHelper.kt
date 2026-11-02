package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object SmsHelper {
    /**
     * Launches the device's default SMS app with the customer's phone number and weekly credit report
     * pre-filled. Uses zero-permission ACTION_SENDTO ("smsto:") to comply with Google Play policy.
     */
    fun launchSmsComposer(context: Context, phoneNumber: String, messageBody: String) {
        val cleanPhone = phoneNumber.trim()
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            shareTextReport(context, "Sandeep Cafe Weekly Credit Report for $cleanPhone", messageBody)
        }
    }

    /**
     * Launches the SMS composer for multiple customers at once (or single customer) after 1-time user confirmation.
     */
    fun launchAutomatedBatchSms(
        context: Context,
        phoneNumbers: List<String>,
        combinedOrSingleBody: String
    ) {
        val cleaned = phoneNumbers.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        val recipients = cleaned.joinToString(";")
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(recipients)}")
                putExtra("sms_body", combinedOrSingleBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            shareTextReport(context, "Sandeep Cafe Automated Weekly Credit SMS", combinedOrSingleBody)
        }
    }

    /**
     * Opens the Android Share Sheet so the shop owner can send the weekly credit report or bill
     * via WhatsApp, SMS, Email, or any messaging app.
     */
    fun shareTextReport(context: Context, subject: String, body: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            val chooser = Intent.createChooser(sendIntent, subject).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open share sheet.", Toast.LENGTH_SHORT).show()
        }
    }
}
