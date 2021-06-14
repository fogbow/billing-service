package cloud.fogbow.fs.core.plugins.plan.postpaid;

import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class InvoiceManager {
    private InMemoryUsersHolder userHolder;
    private RecordUtils resourceItemFactory;
    private InvoiceBuilder invoiceBuilder;
    private FinancePlan financePlan;
    
    public InvoiceManager(InMemoryUsersHolder userHolder, FinancePlan financePlan) {
        this.userHolder = userHolder;
        this.resourceItemFactory = new RecordUtils();
        this.invoiceBuilder = new InvoiceBuilder();
        this.financePlan = financePlan;
    }

    public InvoiceManager(InMemoryUsersHolder userHolder, RecordUtils resourceItemFactory,
            InvoiceBuilder invoiceBuilder, FinancePlan financePlan) {
        this.userHolder = userHolder;
        this.resourceItemFactory = resourceItemFactory;
        this.invoiceBuilder = invoiceBuilder;
        this.financePlan = financePlan;
    }
    
    public boolean hasPaid(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = this.userHolder.getUserById(userId, provider);

        synchronized(user) {
            for (Invoice invoice : user.getInvoices()) {
                if (invoice.getState().equals(InvoiceState.DEFAULTING)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public void generateInvoiceForUser(String userId, String provider, 
            Long paymentStartTime, Long paymentEndTime, List<Record> records) 
                    throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.userHolder.getUserById(userId, provider);
        
        synchronized(user) {
            synchronized(financePlan) {
                boolean lastInvoice = false;
                generateInvoiceAndUpdateUser(userId, provider, paymentStartTime, paymentEndTime, records, user, lastInvoice);
            }
        }
    }

    public void generateLastInvoiceForUser(String userId, String provider, Long paymentStartTime, 
            Long paymentEndTime, List<Record> records) 
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.userHolder.getUserById(userId, provider);
        
        synchronized(user) {
            synchronized(financePlan) {
                boolean lastInvoice = true;
                generateInvoiceAndUpdateUser(userId, provider, paymentStartTime, paymentEndTime, records, user, lastInvoice);
            }
        }        
    }

    private void generateInvoiceAndUpdateUser(String userId, String provider, Long paymentStartTime,
            Long paymentEndTime, List<Record> records, FinanceUser user, boolean lastInvoice)
            throws InternalServerErrorException, InvalidParameterException {
        Invoice invoice = generateInvoice(userId, provider, paymentStartTime, paymentEndTime, records);
        
        if (lastInvoice) {
            user.addInvoiceAsDebt(invoice);                
        } else { 
            user.addInvoice(invoice);
        }
            
        user.setProperty(FinanceUser.USER_LAST_BILLING_TIME, String.valueOf(paymentEndTime));
        this.userHolder.saveUser(user);
    }
    
    private Invoice generateInvoice(String userId, String provider, Long paymentStartTime, Long paymentEndTime,
            List<Record> records) throws InternalServerErrorException {
        this.invoiceBuilder.setUserId(userId);
        this.invoiceBuilder.setProviderId(provider);
        
        // TODO What is the expected behavior for the empty records list case? 
        for (Record record : records) {
            addRecordToInvoice(record, financePlan, paymentStartTime, paymentEndTime);
        }
        
        Invoice invoice = invoiceBuilder.buildInvoice();
        invoiceBuilder.reset();
        return invoice;
    }
    
    private void addRecordToInvoice(Record record, FinancePlan plan, 
            Long paymentStartTime, Long paymentEndTime) throws InternalServerErrorException {
        try {
            ResourceItem resourceItem = resourceItemFactory.getItemFromRecord(record);
            Double valueToPayPerTimeUnit = plan.getItemFinancialValue(resourceItem);
            Double timeUsed = resourceItemFactory.getTimeFromRecord(record, paymentStartTime, paymentEndTime);
            
            invoiceBuilder.addItem(resourceItem, valueToPayPerTimeUnit, timeUsed);
        } catch (InvalidParameterException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }
    
    public void setPlan(FinancePlan financePlan) {
        this.financePlan = financePlan;
    }
}
