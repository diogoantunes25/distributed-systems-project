package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.exceptions.InvalidQualifierException;
import pt.tecnico.distledger.userclient.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.userclient.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.tecnico.distledger.namingserver.NamingServer;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Scanner;

public class CommandParser {

    private static final Logger logger = Logger.getLogger(CommandParser.class.getName());

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String DELETE_ACCOUNT = "deleteAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final UserService userService;

    public CommandParser(UserService userService) {
        this.userService = userService;
    }

    public void setDebug(boolean debug) {
        logger.setLevel(debug ? Level.INFO : Level.WARNING);
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

            try {
                logger.info("Command: " + cmd);
                switch (cmd) {
                    case CREATE_ACCOUNT:
                        this.createAccount(line);
                        break;

                    case DELETE_ACCOUNT:
                        this.deleteAccount(line);
                        break;

                    case TRANSFER_TO:
                        this.transferTo(line);
                        break;

                    case BALANCE:
                        this.balance(line);
                        break;

                    case HELP:
                        this.printUsage();
                        break;

                    case EXIT:
                        exit = true;
                        break;

                    default:
                        System.out.println("Command not found. Type 'help' for a list of commands.");
                        System.out.println("");
                        break;
                }

            } catch (InvalidQualifierException e) {
                System.out.println(String.format("Qualifier '%s' is invalid - must be '%s' or '%s'.", e.getQual(), NamingServer.PRIMARY_QUAL, NamingServer.SECONDARY_QUAL));
            } catch (ServerUnavailableException e) {
                System.out.println(String.format("Server is currently unavailable. For writes, you can use the other server."));
            } catch (ServerLookupFailedException e) {
                System.out.println(String.format("Can't find any server with provided qualifier for service '%s'", NamingServer.SERVICE_NAME));
            } catch (Exception e){
                System.err.println(e.getMessage());
            }
        }

        scanner.close();
        this.userService.delete();
    }

    private void createAccount(String line) throws InvalidQualifierException, ServerLookupFailedException, ServerUnavailableException {
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }


        String server = split[1];
        String username = split[2];

        assertValidQualifier(server);

        logger.info("Server: " + server);
        logger.info("Username: " + username);

        this.userService.createAccount(server, username);
    }

    private void deleteAccount(String line) throws InvalidQualifierException, ServerLookupFailedException, ServerUnavailableException {
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        logger.info("Server: " + server);
        logger.info("Username: " + username);

        assertValidQualifier(server);

        this.userService.deleteAccount(server, username);
    }


    private void balance(String line) throws InvalidQualifierException, ServerLookupFailedException, ServerUnavailableException {
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        logger.info("Server: " + server);
        logger.info("Username: " + username);

        assertValidQualifier(server);

        this.userService.balance(server, username);
    }

    private void transferTo(String line) throws InvalidQualifierException, ServerLookupFailedException, ServerUnavailableException {
        String[] split = line.split(SPACE);

        if (split.length != 5){
            this.printUsage();
            return;
        }
        String server = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);

        logger.info("Server: " + server);
        logger.info("Username: " + from);
        logger.info("Destination: " + dest);
        logger.info("Amount: " + amount);

        assertValidQualifier(server);

        this.userService.transferTo(server, from, dest, amount);
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                        "- createAccount <server> <username>\n" +
                        "- deleteAccount <server> <username>\n" +
                        "- balance <server> <username>\n" +
                        "- transferTo <server> <username_from> <username_to> <amount>\n" +
                        "- exit\n");
    }
}
