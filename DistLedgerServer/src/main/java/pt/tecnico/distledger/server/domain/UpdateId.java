package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.UpdateOp;

import java.util.Objects;

/**
 * Wrapper for string with string compare (behaves in maps as expected)
 */
public class UpdateId {

    private String uid;

    public UpdateId(String id) {
        this.uid = id;
    }

    public String getUid() {
        return uid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateId updateId = (UpdateId) o;
        return uid.equals(updateId.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }
}