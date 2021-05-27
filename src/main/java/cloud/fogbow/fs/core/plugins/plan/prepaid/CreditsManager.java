package cloud.fogbow.fs.core.plugins.plan.prepaid;

import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class CreditsManager {
    public static final String USER_CREDITS = "USER_CREDITS";
    
    private RecordUtils recordUtils;
    private InMemoryUsersHolder usersHolder;
    private FinancePlan plan;
    
    public CreditsManager(InMemoryUsersHolder usersHolder, FinancePlan plan) {
        this.usersHolder = usersHolder;
        this.plan = plan;
        this.recordUtils = new RecordUtils();
    }
    
    public CreditsManager(InMemoryUsersHolder usersHolder, FinancePlan plan, RecordUtils recordUtils) {
        this.usersHolder = usersHolder;
        this.plan = plan;
        this.recordUtils = recordUtils;
    }
    
    // FIXME should be something like isDefaulting
    public boolean hasPaid(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = this.usersHolder.getUserById(userId, provider);
        
        synchronized(user) {
            UserCredits credits = user.getCredits();
            return credits.getCreditsValue() >= 0.0;
        }
    }

    public void startPaymentProcess(String userId, String provider, 
            Long paymentStartTime, Long paymentEndTime, List<Record> records) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.usersHolder.getUserById(userId, provider);
        
        synchronized (user) {
            synchronized (plan) {
                UserCredits credits = user.getCredits();
                
                for (Record record : records) {
                    ResourceItem resourceItem;
                    Double valueToPayPerTimeUnit;

                    try {
                        resourceItem = recordUtils.getItemFromRecord(record);
                        valueToPayPerTimeUnit = plan.getItemFinancialValue(resourceItem);
                    } catch (InvalidParameterException e) {
                        throw new InternalServerErrorException(e.getMessage());
                    }

                    Double timeUsed = recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime);

                    credits.deduct(resourceItem, valueToPayPerTimeUnit, timeUsed);
                }

                user.setProperty(FinanceUser.USER_LAST_BILLING_TIME, String.valueOf(paymentEndTime));
                this.usersHolder.saveUser(user);
            }
        }
    }

    public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException, InternalServerErrorException {
       String propertyValue = "";
        
        if (property.equals(USER_CREDITS)) {
            FinanceUser user = this.usersHolder.getUserById(userId, provider);
            synchronized(user) {
                UserCredits credits = user.getCredits();
                propertyValue = String.valueOf(credits.getCreditsValue());
            }
        } else {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.UNKNOWN_FINANCE_PROPERTY, property));
        }
        
        return propertyValue;
    }
}
