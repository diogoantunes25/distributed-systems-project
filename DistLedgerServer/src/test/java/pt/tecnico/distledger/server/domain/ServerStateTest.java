package pt.tecnico.distledger.server.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import pt.tecnico.distledger.server.domain.exceptions.AccountDoesNotExistException;
import pt.tecnico.distledger.server.domain.exceptions.DistLedgerException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.operation.Operation;

public class ServerStateTest {

	static final String USER_1 = "user1";
	static final String USER_2 = "user2";

	ServerState serverState;

	@BeforeEach
	public void setUp() {}

//	@Test
//	public void testCreateAndRemoveUser() throws DistLedgerException {
//		serverState.createAccount(USER_1);
//		assertEquals(0, serverState.getBalance(USER_1));
//
//		serverState.createAccount(USER_2);
//		assertEquals(0, serverState.getBalance(USER_2));
//
//		serverState.deleteAccount(USER_1);
//		assertThrows(AccountDoesNotExistException.class, () -> serverState.getBalance(USER_1));
//
//		serverState.deleteAccount(USER_2);
//		assertThrows(AccountDoesNotExistException.class, () -> serverState.getBalance(USER_2));
//	}
//
//	@Test
//	public void testTransferAndDeletion() throws DistLedgerException {
//		serverState.createAccount(USER_1);
//		serverState.createAccount(USER_2);
//
//		serverState.transferTo(Account.BROKER_ID, USER_1, 100);
//		assertEquals(100, serverState.getBalance(USER_1));
//		serverState.transferTo(Account.BROKER_ID, USER_2, 200);
//		assertEquals(200, serverState.getBalance(USER_2));
//
//		serverState.transferTo(USER_1, USER_2, 50);
//		assertEquals(50, serverState.getBalance(USER_1));
//		assertEquals(250, serverState.getBalance(USER_2));
//
//		assertThrows(NotEnoughBalanceException.class, () -> serverState.transferTo(USER_2, USER_1, 300));
//
//		assertThrows(BalanceNotZeroException.class, () -> serverState.deleteAccount(USER_1));
//		assertThrows(BalanceNotZeroException.class, () -> serverState.deleteAccount(USER_2));
//
//		serverState.transferTo(USER_1, Account.BROKER_ID, serverState.getBalance(USER_1));
//		serverState.transferTo(USER_2, Account.BROKER_ID, serverState.getBalance(USER_2));
//
//		serverState.deleteAccount(USER_1);
//		serverState.deleteAccount(USER_2);
//		assertThrows(BrokerCannotBeDeletedException.class, () -> serverState.deleteAccount(Account.BROKER_ID));
//	}
//
//	@Test
//	public void testActivationAndDeactivation() throws DistLedgerException {
//		serverState.createAccount(USER_1);
//		serverState.deactivate();
//		assertThrows(ServerUnavailableException.class, () -> serverState.createAccount(USER_2));
//		serverState.activate();
//		assertDoesNotThrow(() -> serverState.createAccount(USER_2));
//	}
//
//	@Test
//	public void testLedgerStateAndUpdates() throws DistLedgerException {
//		serverState.createAccount(USER_1);
//		serverState.createAccount(USER_2);
//		serverState.transferTo(Account.BROKER_ID, USER_1, 100);
//		serverState.transferTo(Account.BROKER_ID, USER_2, 200);
//
//		serverState.transferTo(USER_1, USER_2, 50);
//		serverState.transferTo(USER_1, Account.BROKER_ID, serverState.getBalance(USER_1));
//		serverState.transferTo(USER_2, Account.BROKER_ID, serverState.getBalance(USER_2));
//
//		serverState.deleteAccount(USER_1);
//		serverState.deleteAccount(USER_2);
//
//		List<Operation> ledger = serverState.getLedgerState();
//		assertEquals(9, ledger.size());
//	}
//
//	@Test
//	public void testCreateAccountConcurrency() throws InterruptedException {
//
//		int threadCount = 50;
//		int accountNumber = 5000;
//
//		CountDownLatch latch = new CountDownLatch(threadCount);
//
//		List<Thread> threads = new ArrayList<>();
//
//		// Setup threads
//		for (int i = 0; i < threadCount; i++) {
//			threads.add(new Thread(() -> {
//				for (int j = 0; j < accountNumber; j++) {
//					try {
//						serverState.createAccount(String.format("USER %s", j));
//					} catch (DistLedgerException e) {
//						// ignore
//					}
//				}
//				latch.countDown();
//			}));
//		}
//
//		// Start all threads
//		for (Thread t: threads) t.start();
//
//		latch.await();
//
//		assertEquals(accountNumber + 1, serverState.getAccounts().size());
//	}
//
//	@Test
//	public void testTransferConcurrency() throws InterruptedException, DistLedgerException {
//
//		int threadCount = 50;
//		int accountCount = 50;
//		int transactionPerAccount = 500;
//
//		CountDownLatch latch = new CountDownLatch(threadCount);
//		List<Thread> threads = new ArrayList<>();
//
//		// Create accounts
//		for (int i = 0; i < accountCount; i++) {
//			serverState.createAccount("user_" + i);
//		}
//
//		// Setup threads
//		for (int i = 0; i < threadCount; i++) {
//			final int j = i;
//			threads.add(new Thread(() -> {
//				// Pick some account
//				String userId = "user_" + (j % accountCount);
//
//				for (int k = 0; k < transactionPerAccount; k++) {
//					try {
//						serverState.transferTo(Account.BROKER_ID, userId, 1);
//					} catch (DistLedgerException e) {
//						// ignore
//					}
//				}
//
//				latch.countDown();
//			}));
//		}
//
//		// Start all threads
//		for (Thread t: threads) t.start();
//
//		latch.await();
//
//		int totalCurrency = serverState
//			.getAccounts()
//			.keySet()
//			.stream()
//			.mapToInt((String userId) -> {
//				try {
//					return serverState.getBalance(userId);
//				} catch (DistLedgerException e) {
//					// ignore
//				}
//				return 0;
//			})
//			.sum();
//
//		assertEquals(Account.TOTAL_COIN, totalCurrency);
//	}
//
//	@Test
//	public void testDeleteConcurrency() throws InterruptedException, DistLedgerException {
//
//		int threadCount = 50;
//		int accountCount = 5000;
//
//		CountDownLatch latch = new CountDownLatch(threadCount);
//		List<Thread> threads = new ArrayList<>();
//
//		// Create accounts
//		for (int i = 0; i < accountCount; i++) {
//			serverState.createAccount("user_" + i);
//		}
//
//		assertEquals(accountCount + 1, serverState.getAccounts().size());
//
//		// Setup threads
//		for (int i = 0; i < threadCount; i++) {
//			threads.add(new Thread(() -> {
//				// Pick some account
//				for (int j = 0; j < accountCount; j++) {
//					String userId = "user_" + j;
//					try {
//						serverState.deleteAccount(userId);
//					} catch (DistLedgerException e) {
//						// ignore
//					}
//				}
//
//				latch.countDown();
//			}));
//		}
//
//		// Start all threads
//		for (Thread t: threads) t.start();
//
//		latch.await();
//
//		assertEquals(1, serverState.getAccounts().size());
//	}

}
