package cloud.fogbow.fs.core.plugins.plan.postpaid;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class InvoiceManager {
    private static Logger LOGGER = Logger.getLogger(InvoiceManager.class);
    public static final String PROPERTY_VALUES_SEPARATOR = ",";
    public static final String ALL_USER_INVOICES_PROPERTY_NAME = "ALL_USER_INVOICES";
    
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
        LOGGER.info(this.userHolder);
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
            Long paymentStartTime, Long paymentEndTime, List<Record> records) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.userHolder.getUserById(userId, provider);
        
        synchronized(user) {
            synchronized(financePlan) {
                this.invoiceBuilder.setUserId(userId);
                this.invoiceBuilder.setProviderId(provider);
                
                // TODO What is the expected behavior for the empty records list case? 
                for (Record record : records) {
                    addRecordToInvoice(record, financePlan, paymentStartTime, paymentEndTime);
                }
                
                Invoice invoice = invoiceBuilder.buildInvoice();
                invoiceBuilder.reset();
                
                user.addInvoice(invoice);
                user.setProperty(FinanceUser.USER_LAST_BILLING_TIME, String.valueOf(paymentEndTime));

                this.userHolder.saveUser(user);
            }
        }
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

    public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException, InternalServerErrorException {
        String propertyValue = "";
        
        if (property.equals(ALL_USER_INVOICES_PROPERTY_NAME)) {
            List<String> invoiceJsonReps = new ArrayList<String>();
            FinanceUser user = this.userHolder.getUserById(userId, provider);
            
            synchronized (user) {
                List<Invoice> userInvoices = user.getInvoices();

                for (Invoice invoice : userInvoices) {
                    String invoiceJson = invoice.toString();
                    invoiceJsonReps.add(invoiceJson);
                }

                propertyValue = "[" + String.join(PROPERTY_VALUES_SEPARATOR, invoiceJsonReps) + "]";
            }
        } else {
            throw new InvalidParameterException(String.format(Messages.Exception.UNKNOWN_FINANCE_PROPERTY, property));
        }
        
        return propertyValue;
    }
    
    public void setPlan(FinancePlan financePlan) {
        this.financePlan = financePlan;
    }
}
