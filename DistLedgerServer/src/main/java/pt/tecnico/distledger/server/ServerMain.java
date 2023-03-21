package pt.tecnico.distledger.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.ServerRegistrationFailedException;
import pt.tecnico.distledger.server.exceptions.ServerUnregistrationFailedException;
import pt.tecnico.distledger.server.grpc.AdminServiceImpl;
import pt.tecnico.distledger.server.grpc.CrossServerServiceImpl;
import pt.tecnico.distledger.server.grpc.UserServiceImpl;

import java.io.IOException;


public class ServerMain {

    public static final String NAME_SERVER = "localhost:5001";

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port qual", ServerMain.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);
        final String qual = args[1];
        final String myAddress = String.format("localhost:%s", port);

        ServerState state = new ServerState();

        UserServiceImpl userService = new UserServiceImpl(state, qual, NAME_SERVER);
        AdminServiceImpl adminService = new AdminServiceImpl(state, qual, NAME_SERVER);

        try {
            userService.register(myAddress);
            adminService.register(myAddress);
        } catch (ServerRegistrationFailedException e) {
            e.printStackTrace();
            return;
        }

        Server server = ServerBuilder.forPort(port)
                                     .addService(userService)
                                     .addService(adminService)
                                     .addService(new CrossServerServiceImpl(state))
                                     .build();

        server.start();

        System.out.println("Server started, listening on " + port);

        server.awaitTermination();
    }
}
