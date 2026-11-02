package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val notes: String = "",
    val creditBalance: Double = 0.0,
    val totalBilled: Double = 0.0,
    val totalPaid: Double = 0.0,
    val weeklyReminderEnabled: Boolean = true,
    val lastReminderSentAt: String = "",
    val loyaltyPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InventoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Chocolates",
    val sku: String = "",
    val unit: String = "box",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val stockQuantity: Long = 0L,
    val lowStockThreshold: Long = 5L,
    val expiryDate: String = "",
    val supplierName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShopTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val type: String = "CREDIT_SALE", // CREDIT_SALE, CASH_SALE, CREDIT_PAYMENT
    val itemsSummary: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val balanceDelta: Double = 0.0,
    val paymentMethod: String = "CREDIT", // CREDIT, CASH, UPI_CARD
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BillLineItem(
    val item: InventoryItem,
    val quantity: Int
) {
    val lineTotal: Double get() = item.price * quantity
}

data class WeeklyCustomerReport(
    val customer: Customer,
    val weekPurchasesTotal: Double,
    val weekSubmittedTotal: Double,
    val remainingBalance: Double,
    val weekTransactions: List<ShopTransaction>,
    val formattedSmsBody: String
)

data class SuggestedFeatureIdea(
    val id: String,
    val title: String,
    val subtitle: String,
    val benefitTag: String,
    val description: String,
    val isActiveInApp: Boolean = true
)
