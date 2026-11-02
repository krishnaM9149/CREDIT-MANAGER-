package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.model.Customer
import com.example.data.model.InventoryItem
import com.example.data.model.ShopTransaction
import com.example.ui.screens.AiStudioAndIdeasScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnVibrantBerryContainer
import com.example.ui.theme.OnVibrantEmeraldContainer
import com.example.ui.theme.OnVibrantSaffronContainer
import com.example.ui.theme.VibrantBerryContainer
import com.example.ui.theme.VibrantBerryPink
import com.example.ui.theme.VibrantEmeraldContainer
import com.example.ui.theme.VibrantEmeraldGreen
import com.example.ui.theme.VibrantRoyalPurple
import com.example.ui.theme.VibrantSaffronContainer
import com.example.ui.theme.VibrantSaffronGold
import com.example.ui.theme.VibrantSkyBlue
import com.example.util.SmsHelper
import com.example.viewmodel.AiStudioViewModel
import com.example.viewmodel.ShopViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SandeepCafeRootApp()
            }
        }
    }
}

private fun configureAppCheck(context: Context, intent: Intent?) {
    val debugToken = intent?.getStringExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")
        ?: runCatching {
            BuildConfig::class.java.getField("FIREBASE_APPCHECK_DEBUG_TOKEN").get(null) as? String
        }.getOrNull()

    val isDebugTokenValid = !debugToken.isNullOrBlank() && debugToken != "null"

    if (isDebugTokenValid) {
        try {
            val factory = DebugAppCheckProviderFactory.getInstance()
            val secretField = DebugAppCheckProviderFactory::class.java.getDeclaredField("debugSecret")
            secretField.isAccessible = true
            secretField.set(factory, debugToken)
            Firebase.appCheck.installAppCheckProviderFactory(factory)
            return
        } catch (e: Exception) {
            // Fall through to standard provider installation
        }
    }

    Firebase.appCheck.installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
    )
}

enum class ShopTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    OVERVIEW("overview", "Cafe Home", Icons.Default.Storefront, VibrantBerryPink),
    CUSTOMERS("customers", "Customers", Icons.Default.People, VibrantSaffronGold),
    BILLING("billing", "Billing", Icons.Default.PointOfSale, VibrantEmeraldGreen),
    INVENTORY("inventory", "Stocks", Icons.Default.Inventory2, VibrantSkyBlue),
    STUDIO("studio", "AI Studio", Icons.Default.AutoAwesome, VibrantRoyalPurple)
}

