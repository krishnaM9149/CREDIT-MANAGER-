package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BillLineItem
import com.example.data.model.Customer
import com.example.data.model.InventoryItem
import com.example.data.model.ShopTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class ShopRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifEmpty {
            "id_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
    }

    // --- CUSTOMERS ---

    fun observeCustomers(userId: String): Flow<List<Customer>> {
        val path = "users/$userId/customers"
        return db.collection("users").document(userId).collection("customers")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.creditBalance }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getCustomerById(ownerUserId: String, customerId: String): Result<Customer?> {
        val path = "users/$ownerUserId/customers/$customerId"
        return try {
            val doc = db.collection("users").document(ownerUserId)
                .collection("customers").document(customerId)
                .get()
                .await()
            Result.success(doc.toObject(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun addCustomer(
        name: String,
        phone: String,
        notes: String = "",
        initialCredit: Double = 0.0,
        weeklyReminderEnabled: Boolean = true
    ): Result<String> {
        val uid = requireUserId()
        val docId = sanitizeId("cust_${UUID.randomUUID().toString().take(12)}")
        val docRef = db.collection("users").document(uid).collection("customers").document(docId)
        val trimmedName = name.trim().take(100)
        val trimmedPhone = phone.trim().take(25)
        val trimmedNotes = notes.trim().take(300)
        val safeInitial = initialCredit.coerceAtLeast(0.0)

        val payload = mapOf(
            "userId" to uid,
            "name" to trimmedName,
            "phone" to trimmedPhone,
            "notes" to trimmedNotes,
            "creditBalance" to safeInitial,
            "totalBilled" to safeInitial,
            "totalPaid" to 0.0,
            "weeklyReminderEnabled" to weeklyReminderEnabled,
            "lastReminderSentAt" to "",
            "loyaltyPoints" to (safeInitial / 100.0).toLong().coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerDetails(
        customerId: String,
        name: String,
        phone: String,
        notes: String,
        weeklyReminderEnabled: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
        val updates = mapOf(
            "name" to name.trim().take(100),
            "phone" to phone.trim().take(25),
            "notes" to notes.trim().take(300),
            "weeklyReminderEnabled" to weeklyReminderEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markReminderSent(customerId: String, timestampLabel: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
        val updates = mapOf(
            "lastReminderSentAt" to timestampLabel.take(60),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- INVENTORY & STOCKS ---

    fun observeInventory(userId: String): Flow<List<InventoryItem>> {
        val path = "users/$userId/inventory"
        return db.collection("users").document(userId).collection("inventory")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addInventoryItem(
        name: String,
        category: String,
        sku: String,
        unit: String,
        price: Double,
        costPrice: Double,
        stockQuantity: Long,
        lowStockThreshold: Long,
        expiryDate: String,
        supplierName: String
    ): Result<String> {
        val uid = requireUserId()
        val docId = sanitizeId("item_${UUID.randomUUID().toString().take(12)}")
        val docRef = db.collection("users").document(uid).collection("inventory").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(100),
            "category" to category.trim().ifEmpty { "Chocolates" }.take(50),
            "sku" to sku.trim().ifEmpty { "SKU-${(100..999).random()}" }.take(40),
            "unit" to unit.trim().ifEmpty { "box" }.take(20),
            "price" to price.coerceAtLeast(0.0),
            "costPrice" to costPrice.coerceAtLeast(0.0),
            "stockQuantity" to stockQuantity.coerceAtLeast(0L),
            "lowStockThreshold" to lowStockThreshold.coerceAtLeast(0L),
            "expiryDate" to expiryDate.trim().take(30),
            "supplierName" to supplierName.trim().take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStockQuantity(itemId: String, newQuantity: Long): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
        val updates = mapOf(
            "stockQuantity" to newQuantity.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- TRANSACTIONS, DAILY CREDIT & BILLING ---

    fun observeTransactions(userId: String): Flow<List<ShopTransaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ShopTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createBillTransaction(
        customer: Customer?,
        lineItems: List<BillLineItem>,
        paidNow: Double,
        paymentMethod: String,
        customSummaryNote: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val totalAmount = lineItems.sumOf { it.lineTotal }.coerceAtLeast(0.0)
        val safePaid = paidNow.coerceAtLeast(0.0)
        val balanceDelta = totalAmount - safePaid
        val txType = if (balanceDelta > 0.001) "CREDIT_SALE" else "CASH_SALE"
        val safeMethod = if (paymentMethod in listOf("CREDIT", "CASH", "UPI_CARD")) paymentMethod else "CREDIT"

        val itemsDescription = if (lineItems.isNotEmpty()) {
            lineItems.joinToString(", ") { "${it.quantity}x ${it.item.name} (₹${String.format(Locale.US, "%.2f", it.lineTotal)})" }
        } else {
            customSummaryNote.ifBlank { "Sandeep Cafe Counter Purchase" }
        }.take(500)

        val txId = sanitizeId("tx_${UUID.randomUUID().toString().take(12)}")
        val txRef = db.collection("users").document(uid).collection("transactions").document(txId)

        val txPayload = mapOf(
            "userId" to uid,
            "customerId" to (customer?.id?.ifBlank { "walk_in" } ?: "walk_in"),
            "customerName" to (customer?.name?.ifBlank { "Walk-in Customer" } ?: "Walk-in Customer").take(100),
            "customerPhone" to (customer?.phone ?: "").take(25),
            "type" to txType,
            "itemsSummary" to itemsDescription,
            "totalAmount" to totalAmount,
            "paidAmount" to safePaid,
            "balanceDelta" to balanceDelta,
            "paymentMethod" to safeMethod,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val batch = db.batch()
            batch.set(txRef, txPayload)

            // Deduct stock for each line item
            for (line in lineItems) {
                if (line.item.id.isNotBlank()) {
                    val itemRef = db.collection("users").document(uid).collection("inventory").document(line.item.id)
                    val newStock = (line.item.stockQuantity - line.quantity).coerceAtLeast(0L)
                    batch.update(
                        itemRef,
                        mapOf(
                            "stockQuantity" to newStock,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            // Update customer credit balance and totals if linked to a customer
            if (customer != null && customer.id.isNotBlank()) {
                val custRef = db.collection("users").document(uid).collection("customers").document(customer.id)
                val newCredit = customer.creditBalance + balanceDelta
                val newTotalBilled = (customer.totalBilled + totalAmount).coerceAtLeast(0.0)
                val newTotalPaid = (customer.totalPaid + safePaid).coerceAtLeast(0.0)
                val earnedPoints = (totalAmount / 100.0).toLong().coerceAtLeast(0L)
                batch.update(
                    custRef,
                    mapOf(
                        "creditBalance" to newCredit,
                        "totalBilled" to newTotalBilled,
                        "totalPaid" to newTotalPaid,
                        "loyaltyPoints" to (customer.loyaltyPoints + earnedPoints),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            Result.failure(e)
        }
    }

    /**
     * Quick Daily Credit Updater: Atomically adds today's credit purchase for a customer
     * and updates their overall credit balance and total billed in Firestore.
     */
    suspend fun addDailyCreditEntry(
        customer: Customer,
        creditAmount: Double,
        itemsSummaryNote: String
    ): Result<String> {
        val uid = requireUserId()
        val safeAmount = creditAmount.coerceAtLeast(0.01)
        val summary = itemsSummaryNote.trim().ifBlank {
            "Daily Cafe & Confectionery Credit Entry"
        }.take(500)

        val txId = sanitizeId("daily_${UUID.randomUUID().toString().take(12)}")
        val txRef = db.collection("users").document(uid).collection("transactions").document(txId)
        val custRef = db.collection("users").document(uid).collection("customers").document(customer.id)

        val txPayload = mapOf(
            "userId" to uid,
            "customerId" to customer.id,
            "customerName" to customer.name.take(100),
            "customerPhone" to customer.phone.take(25),
            "type" to "CREDIT_SALE",
            "itemsSummary" to summary,
            "totalAmount" to safeAmount,
            "paidAmount" to 0.0,
            "balanceDelta" to safeAmount,
            "paymentMethod" to "CREDIT",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val earnedPoints = (safeAmount / 100.0).toLong().coerceAtLeast(0L)
        return try {
            val batch = db.batch()
            batch.set(txRef, txPayload)
            batch.update(
                custRef,
                mapOf(
                    "creditBalance" to (customer.creditBalance + safeAmount),
                    "totalBilled" to (customer.totalBilled + safeAmount).coerceAtLeast(0.0),
                    "loyaltyPoints" to (customer.loyaltyPoints + earnedPoints),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordCreditPayment(
        customer: Customer,
        paymentAmount: Double,
        paymentMethod: String = "CASH",
        note: String = "Credit Balance Payment Submitted"
    ): Result<String> {
        val uid = requireUserId()
        val safePayment = paymentAmount.coerceAtLeast(0.01)
        val safeMethod = if (paymentMethod in listOf("CREDIT", "CASH", "UPI_CARD")) paymentMethod else "CASH"
        val txId = sanitizeId("pay_${UUID.randomUUID().toString().take(12)}")
        val txRef = db.collection("users").document(uid).collection("transactions").document(txId)
        val custRef = db.collection("users").document(uid).collection("customers").document(customer.id)

        val txPayload = mapOf(
            "userId" to uid,
            "customerId" to customer.id,
            "customerName" to customer.name.take(100),
            "customerPhone" to customer.phone.take(25),
            "type" to "CREDIT_PAYMENT",
            "itemsSummary" to note.ifBlank { "Credit Balance Payment Submitted" }.take(500),
            "totalAmount" to 0.0,
            "paidAmount" to safePayment,
            "balanceDelta" to -safePayment,
            "paymentMethod" to safeMethod,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val batch = db.batch()
            batch.set(txRef, txPayload)
            batch.update(
                custRef,
                mapOf(
                    "creditBalance" to (customer.creditBalance - safePayment),
                    "totalPaid" to (customer.totalPaid + safePayment).coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            Result.failure(e)
        }
    }
}
