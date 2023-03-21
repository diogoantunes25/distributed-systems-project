package pt.tecnico.distledger.namingserver.exceptions;

public class NoSuchServerException extends NameServerException {

    private String hostname;
    private int port;

    public NoSuchServerException(String hostname, int port, Throwable cause) {
        super(String.format("No server %s:%s", hostname, port), cause);
        this.hostname = hostname;
        this.port = port;
    }

    public NoSuchServerException(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
