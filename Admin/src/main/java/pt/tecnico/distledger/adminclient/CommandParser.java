package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.namingserver.NamingServer;

import pt.tecnico.distledger.client.exceptions.InvalidQualifierException;
import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.client.exceptions.ServerUnavailableException;
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

        debug("Server: " + server);

        this.adminService.getLedgerState(server);
    }

    private void gossip(String line) throws ServerLookupFailedException, ServerUnavailableException {

        System.out.printf("[CommandParser] gossip command found (line=%s)\n", line);

        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        debug("Server: " + server);

        this.adminService.gossip(server);
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
