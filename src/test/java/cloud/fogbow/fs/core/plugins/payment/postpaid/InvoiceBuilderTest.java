package cloud.fogbow.fs.core.plugins.payment.postpaid;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.ComputeItem;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.models.ResourceItem;
import cloud.fogbow.fs.core.models.VolumeItem;

public class InvoiceBuilderTest {

	private static final String USER_ID_1 = "userId1";
	private static final String PROVIDER_ID_1 = "provider1";
	private static final String USER_ID_2 = "userId2";
	private static final String PROVIDER_ID_2 = "provider2";
	private static final int VCPU_ITEM_1 = 2;
	private static final int RAM_ITEM_1 = 4;
	private static final Double VALUE_ITEM_1 = 3.0;
	private static final Double TIME_USED_ITEM_1 = 2.0;
	private static final int VCPU_ITEM_2 = 1;
	private static final int RAM_ITEM_2 = 1;
	private static final Double VALUE_ITEM_2 = 0.5;
	private static final Double TIME_USED_ITEM_2 = 0.1;
	private static final int SIZE_ITEM_3 = 130;
	private static final Double VALUE_ITEM_3 = 5.0;
	private static final Double TIME_USED_ITEM_3 = 4.0;

	// test case: When calling the buildInvoice method, 
	// it must create a new Invoice object, considering the
	// items previously added using the method addItem.
	@Test
	public void testBuildInvoice() throws InvalidParameterException {
		ResourceItem resourceItem1 = new ComputeItem(VCPU_ITEM_1, RAM_ITEM_1);
		ResourceItem resourceItem2 = new ComputeItem(VCPU_ITEM_2, RAM_ITEM_2);
		ResourceItem resourceItem3 = new VolumeItem(SIZE_ITEM_3);
		
		InvoiceBuilder invoiceBuilder = new InvoiceBuilder();
		
		invoiceBuilder.setUserId(USER_ID_1);
		invoiceBuilder.setProviderId(PROVIDER_ID_1);
		invoiceBuilder.addItem(resourceItem1, VALUE_ITEM_1, TIME_USED_ITEM_1);
		invoiceBuilder.addItem(resourceItem2, VALUE_ITEM_2, TIME_USED_ITEM_2);
		invoiceBuilder.addItem(resourceItem3, VALUE_ITEM_3, TIME_USED_ITEM_3);
		Invoice invoice = invoiceBuilder.buildInvoice();
		
		List<Double> possibleValues = new ArrayList<Double>();
		possibleValues.addAll(Arrays.asList(VALUE_ITEM_1*TIME_USED_ITEM_1, 
                VALUE_ITEM_2*TIME_USED_ITEM_2, VALUE_ITEM_3*TIME_USED_ITEM_3));
		
		assertEquals(USER_ID_1, invoice.getUserId());
		assertEquals(PROVIDER_ID_1, invoice.getProviderId());
		assertEquals(InvoiceState.WAITING, invoice.getState());
		assertEquals(3, invoice.getInvoiceItemsList().size());
		
		assertTrue(possibleValues.contains(invoice.getInvoiceItemsList().get(0).getValue()));
		possibleValues.remove(invoice.getInvoiceItemsList().get(0).getValue());
		
		assertTrue(possibleValues.contains(invoice.getInvoiceItemsList().get(1).getValue()));
		possibleValues.remove(invoice.getInvoiceItemsList().get(1).getValue());
		
		assertTrue(possibleValues.contains(invoice.getInvoiceItemsList().get(2).getValue()));
		possibleValues.remove(invoice.getInvoiceItemsList().get(2).getValue());

		assertEquals(new Double(VALUE_ITEM_1*TIME_USED_ITEM_1 + VALUE_ITEM_2*TIME_USED_ITEM_2 
				+ VALUE_ITEM_3*TIME_USED_ITEM_3), invoice.getInvoiceTotal());
	}
	
	// test case: When calling the buildInvoice method and
	// no item has been added through the method addItem, 
	// it must create an empty Invoice.
	@Test
	public void testBuildEmptyInvoice() {
		InvoiceBuilder invoiceBuilder = new InvoiceBuilder();
		invoiceBuilder.setUserId(USER_ID_1);
		invoiceBuilder.setProviderId(PROVIDER_ID_1);
		
		Invoice invoice = invoiceBuilder.buildInvoice();
		
		assertEquals(USER_ID_1, invoice.getUserId());
		assertEquals(PROVIDER_ID_1, invoice.getProviderId());
		assertEquals(InvoiceState.WAITING, invoice.getState());
		assertEquals(0, invoice.getInvoiceItemsList().size());
		assertEquals(new Double(0.0), invoice.getInvoiceTotal());
	}
	
	// test case: When calling the reset method, it must reset all
	// information stored by the builder.
	@Test
	public void testInvoiceBuilderReuse() throws InvalidParameterException {
	    // Build first invoice
        ResourceItem resourceItem1 = new ComputeItem(VCPU_ITEM_1, RAM_ITEM_1);
        
        InvoiceBuilder invoiceBuilder = new InvoiceBuilder();
        
        invoiceBuilder.setUserId(USER_ID_1);
        invoiceBuilder.setProviderId(PROVIDER_ID_1);
        invoiceBuilder.addItem(resourceItem1, VALUE_ITEM_1, TIME_USED_ITEM_1);
        Invoice invoice1 = invoiceBuilder.buildInvoice();
        
        assertEquals(USER_ID_1, invoice1.getUserId());
        assertEquals(PROVIDER_ID_1, invoice1.getProviderId());
        assertEquals(InvoiceState.WAITING, invoice1.getState());
        assertEquals(1, invoice1.getInvoiceItemsList().size());
        assertEquals(new Double(VALUE_ITEM_1*TIME_USED_ITEM_1), 
                invoice1.getInvoiceItemsList().get(0).getValue());
        assertEquals(new Double(VALUE_ITEM_1*TIME_USED_ITEM_1), invoice1.getInvoiceTotal());
        
        
        // Reset the builder
        invoiceBuilder.reset();
        
        
        // Build second invoice
        invoiceBuilder.setUserId(USER_ID_2);
        invoiceBuilder.setProviderId(PROVIDER_ID_2);
        
        Invoice invoice2 = invoiceBuilder.buildInvoice();
        
        assertEquals(USER_ID_2, invoice2.getUserId());
        assertEquals(PROVIDER_ID_2, invoice2.getProviderId());
        assertEquals(InvoiceState.WAITING, invoice2.getState());
        assertEquals(0, invoice2.getInvoiceItemsList().size());
        assertEquals(new Double(0.0), invoice2.getInvoiceTotal());
	}
}
