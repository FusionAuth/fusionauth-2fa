/*
 * Copyright (c) 2015-2018, FusionAuth, All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package io.fusionauth.twofactor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * A Two Factor Authentication implementation using the HMAC-Based One-Time Password Algorithm.
 * <p>
 * <ul> <li>http://tools.ietf.org/html/rfc6238 : TOTP: Time-Based One-Time Password Algorithm</li>
 * <li>https://tools.ietf.org/html/rfc4226 : HOTP: An HMAC-Based One-Time Password Algorithm</li> </ul>
 * <p>
 * The examples and documentation credited below were utilized in the production of this implementation.
 *
 * @author Daniel DeGroff
 * @see <a href="http://thegreyblog.blogspot.com/2011/12/google-authenticator-using-it-in-your.html"/>
 * @see <a href="https://github.com/wstrange/GoogleAuth/blob/master/src/main/java/com/warrenstrange/googleauth/GoogleAuthenticator.java"/>
 * @see <a href="https://github.com/wstrange/GoogleAuth/blob/master/src/main/java/com/warrenstrange/googleauth/GoogleAuthenticatorConfig.java"/>
 * @see <a href="https://code.google.com/p/vellum/wiki/GoogleAuthenticator"/>
 */
public final class TwoFactor {
  /**
   * Calculate the verification code based upon the provided instant.
   *
   * @param rawSecret The secret.
   * @param instant   The windowed instant to calculate the code.
   * @return The verification code.
   */
  public static String calculateVerificationCode(String rawSecret, long instant) {
    // Generate Hashed Message from the secret
    byte[] hash = generateSha1HMAC(rawSecret, ByteBuffer.allocate(8).putLong(instant).order(ByteOrder.BIG_ENDIAN).array());

    // Truncate the hash and return a left padded string representation
    int code = bytesToUnsignedInt(hash);
    return String.format("%06d", code);
  }

  /**
   * Generates the secret used to generate time based one time passwords. The length of the string will be 20
   * characters or 160 bits.
   * <p>
   * If this secret will be used to generate  QR code to display to a user, it will need to be Base32 encoded.
   *
   * @return The raw secret.
   */
  public static String generateRawSecret() {
    return generateRawSecret(20);
  }

  /**
   * Generates the secret used to generate time based one time passwords.
   * <p>
   * If this secret will be used to generate  QR code to display to a user, it will need to be Base32 encoded.
   *
   * @param length the length of the raw secret in characters
   * @return The raw secret.
   */
  public static String generateRawSecret(int length) {
    byte[] buf = new byte[length];
    new SecureRandom().nextBytes(buf);
    String rawSecret = Base64.getEncoder().encodeToString(buf);
    return rawSecret.substring(1, length + 1);
  }

  /**
   * Return the HMAC SHA-1 encoded byte array using the base32 encoded secret and the data as
   * defined by the HOTP algorithm defined by <a href="https://tools.ietf.org/html/rfc4226">RFC 4226</a>.
   *
   * @param rawSecret The secret.
   * @param data      The data to add to the secret - assumed to be a time instant.
   * @return A byte array of the HMAC SHA-1 hash.
   */
  public static byte[] generateSha1HMAC(String rawSecret, byte[] data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(rawSecret.getBytes(), "HmacSHA1"));
      return mac.doFinal(data);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Return the HMAC SHA-256 encoded byte array using the base32 encoded secret and the data as
   * defined by the HOTP algorithm defined by <a href="https://tools.ietf.org/html/rfc4226">RFC 4226</a>.
   *
   * @param rawSecret The secret.
   * @param data      The data to add to the secret - assumed to be a time instant.
   * @return A byte array of the HMAC SHA-256 hash.
   */
  public static byte[] generateSha256HMAC(String rawSecret, byte[] data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(rawSecret.getBytes(), "HmacSHA256"));
      return mac.doFinal(data);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Return the HMAC SHA-512 encoded byte array using the base32 encoded secret and the data as
   * defined by the HOTP algorithm defined by <a href="https://tools.ietf.org/html/rfc4226">RFC 4226</a>.
   *
   * @param rawSecret The secret.
   * @param data      The data to add to the secret - assumed to be a time instant.
   * @return A byte array of the HMAC SHA-512 hash.
   */
  public static byte[] generateSha512HMAC(String rawSecret, byte[] data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA512");
      mac.init(new SecretKeySpec(rawSecret.getBytes(), "HmacSHA512"));
      return mac.doFinal(data);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Return the current timestamp using the default window size of 30 seconds.
   *
   * <p>
   * A 30 second time step is the recommended default value in <a href="https://tools.ietf.org/html/rfc6238#section-5.2">section 5.2</a>
   * of <a href="https://tools.ietf.org/html/rfc6238">RFC 6238</a>.
   * </p>
   *
   * @return The current window instant.
   */
  public static long getCurrentWindowInstant() {
    return getCurrentWindowInstant(30);
  }

  /**
   * Return the current timestamp using the specified window size.
   *
   * <p>
   * The validation time-step size as defined by <a href="https://tools.ietf.org/html/rfc6238">RFC 6238</a> in
   * <a href="https://tools.ietf.org/html/rfc6238#section-5.2">section 5.2</a>.
   * </p>
   *
   * @param windowSize The window size in seconds.
   * @return The current window instant.
   */
  public static long getCurrentWindowInstant(int windowSize) {
    return System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(windowSize);
  }

  /**
   * Return true if the provided code equals the calculated one based upon the secret and instant.
   *
   * @param secret  The base32 encoded secret.
   * @param instant The windowed instant to calculate the code.
   * @param code    The code to validate.
   * @return True if the code is valid.
   */
  public static boolean validateVerificationCode(String secret, long instant, String code) {
    String actual = calculateVerificationCode(secret, instant);
    return code.equals(actual);
  }

  /**
   * Return an int by taking four bytes of the provided byte array and taking the unsigned int value.
   *
   * @param hash the hashed message
   * @return an int value
   */
  private static int bytesToUnsignedInt(byte[] hash) {
    int offset = hash[hash.length - 1] & 0xF;
    long truncated = 0;
    for (int i = 0; i < 4; ++i) {
      truncated <<= 8;
      truncated |= (hash[offset + i] & 0xFF);
    }

    truncated &= Integer.MAX_VALUE; // 0x7FFFFFFF
    truncated %= 0x000F4240; // 10 ^ 6

    return (int) truncated;
  }
}