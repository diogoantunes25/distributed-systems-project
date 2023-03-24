package pt.tecnico.distledger.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.ServerUnregistrationFailedException;
import pt.tecnico.distledger.server.exceptions.ServerRegistrationFailedException;
import pt.tecnico.distledger.server.grpc.AdminServiceImpl;
import pt.tecnico.distledger.server.grpc.CrossServerServiceImpl;
import pt.tecnico.distledger.server.grpc.UserServiceImpl;
import pt.tecnico.distledger.server.grpc.NamingServiceClient;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;


public class ServerMain {

    private static final String SERVICE_NAME = "DistLedger";
    private static final String HOST_NAME = "localhost";

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port qual", ServerMain.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);
        final String qual = args[1];
        final String target = HOST_NAME + ":" + port;

        try {
            NamingServiceClient.register(SERVICE_NAME, qual, target);

            ReentrantLock lock = new ReentrantLock();
            ServerState state = new ServerState();
            Server server = ServerBuilder.forPort(port)
                    .addService(new AdminServiceImpl(state, lock))
                    .addService(new UserServiceImpl(state, qual, lock))
                    .addService(new CrossServerServiceImpl(state))
                    .build();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    NamingServiceClient.remove(SERVICE_NAME, target);
                } catch (ServerUnregistrationFailedException e) {
                    e.printStackTrace();
                }
            }));

            server.start();

            System.out.println("Server started, listening on " + port);

            server.awaitTermination();

            NamingServiceClient.remove(SERVICE_NAME, target);

        } catch (ServerRegistrationFailedException e) {
            System.out.println("Could not register server. Exiting.");
        } catch (ServerUnregistrationFailedException e) {
            System.out.println("Could not unregister server. Exiting.");
        } catch (InterruptedException e) {
            System.out.println("Server interrupted. Exiting.");
        } catch (IOException e) {
            System.out.println("Could not start server. Exiting.");
        }
    }
}
