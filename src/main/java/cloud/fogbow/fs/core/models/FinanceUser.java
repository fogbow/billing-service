package cloud.fogbow.fs.core.models;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

@Entity
@Table(name = "finance_user_table")
public class FinanceUser implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final String PAYMENT_TYPE_KEY = "paymentType";
    public static final String PAYMENT_STATUS_KEY = "paymentStatus";
    public static final String USER_LAST_BILLING_TIME = "last_billing_time";
    
    private static final String FINANCE_PLUGIN_NAME_COLUMN_NAME = "finance_plugin_name";
    private static final String STOPPED_RESOURCES_COLUMN_NAME = "stopped_resources";
    private static final String PROPERTIES_COLUMN_NAME = "properties";
    private static final String INVOICES_COLUMN_NAME = "invoices";
    
    @Transient
	private static final Logger LOGGER = Logger.getLogger(FinanceUser.class);
	
    @EmbeddedId
    private UserId userId;
    
    @Column(name = FINANCE_PLUGIN_NAME_COLUMN_NAME)
	private String financePluginName;
    
    @Column(name = STOPPED_RESOURCES_COLUMN_NAME)
	private boolean stoppedResources;
    
    @Column(name = PROPERTIES_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> properties;
    
    @Column(name = INVOICES_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade={CascadeType.ALL})
	private List<Invoice> invoices;
    
    @OneToOne(cascade={CascadeType.ALL})
	private UserCredits credits;

    public FinanceUser() {

	}
	
	public FinanceUser(Map<String, String> properties) {
		this.stoppedResources = false;
		this.properties = properties;
        long billingTime = System.currentTimeMillis();
        this.setProperty(USER_LAST_BILLING_TIME, String.valueOf(billingTime));
	}
	
	public void setUserId(String id, String provider) {
	    this.userId = new UserId(id, provider);
	}
	
	public String getProvider() {
	    return this.userId.getProvider();
	}

	public String getId() {
	    return this.userId.getUserId();
	}

	public boolean stoppedResources() {
		return stoppedResources;
	}

	public void setStoppedResources(boolean stoppedResources) {
		this.stoppedResources = stoppedResources;
	}
	
	public String getProperty(String propertyName) {
		return this.properties.get(propertyName);
	}
	
	public void setProperty(String propertyName, String propertyValue) {
		this.properties.put(propertyName, propertyValue);
	}
	
	public String getFinancePluginName() {
		return financePluginName;
	}

	public void setFinancePluginName(String financePluginName) {
		this.financePluginName = financePluginName;
	}
	
    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void addInvoice(Invoice invoice) {
        this.invoices.add(invoice);
    }
    
    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }

    public UserCredits getCredits() {
        return credits;
    }

    public void setCredits(UserCredits credits) {
        this.credits = credits;
    }
}
