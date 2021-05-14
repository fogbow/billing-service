package cloud.fogbow.fs.core.plugins.authorization.role;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.fs.core.models.OperationType;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;

public class AllowOnlyPermission implements Permission<FsOperation> {

    private String name;
    private Set<OperationType> allowedOperationTypes;
    
    public AllowOnlyPermission() {
        
    }
    
    public AllowOnlyPermission(Set<OperationType> allowedOperationTypes) {
        this.allowedOperationTypes = allowedOperationTypes;
    }

    @Override
    public boolean isAuthorized(FsOperation operation) {
        return this.allowedOperationTypes.contains(operation.getOperationType());
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AllowOnlyPermission)) {
            return false;
        }
        
        AllowOnlyPermission other = (AllowOnlyPermission) o;
        
        if (!this.name.equals(other.name)) {
            return false;
        }
        
        if (!this.allowedOperationTypes.equals(other.allowedOperationTypes)) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public Set<String> getOperationsTypes() {
        HashSet<String> operationsStrings = new HashSet<String>(); 
        
        for (OperationType operation : allowedOperationTypes) {
            operationsStrings.add(operation.getValue());
        }
        
        return operationsStrings;
    }

    @Override
    public void setOperationTypes(Set<String> operations) throws InvalidParameterException {
        Set<OperationType> fsOperations = new HashSet<OperationType>();
        
        for (String operationName : operations) {
            fsOperations.add(OperationType.fromString(operationName));
        }
        
        this.allowedOperationTypes = fsOperations;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
