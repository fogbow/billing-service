package cloud.fogbow.fs.core.plugins.payment.prepaid;

import cloud.fogbow.fs.core.models.UserCredits;

public class UserCreditsFactory {
    public UserCredits getUserCredits(String userId, String provider) {
        return new UserCredits(userId, provider);
    }
}
