package cloud.fogbow.fs.core.plugins.authorization;

import java.util.Objects;

import cloud.fogbow.common.models.FogbowOperation;
import cloud.fogbow.fs.core.models.OperationType;

public class FsOperation extends FogbowOperation {
    private OperationType operationType;

    public FsOperation(OperationType type) {
        this.operationType = type;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FsOperation other = (FsOperation) obj;
        if (operationType != other.operationType)
            return false;
        return true;
    }
}
