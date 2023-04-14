package pt.tecnico.distledger.server;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import pt.tecnico.distledger.namingserver.exceptions.ServerRegistrationFailedException;
import pt.tecnico.distledger.namingserver.exceptions.ServerUnregistrationFailedException;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceClient;
import pt.tecnico.distledger.namingserver.NamingServer;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.grpc.AdminServiceImpl;
import pt.tecnico.distledger.server.grpc.CrossServerClient;
import pt.tecnico.distledger.server.grpc.UserServiceImpl;
import pt.tecnico.distledger.server.grpc.CrossServerServiceImpl;

public class ServerMain {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port qual", ServerMain.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);
        final String qual = args[1];
        final String target = "localhost:" + port;

        NamingServiceClient namingServiceClient = new NamingServiceClient();

        try {
            namingServiceClient.register(NamingServer.SERVICE_NAME, qual, target);

            ServerState state = new ServerState(target);
            CrossServerClient client = new CrossServerClient(state);
            Server server = ServerBuilder.forPort(port)
                    .addService(new AdminServiceImpl(state, client))
                    .addService(new UserServiceImpl(state))
                    .addService(new CrossServerServiceImpl(state, client))
                    .build();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    namingServiceClient.remove(NamingServer.SERVICE_NAME, target);
                } catch (ServerUnregistrationFailedException e) {
                    e.printStackTrace();
                }
            }));

            server.start();

            System.out.println("Server started, listening on " + port);

            server.awaitTermination();

            namingServiceClient.remove(NamingServer.SERVICE_NAME, target);

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
