package pt.tecnico.distledger.adminclient;

import java.util.logging.Logger;
import java.util.logging.Level;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

public class AdminClientMain {

    private static boolean debug_flag = false;
    private static final Logger logger = Logger.getLogger(AdminClientMain.class.getName());
    
    public static void main(String[] args) {

        System.out.println(AdminClientMain.class.getSimpleName());

        // receive and print arguments
        logger.info(String.format("Received %d tokens%n", args.length));

        if (args.length != 1 && args.length != 2) {
            System.err.println("Incorrect usage!");
            System.err.println("Usage: mvn exec:java clientId -Dexec.args=[-Ddebug]");
            return;
        }

        String id = args[0];
        if (args.length == 2) {
            if (args[1].equals(("-Ddebug"))) {
                debug_flag = true;
                logger.setLevel(Level.INFO);
                logger.info(String.format("Debug flag found%n"));
            } else {
                System.err.println("Incorrect usage!");
                System.err.println("Usage: mvn exec:java clientId -Dexec.args=[-Ddebug]");
            }
        }

        AdminService adminService = new AdminService(id);
        CommandParser parser = new CommandParser(adminService);
        parser.setDebug(debug_flag);
        parser.parseInput();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            adminService.delete();
        }));
    }
}
