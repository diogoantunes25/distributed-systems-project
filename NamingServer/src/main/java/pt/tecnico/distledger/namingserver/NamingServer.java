package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.exceptions.CannotDeleteException;
import pt.tecnico.distledger.namingserver.exceptions.DuplicateServiceException;
import pt.tecnico.distledger.namingserver.exceptions.NoSuchServerException;
import pt.tecnico.distledger.namingserver.grpc.NamingServerServiceImpl;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void delete(String serviceName, String hostname, int port)
        throws CannotDeleteException {
        if (!services.containsKey(serviceName)) {
            throw new CannotDeleteException(serviceName, hostname, port);
        }

        try {
            services.get(serviceName).deleteServer(hostname, port);
        } catch (NoSuchServerException e) {
            throw new CannotDeleteException(serviceName, hostname, port, e);
        }
    }

    public List<ServerEntry> lookup(String serviceName, String qualifier) {
        if (qualifier.length() == 0) {
            return services.get(serviceName).getServers().stream().collect(Collectors.toList());
        }

        if (!services.containsKey(serviceName)) {
            return new LinkedList<ServerEntry>();
        }

        return services.get(serviceName).getServers(qualifier);
    }
}
