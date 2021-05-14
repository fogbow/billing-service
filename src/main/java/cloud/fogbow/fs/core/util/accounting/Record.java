package cloud.fogbow.fs.core.util.accounting;

import java.sql.Timestamp;
import java.util.Objects;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

public abstract class Record {
	private Long id;
	private String orderId;
	private String resourceType;
	private String requester;
	private Timestamp startTime;
	private Timestamp startDate;
	private Timestamp endDate;
	private Timestamp endTime;
	private long duration = 0;
	private OrderState state;

	public abstract OrderSpec getSpec();
	public abstract void setSpec(OrderSpec orderSpec);
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public String getRequester() {
		return requester;
	}

	public void setRequester(String requester) {
		this.requester = requester;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public OrderState getState() {
		return state;
	}

	public void setState(OrderState newState) {
		this.state = newState;
	}

	public void setStartDate(Timestamp startDate) {
		this.startDate = startDate;
	}

	public Timestamp getStartDate() {
		return startDate;
	}

	public void setEndDate(Timestamp endDate) {
		this.endDate = endDate;
	}

	public Timestamp getEndDate() {
		return endDate;
	}
	
    protected void checkRecordPropertyIsNotNull(String propertyName, Object o) 
            throws InvalidParameterException {
        if (o == null) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_RECORD_PROPERTY, propertyName));
        }
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Record record = (Record) o;
		return duration == record.duration &&
				id.equals(record.id) &&
				Objects.equals(orderId, record.orderId) &&
				Objects.equals(resourceType, record.resourceType) &&
				//Objects.equals(spec, record.spec) &&
				Objects.equals(requester, record.requester) &&
				Objects.equals(startTime, record.startTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, orderId, resourceType, requester, startTime, duration);
	}

	Record(Long id, String orderId, String resourceType, String requester, Timestamp startTime,
				  Timestamp startDate, Timestamp endTime, Timestamp endDate, long duration, OrderState state) {
		this.id = id;
		this.orderId = orderId;
		this.resourceType = resourceType;
		this.requester = requester;
		this.startTime = startTime;
		this.startDate = startDate;
		this.endDate = endDate;
		this.endTime = endTime;
		this.duration = duration;
		this.state = state;
	}

	Record() {}
}
