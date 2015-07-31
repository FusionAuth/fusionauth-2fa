/*
 * Copyright (c) 2015, Inversoft Inc., All Rights Reserved
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
package org.primeframework.twofactor;


import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A Two Factor Authentication implementation using the HMAC-Based One-Time Password Algorithm.
 * <p>
 * <ul>
 * <li>http://tools.ietf.org/html/rfc6238 : TOTP: Time-Based One-Time Password Algorithm</li>
 * <li>https://tools.ietf.org/html/rfc4226 : HOTP: An HMAC-Based One-Time Password Algorithm</li>
 * </ul>
 * <p>
 * The examples and documentation credited below were utilized in the production of this implementation.
 *
 * @author Daniel DeGroff
 * @see <a href="http://thegreyblog.blogspot.com/2011/12/google-authenticator-using-it-in-your.html"/>
 * @see <a href="https://github.com/wstrange/GoogleAuth/blob/master/src/main/java/com/warrenstrange/googleauth/GoogleAuthenticator.java"/>
 * @see <a href="https://github.com/wstrange/GoogleAuth/blob/master/src/main/java/com/warrenstrange/googleauth/GoogleAuthenticatorConfig.java"/>
 * @see <a href="https://code.google.com/p/vellum/wiki/GoogleAuthenticator"/>
 */
public class TwoFactor {

  private TwoFactor() {
  }

  public static TwoFactor getInstance() {
    return new TwoFactor();
  }

  /**
   * Return the current timestamp using the default window size of 30 seconds.
   *
   * @return
   */
  public static long getCurrentWindowInstant() {
    return getCurrentWindowInstant(30);
  }

  /**
   * Return the current timestamp using the specified window size.
   *
   * @param windowSize The window size in seconds.
   * @return
   */
  public static long getCurrentWindowInstant(int windowSize) {
    return new Date().getTime() / TimeUnit.SECONDS.toMillis(windowSize);
  }

  /**
   * Base32 encode a string and return the byte array.
   *
   * @param rawSecret
   * @return the encoded byte array.
   */
  public static byte[] encode(String rawSecret) {
    try {
      return new Base32().encode(rawSecret.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decode a base32 encoded string and return the byte array.
   *
   * @param secret
   * @return the decoded byte array.
   */
  public static byte[] decode(String secret) {
    return new Base32().decode(secret.toUpperCase());
  }

  /**
   * Calculate the verification code based upon the provided instant.
   *
   * @param encodedSecret The base32 encoded secret.
   * @param instant       The instant to calculate the code.
   * @return
   */
  public String calculateVerificationCode(String encodedSecret, long instant) {
    // Generate Hashed Message from the secret
    byte[] hash = generateSha1HMAC(encodedSecret,
        ByteBuffer.allocate(8).putLong(instant).order(ByteOrder.BIG_ENDIAN).array());

    // Truncate the hash and return a left padded string representation
    int code = (int) bytesToUnsignedLong(hash);
    return String.format("%06d", code);
  }

  private long bytesToUnsignedLong(byte[] hash) {
    int offset = hash[hash.length - 1] & 0xF;
    long truncated = 0;
    for (int i = 0; i < 4; ++i) {
      truncated <<= 8;
      truncated |= (hash[offset + i] & 0xFF);
    }

    truncated &= Integer.MAX_VALUE; // 0x7FFFFFFF
    truncated %= 0x000F4240; // 10 ^ 6

    return truncated;
  }

  /**
   * Return true if the provided code equals the calculated one based upon the secret and instant.
   *
   * @param secret  The base32 encoded secret.
   * @param instant The instant to calculate the code.
   * @param code    The code to validate.
   * @return True if the code is valid.
   */
  public boolean validateVerificationCode(String secret, long instant, String code) {
    String actual = calculateVerificationCode(secret, instant);
    return code.equals(actual);
  }

  /**
   * Return the HMAC SHA-1 encoded byte array using the base32 encoded secret and the data.
   *
   * @param secret The base32 encoded secret.
   * @param data   The data to add to the secret - assumed to be a time instant.
   * @return A byte array of the HMAC SHA-1 hash.
   */
  public byte[] generateSha1HMAC(String secret, byte[] data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
      return mac.doFinal(data);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }
}

