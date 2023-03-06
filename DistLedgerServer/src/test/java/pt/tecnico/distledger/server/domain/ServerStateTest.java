package pt.tecnico.distledger.server.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import pt.tecnico.distledger.server.domain.exceptions.AccountDoesNotExistException;
import pt.tecnico.distledger.server.domain.exceptions.BalanceNotZeroException;
import pt.tecnico.distledger.server.domain.exceptions.DistLedgerException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.operation.Operation;

public class ServerStateTest {

	static final String USER_1 = "user1";
	static final String USER_2 = "user2";

	ServerState serverState;

	@BeforeEach
	public void setUp() {
		serverState = new ServerState();
	}

	@Test
	public void testCreateAndRemoveUser() throws DistLedgerException {
		serverState.createAccount(USER_1);
		assertEquals(0, serverState.getBalance(USER_1));

		serverState.createAccount(USER_2);
		assertEquals(0, serverState.getBalance(USER_2));

		serverState.deleteAccount(USER_1);
		assertThrows(AccountDoesNotExistException.class, () -> serverState.getBalance(USER_1));

		serverState.deleteAccount(USER_2);
		assertThrows(AccountDoesNotExistException.class, () -> serverState.getBalance(USER_2));
	}

	@Test
	public void testTransferAndDeletion() throws DistLedgerException {
		serverState.createAccount(USER_1);
		serverState.createAccount(USER_2);

		serverState.transferTo(Account.BROKER_ID, USER_1, 100);
		assertEquals(100, serverState.getBalance(USER_1));
		serverState.transferTo(Account.BROKER_ID, USER_2, 200);
		assertEquals(200, serverState.getBalance(USER_2));

		serverState.transferTo(USER_1, USER_2, 50);
		assertEquals(50, serverState.getBalance(USER_1));
		assertEquals(250, serverState.getBalance(USER_2));

		assertThrows(NotEnoughBalanceException.class, () -> serverState.transferTo(USER_2, USER_1, 300));

		assertThrows(BalanceNotZeroException.class, () -> serverState.deleteAccount(USER_1));
		assertThrows(BalanceNotZeroException.class, () -> serverState.deleteAccount(USER_2));

		serverState.transferTo(USER_1, Account.BROKER_ID, serverState.getBalance(USER_1));
		serverState.transferTo(USER_2, Account.BROKER_ID, serverState.getBalance(USER_2));

		serverState.deleteAccount(USER_1);
		serverState.deleteAccount(USER_2);
	}

	@Test
	public void testActivationAndDeactivation() throws DistLedgerException {
		serverState.createAccount(USER_1);
		serverState.deactivate();
		assertThrows(ServerUnavailableException.class, () -> serverState.createAccount(USER_2));
		serverState.activate();
		assertDoesNotThrow(() -> serverState.createAccount(USER_2));
	}

	@Test
	public void testLedgerStateAndUpdates() throws DistLedgerException {
		serverState.createAccount(USER_1);
		serverState.createAccount(USER_2);
		serverState.transferTo(Account.BROKER_ID, USER_1, 100);
		serverState.transferTo(Account.BROKER_ID, USER_2, 200);

		serverState.transferTo(USER_1, USER_2, 50);
		serverState.transferTo(USER_1, Account.BROKER_ID, serverState.getBalance(USER_1));
		serverState.transferTo(USER_2, Account.BROKER_ID, serverState.getBalance(USER_2));

		serverState.deleteAccount(USER_1);
		serverState.deleteAccount(USER_2);

		List<Operation> ledger = serverState.getLedgerState();
		assertEquals(9, ledger.size());
	}
}
