package pt.tecnico.distledger.client;

import java.util.logging.Logger;
import java.util.logging.Level;

import pt.tecnico.distledger.namingserver.NamingServer;

import pt.tecnico.distledger.client.exceptions.InvalidQualifierException;
import pt.tecnico.distledger.client.grpc.Service;

public abstract class CommandParser {

    private static final Logger logger = Logger.getLogger(CommandParser.class.getName());

    private final Service service;

    public CommandParser(Service service) {
        this.service = service;
    }

    public void setDebug(boolean debug) {
        logger.setLevel(debug ? Level.INFO : Level.WARNING);
    }

    private void assertValidQualifier(String qual) throws InvalidQualifierException {
        if (!qual.equals(NamingServer.PRIMARY_QUAL) && !qual.equals(NamingServer.SECONDARY_QUAL)) {
            throw new InvalidQualifierException(qual);
        }
    }

    abstract void parseInput();

    private abstract void printUsage();
}
