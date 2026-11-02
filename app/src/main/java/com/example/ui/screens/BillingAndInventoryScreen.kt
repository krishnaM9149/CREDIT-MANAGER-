package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.viewmodel.ShopViewModel
import java.util.Locale

@Composable
fun BillingScreen(
    customers: List<Customer>,
    inventory: List<InventoryItem>,
    transactions: List<ShopTransaction>,
    viewModel: ShopViewModel
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedBillingCustomer.collectAsStateWithLifecycle()

    var showCheckoutDialog by rememberSaveable { mutableStateOf(false) }
    var activeSubTab by rememberSaveable { mutableStateOf("POS") } // POS or HISTORY

    val cartTotal = cartItems.sumOf { it.lineTotal }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeSubTab == "POS",
                onClick = { activeSubTab = "POS" },
                label = { Text("Sandeep Cafe POS Counter (₹)") },
                leadingIcon = { Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VibrantEmeraldGreen,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
            FilterChip(
                selected = activeSubTab == "HISTORY",
                onClick = { activeSubTab = "HISTORY" },
                label = { Text("Bills History (${transactions.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VibrantBerryPink,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeSubTab == "HISTORY") {
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bills or credit transactions recorded yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRowCard(tx = tx)
                    }
                }
            }
            return@Column
        }

        // 1. Customer Selector Strip for Credit / Khata Billing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = VibrantSaffronContainer.copy(alpha = 0.65f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Bill To Customer (Select for ₹ Credit Balance Tracking):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnVibrantSaffronContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCustomer == null,
                        onClick = { viewModel.selectCustomerForBilling(null) },
                        label = { Text("Walk-in (Cash/UPI)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VibrantEmeraldGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    customers.forEach { cust ->
                        FilterChip(
                            selected = selectedCustomer?.id == cust.id,
                            onClick = { viewModel.selectCustomerForBilling(cust) },
                            label = {
                                Text("${cust.name} (Due: ₹${String.format(Locale.US, "%.0f", cust.creditBalance)})")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VibrantBerryPink,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Cart Summary Bar if items added
        if (cartItems.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantEmeraldContainer
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT BILL (${cartItems.sumOf { it.quantity }} items)",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantEmeraldContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", cartTotal)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = VibrantEmeraldGreen
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.clearCart() }) {
                                Text("Clear", color = VibrantCoralRed)
                            }
                            Button(
                                onClick = { showCheckoutDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibrantEmeraldGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("open_checkout_button")
                            ) {
                                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checkout ₹")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    cartItems.forEach { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${line.item.name} (₹${String.format(Locale.US, "%.2f", line.item.price)})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(line.item.id, line.quantity - 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${line.quantity}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(line.item.id, line.quantity + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", line.lineTotal)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Available Confectionery Stock List to Add to Bill
        Text(
            text = "Tap Sandeep Cafe Items to Add to Bill:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (inventory.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantPurpleContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No cafe or confectionery items in stock yet.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.seedSampleConfectioneryShopData() },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantBerryPink)
                    ) {
                        Text("Load Sandeep Cafe Menu (₹)")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(inventory, key = { it.id }) { item ->
                    val inCartQty = cartItems.find { it.item.id == item.id }?.quantity ?: 0
                    val catColor = categoryAccentColor(item.category)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "₹",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = catColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.category} • ₹${String.format(Locale.US, "%.2f", item.price)} / ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = catColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Stock: ${item.stockQuantity} ${item.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.stockQuantity <= item.lowStockThreshold) {
                                            VibrantCoralRed
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { viewModel.addToCart(item) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (inCartQty > 0) VibrantEmeraldGreen else catColor.copy(alpha = 0.15f),
                                    contentColor = if (inCartQty > 0) Color.White else catColor
                                ),
                                modifier = Modifier.testTag("add_to_bill_${item.id}")
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (inCartQty > 0) "Added ($inCartQty)" else "Add")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCheckoutDialog) {
        CheckoutBillDialog(
            customer = selectedCustomer,
            totalAmount = cartTotal,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { paidNow, method ->
                viewModel.checkoutBill(
                    paidNow = paidNow,
                    paymentMethod = method
                )
                showCheckoutDialog = false
            }
        )
    }
}

private fun categoryAccentColor(category: String): Color {
    return when {
        category.contains("Chocolate", ignoreCase = true) -> VibrantBerryPink
        category.contains("Sweet", ignoreCase = true) -> VibrantSaffronGold
        category.contains("Pastries", ignoreCase = true) -> VibrantRoyalPurple
        category.contains("Hamper", ignoreCase = true) -> VibrantEmeraldGreen
        else -> VibrantSkyBlue
    }
}

@Composable
private fun CheckoutBillDialog(
    customer: Customer?,
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var paidAmountText by rememberSaveable {
        mutableStateOf(
            if (customer != null) "0.00" else String.format(Locale.US, "%.2f", totalAmount)
        )
    }
    var paymentMethod by rememberSaveable {
        mutableStateOf(if (customer != null) "CREDIT" else "CASH")
    }

    val paidNow = paidAmountText.toDoubleOrNull() ?: 0.0
    val creditAdded = (totalAmount - paidNow).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Sandeep Cafe Bill (₹)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Customer: ${customer?.name ?: "Walk-in Customer"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bill Total: ₹${String.format(Locale.US, "%.2f", totalAmount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VibrantBerryPink,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = paymentMethod == "CREDIT",
                        onClick = {
                            paymentMethod = "CREDIT"
                            paidAmountText = "0.00"
                        },
                        label = { Text("Full Credit") }
                    )
                    FilterChip(
                        selected = paymentMethod == "CASH",
                        onClick = {
                            paymentMethod = "CASH"
                            paidAmountText = String.format(Locale.US, "%.2f", totalAmount)
                        },
                        label = { Text("Cash Paid") }
                    )
                    FilterChip(
                        selected = paymentMethod == "UPI_CARD",
                        onClick = {
                            paymentMethod = "UPI_CARD"
                            paidAmountText = String.format(Locale.US, "%.2f", totalAmount)
                        },
                        label = { Text("UPI / Card") }
                    )
                }

                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it },
                    label = { Text("Amount Paid Right Now (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_paid_now_input")
                )

                if (creditAdded > 0.001) {
                    Surface(
                        color = VibrantCoralContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (customer != null) {
                                "₹${String.format(Locale.US, "%.2f", creditAdded)} will be added to ${customer.name}'s credit balance (New Total: ₹${String.format(Locale.US, "%.2f", customer.creditBalance + creditAdded)})."
                            } else {
                                "Please select a customer on the billing screen to allow credit balance sales."
                            },
                            color = OnVibrantCoralContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(paidNow, paymentMethod) },
                enabled = !(creditAdded > 0.001 && customer == null),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantEmeraldGreen),
                modifier = Modifier.testTag("confirm_checkout_bill_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Bill & Deduct Stock")
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
fun InventoryScreen(
    inventory: List<InventoryItem>,
    viewModel: ShopViewModel
) {
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var showAddStockDialog by rememberSaveable { mutableStateOf(false) }

    val categories = remember(inventory) {
        listOf("ALL") + inventory.map { it.category }.distinct().sorted()
    }

    val filteredItems = remember(inventory, selectedCategory) {
        if (selectedCategory == "ALL") inventory
        else inventory.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val totalStockValue = remember(inventory) {
        inventory.sumOf { it.price * it.stockQuantity }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddStockDialog = true },
                containerColor = VibrantSkyBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_stock_item_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Stock Item")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Stock Item", fontWeight = FontWeight.Bold)
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

            // Stock Summary Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VibrantSkyContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SANDEEP CAFE STOCK VALUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnVibrantSkyContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", totalStockValue)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = VibrantSkyBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = VibrantSaffronContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${inventory.size} SKUs • ${inventory.count { it.stockQuantity <= it.lowStockThreshold }} Low Stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnVibrantSaffronContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VibrantSkyBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = VibrantSkyBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No confectionery stock items yet")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.seedSampleConfectioneryShopData() }) {
                            Text("Load Sample Sandeep Cafe Stock")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isLowStock = item.stockQuantity <= item.lowStockThreshold
                        val marginPerUnit = (item.price - item.costPrice).coerceAtLeast(0.0)
                        val catColor = categoryAccentColor(item.category)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isLowStock) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.WarningAmber,
                                                    contentDescription = "Low Stock",
                                                    tint = VibrantCoralRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${item.category} • SKU: ${item.sku} • Batch Exp: ${item.expiryDate.ifBlank { "N/A" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = catColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Retail: ₹${String.format(Locale.US, "%.2f", item.price)} | Cost: ₹${String.format(Locale.US, "%.2f", item.costPrice)} (Profit: +₹${String.format(Locale.US, "%.2f", marginPerUnit)}/${item.unit})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VibrantEmeraldGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteInventoryItem(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Stock Item",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isLowStock) VibrantCoralRed
                                                    else VibrantEmeraldGreen
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "In Stock: ${item.stockQuantity} ${item.unit} (Alert ≤ ${item.lowStockThreshold})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLowStock) VibrantCoralRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalButton(
                                            onClick = { viewModel.adjustStockQuantity(item, -1L) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("-1")
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = { viewModel.adjustStockQuantity(item, +5L) },
                                            colors = ButtonDefaults.buttonColors(containerColor = catColor),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("restock_plus5_${item.id}")
                                        ) {
                                            Text("+5 Restock")
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

    if (showAddStockDialog) {
        AddStockItemDialog(
            onDismiss = { showAddStockDialog = false },
            onConfirm = { name, cat, sku, unit, price, cost, qty, threshold, expiry, supplier ->
                viewModel.addInventoryItem(
                    name = name,
                    category = cat,
                    sku = sku,
                    unit = unit,
                    price = price,
                    costPrice = cost,
                    stockQuantity = qty,
                    lowStockThreshold = threshold,
                    expiryDate = expiry,
                    supplierName = supplier
                )
                showAddStockDialog = false
            }
        )
    }
}

@Composable
private fun AddStockItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Double, Double, Long, Long, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Chocolates") }
    var sku by rememberSaveable { mutableStateOf("SKU-105") }
    var unit by rememberSaveable { mutableStateOf("box") }
    var price by rememberSaveable { mutableStateOf("250.00") }
    var costPrice by rememberSaveable { mutableStateOf("140.00") }
    var stockQty by rememberSaveable { mutableStateOf("20") }
    var lowThreshold by rememberSaveable { mutableStateOf("5") }
    var expiryDate by rememberSaveable { mutableStateOf("2026-12-15") }
    var supplier by rememberSaveable { mutableStateOf("Sandeep Cafe Kitchen") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sandeep Cafe Stock Item (₹)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (box/kg/cup)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Selling Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQty,
                        onValueChange = { stockQty = it },
                        label = { Text("Opening Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lowThreshold,
                        onValueChange = { lowThreshold = it },
                        label = { Text("Low Stock Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Batch Expiry Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        category,
                        sku,
                        unit,
                        price.toDoubleOrNull() ?: 0.0,
                        costPrice.toDoubleOrNull() ?: 0.0,
                        stockQty.toLongOrNull() ?: 0L,
                        lowThreshold.toLongOrNull() ?: 5L,
                        expiryDate,
                        supplier
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantSkyBlue),
                modifier = Modifier.testTag("confirm_add_stock_button")
            ) {
                Text("Save Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
