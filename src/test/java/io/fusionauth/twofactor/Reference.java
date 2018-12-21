package io.fusionauth.twofactor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;

/**
 * Reference from <a href="https://tools.ietf.org/html/rfc6238#appendix-A">https://tools.ietf.org/html/rfc6238 Appendix-A</a>
 *
 * @author Daniel DeGroff
 */
public class Reference {
  private static final long[] DIGITS_POWER = {
    1L,               // 0
    10L,              // 1
    100L,             // 2
    1_000L,           // 3
    10_000L,          // 4
    100_000L,         // 5
    1_000_000L,       // 6
    10_000_000L,      // 7
    100_000_000L,     // 8
    1_000_000_000L,   // 9
    10_000_000_000L   // 10
  };

  public static String generateTOTP(String key,
                                    String time,
                                    String returnDigits,
                                    String crypto) {
    int codeDigits = Integer.decode(returnDigits).intValue();
    String result;

    // Using the counter
    // First 8 bytes are for the movingFactor
    // Compliant with base RFC 4226 (HOTP)
    while (time.length() < 16) {
      time = "0" + time;
    }

    // Get the HEX in a Byte[]
    byte[] msg = hexStr2Bytes(time);
    byte[] k = hexStr2Bytes(key);
    byte[] hash = hmac_sha(crypto, k, msg);

    // put selected bytes into result int
    int offset = hash[hash.length - 1] & 0xf;

    int binary =
      ((hash[offset] & 0x7f) << 24) |
        ((hash[offset + 1] & 0xff) << 16) |
        ((hash[offset + 2] & 0xff) << 8) |
        (hash[offset + 3] & 0xff);

    long otp = binary % DIGITS_POWER[codeDigits];

    result = Long.toString(otp);
    while (result.length() < codeDigits) {
      result = "0" + result;
    }
    return result;
  }

  private static byte[] hexStr2Bytes(String hex) {
    // Adding one byte to get the right conversion
    // Values starting with "0" can be converted
    byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

    // Copy all the REAL bytes, not the "first"
    byte[] ret = new byte[bArray.length - 1];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = bArray[i + 1];
    }
    return ret;
  }

  private static byte[] hmac_sha(String crypto, byte[] keyBytes,
                                 byte[] text) {
    try {
      Mac hmac;
      hmac = Mac.getInstance(crypto);
      SecretKeySpec macKey =
        new SecretKeySpec(keyBytes, "RAW");
      hmac.init(macKey);
      return hmac.doFinal(text);
    } catch (GeneralSecurityException gse) {
      throw new UndeclaredThrowableException(gse);
    }
  }
}
