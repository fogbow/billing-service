package cloud.fogbow.fs.core.models;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

public enum InvoiceState {
	PAID("PAID"),
	WAITING("WAITING"),
	DEFAULTING("DEFAULTING");

	private String value;
	
	private InvoiceState(String value) {
		this.value = value;
	}
	
	public static InvoiceState fromValue(String value) throws InvalidParameterException {
		for (InvoiceState state: InvoiceState.values()) {
			if (state.value.equals(value)) {
				return state;
			}
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNKNOWN_INVOICE_STATE, value));
	}
	
	public String getValue() {
	    return value;
	}
}
