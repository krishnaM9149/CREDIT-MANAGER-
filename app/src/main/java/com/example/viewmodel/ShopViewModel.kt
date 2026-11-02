package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.BillLineItem
import com.example.data.model.Customer
import com.example.data.model.InventoryItem
import com.example.data.model.ShopTransaction
import com.example.data.model.SuggestedFeatureIdea
import com.example.data.model.WeeklyCustomerReport
import com.example.data.repository.ShopRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class ShopViewModel(
    private val repository: ShopRepository,
    private val currentUserId: String
) : ViewModel() {

    val customersState: StateFlow<UiState<List<Customer>>> = repository.observeCustomers(currentUserId)
        .map<List<Customer>, UiState<List<Customer>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing customers", error)
            emit(UiState.Error(error.message ?: "Failed to load customers"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val inventoryState: StateFlow<UiState<List<InventoryItem>>> = repository.observeInventory(currentUserId)
        .map<List<InventoryItem>, UiState<List<InventoryItem>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing inventory", error)
            emit(UiState.Error(error.message ?: "Failed to load stock items"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val transactionsState: StateFlow<UiState<List<ShopTransaction>>> = repository.observeTransactions(currentUserId)
        .map<List<ShopTransaction>, UiState<List<ShopTransaction>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing transactions", error)
            emit(UiState.Error(error.message ?: "Failed to load bills & ledger history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<BillLineItem>>(emptyList())
    val cartItems: StateFlow<List<BillLineItem>> = _cartItems.asStateFlow()

    private val _selectedBillingCustomer = MutableStateFlow<Customer?>(null)
    val selectedBillingCustomer: StateFlow<Customer?> = _selectedBillingCustomer.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Tracks whether the 1-time automated SMS confirmation has already been shown in this session
    private val _autoSmsPromptShownThisSession = MutableStateFlow(false)
    val autoSmsPromptShownThisSession: StateFlow<Boolean> = _autoSmsPromptShownThisSession.asStateFlow()

    val suggestedFeatures: List<SuggestedFeatureIdea> = listOf(
        SuggestedFeatureIdea(
            id = "expiry_radar",
            title = "1. Shelf-Life & Batch Expiry Radar",
            subtitle = "Built into Stock Manager",
            benefitTag = "Zero Spoilage",
            description = "Confectionery items like fresh cream truffles, rasmalai fudge, and fruit tarts have short shelf lives. Track batch expiry dates and automatically discount items 3 days before expiry."
        ),
        SuggestedFeatureIdea(
            id = "loyalty_points",
            title = "2. Sandeep Cafe SweetPoints & VIP Tiers",
            subtitle = "Built into Customer Ledger",
            benefitTag = "+28% Repeat Sales",
            description = "Automatically award 1 SweetPoint for every ₹100 billed. Customers can redeem points for complimentary festival gift boxes or artisan cold coffee."
        ),
        SuggestedFeatureIdea(
            id = "festival_hampers",
            title = "3. Festival & Wedding Bulk Hamper Pre-Orders",
            subtitle = "High-Ticket Confectionery Orders",
            benefitTag = "Seasonal Boost",
            description = "Manage custom gift box assortments for Diwali, Rakhi, Christmas, and weddings with advance ₹ deposit tracking and scheduled pickup alerts."
        ),
        SuggestedFeatureIdea(
            id = "margin_analyzer",
            title = "4. Recipe Cost vs. Selling Margin Tracker (₹)",
            subtitle = "Built into Stock & Dashboard",
            benefitTag = "Profit Visibility",
            description = "Compare batch production cost in ₹ (cocoa, dry fruits, dairy, packaging) against retail price to spot your highest-margin confections."
        ),
        SuggestedFeatureIdea(
            id = "whatsapp_upi_link",
            title = "5. Instant UPI Payment Link in Weekly SMS",
            subtitle = "Built into Weekly Credit Report",
            benefitTag = "2x Faster Collection",
            description = "Embed itemized purchase dates, submitted ₹ payments, and remaining credit balance in a single SMS or WhatsApp statement so customers pay on the spot."
        ),
        SuggestedFeatureIdea(
            id = "ai_visual_studio",
            title = "6. AI Cafe Packaging & Poster Studio",
            subtitle = "Available in Studio Tab",
            benefitTag = "Pro Marketing",
            description = "Generate 1K/2K/4K promotional posters for Sandeep Cafe and edit product photos with AI to showcase new seasonal sweets on Instagram and WhatsApp Status."
        )
    )

    fun markAutoSmsPromptShown() {
        _autoSmsPromptShownThisSession.value = true
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun selectCustomerForBilling(customer: Customer?) {
        _selectedBillingCustomer.value = customer
    }

    fun addToCart(item: InventoryItem) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        if (index >= 0) {
            val existing = current[index]
            if (existing.quantity < item.stockQuantity || item.stockQuantity == 0L) {
                current[index] = existing.copy(quantity = existing.quantity + 1)
            }
        } else {
            current.add(BillLineItem(item = item, quantity = 1))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(itemId: String, newQty: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index >= 0) {
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQty)
            }
            _cartItems.value = current
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun addCustomer(
        name: String,
        phone: String,
        notes: String,
        initialCredit: Double,
        weeklyReminderEnabled: Boolean
    ) {
        if (name.isBlank() || phone.isBlank()) {
            _statusBannerMessage.value = "Customer name and phone number are required."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addCustomer(
                name = name,
                phone = phone,
                notes = notes,
                initialCredit = initialCredit,
                weeklyReminderEnabled = weeklyReminderEnabled
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Added customer ${name.trim()} to Sandeep Cafe ledger."
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to add customer."
            }
        }
    }

    fun updateCustomer(
        customerId: String,
        name: String,
        phone: String,
        notes: String,
        weeklyReminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.updateCustomerDetails(
                customerId = customerId,
                name = name,
                phone = phone,
                notes = notes,
                weeklyReminderEnabled = weeklyReminderEnabled
            )
            result.onSuccess {
                _statusBannerMessage.value = "Updated ${name.trim()}'s settings."
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to update customer."
            }
        }
    }

    fun deleteCustomer(customerId: String, customerName: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId).onSuccess {
                _statusBannerMessage.value = "Removed $customerName."
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to delete customer."
            }
        }
    }

    /**
     * Updates daily credit of a customer and adds to their total credit balance.
     */
    fun addDailyCreditForCustomer(
        customer: Customer,
        creditAmount: Double,
        itemsNote: String
    ) {
        if (creditAmount <= 0.0) {
            _statusBannerMessage.value = "Enter a daily credit amount greater than ₹0."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addDailyCreditEntry(
                customer = customer,
                creditAmount = creditAmount,
                itemsSummaryNote = itemsNote
            )
            _isBusy.value = false
            result.onSuccess {
                val newTotal = customer.creditBalance + creditAmount
                _statusBannerMessage.value =
                    "Added ₹${String.format(Locale.US, "%.2f", creditAmount)} daily credit for ${customer.name}. Total Due: ₹${String.format(Locale.US, "%.2f", newTotal)}"
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to update daily credit."
            }
        }
    }

    fun recordPayment(
        customer: Customer,
        amount: Double,
        paymentMethod: String,
        note: String
    ) {
        if (amount <= 0.0) {
            _statusBannerMessage.value = "Enter a valid payment amount greater than ₹0."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordCreditPayment(
                customer = customer,
                paymentAmount = amount,
                paymentMethod = paymentMethod,
                note = note
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value =
                    "Recorded ₹${String.format(Locale.US, "%.2f", amount)} payment from ${customer.name}."
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to record payment."
            }
        }
    }

    fun markWeeklyReminderDispatched(customer: Customer) {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        viewModelScope.launch {
            repository.markReminderSent(customer.id, dateStr)
        }
    }

    fun markBatchWeeklyRemindersDispatched(customers: List<Customer>) {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        viewModelScope.launch {
            customers.forEach { customer ->
                repository.markReminderSent(customer.id, dateStr)
            }
            _statusBannerMessage.value =
                "Automated weekly SMS reminders prepared & logged for ${customers.size} customer(s)!"
        }
    }

    fun addInventoryItem(
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
    ) {
        if (name.isBlank()) {
            _statusBannerMessage.value = "Item name is required."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addInventoryItem(
                name = name,
                category = category,
                sku = sku,
                unit = unit,
                price = price,
                costPrice = costPrice,
                stockQuantity = stockQuantity,
                lowStockThreshold = lowStockThreshold,
                expiryDate = expiryDate,
                supplierName = supplierName
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Added ${name.trim()} to Sandeep Cafe inventory."
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to add stock item."
            }
        }
    }

    fun adjustStockQuantity(item: InventoryItem, delta: Long) {
        val updated = (item.stockQuantity + delta).coerceAtLeast(0L)
        viewModelScope.launch {
            repository.updateStockQuantity(item.id, updated).onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to update stock."
            }
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item.id).onSuccess {
                _statusBannerMessage.value = "Deleted ${item.name} from inventory."
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to delete item."
            }
        }
    }

    fun checkoutBill(
        paidNow: Double,
        paymentMethod: String,
        customNote: String = ""
    ) {
        val lines = _cartItems.value
        val customer = _selectedBillingCustomer.value
        val total = lines.sumOf { it.lineTotal }
        if (lines.isEmpty() && customNote.isBlank()) {
            _statusBannerMessage.value = "Add at least one item to the bill."
            return
        }
        if (paidNow < total && customer == null) {
            _statusBannerMessage.value = "Select a customer to record a credit balance sale."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createBillTransaction(
                customer = customer,
                lineItems = lines,
                paidNow = paidNow,
                paymentMethod = paymentMethod,
                customSummaryNote = customNote
            )
            _isBusy.value = false
            result.onSuccess {
                _cartItems.value = emptyList()
                val creditAdded = (total - paidNow).coerceAtLeast(0.0)
                _statusBannerMessage.value = if (creditAdded > 0.001 && customer != null) {
                    "Bill saved! Added ₹${String.format(Locale.US, "%.2f", creditAdded)} to ${customer.name}'s credit ledger."
                } else {
                    "Bill completed (₹${String.format(Locale.US, "%.2f", total)}) & stock updated!"
                }
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "Checkout failed."
            }
        }
    }

    /**
     * Returns start of today (00:00:00) in epoch milliseconds for calculating Today's Daily Credit.
     */
    private fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun calculateTodayCreditForCustomer(
        customerId: String,
        allTransactions: List<ShopTransaction>
    ): Double {
        val todayStart = startOfTodayMillis()
        return allTransactions
            .filter { tx ->
                tx.customerId == customerId &&
                    tx.balanceDelta > 0.0 &&
                    ((tx.createdAt ?: Timestamp.now()).toDate().time >= todayStart)
            }
            .sumOf { it.balanceDelta }
    }

    fun calculateTodayTotalCreditAllCustomers(allTransactions: List<ShopTransaction>): Double {
        val todayStart = startOfTodayMillis()
        return allTransactions
            .filter { tx ->
                tx.balanceDelta > 0.0 &&
                    ((tx.createdAt ?: Timestamp.now()).toDate().time >= todayStart)
            }
            .sumOf { it.balanceDelta }
    }

    fun buildWeeklyCustomerReport(
        customer: Customer,
        allTransactions: List<ShopTransaction>
    ): WeeklyCustomerReport {
        val sevenDaysAgoMillis = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L)
        val customerTxs = allTransactions.filter { it.customerId == customer.id }
        val weekTxs = customerTxs.filter { tx ->
            val txMillis = (tx.createdAt ?: Timestamp.now()).toDate().time
            txMillis >= sevenDaysAgoMillis
        }

        val weekBought = weekTxs.sumOf { it.totalAmount }
        val weekSubmitted = weekTxs.sumOf { it.paidAmount }
        val remaining = customer.creditBalance

        val dateFormat = SimpleDateFormat("dd MMM hh:mm a", Locale.US)
        val itemizedLines = if (weekTxs.isNotEmpty()) {
            weekTxs.take(5).joinToString("\n") { tx ->
                val dt = dateFormat.format((tx.createdAt ?: Timestamp.now()).toDate())
                when (tx.type) {
                    "CREDIT_PAYMENT" -> "• [$dt] Paid: ₹${String.format(Locale.US, "%.2f", tx.paidAmount)}"
                    else -> "• [$dt] ${tx.itemsSummary} (Bill: ₹${String.format(Locale.US, "%.2f", tx.totalAmount)}, Paid: ₹${String.format(Locale.US, "%.2f", tx.paidAmount)})"
                }
            }
        } else {
            "• No new purchases in the last 7 days"
        }

        val smsBody = buildString {
            appendLine("Sandeep Cafe - Weekly Credit Report")
            appendLine("Namaste ${customer.name},")
            appendLine("Here is your weekly credit summary at Sandeep Cafe:")
            appendLine("• Bought this week: ₹${String.format(Locale.US, "%.2f", weekBought)}")
            appendLine("• Submitted/Paid this week: ₹${String.format(Locale.US, "%.2f", weekSubmitted)}")
            appendLine("• Total Credit Balance Left: ₹${String.format(Locale.US, "%.2f", remaining)}")
            appendLine("Recent Activity:")
            appendLine(itemizedLines)
            if (remaining > 0.0) {
                append("Kindly clear the remaining credit of ₹${String.format(Locale.US, "%.2f", remaining)} at your convenience. Thank you for visiting Sandeep Cafe!")
            } else {
                append("Your credit account is all clear! Thank you for visiting Sandeep Cafe.")
            }
        }

        return WeeklyCustomerReport(
            customer = customer,
            weekPurchasesTotal = weekBought,
            weekSubmittedTotal = weekSubmitted,
            remainingBalance = remaining,
            weekTransactions = weekTxs,
            formattedSmsBody = smsBody
        )
    }

    fun seedSampleConfectioneryShopData() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                // 1. Add sample Sandeep Cafe & Confectionery inventory items in ₹ (INR)
                val item1Id = repository.addInventoryItem(
                    name = "Belgian Dark Chocolate Truffles",
                    category = "Chocolates",
                    sku = "CHO-101",
                    unit = "box",
                    price = 320.00,
                    costPrice = 180.00,
                    stockQuantity = 24L,
                    lowStockThreshold = 6L,
                    expiryDate = "2026-11-25",
                    supplierName = "Sandeep Cafe Chocolatier"
                ).getOrNull()

                val item2Id = repository.addInventoryItem(
                    name = "Pistachio Rose Baklava Assortment",
                    category = "Traditional Sweets",
                    sku = "SWT-204",
                    unit = "box",
                    price = 450.00,
                    costPrice = 260.00,
                    stockQuantity = 4L, // Low stock trigger
                    lowStockThreshold = 5L,
                    expiryDate = "2026-11-12",
                    supplierName = "Artisanal Oven"
                ).getOrNull()

                repository.addInventoryItem(
                    name = "Salted Caramel French Macarons (6pc)",
                    category = "Pastries & Tartlets",
                    sku = "PAS-309",
                    unit = "pack",
                    price = 280.00,
                    costPrice = 150.00,
                    stockQuantity = 18L,
                    lowStockThreshold = 5L,
                    expiryDate = "2026-11-10",
                    supplierName = "Sandeep Bakehouse"
                )

                repository.addInventoryItem(
                    name = "Royal Saffron Kaju Katli Gift Hamper",
                    category = "Gift Hampers",
                    sku = "HMP-401",
                    unit = "box",
                    price = 650.00,
                    costPrice = 380.00,
                    stockQuantity = 12L,
                    lowStockThreshold = 4L,
                    expiryDate = "2026-12-05",
                    supplierName = "Heritage Mithai Craft"
                )

                repository.addInventoryItem(
                    name = "Hazelnut Cold Coffee & Brownie Combo",
                    category = "Chocolates",
                    sku = "CAF-502",
                    unit = "combo",
                    price = 180.00,
                    costPrice = 90.00,
                    stockQuantity = 30L,
                    lowStockThreshold = 8L,
                    expiryDate = "2026-11-18",
                    supplierName = "Sandeep Cafe Counter"
                )

                // 2. Add sample customers and itemized transactions in ₹
                val cust1Id = repository.addCustomer(
                    name = "Ananya Verma",
                    phone = "+919876501234",
                    notes = "Daily cold coffee, dark chocolate truffles & macarons",
                    initialCredit = 0.0,
                    weeklyReminderEnabled = true
                ).getOrNull()

                val cust2Id = repository.addCustomer(
                    name = "Rohan Kapoor",
                    phone = "+919812345678",
                    notes = "Prefers festive gift hampers; settles credit every Sunday",
                    initialCredit = 0.0,
                    weeklyReminderEnabled = true
                ).getOrNull()

                if (cust1Id != null && item1Id != null) {
                    val cust1 = Customer(
                        id = cust1Id,
                        userId = currentUserId,
                        name = "Ananya Verma",
                        phone = "+919876501234",
                        notes = "Daily cold coffee, dark chocolate truffles & macarons",
                        creditBalance = 0.0,
                        totalBilled = 0.0,
                        totalPaid = 0.0,
                        weeklyReminderEnabled = true
                    )
                    val truffleItem = InventoryItem(
                        id = item1Id,
                        userId = currentUserId,
                        name = "Belgian Dark Chocolate Truffles",
                        price = 320.00,
                        costPrice = 180.00,
                        stockQuantity = 24L
                    )
                    repository.createBillTransaction(
                        customer = cust1,
                        lineItems = listOf(BillLineItem(truffleItem, 2)),
                        paidNow = 200.00,
                        paymentMethod = "CREDIT"
                    )
                }

                if (cust2Id != null && item2Id != null) {
                    val cust2 = Customer(
                        id = cust2Id,
                        userId = currentUserId,
                        name = "Rohan Kapoor",
                        phone = "+919812345678",
                        notes = "Prefers festive gift hampers; settles credit every Sunday",
                        creditBalance = 0.0,
                        totalBilled = 0.0,
                        totalPaid = 0.0,
                        weeklyReminderEnabled = true
                    )
                    val baklavaItem = InventoryItem(
                        id = item2Id,
                        userId = currentUserId,
                        name = "Pistachio Rose Baklava Assortment",
                        price = 450.00,
                        costPrice = 260.00,
                        stockQuantity = 6L
                    )
                    repository.createBillTransaction(
                        customer = cust2,
                        lineItems = listOf(BillLineItem(baklavaItem, 2)),
                        paidNow = 300.00,
                        paymentMethod = "CREDIT"
                    )
                }

                _statusBannerMessage.value = "Loaded Sandeep Cafe menu, customers & ₹ credit bills!"
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to seed sample data."
            } finally {
                _isBusy.value = false
            }
        }
    }

    companion object {
        private const val TAG = "ShopViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ShopViewModel(ShopRepository(db), currentUserId)
            }
        }
    }
}
