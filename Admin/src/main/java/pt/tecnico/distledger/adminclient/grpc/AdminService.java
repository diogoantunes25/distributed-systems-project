package pt.tecnico.distledger.adminclient.grpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.tecnico.distledger.client.grpc.Service;
import pt.tecnico.distledger.gossip.Timestamp;

import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.client.exceptions.ServerUnavailableException;

public class AdminService extends Service {

    private Timestamp ts = new Timestamp();
    private int requestID = 0;

    public AdminService(String id) {
        super(id);
    }

    public void activate(String server) throws ServerUnavailableException, ServerLookupFailedException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryActivate(server);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryActivate(server);
        }
    }

    private void tryActivate(String server) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            AdminDistLedger.ActivateRequest request = 
                AdminDistLedger.ActivateRequest.newBuilder().build();
            AdminDistLedger.ActivateResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).activate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");

            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }

    public void deactivate(String server) throws ServerUnavailableException, ServerLookupFailedException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryDeactivate(server);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryDeactivate(server);
        }
    }

    private void tryDeactivate(String server) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            AdminDistLedger.DeactivateRequest request =
                AdminDistLedger.DeactivateRequest.newBuilder().build();
            AdminDistLedger.DeactivateResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).deactivate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");

            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }

    public void getLedgerState(String server) throws ServerLookupFailedException, ServerUnavailableException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryGetLedgerState(server);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryGetLedgerState(server);
        }
    }

    private void tryGetLedgerState(String server) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            AdminDistLedger.getLedgerStateRequest request =
                AdminDistLedger.getLedgerStateRequest.newBuilder().setPrev(ts.toGrpc()).build();
            
            AdminDistLedger.getLedgerStateResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).getLedgerState(request);

            ts.merge(Timestamp.fromGrpc(response.getNew()));

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");

            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }

    // TODO: rethink this
    public void gossip(String server) throws ServerLookupFailedException, ServerUnavailableException {
        System.out.printf("[AdminService] request gossip from %s\n", server);
        if (!cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryGossip(server);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryGossip(server);
        }
    }

    private void tryGossip(String server) throws ServerUnavailableException {
        System.out.printf("[AdminService] trying to request gossip from %s\n", server);
        ManagedChannel channel = getServerChannel(server);

        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            AdminDistLedger.GossipRequest request =
                    AdminDistLedger.GossipRequest.newBuilder().build();

            System.out.printf("[AdminService] sending gossip request to %s\n", server);
            AdminDistLedger.GossipResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).gossip(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");

            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }
}
