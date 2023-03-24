package pt.tecnico.distledger.client;

import java.util.logging.Logger;
import java.util.logging.Level;

import pt.tecnico.distledger.client.grpc.Service;

public abstract class ClientMain<Service> {

    private static boolean debug_flag = false;
    private static final Logger logger = Logger.getLogger(ClientMain.class.getName());
    
    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        // receive and print arguments
        logger.info(String.format("Received %d tokens%n", args.length));

        if (args.length == 1) {
            if (args[0].equals(("-Ddebug"))) {
                debug_flag = true;
                logger.setLevel(Level.INFO);
                logger.info(String.format("Debug flag found%n"));
            } else {
                System.err.println("Incorrect usage!");
                System.err.println("Usage: mvn exec:java -Dexec.args=[-Ddebug]");
            }
        }

        Service service = new Service();
        CommandParser parser = new CommandParser(service);
        parser.setDebug(debug_flag);
        parser.parseInput();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Service.delete();
        }));
    }
}
