package pt.tecnico.distledger.namingserver;

public class ServerEntry {

    private int port;
    private String hostname;
    private String qualifier;

    public ServerEntry(String hostname, int port, String qualifier) {
        this.port = port;
        this.hostname = hostname;
        this.qualifier = qualifier;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }
}