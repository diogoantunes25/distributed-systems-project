package pt.tecnico.distledger.server.exceptions;

import pt.tecnico.distledger.server.domain.exceptions.DistLedgerException;

public class CannotGetServerAddressesException extends DistLedgerException {
    private String serviceName;
    private String serverQualifier;

    public CannotGetServerAddressesException(String serviceName, String serverQualifier) {
        this.serviceName = serviceName;
        this.serverQualifier = serverQualifier;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getServerQualifier() {
        return this.serverQualifier;
    }

 }