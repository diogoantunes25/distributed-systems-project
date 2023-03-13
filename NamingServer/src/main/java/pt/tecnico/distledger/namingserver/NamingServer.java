package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.exceptions.DuplicateServiceException;
import pt.tecnico.distledger.namingserver.grpc.NamingServerServiceImpl;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NamingServer {

    private static final int PORT = 5001;

    private Map<String, ServiceEntry> services = new HashMap<String, ServiceEntry>();

    public static void main(String[] args) throws InterruptedException, IOException {
        NamingServer namingServer = new NamingServer();
        Server server = ServerBuilder.forPort(PORT)
                                     .addService(new NamingServerServiceImpl(namingServer))
                                     .build();

        server.start();

        System.out.println("Naming server started, listening on " + PORT);

        server.awaitTermination();
    }

    public void register(String serviceName, String qualifier, String hostname, int port) throws DuplicateServiceException {
        if (!services.containsKey(serviceName)) {
            services.put(serviceName, new ServiceEntry(serviceName));
        }

        System.out.println(String.format("New service: %s", serviceName));

        ServiceEntry serviceEntry = services.get(serviceName);

        if (serviceEntry.hasServer(hostname, port)) {
            System.out.println(String.format("Duplicate service: %s @ %s:%s", serviceName, hostname, port));
            throw new DuplicateServiceException(serviceName, hostname, port);
        }

        serviceEntry.addServer(new ServerEntry(hostname, port, qualifier));
        System.out.println(String.format("Service added: %s @ %s:%s", serviceName, hostname, port));
    }
}
