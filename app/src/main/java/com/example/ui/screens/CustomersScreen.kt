package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.ShopTransaction
import com.example.ui.theme.OnVibrantBerryContainer
import com.example.ui.theme.OnVibrantEmeraldContainer
import com.example.ui.theme.OnVibrantPurpleContainer
import com.example.ui.theme.OnVibrantSaffronContainer
import com.example.ui.theme.OnVibrantSkyContainer
import com.example.ui.theme.VibrantBerryContainer
import com.example.ui.theme.VibrantBerryPink
import com.example.ui.theme.VibrantEmeraldContainer
import com.example.ui.theme.VibrantEmeraldGreen
import com.example.ui.theme.VibrantPurpleContainer
import com.example.ui.theme.VibrantRoyalPurple
import com.example.ui.theme.VibrantSaffronContainer
import com.example.ui.theme.VibrantSaffronGold
import com.example.ui.theme.VibrantSkyBlue
import com.example.ui.theme.VibrantSkyContainer
import com.example.util.SmsHelper
import com.example.viewmodel.ShopViewModel
import java.util.Locale

@Composable
fun CustomersScreen(
    customers: List<Customer>,
    transactions: List<ShopTransaction>,
    viewModel: ShopViewModel,
    onStartBillForCustomer: (Customer) -> Unit,
    onTriggerOneTimeCustomerSms: (Customer) -> Unit = {}
) {
    var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterMode by rememberSaveable { mutableStateOf("ALL") } // ALL, OWING, WEEKLY_SMS
    var showAddCustomerDialog by rememberSaveable { mutableStateOf(false) }
    var customerForDailyCreditDialog by remember { mutableStateOf<Customer?>(null) }
    var customerForPaymentDialog by remember { mutableStateOf<Customer?>(null) }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }

    if (selectedCustomer != null) {
        CustomerDetailLedgerView(
            customer = selectedCustomer,
            allTransactions = transactions,
            viewModel = viewModel,
            onBack = { selectedCustomerId = null },
            onStartBill = {
                onStartBillForCustomer(selectedCustomer)
            },
            onOpenDailyCreditDialog = { customerForDailyCreditDialog = selectedCustomer },
            onOpenPaymentDialog = { customerForPaymentDialog = selectedCustomer },
            onTriggerOneTimeSms = { onTriggerOneTimeCustomerSms(selectedCustomer) }
        )
    } else {
        val totalCreditAllCustomers = remember(customers) {
            customers.sumOf { it.creditBalance.coerceAtLeast(0.0) }
        }
        val todayCreditAllCustomers = remember(transactions) {
            viewModel.calculateTodayTotalCreditAllCustomers(transactions)
        }

        val filteredCustomers = remember(customers, searchQuery, filterMode) {
            customers.filter { cust ->
                val matchesQuery = searchQuery.isBlank() ||
                    cust.name.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (filterMode) {
                    "OWING" -> cust.creditBalance > 0.01
                    "WEEKLY_SMS" -> cust.weeklyReminderEnabled
                    else -> true
                }
                matchesQuery && matchesFilter
            }
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddCustomerDialog = true },
                    containerColor = VibrantBerryPink,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_customer_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Customer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Prominent Daily Credit & Total Credit Summary Banner (Requirement 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = VibrantSaffronContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "TODAY'S CREDIT ADDED",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantSaffronContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+₹${String.format(Locale.US, "%.2f", todayCreditAllCustomers)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = VibrantSaffronGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap '+ Daily Credit' below",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantSaffronContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = VibrantBerryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "TOTAL CREDIT BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantBerryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", totalCreditAllCustomers)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = VibrantBerryPink,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${customers.count { it.creditBalance > 0.01 }} of ${customers.size} customers due",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantBerryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Sandeep Cafe customer or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { filterMode = "ALL" },
                        label = { Text("All (${customers.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VibrantBerryPink,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterMode == "OWING",
                        onClick = { filterMode = "OWING" },
                        label = { Text("Credit Due (${customers.count { it.creditBalance > 0.01 }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VibrantSaffronGold,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterMode == "WEEKLY_SMS",
                        onClick = { filterMode = "WEEKLY_SMS" },
                        label = { Text("Auto SMS (${customers.count { it.weeklyReminderEnabled }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VibrantEmeraldGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = VibrantBerryPink
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matching customers found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap 'Add Customer' to register a customer and update their daily ₹ credit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filteredCustomers, key = { _, it -> it.id }) { index, customer ->
                            val todayCustomerCredit = remember(customer.id, transactions) {
                                viewModel.calculateTodayCreditForCustomer(customer.id, transactions)
                            }
                            CustomerCardItem(
                                index = index,
                                customer = customer,
                                todayCreditAdded = todayCustomerCredit,
                                onClick = { selectedCustomerId = customer.id },
                                onAddDailyCreditClick = { customerForDailyCreditDialog = customer },
                                onReceivePaymentClick = { customerForPaymentDialog = customer },
                                onSendQuickSms = { onTriggerOneTimeCustomerSms(customer) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, notes, initialCredit, weeklyEnabled ->
                viewModel.addCustomer(
                    name = name,
                    phone = phone,
                    notes = notes,
                    initialCredit = initialCredit,
                    weeklyReminderEnabled = weeklyEnabled
                )
                showAddCustomerDialog = false
            }
        )
    }

    customerForDailyCreditDialog?.let { targetCustomer ->
        val todayCreditSoFar = viewModel.calculateTodayCreditForCustomer(targetCustomer.id, transactions)
        DailyCreditQuickUpdateDialog(
            customer = targetCustomer,
            todayCreditSoFar = todayCreditSoFar,
            onDismiss = { customerForDailyCreditDialog = null },
            onConfirm = { creditAmount, itemsNote ->
                viewModel.addDailyCreditForCustomer(
                    customer = targetCustomer,
                    creditAmount = creditAmount,
                    itemsNote = itemsNote
                )
                customerForDailyCreditDialog = null
            }
        )
    }

    customerForPaymentDialog?.let { targetCustomer ->
        RecordPaymentDialog(
            customer = targetCustomer,
            onDismiss = { customerForPaymentDialog = null },
            onConfirm = { amount, method, note ->
                viewModel.recordPayment(targetCustomer, amount, method, note)
                customerForPaymentDialog = null
            }
        )
    }
}

@Composable
private fun CustomerCardItem(
    index: Int,
    customer: Customer,
    todayCreditAdded: Double,
    onClick: () -> Unit,
    onAddDailyCreditClick: () -> Unit,
    onReceivePaymentClick: () -> Unit,
    onSendQuickSms: () -> Unit
) {
    val avatarColors = listOf(
        VibrantBerryPink,
        VibrantSaffronGold,
        VibrantEmeraldGreen,
        VibrantRoyalPurple,
        VibrantSkyBlue
    )
    val badgeColor = avatarColors[index % avatarColors.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (customer.weeklyReminderEnabled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Weekly SMS Alert Enabled",
                                tint = VibrantSkyBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Billed: ₹${String.format(Locale.US, "%.2f", customer.totalBilled)} • Paid: ₹${String.format(Locale.US, "%.2f", customer.totalPaid)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", customer.creditBalance)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.creditBalance > 0.01) {
                            VibrantBerryPink
                        } else {
                            VibrantEmeraldGreen
                        }
                    )
                    Text(
                        text = if (customer.creditBalance > 0.01) "Total Credit Due" else "Settled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (todayCreditAdded > 0.001) {
                        Surface(
                            color = VibrantSaffronContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Text(
                                text = "Today: +₹${String.format(Locale.US, "%.0f", todayCreditAdded)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantSaffronContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Colorful Quick Action Buttons Row on every Customer Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddDailyCreditClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantSaffronGold,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(38.dp)
                        .testTag("quick_daily_credit_${customer.id}")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Daily Credit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onReceivePaymentClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VibrantEmeraldContainer,
                        contentColor = OnVibrantEmeraldContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("₹ Receive", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onSendQuickSms,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VibrantSkyContainer,
                        contentColor = OnVibrantSkyContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("card_auto_sms_${customer.id}")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send Weekly SMS Report", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailLedgerView(
    customer: Customer,
    allTransactions: List<ShopTransaction>,
    viewModel: ShopViewModel,
    onBack: () -> Unit,
    onStartBill: () -> Unit,
    onOpenDailyCreditDialog: () -> Unit,
    onOpenPaymentDialog: () -> Unit,
    onTriggerOneTimeSms: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val weeklyReport = remember(customer, allTransactions) {
        viewModel.buildWeeklyCustomerReport(customer, allTransactions)
    }
    val customerTransactions = remember(customer.id, allTransactions) {
        allTransactions.filter { it.customerId == customer.id }
    }
    val todayCreditForCustomer = remember(customer.id, allTransactions) {
        viewModel.calculateTodayCreditForCustomer(customer.id, allTransactions)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_detail_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row with Back button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("customer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Customers"
                        )
                    }
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = VibrantSaffronGold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${customer.loyaltyPoints} SweetPoints",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibrantSaffronGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        viewModel.deleteCustomer(customer.id, customer.name)
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Customer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Credit Balance & Daily Credit Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantBerryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL CREDIT BALANCE DUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnVibrantBerryContainer.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = VibrantSaffronContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Today's Credit: +₹${String.format(Locale.US, "%.2f", todayCreditForCustomer)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantSaffronContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", customer.creditBalance)}",
                        style = MaterialTheme.typography.displayLarge,
                        color = VibrantBerryPink,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Billed",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", customer.totalBilled)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Submitted / Paid",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", customer.totalPaid)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VibrantEmeraldGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenDailyCreditDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibrantSaffronGold,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_add_daily_credit_button")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Daily Credit")
                        }
                        Button(
                            onClick = onOpenPaymentDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibrantEmeraldGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("record_credit_payment_button")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receive ₹")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = onStartBill,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = VibrantPurpleContainer,
                            contentColor = OnVibrantPurpleContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_new_bill_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Itemized POS Bill from Stock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Weekly SMS Alert & Weekly Credit Report Card (2.1, 2.2 & 4)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantSkyContainer.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automated Weekly SMS Reminder",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnVibrantSkyContainer
                            )
                            Text(
                                text = if (customer.lastReminderSentAt.isBlank()) {
                                    "Asks 1 time right before sending weekly SMS"
                                } else {
                                    "Last sent: ${customer.lastReminderSentAt}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = OnVibrantSkyContainer.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = customer.weeklyReminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateCustomer(
                                    customerId = customer.id,
                                    name = customer.name,
                                    phone = customer.phone,
                                    notes = customer.notes,
                                    weeklyReminderEnabled = enabled
                                )
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Text(
                        text = "Weekly Credit Report (Last 7 Days)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnVibrantSkyContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Bought (7d)", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", weeklyReport.weekPurchasesTotal)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Submitted (7d)", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", weeklyReport.weekSubmittedTotal)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = VibrantEmeraldGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Balance Left", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", weeklyReport.remainingBalance)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = VibrantBerryPink
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = weeklyReport.formattedSmsBody,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTriggerOneTimeSms,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibrantSkyBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_customer_sms_report_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto SMS Report")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.markWeeklyReminderDispatched(customer)
                                SmsHelper.shareTextReport(
                                    context = context,
                                    subject = "Sandeep Cafe Weekly Credit Report - ${customer.name}",
                                    body = weeklyReport.formattedSmsBody
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Report")
                        }
                    }
                }
            }
        }

        // Itemized Purchase & Payment History with Date & Time (Requirement 2.3)
        item {
            Text(
                text = "Purchase & Payment History (${customerTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (customerTransactions.isEmpty()) {
            item {
                Text(
                    text = "No purchases or credit payments recorded for ${customer.name} yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(customerTransactions, key = { it.id }) { tx ->
                TransactionRowCard(tx = tx)
            }
        }
    }
}

@Composable
private fun DailyCreditQuickUpdateDialog(
    customer: Customer,
    todayCreditSoFar: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var creditAmountText by rememberSaveable { mutableStateOf("") }
    var itemsBoughtNote by rememberSaveable { mutableStateOf("Daily Cafe & Sweets Purchase") }

    val enteredAmount = creditAmountText.toDoubleOrNull() ?: 0.0
    val updatedTodayCredit = todayCreditSoFar + enteredAmount
    val updatedTotalCredit = customer.creditBalance + enteredAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Update Daily Credit — ${customer.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sandeep Cafe Daily Khata Entry (₹)",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibrantSaffronGold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibrantSaffronContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Previous Balance:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", customer.creditBalance)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Today's Credit:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "+₹${String.format(Locale.US, "%.2f", updatedTodayCredit)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = VibrantSaffronGold
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "New Total Credit Due:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "₹${String.format(Locale.US, "%.2f", updatedTotalCredit)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VibrantBerryPink
                            )
                        }
                    }
                }

                // Quick Preset ₹ Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50, 100, 150, 250, 500).forEach { preset ->
                        AssistChip(
                            onClick = { creditAmountText = preset.toString() },
                            label = { Text("+₹$preset", fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = VibrantBerryContainer,
                                labelColor = OnVibrantBerryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = creditAmountText,
                    onValueChange = { creditAmountText = it },
                    label = { Text("Today's Credit Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_credit_amount_input")
                )

                OutlinedTextField(
                    value = itemsBoughtNote,
                    onValueChange = { itemsBoughtNote = it },
                    label = { Text("What they bought today") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = creditAmountText.toDoubleOrNull() ?: 0.0
                    onConfirm(amt, itemsBoughtNote)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantSaffronGold,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_daily_credit_button")
            ) {
                Text("Add ₹ Credit & Update Total")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Boolean) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var initialCredit by rememberSaveable { mutableStateOf("0") }
    var weeklyReminder by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sandeep Cafe Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (for SMS alerts) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_phone_input")
                )
                OutlinedTextField(
                    value = initialCredit,
                    onValueChange = { initialCredit = it },
                    label = { Text("Opening / Today's Credit Balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Cafe & Sweet Preferences / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automated Weekly SMS Reminder",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = weeklyReminder,
                        onCheckedChange = { weeklyReminder = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        phone,
                        notes,
                        initialCredit.toDoubleOrNull() ?: 0.0,
                        weeklyReminder
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantBerryPink),
                modifier = Modifier.testTag("confirm_add_customer_button")
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RecordPaymentDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var amountText by rememberSaveable {
        mutableStateOf(
            if (customer.creditBalance > 0) String.format(Locale.US, "%.2f", customer.creditBalance) else ""
        )
    }
    var method by rememberSaveable { mutableStateOf("CASH") }
    var note by rememberSaveable { mutableStateOf("Weekly Credit Balance Submitted") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive ₹ Payment from ${customer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current Balance Due: ₹${String.format(Locale.US, "%.2f", customer.creditBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = VibrantBerryPink
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Submitted (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = method == "CASH",
                        onClick = { method = "CASH" },
                        label = { Text("Cash (₹)") }
                    )
                    FilterChip(
                        selected = method == "UPI_CARD",
                        onClick = { method = "UPI_CARD" },
                        label = { Text("UPI / Card") }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Payment Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(amt, method, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantEmeraldGreen),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Record ₹ Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
