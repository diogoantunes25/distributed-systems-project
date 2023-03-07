package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;

public class AdminService {
    private final ManagedChannel channel;
    private final AdminServiceGrpc.AdminServiceBlockingStub stub;

    public AdminService(String target) {
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = AdminServiceGrpc.newBlockingStub(this.channel);
    }

    public void activate(String server){
        try{
            AdminDistLedger.ActivateRequest request = 
                AdminDistLedger.ActivateRequest.newBuilder().build();
            AdminDistLedger.ActivateResponse response = stub.activate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void deactivate(String server){
        try{
            AdminDistLedger.DeactivateRequest request = 
                AdminDistLedger.DeactivateRequest.newBuilder().build();
            AdminDistLedger.DeactivateResponse response = stub.deactivate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public void getLedgerState(String server){
        try{
            AdminDistLedger.getLedgerStateRequest request = 
                AdminDistLedger.getLedgerStateRequest.newBuilder().build();
            AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void delete() {
        channel.shutdown();
    }

}
