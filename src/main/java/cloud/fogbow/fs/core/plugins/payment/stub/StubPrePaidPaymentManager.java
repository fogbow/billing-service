package cloud.fogbow.fs.core.plugins.payment.stub;

import java.util.List;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.accounting.Record;

public class StubPrePaidPaymentManager implements PaymentManager {

	public StubPrePaidPaymentManager(DatabaseManager databaseManager) {
	}
	
	@Override
	public boolean hasPaid(String userId, String provider) throws InvalidParameterException {
	    return true;
	}

	@Override
	public void startPaymentProcess(String userId, String provider, 
	        Long paymentStartTime, Long paymentEndTime, List<Record> records) throws InvalidParameterException {
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException {
		return property;
	}

	@Override
	public void setFinancePlan(String planName) {
		// do nothing
	}
}
