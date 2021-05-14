package cloud.fogbow.fs.core.plugins;

import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.util.accounting.Record;

/**
 * A payment management abstraction, capable of starting
 * payment for users based in their financial state. This 
 * interface also contains methods for providing information
 * on the users financial state.
 */
public interface PaymentManager {
	
	/**
	 * Verifies if the user financial state is adequate to 
	 * perform new operations and allocate new resources.
	 * The meaning of this operation depends on the payment type.
	 * For example, in a postpaid scenario, this operation might 
	 * check if the user has paid the previous invoices, whereas 
	 * in a prepaid scenario, this operation might check if the user
	 * still has credits to use.
	 * 
	 * @param userId the id of the user whose state must be evaluated.
	 * @param provider the id of the provider of the user whose state must be evaluated.
	 * @return a boolean stating whether the user state is adequate or not.
	 * @throws InvalidParameterException if the user is not found or the parameters 
	 * are invalid.
	 * @throws InternalServerErrorException if an error occurs while trying to retrieve the user
	 * state.
	 */
	boolean hasPaid(String userId, String provider) throws InvalidParameterException, InternalServerErrorException;
	
	/**
	 * Starts payment for the resources consumed by the 
	 * given user. The meaning of this operation depends on the
	 * payment type. For example, in a postpaid scenario, 
	 * this operation is an invoice generation process, 
	 * whereas in a prepaid scenario, this operation is a user 
	 * credits update process.
	 * 
	 * @param userId the id of the user to start payment.
	 * @param provider the id of the provider of the user to start payment.
	 * @param paymentStartTime the start time of the period to consider when starting the payment process.
	 * @param paymentEndTime the end time of the period to consider when starting the payment process.
	 * @param records a list containing the records related to the user in the period.
	 * @throws InternalServerErrorException if some error occurs in the payment process.
	 * @throws InvalidParameterException if the user is not found or the parameters 
     * are invalid.
	 */
	void startPaymentProcess(String userId, String provider, Long paymentStartTime, Long paymentEndTime, 
	        List<Record> records) throws InternalServerErrorException, InvalidParameterException;
	
	/**
	 * Generates and returns a representation of the given property of
	 * a user finance state.
	 * 
	 * @param userId the id of the user whose state is used in the operation.
	 * @param provider the id of the provider of the user whose state is used in the operation.
	 * @param property a description of the property to generate.
	 * @return a representation of the property.
	 * @throws InvalidParameterException if the user is not found or the property is not known.
	 * @throws InternalServerErrorException if an error occurs while trying to retrieve the user state.
	 */
	String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException, InternalServerErrorException;
	
	/**
	 * Sets the name of the {@link cloud.fogbow.fs.core.models.FinancePlan} this payment manager 
	 * is going to use in the payment process.
	 * 
	 * @param planName the name of the finance plan to use.
	 * @throws InvalidParameterException if the finance plan is not known.
	 * @throws InternalServerErrorException if some error occurs while setting the finance plan.
	 */
	void setFinancePlan(String planName) throws InvalidParameterException, InternalServerErrorException;
}
