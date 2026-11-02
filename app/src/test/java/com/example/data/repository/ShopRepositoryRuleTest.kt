package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ShopRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addCustomerAndObserve_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ShopRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCustomer(
                name = "Aarav Mehta",
                phone = "+919811223344",
                notes = "Likes pistachio baklava",
                initialCredit = 240.0,
                weeklyReminderEnabled = true
            )
        }
        assertTrue("Expected addCustomer to succeed", createResult.isSuccess)
        val customerId = createResult.getOrThrow()

        val emittedCustomers = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomers(aliceUid).first { list -> list.any { it.id == customerId } }
        }
        val saved = emittedCustomers.first { it.id == customerId }
        assertEquals("Aarav Mehta", saved.name)
        assertEquals(240.0, saved.creditBalance, 0.01)
    }

    @Test
    fun addInventoryAndCreateBill_updatesStockAndCustomerCredit() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ShopRepository(firestore)

        val custId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCustomer(
                name = "Meera Nair",
                phone = "+919900112233",
                initialCredit = 50.0
            ).getOrThrow()
        }

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addInventoryItem(
                name = "Rose Turkish Delight",
                category = "Traditional Sweets",
                sku = "TRD-01",
                unit = "box",
                price = 120.0,
                costPrice = 70.0,
                stockQuantity = 10L,
                lowStockThreshold = 3L,
                expiryDate = "2026-12-20",
                supplierName = "Artisanal Kitchen"
            ).getOrThrow()
        }

        val customer = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomers(aliceUid).first { list -> list.any { it.id == custId } }
                .first { it.id == custId }
        }
        val item = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(aliceUid).first { list -> list.any { it.id == itemId } }
                .first { it.id == itemId }
        }

        val billResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBillTransaction(
                customer = customer,
                lineItems = listOf(com.example.data.model.BillLineItem(item, 2)),
                paidNow = 100.0,
                paymentMethod = "CREDIT"
            )
        }
        assertTrue("Expected createBillTransaction to succeed", billResult.isSuccess)

        val updatedCustomer = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCustomerById(aliceUid, custId).getOrThrow()
        }
        assertNotNull(updatedCustomer)
        // Initial 50.0 + (2 * 120.0 - 100.0 = 140.0) = 190.0
        assertEquals(190.0, updatedCustomer!!.creditBalance, 0.01)
    }

    @Test
    fun getCustomerById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ShopRepository(firestore)
        val customerId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addCustomer(name = "Private Customer", phone = "1234567890").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ShopRepository(firestore)
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCustomerById(aliceUid, customerId)
        }
        assertTrue("Expected cross-user read to fail", bobResult.isFailure)
        val firestoreException = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun observeCustomers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = ShopRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCustomers(aliceUid).first()
            }
            fail("Expected observeCustomers to throw when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@sweetledger.test"
        const val BOB_EMAIL = "bob@sweetledger.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
