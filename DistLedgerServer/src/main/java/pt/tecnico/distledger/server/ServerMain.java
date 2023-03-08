package pt.tecnico.distledger.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.grpc.AdminServiceImpl;
import pt.tecnico.distledger.server.grpc.UserServiceImpl;

import java.io.IOException;


public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port qual", ServerMain.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);

        ServerState state = new ServerState();
        Server server = ServerBuilder.forPort(port)
                                     .addService(new UserServiceImpl(state))
                                     .addService(new AdminServiceImpl(state))
                                     .build();

        server.start();

        System.out.println("Server started, listening on " + port);

        server.awaitTermination();
    }

}
