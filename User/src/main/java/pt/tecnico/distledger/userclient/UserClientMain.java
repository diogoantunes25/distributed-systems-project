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
        String[] arguments = new String[2];
        int arg_index = 0;
        for (int i = 0; i < args.length; i++) {
            if(args[i].equals("-Ddebug")){
                debug_flag = true;
                logger.setLevel(Level.INFO);
                logger.info(String.format("Debug flag found%n"));
            } else {
                arguments[arg_index++] = args[i];
                logger.info(String.format("Argument: arg[%d] = %s%n", i, args[i]));
            }
        }

        // check arguments
        if (arguments.length != 2) {
            System.err.println("Incorrect usage!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port> [-Ddebug]");
            return;
        }

        final String host = arguments[0];
        final int port = Integer.parseInt(arguments[1]);
        final String target = host + ":" + port;
        logger.info("target = " + target);

        CommandParser parser = new CommandParser(new UserService(target));
        parser.setDebug(debug_flag);
        parser.parseInput();

    }
}