@Composable
fun SandeepCafeRootApp() {
    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val shopViewModel: ShopViewModel = viewModel(
            key = "shop_${user.uid}",
            factory = ShopViewModel.provideFactory(user.uid)
        )
        val aiStudioViewModel: AiStudioViewModel = viewModel(
            key = "ai_studio_${user.uid}"
        )
        AuthenticatedShopWorkspace(
            userEmailOrName = user.displayName ?: user.email ?: "Sandeep Cafe Owner",
            shopViewModel = shopViewModel,
            aiStudioViewModel = aiStudioViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShopWorkspace(
    userEmailOrName: String,
    shopViewModel: ShopViewModel,
    aiStudioViewModel: AiStudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by rememberSaveable { mutableStateOf(ShopTab.OVERVIEW) }

    val customersUiState by shopViewModel.customersState.collectAsStateWithLifecycle()
    val inventoryUiState by shopViewModel.inventoryState.collectAsStateWithLifecycle()
    val transactionsUiState by shopViewModel.transactionsState.collectAsStateWithLifecycle()
    val statusMessage by shopViewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isBusy by shopViewModel.isBusy.collectAsStateWithLifecycle()
    val cartItems by shopViewModel.cartItems.collectAsStateWithLifecycle()
    val autoSmsPromptShown by shopViewModel.autoSmsPromptShownThisSession.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            shopViewModel.clearBannerMessage()
        }
    }

    val customers: List<Customer> = (customersUiState as? UiState.Success)?.data ?: emptyList()
    val inventory: List<InventoryItem> = (inventoryUiState as? UiState.Success)?.data ?: emptyList()
    val transactions: List<ShopTransaction> = (transactionsUiState as? UiState.Success)?.data ?: emptyList()

    // Customers due for automated weekly SMS reminder
    val dueCustomersForAutoSms = remember(customers) {
        val todayPrefix = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        customers.filter {
            it.weeklyReminderEnabled &&
                it.creditBalance > 0.01 &&
                !it.lastReminderSentAt.startsWith(todayPrefix)
        }
    }

    // State for the 1-time confirmation dialog before automated SMS dispatch
    var showOneTimeAutoSmsConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var singleCustomerForOneTimeSms by remember { mutableStateOf<Customer?>(null) }

    // Automatically trigger the 1-time confirmation prompt once per session when due customers exist
    LaunchedEffect(dueCustomersForAutoSms.size, autoSmsPromptShown) {
        if (dueCustomersForAutoSms.isNotEmpty() && !autoSmsPromptShown) {
            shopViewModel.markAutoSmsPromptShown()
            singleCustomerForOneTimeSms = null
            showOneTimeAutoSmsConfirmDialog = true
        }
    }

    val lowStockCount = inventory.count { it.stockQuantity <= it.lowStockThreshold }
    val owingCustomersCount = customers.count { it.creditBalance > 0.01 }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(VibrantBerryPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sandeep Cafe",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnVibrantBerryContainer
                            )
                            Text(
                                text = "$userEmailOrName • ₹ Credit & Stock",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnVibrantBerryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    if (customers.any { it.weeklyReminderEnabled && it.creditBalance > 0.01 }) {
                        FilledTonalButton(
                            onClick = {
                                singleCustomerForOneTimeSms = null
                                showOneTimeAutoSmsConfirmDialog = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = VibrantSaffronContainer,
                                contentColor = OnVibrantSaffronContainer
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("top_auto_sms_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Automated Weekly SMS",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto SMS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = {
                            signOutUser(
                                context = context,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            tint = OnVibrantBerryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VibrantBerryContainer.copy(alpha = 0.75f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                ShopTab.entries.forEach { tab ->
                    val badgeCount = when (tab) {
                        ShopTab.CUSTOMERS -> owingCustomersCount
                        ShopTab.BILLING -> cartItems.sumOf { it.quantity }
                        ShopTab.INVENTORY -> lowStockCount
                        else -> 0
                    }
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = tab.accentColor,
                            indicatorColor = tab.accentColor
                        ),
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge(
                                            containerColor = VibrantSaffronGold,
                                            contentColor = Color.White
                                        ) {
                                            Text(badgeCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = VibrantBerryPink
                )
            }

            // Show non-blocking error banner if any Firestore flow emitted an error
            val errorMsg = (customersUiState as? UiState.Error)?.message
                ?: (inventoryUiState as? UiState.Error)?.message
                ?: (transactionsUiState as? UiState.Error)?.message

            if (errorMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    ShopTab.OVERVIEW -> DashboardScreen(
                        customers = customers,
                        inventory = inventory,
                        transactions = transactions,
                        viewModel = shopViewModel,
                        onNavigateToCustomers = { currentTab = ShopTab.CUSTOMERS },
                        onNavigateToBilling = { currentTab = ShopTab.BILLING },
                        onNavigateToInventory = { currentTab = ShopTab.INVENTORY },
                        onNavigateToStudio = { currentTab = ShopTab.STUDIO },
                        onTriggerOneTimeAutoSms = { specificCustomer ->
                            singleCustomerForOneTimeSms = specificCustomer
                            showOneTimeAutoSmsConfirmDialog = true
                        }
                    )
                    ShopTab.CUSTOMERS -> CustomersScreen(
                        customers = customers,
                        transactions = transactions,
                        viewModel = shopViewModel,
                        onStartBillForCustomer = { customer ->
                            shopViewModel.selectCustomerForBilling(customer)
                            currentTab = ShopTab.BILLING
                        },
                        onTriggerOneTimeCustomerSms = { customer ->
                            singleCustomerForOneTimeSms = customer
                            showOneTimeAutoSmsConfirmDialog = true
                        }
                    )
                    ShopTab.BILLING -> BillingScreen(
                        customers = customers,
                        inventory = inventory,
                        transactions = transactions,
                        viewModel = shopViewModel
                    )
                    ShopTab.INVENTORY -> InventoryScreen(
                        inventory = inventory,
                        viewModel = shopViewModel
                    )
                    ShopTab.STUDIO -> AiStudioAndIdeasScreen(
                        aiViewModel = aiStudioViewModel,
                        suggestedFeatures = shopViewModel.suggestedFeatures
                    )
                }
            }
        }
    }

    // 1-Time Confirmation Dialog Before Automated SMS Dispatch (Requirement 4)
    if (showOneTimeAutoSmsConfirmDialog) {
        val targetCustomers = remember(singleCustomerForOneTimeSms, customers, dueCustomersForAutoSms) {
            if (singleCustomerForOneTimeSms != null) {
                listOf(singleCustomerForOneTimeSms!!)
            } else if (dueCustomersForAutoSms.isNotEmpty()) {
                dueCustomersForAutoSms
            } else {
                customers.filter { it.weeklyReminderEnabled && it.creditBalance > 0.01 }
            }
        }

        if (targetCustomers.isNotEmpty()) {
            OneTimeAutoSmsConfirmationDialog(
                targetCustomers = targetCustomers,
                allTransactions = transactions,
                viewModel = shopViewModel,
                onDismiss = {
                    showOneTimeAutoSmsConfirmDialog = false
                    singleCustomerForOneTimeSms = null
                },
                onConfirmSendNow = {
                    val reports = targetCustomers.map { cust ->
                        shopViewModel.buildWeeklyCustomerReport(cust, transactions)
                    }
                    shopViewModel.markBatchWeeklyRemindersDispatched(targetCustomers)
                    if (reports.size == 1) {
                        val single = reports.first()
                        SmsHelper.launchSmsComposer(
                            context = context,
                            phoneNumber = single.customer.phone,
                            messageBody = single.formattedSmsBody
                        )
                    } else {
                        val firstReport = reports.first()
                        SmsHelper.launchAutomatedBatchSms(
                            context = context,
                            phoneNumbers = targetCustomers.map { it.phone },
                            combinedOrSingleBody = firstReport.formattedSmsBody
                        )
                    }
                    showOneTimeAutoSmsConfirmDialog = false
                    singleCustomerForOneTimeSms = null
                }
            )
        } else {
            showOneTimeAutoSmsConfirmDialog = false
        }
    }
}

@Composable
private fun OneTimeAutoSmsConfirmationDialog(
    targetCustomers: List<Customer>,
    allTransactions: List<ShopTransaction>,
    viewModel: ShopViewModel,
    onDismiss: () -> Unit,
    onConfirmSendNow: () -> Unit
) {
    val totalDueAcrossTargets = targetCustomers.sumOf { it.creditBalance.coerceAtLeast(0.0) }
    val sampleReport = remember(targetCustomers, allTransactions) {
        viewModel.buildWeeklyCustomerReport(targetCustomers.first(), allTransactions)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VibrantEmeraldGreen),
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
                        text = "Automated SMS Ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Asking 1 time just before sending",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantEmeraldGreen
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibrantEmeraldContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (targetCustomers.size == 1) {
                                "Customer: ${targetCustomers.first().name} (${targetCustomers.first().phone})"
                            } else {
                                "${targetCustomers.size} Customers Due for Weekly Credit Alert"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnVibrantEmeraldContainer
                        )
                        Text(
                            text = "Total Credit Balance Due: ₹${String.format(Locale.US, "%.2f", totalDueAcrossTargets)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibrantBerryPink
                        )
                    }
                }

                if (targetCustomers.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        targetCustomers.take(4).forEach { cust ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${cust.name} (${cust.phone})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", cust.creditBalance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VibrantBerryPink
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Automated Weekly SMS Preview:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sampleReport.formattedSmsBody,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmSendNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantEmeraldGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_one_time_auto_sms_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm & Send SMS Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
