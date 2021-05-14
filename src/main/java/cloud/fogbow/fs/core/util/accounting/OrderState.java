package cloud.fogbow.fs.core.util.accounting;

public enum OrderState {
    OPEN("OPEN"),
    PENDING("PENDING"),
    SPAWNING("SPAWNING"),
    FULFILLED("FULFILLED"),
    FAILED_AFTER_SUCCESSFUL_REQUEST("FAILED_AFTER_SUCCESSFUL_REQUEST"),
    FAILED_ON_REQUEST("FAILED_ON_REQUEST"),
    CLOSED("CLOSED"),
    UNABLE_TO_CHECK_STATUS("UNABLE_TO_CHECK_STATUS"),
    SELECTED("SELECTED"),
    ASSIGNED_FOR_DELETION("ASSIGNED_FOR_DELETION"),
    CHECKING_DELETION("CHECKING_DELETION"),
    PAUSED("PAUSED"),
    HIBERNATED("HIBERNATED"),
    PAUSING("PAUSING"),
    HIBERNATING("HIBERNATING"),
    RESUMING("RESUMING"),    
    DEACTIVATED("DEACTIVATED");
    // an order that has been closed is stored twice in stable storage:
    // one when the order is deleted (but instanceId != null),
    // and another when it is deactivated (when instanceId == null);
    // we need the deactivate state so that the add in the timestamp table won't break.

    private final String repr;

    OrderState(String repr) {
        this.repr = repr;
    }
}
