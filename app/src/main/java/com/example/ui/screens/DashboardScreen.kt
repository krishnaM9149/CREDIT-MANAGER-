package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.InventoryItem
import com.example.data.model.ShopTransaction
import com.example.ui.theme.OnVibrantBerryContainer
import com.example.ui.theme.OnVibrantCoralContainer
import com.example.ui.theme.OnVibrantEmeraldContainer
import com.example.ui.theme.OnVibrantPurpleContainer
import com.example.ui.theme.OnVibrantSaffronContainer
import com.example.ui.theme.OnVibrantSkyContainer
import com.example.ui.theme.VibrantBerryContainer
import com.example.ui.theme.VibrantBerryPink
import com.example.ui.theme.VibrantCoralContainer
import com.example.ui.theme.VibrantCoralRed
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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    customers: List<Customer>,
    inventory: List<InventoryItem>,
    transactions: List<ShopTransaction>,
    viewModel: ShopViewModel,
    onNavigateToCustomers: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onTriggerOneTimeAutoSms: (Customer?) -> Unit = {}
) {
    val context = LocalContext.current
    val totalCreditOwed = customers.sumOf { it.creditBalance.coerceAtLeast(0.0) }
    val todayTotalCreditAdded = remember(transactions) {
        viewModel.calculateTodayTotalCreditAllCustomers(transactions)
    }

    val sevenDaysAgoMillis = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L)
    val weekTransactions = transactions.filter {
        ((it.createdAt ?: Timestamp.now()).toDate().time) >= sevenDaysAgoMillis
    }
    val weekCollected = weekTransactions.sumOf { it.paidAmount }
    val weekBilled = weekTransactions.sumOf { it.totalAmount }
    val lowStockItems = inventory.filter { it.stockQuantity <= it.lowStockThreshold }
    val dueCustomersForWeeklySms = customers.filter { it.weeklyReminderEnabled && it.creditBalance > 0.01 }

    // State for Quick Daily Credit Updater right on Dashboard
    var selectedQuickCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickCreditAmount by rememberSaveable { mutableStateOf("") }
    var quickCreditNote by rememberSaveable { mutableStateOf("Tea, Cold Coffee & Confectionery") }

    val activeQuickCustomer = customers.find { it.id == selectedQuickCustomerId } ?: customers.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sandeep Cafe Vibrant Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_shop_hero_illustration),
                        contentDescription = "Sandeep Cafe Counter",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(184.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = VibrantSaffronContainer.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SANDEEP CAFE • KHATA & POS (₹)",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantSaffronContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sandeep Cafe",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${customers.size} Customers • Total Credit: ₹${String.format(Locale.US, "%.2f", totalCreditOwed)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToBilling,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibrantSaffronContainer,
                                    contentColor = OnVibrantSaffronContainer
                                ),
                                modifier = Modifier.testTag("hero_new_bill_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New ₹ Bill", fontWeight = FontWeight.Bold)
                            }
                            if (customers.isEmpty() && inventory.isEmpty()) {
                                Button(
                                    onClick = { viewModel.seedSampleConfectioneryShopData() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibrantEmeraldContainer,
                                        contentColor = OnVibrantEmeraldContainer
                                    ),
                                    modifier = Modifier.testTag("seed_sample_data_button")
                                ) {
                                    Text("Load Sandeep Cafe Demo", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onNavigateToStudio,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("hero_ai_studio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Studio")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Vibrant Multi-Color Metrics Grid (Total Credit, Today's Daily Credit, Weekly Paid, SMS & Stock)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricSummaryCard(
                        title = "Total Credit Balance",
                        value = "₹${String.format(Locale.US, "%.2f", totalCreditOwed)}",
                        subtitle = "${customers.count { it.creditBalance > 0.01 }} customers owing",
                        icon = Icons.Default.AccountBalanceWallet,
                        containerColor = VibrantBerryContainer,
                        contentColor = OnVibrantBerryContainer,
                        accentIconBg = VibrantBerryPink,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToCustomers() }
                    )
                    MetricSummaryCard(
                        title = "Today's Credit Added",
                        value = "+₹${String.format(Locale.US, "%.2f", todayTotalCreditAdded)}",
                        subtitle = "Daily customer credit",
                        icon = Icons.Default.Today,
                        containerColor = VibrantSaffronContainer,
                        contentColor = OnVibrantSaffronContainer,
                        accentIconBg = VibrantSaffronGold,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToCustomers() }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricSummaryCard(
                        title = "Submitted (7d)",
                        value = "₹${String.format(Locale.US, "%.2f", weekCollected)}",
                        subtitle = "Billed: ₹${String.format(Locale.US, "%.2f", weekBilled)}",
                        icon = Icons.Default.TrendingUp,
                        containerColor = VibrantEmeraldContainer,
                        contentColor = OnVibrantEmeraldContainer,
                        accentIconBg = VibrantEmeraldGreen,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToBilling() }
                    )
                    MetricSummaryCard(
                        title = if (lowStockItems.isNotEmpty()) "Low Stock Alerts" else "Auto SMS Queue",
                        value = if (lowStockItems.isNotEmpty()) "${lowStockItems.size} Items" else "${dueCustomersForWeeklySms.size} Due",
                        subtitle = if (lowStockItems.isNotEmpty()) "Tap to restock" else "1-prompt SMS ready",
                        icon = if (lowStockItems.isNotEmpty()) Icons.Default.WarningAmber else Icons.Default.NotificationsActive,
                        containerColor = if (lowStockItems.isNotEmpty()) VibrantCoralContainer else VibrantPurpleContainer,
                        contentColor = if (lowStockItems.isNotEmpty()) OnVibrantCoralContainer else OnVibrantPurpleContainer,
                        accentIconBg = if (lowStockItems.isNotEmpty()) VibrantCoralRed else VibrantRoyalPurple,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (lowStockItems.isNotEmpty()) onNavigateToInventory()
                                else onTriggerOneTimeAutoSms(null)
                            }
                    )
                }
            }
        }

        // 3. Dedicated Daily Credit Quick-Updater for Any Customer (Requirement 3)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantSaffronContainer.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(VibrantSaffronGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Update Daily Customer Credit (₹)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnVibrantSaffronContainer
                                )
                                Text(
                                    text = "Today's Total Added: ₹${String.format(Locale.US, "%.2f", todayTotalCreditAdded)} • Overall Due: ₹${String.format(Locale.US, "%.2f", totalCreditOwed)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnVibrantSaffronContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (customers.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add a customer or load demo data to update daily credit.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onNavigateToCustomers,
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantBerryPink)
                            ) {
                                Text("Add Customer")
                            }
                        }
                    } else {
                        Text(
                            text = "1. Select Customer:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnVibrantSaffronContainer
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customers.forEach { cust ->
                                val isSelected = activeQuickCustomer?.id == cust.id
                                val custToday = viewModel.calculateTodayCreditForCustomer(cust.id, transactions)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedQuickCustomerId = cust.id },
                                    label = {
                                        Text(
                                            text = "${cust.name} (Total: ₹${String.format(Locale.US, "%.0f", cust.creditBalance)}${if (custToday > 0) " | Today: +₹${String.format(Locale.US, "%.0f", custToday)}" else ""})"
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VibrantBerryPink,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Rupee amount chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick ₹:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            listOf(50, 100, 150, 250, 500).forEach { preset ->
                                AssistChip(
                                    onClick = { quickCreditAmount = preset.toString() },
                                    label = { Text("+₹$preset", fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color.White,
                                        labelColor = VibrantBerryPink
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = quickCreditAmount,
                                onValueChange = { quickCreditAmount = it },
                                label = { Text("Today's Credit (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("dashboard_daily_credit_amount_input")
                            )
                            OutlinedTextField(
                                value = quickCreditNote,
                                onValueChange = { quickCreditNote = it },
                                label = { Text("Items Bought") },
                                singleLine = true,
                                modifier = Modifier.weight(1.1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (activeQuickCustomer != null) {
                            val enteredAmt = quickCreditAmount.toDoubleOrNull() ?: 0.0
                            val previewTotal = activeQuickCustomer.creditBalance + enteredAmt
                            Button(
                                onClick = {
                                    if (enteredAmt > 0.0) {
                                        viewModel.addDailyCreditForCustomer(
                                            customer = activeQuickCustomer,
                                            creditAmount = enteredAmt,
                                            itemsNote = quickCreditNote
                                        )
                                        quickCreditAmount = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibrantBerryPink,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_add_daily_credit_button")
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (enteredAmt > 0) {
                                        "Add +₹${String.format(Locale.US, "%.2f", enteredAmt)} to ${activeQuickCustomer.name} (New Total: ₹${String.format(Locale.US, "%.2f", previewTotal)})"
                                    } else {
                                        "Update Today's Credit for ${activeQuickCustomer.name}"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Automated Weekly SMS Credit Reminder & Report Center (Requirements 2.1, 2.2 & 4)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantSkyContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(VibrantSkyBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Automated Weekly SMS Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnVibrantSkyContainer
                                )
                                Text(
                                    text = "Asks just 1 time before sending ₹ credit reports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnVibrantSkyContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (dueCustomersForWeeklySms.isNotEmpty()) {
                            Button(
                                onClick = { onTriggerOneTimeAutoSms(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibrantSkyBlue,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("auto_send_all_sms_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send All (${dueCustomersForWeeklySms.size})", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (dueCustomersForWeeklySms.isEmpty()) {
                        Text(
                            text = "No customers currently have pending weekly SMS credit alerts. Add daily credit or enable weekly reminders on a customer profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnVibrantSkyContainer
                        )
                    } else {
                        dueCustomersForWeeklySms.take(4).forEach { customer ->
                            val report = viewModel.buildWeeklyCustomerReport(customer, transactions)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = customer.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${customer.phone} • Balance Left: ₹${String.format(Locale.US, "%.2f", report.remainingBalance)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VibrantBerryPink,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "7d: Bought ₹${String.format(Locale.US, "%.2f", report.weekPurchasesTotal)} | Submitted ₹${String.format(Locale.US, "%.2f", report.weekSubmittedTotal)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (customer.lastReminderSentAt.isNotBlank()) {
                                                Text(
                                                    text = "Last SMS sent: ${customer.lastReminderSentAt}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = VibrantEmeraldGreen
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { onTriggerOneTimeAutoSms(customer) },
                                                modifier = Modifier.testTag("send_weekly_sms_${customer.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Send Weekly SMS Reminder",
                                                    tint = VibrantSkyBlue
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.markWeeklyReminderDispatched(customer)
                                                    SmsHelper.shareTextReport(
                                                        context = context,
                                                        subject = "Sandeep Cafe Weekly Credit Statement - ${customer.name}",
                                                        body = report.formattedSmsBody
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share Weekly Credit Report",
                                                    tint = VibrantEmeraldGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Recent Itemized Purchases & Bills with Date/Time (Requirement 2.3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Purchases & ₹ Credit Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = onNavigateToBilling,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VibrantPurpleContainer,
                        contentColor = OnVibrantPurpleContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("View All Bills", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = VibrantBerryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = VibrantBerryPink
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions recorded yet",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Tap 'Load Sandeep Cafe Demo' above or create your first bill.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transactions.take(6), key = { it.id }) { tx ->
                TransactionRowCard(tx = tx)
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    accentIconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(tx: ShopTransaction) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.US)
    val dateLabel = dateFormat.format((tx.createdAt ?: Timestamp.now()).toDate())
    val isPayment = tx.type == "CREDIT_PAYMENT"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPayment) {
                            VibrantEmeraldContainer
                        } else {
                            VibrantBerryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPayment) Icons.Default.AccountBalanceWallet else Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = if (isPayment) {
                        VibrantEmeraldGreen
                    } else {
                        VibrantBerryPink
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.customerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tx.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateLabel • ${tx.paymentMethod}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (isPayment) {
                    Text(
                        text = "+₹${String.format(Locale.US, "%.2f", tx.paidAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibrantEmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Submitted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", tx.totalAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (tx.balanceDelta > 0.01) {
                        Text(
                            text = "Credit +₹${String.format(Locale.US, "%.2f", tx.balanceDelta)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantBerryPink,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Paid Full",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantEmeraldGreen
                        )
                    }
                }
            }
        }
    }
}
