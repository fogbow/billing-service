package cloud.fogbow.fs.core.plugins.payment.stub;

import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;

public class StubPostPaidPaymentManager implements PaymentManager {

	public static final String PAYMENT_STATUS_OK = "payment_ok";
	public static final String PAYMENT_STATUS_WAITING = "payment_waiting";
	public static final String PAYMENT_STATUS_DEFAULTING = "payment_defaulting";
	
	private DatabaseManager databaseManager;
	
	public StubPostPaidPaymentManager(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}
	
	@Override
	public boolean hasPaid(String userId, String provider) {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		String paymentStatusString = user.getProperty(FinanceUser.PAYMENT_STATUS_KEY);

		// TODO Improve
		if (paymentStatusString == null) {
			user.setProperty(FinanceUser.PAYMENT_STATUS_KEY, PAYMENT_STATUS_OK);
			return true;
		} else {
			if (paymentStatusString.equals(PAYMENT_STATUS_OK) || paymentStatusString.equals(PAYMENT_STATUS_WAITING)) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public void startPaymentProcess(String userId, String provider) {
		FinanceUser user = databaseManager.getUserById(userId, provider);
		user.setProperty(FinanceUser.PAYMENT_STATUS_KEY, PAYMENT_STATUS_WAITING);
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) {
		// TODO implement
		return property;
	}
}
