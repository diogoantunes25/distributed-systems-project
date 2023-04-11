package pt.tecnico.distledger.userclient;

import java.util.logging.Logger;
import java.util.logging.Level;

import pt.tecnico.distledger.userclient.grpc.UserService;

public class UserClientMain {

    private static boolean debug_flag = false;
    private static final Logger logger = Logger.getLogger(UserClientMain.class.getName());
    
    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        // receive and print arguments
        logger.info(String.format("Received %d tokens%n", args.length));

        if (args.length != 1 && args.length != 2) {
            System.err.println("Incorrect usage!");
            System.err.println("Usage: mvn exec:java userId -Dexec.args=[-Ddebug]");
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
                System.err.println("Usage: mvn exec:java userId -Dexec.args=[-Ddebug]");
            }
        }

        UserService userService = new UserService(id);
        CommandParser parser = new CommandParser(userService);
        parser.setDebug(debug_flag);
        parser.parseInput();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            userService.delete();
        }));
    }
}
