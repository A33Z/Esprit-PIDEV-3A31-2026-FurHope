package integrations.auth;

import java.security.SecureRandom;

public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_BOUND = 1_000_000;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtpCode() {
        int number = secureRandom.nextInt(OTP_BOUND);
        return String.format("%0" + OTP_LENGTH + "d", number);
    }
}
