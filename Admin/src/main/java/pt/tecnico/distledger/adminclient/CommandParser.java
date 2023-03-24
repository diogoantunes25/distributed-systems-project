package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.namingserver.NamingServer;

import pt.tecnico.distledger.adminclient.exceptions.InvalidQualifierException;
import pt.tecnico.distledger.adminclient.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.adminclient.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.adminclient.grpc.AdminService;

import java.util.Scanner;

public class CommandParser {

    private boolean DEBUG_FLAG;

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GOSSIP = "gossip";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final AdminService adminService;

    public CommandParser(AdminService adminService) {
        this.adminService = adminService;
    }

    public void setDebug(boolean debug) {
        this.DEBUG_FLAG = debug;
    }

    private void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

    private void assertValidQualifier(String qual) throws InvalidQualifierException {
        if (!qual.equals(NamingServer.PRIMARY_QUAL) && !qual.equals(NamingServer.SECONDARY_QUAL)) {
            throw new InvalidQualifierException(qual);
        }
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try{
                debug("Command: " + cmd);
                switch (cmd) {
                    case ACTIVATE:
                        this.activate(line);
                        break;

                    case DEACTIVATE:
                        this.deactivate(line);
                        break;

                    case GET_LEDGER_STATE:
                        this.dump(line);
                        break;

                    case GOSSIP:
                        this.gossip(line);
                        break;

                    case HELP:
                        this.printUsage();
                        break;

                    case EXIT:
                        exit = true;
                        break;

                    default:
                        break;
                }

            } catch (InvalidQualifierException e) {
                System.out.println(String.format("Qualifier '%s' is invalid - must be '%s' or '%s'.", e.getQual(), NamingServer.PRIMARY_QUAL, NamingServer.SECONDARY_QUAL));
            } catch (ServerUnavailableException e) {
                System.out.println(String.format("Server is currently unavailable. For writes, you can use the other server."));
            } catch (ServerLookupFailedException e) {
                System.out.println(String.format("Can't find any server with provided qualifier for service '%s'", NamingServer.SERVICE_NAME));
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

        }

        scanner.close();
        this.adminService.delete();
    }

    private void activate(String line) throws InvalidQualifierException, ServerLookupFailedException, ServerUnavailableException {
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        assertValidQualifier(server);

        debug("Server: " + server);

        this.adminService.activate(server);
    }

    private void deactivate(String line) throws InvalidQualifierException, ServerLookupFailedException, ServerUnavailableException {
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        assertValidQualifier(server);

        debug("Server: " + server);

        this.adminService.deactivate(server);
    }

    private void dump(String line) throws ServerLookupFailedException, ServerUnavailableException, InvalidQualifierException {
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        assertValidQualifier(server);

        debug("Server: " + server);

        this.adminService.getLedgerState(server);
    }

    @SuppressWarnings("unused")
    private void gossip(String line){
        /* TODO Phase-3 */
        System.out.println("TODO: implement gossip command (only for Phase-3)");
    }
    private void printUsage() {
        System.out.println("Usage:\n" +
                "- activate <server>\n" +
                "- deactivate <server>\n" +
                "- getLedgerState <server>\n" +
                "- gossip <server>\n" +
                "- exit\n");
    }

}
