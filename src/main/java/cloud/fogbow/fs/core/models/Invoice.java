package cloud.fogbow.fs.core.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.google.gson.Gson;

@Entity
@Table(name = "invoice_table")
public class Invoice {

    private static final String INVOICE_ID_COLUMN_NAME = "invoice_id";
    private static final String USER_ID_COLUMN_NAME = "user_id";
    private static final String PROVIDER_ID_COLUMN_NAME = "provider_id";
    private static final String INVOICE_STATE_COLUMN_NAME = "state";
    private static final String INVOICE_ITEMS_COLUMN_NAME = "invoice_items";
    private static final String INVOICE_TOTAL_COLUMN_NAME = "invoice_total";

    @Column(name = INVOICE_ID_COLUMN_NAME)
    @Id
	private String invoiceId;
    
    @Column(name = USER_ID_COLUMN_NAME)
	private String userId;
    
    @Column(name = PROVIDER_ID_COLUMN_NAME)
	private String providerId;
    
    @Column(name = INVOICE_STATE_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
	private InvoiceState state;

    @Column(name = INVOICE_ITEMS_COLUMN_NAME)
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade={CascadeType.ALL})
    private List<InvoiceItem> invoiceItems;
    
    @Column(name = INVOICE_TOTAL_COLUMN_NAME)
	private Double invoiceTotal;
	
    public Invoice() {
        
    }
    
	public Invoice(String invoiceId, String userId, String providerId, 
			InvoiceState state, Map<ResourceItem, Double> items, 
			Double invoiceTotal) {
		this.invoiceId = invoiceId;
		this.userId = userId;
		this.providerId = providerId;
		this.state = state;
		this.invoiceTotal = invoiceTotal;
		
		this.invoiceItems = new ArrayList<InvoiceItem>();
		
		for (ResourceItem item : items.keySet()) {
		    invoiceItems.add(new InvoiceItem(item, items.get(item)));
		}
	}

	public List<InvoiceItem> getInvoiceItemsList() {
        return invoiceItems;
    }

    public void setInvoiceItemsList(List<InvoiceItem> invoiceItemsList) {
        this.invoiceItems = invoiceItemsList;
    }

    public String getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(String invoiceId) {
		this.invoiceId = invoiceId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public Double getInvoiceTotal() {
		return invoiceTotal;
	}

	public void setInvoiceTotal(Double invoiceTotal) {
		this.invoiceTotal = invoiceTotal;
	}

	public InvoiceState getState() {
		return state;
	}
	
	public void setState(InvoiceState state) {
		this.state = state;
	}
	
	public String jsonRepr() {
		return new Gson().toJson(this);
	}
	
	@Override
	public String toString() {
		String invoiceItemsString = "{";
		List<String> invoiceItemsStringList = new ArrayList<String>();
		
		for (InvoiceItem invoiceItem : invoiceItems) {
            String itemString = invoiceItem.getItem().toString();
            String valueString = String.format("%.3f", invoiceItem.getValue());
            String itemValuePairString = itemString + ":" + valueString;
            invoiceItemsStringList.add(itemValuePairString);
		}

		invoiceItemsString += String.join(",", invoiceItemsStringList);
		invoiceItemsString += "}";
		
		String invoiceTotalString = String.format("%.3f", invoiceTotal);
		
		return "{\"id\":\"" + invoiceId + "\", \"userId\":\"" + userId + "\", \"providerId\":\"" + providerId + "\", \"state\":\""
				+ state + "\", \"invoiceItems\":" + invoiceItemsString + ", \"invoiceTotal\":" + invoiceTotalString + "}";
	}
}
