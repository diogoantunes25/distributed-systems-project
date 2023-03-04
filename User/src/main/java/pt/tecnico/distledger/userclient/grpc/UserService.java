package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;

public class UserService {
    private final ManagedChannel channel;
    private final UserServiceGrpc.UserServiceBlockingStub stub;

    public UserService(String target) {
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = UserServiceGrpc.newBlockingStub(this.channel);
    }

    public void createAccount(String server, String username){
        try{
            UserDistLedger.CreateAccountRequest request = 
                UserDistLedger.CreateAccountRequest.newBuilder().setUserId(username).build();
            UserDistLedger.CreateAccountResponse response = stub.createAccount(request);

            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void deleteAccount(String server, String username){
        try{
            UserDistLedger.DeleteAccountRequest request = 
                UserDistLedger.DeleteAccountRequest.newBuilder().setUserId(username).build();
            UserDistLedger.DeleteAccountResponse response = stub.deleteAccount(request);

            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public void balance(String server, String username){
        try{
            UserDistLedger.BalanceRequest request = 
                UserDistLedger.BalanceRequest.newBuilder().setUserId(username).build();
            UserDistLedger.BalanceResponse response = stub.balance(request);

            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void transferTo(String server, String username, String dest, Integer amount){
        try{
            UserDistLedger.TransferToRequest request = 
                UserDistLedger.TransferToRequest.newBuilder()
                .setAccountFrom(username).setAccountTo(dest).setAmount(amount).build();
            UserDistLedger.TransferToResponse response = stub.transferTo(request);
            
            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void delete() {
        channel.shutdown();
    }

}
