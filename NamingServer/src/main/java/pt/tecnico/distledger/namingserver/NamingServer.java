package pt.tecnico.distledger.namingserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import pt.tecnico.distledger.namingserver.exceptions.CannotRemoveException;
import pt.tecnico.distledger.namingserver.exceptions.CannotRegisterException;
import pt.tecnico.distledger.namingserver.grpc.NamingServerServiceImpl;

public class NamingServer {

    private Map<String, ServiceEntry> services = new HashMap<String, ServiceEntry>();

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port", NamingServer.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);

        Server server = ServerBuilder.forPort(port)
                .addService(new NamingServerServiceImpl(new NamingServer()))
                .build();

        server.start();

        System.out.println("Naming server started, listening on " + port);

        server.awaitTermination();
    }

    public void register(String serviceName, String qualifier, String hostname, int port) throws CannotRegisterException {
        ServiceEntry service = services.get(serviceName);
        if (service == null) {
            service = new ServiceEntry(serviceName);
            services.put(serviceName, service);
        } else {
            throw new CannotRegisterException(serviceName, hostname, port);
        }

        service.addServer(new ServerEntry(hostname, port, qualifier));
    }

    public void remove(String serviceName, String hostname, int port)
        throws CannotRemoveException {
        ServiceEntry service = services.get(serviceName);
        if (service == null) {
            throw new CannotRemoveException(serviceName, hostname, port);
        }

        ServerEntry server = service.getServer(hostname, port);
        if (server == null) {
            throw new CannotRemoveException(serviceName, hostname, port);
        }

        service.removeServer(server);
        if(!service.hasServers()) {
            services.remove(serviceName);
        }
        
    }

    public List<ServerEntry> lookup(String serviceName, String qualifier) {
        ServiceEntry service = services.get(serviceName);
        if (service == null) {
            return new LinkedList<ServerEntry>();
        }

        if (qualifier.length() == 0) {
            return service.getServers().stream().collect(Collectors.toList());
        }

        return services.get(serviceName).getServers(qualifier);
    }
}
