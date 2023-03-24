package pt.tecnico.distledger.client.exceptions;

public class InvalidQualifierException extends Exception {

    private String qual;

    public InvalidQualifierException(String qual) {
        this.qual = qual;
    }

    public String getQual() {
        return qual;
    }
}
