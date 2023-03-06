package pt.tecnico.distledger.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;


public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s server%n", ServerMain.class.getName());
            return;
        }

        String sv = args[0];
        final int port = Integer.parseInt(args[0]);


        Server server = ServerBuilder.forPort(port).build();

        server.start();

        System.out.println("Server started, listening on " + port);

        server.awaitTermination();

    }

}

