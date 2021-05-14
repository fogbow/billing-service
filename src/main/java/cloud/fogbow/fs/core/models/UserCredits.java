package cloud.fogbow.fs.core.models;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

@Entity
@Table(name = "credits_table")
public class UserCredits {

    @EmbeddedId
    private UserId userId;

    @Column
    private Double credits;

    public UserCredits() {
        
    }
    
	public UserCredits(String userId, String provider) {
	    this.userId = new UserId(userId, provider);
        this.credits = 0.0;
    }

    public void deduct(ResourceItem resourceItem, Double valueToPayPerTimeUnit, Double timeUsed) 
            throws InvalidParameterException {
        if (valueToPayPerTimeUnit < 0) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_VALUE_TO_PAY, 
                    resourceItem.toString(), valueToPayPerTimeUnit));
        }
        
        if (timeUsed < 0) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_TIME_USED, 
                    resourceItem.toString(), valueToPayPerTimeUnit));
        }
        
	    this.credits -= valueToPayPerTimeUnit*timeUsed; 
	}

    public void addCredits(Double creditsToAdd) throws InvalidParameterException {
        if (creditsToAdd < 0) {
            throw new InvalidParameterException(Messages.Exception.CANNOT_ADD_NEGATIVE_CREDITS);
        }
        
        this.credits += creditsToAdd;
    }
    
    public Double getCreditsValue() {
        return credits;
    }

    public String getUserId() {
        return userId.getUserId();
    }

    public String getProvider() {
        return userId.getProvider();
    }
}
