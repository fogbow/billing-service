package cloud.fogbow.fs.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
	public long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}
	
	public String toDate(String dateFormat, long lastBillingTime) {
		Date date = new Date(lastBillingTime); 
		return new SimpleDateFormat(dateFormat).format(date);
	}
}
