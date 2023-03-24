package pt.tecnico.distledger.userclient.exceptions;

public class InvalidQualifierException extends Exception {

    private String qual;

    public InvalidQualifierException(String qual) {
        this.qual = qual;
    }

    public String getQual() {
        return qual;
    }
}
