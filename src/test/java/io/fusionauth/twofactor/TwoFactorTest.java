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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Daniel DeGroff
 */
public class TwoFactorTest {
  @Test
  public void control() throws Exception {
    byte[] hash = TwoFactor.generateSha1HMAC("FusionAuth", "These pretzels are making me thirsty".getBytes(StandardCharsets.UTF_8));

    Formatter formatter = new Formatter();
    for (byte b : hash) {
      formatter.format("%02x", b);
    }
    assertEquals(formatter.toString(), "b8618ace38a6202a0cd4469e18d29c83ec7c6b43");
  }

  @Test
  public void generateRawSecret() {
    String rawSecret = TwoFactor.generateRawSecret();
    assertNotNull(rawSecret);
    System.out.println(rawSecret);

    String code = TwoFactor.calculateVerificationCode(rawSecret, TwoFactor.getCurrentWindowInstant());
    assertTrue(TwoFactor.validateVerificationCode(rawSecret, TwoFactor.getCurrentWindowInstant(), code));
  }

  /**
   * RFC 4226 in section 4 suggests a minimum of 128 bits with a recommended length of 160 bits.
   *
   * https://tools.ietf.org/html/rfc4226
   */
  @Test
  public void test_bitLengths() {
    assertEquals(TwoFactor.generateRawSecret(10).getBytes().length * 8, 80);
    assertEquals(TwoFactor.generateRawSecret(20).getBytes().length * 8, 160);
    assertEquals(TwoFactor.generateRawSecret(32).getBytes().length * 8, 256);
    assertEquals(TwoFactor.generateRawSecret().getBytes().length * 8, 160);
  }

  @Test(enabled = false)
  public void test_code_generations() {
    String rawSecret = TwoFactor.generateRawSecret(); // Set your secret here for testing purposes
    AtomicReference<String> last = new AtomicReference<>();
    IntStream.range(0, 100).forEach((n) -> {
      try {
        long instant = TwoFactor.getCurrentWindowInstant();
        String oneTimePassword = TwoFactor.calculateVerificationCode(rawSecret, instant);
        if (!oneTimePassword.equals(last.get())) {
          System.out.print(oneTimePassword + "\n");
        }
        last.set(oneTimePassword);
        Thread.sleep(1000);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Test
  public void test_multipleSHAs() {
    String rawSecret = "12345678901234567890";
    String message = "These pretzels are making me thirsty.";

    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, message.getBytes(StandardCharsets.UTF_8))), "9ed7cd3845e219e7b07543af595d624b777d182d");
    assertEquals(toHex(TwoFactor.generateSha256HMAC(rawSecret, message.getBytes(StandardCharsets.UTF_8))), "b51f2fb3c5b70e83c251f17ee6d3dec4ea41b507bfe74a88725b5222b93fe589");
    assertEquals(toHex(TwoFactor.generateSha512HMAC(rawSecret, message.getBytes(StandardCharsets.UTF_8))), "12dba679b1b0ad794852b15ea5d0baf3c342211e4f4aa12eb1e053cdec0bef6d2e5990e67b5532b598f50f9de3415b357eec78eb990d61135616cfe311600262");
  }

  /**
   * Test values from <a href="https://tools.ietf.org/html/rfc4226">RFC 4226</a>
   *
   * Appendix D - HOTP Algorithm: Test Values
   */
  @Test
  public void test_rfc4226_appendixD_HOTP_TestValues() {
    String rawSecret = "12345678901234567890";

    // Table 1 details for each count, the intermediate HMAC value.
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(0))), "cc93cf18508d94934c64b65d8ba7667fb7cde4b0");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(1))), "75a48a19d4cbe100644e8ac1397eea747a2d33ab");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(2))), "0bacb7fa082fef30782211938bc1c5e70416ff44");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(3))), "66c28227d03a2d5529262ff016a1e6ef76557ece");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(4))), "a904c900a64b35909874b33e61c5938a8e15ed1c");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(5))), "a37e783d7b7233c083d4f62926c7a25f238d0316");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(6))), "bc9cd28561042c83f219324d3c607256c03272ae");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(7))), "a4fb960c0bc06e1eabb804e5b397cdc4b45596fa");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(8))), "1b3c89f65e6c9e883012052823443f048b4332db");
    assertEquals(toHex(TwoFactor.generateSha1HMAC(rawSecret, toBigEndianArray(9))), "1637409809a679dc698207310c8c7fc07290d9e5");

    // Table 2 details for each count the truncated values (both in hexadecimal and decimal) and then the HOTP value.
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 0), String.valueOf(755224));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 1), String.valueOf(287082));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 2), String.valueOf(359152));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 3), String.valueOf(969429));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 4), String.valueOf(338314));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 5), String.valueOf(254676));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 6), String.valueOf(287922));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 7), String.valueOf(162583));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 8), String.valueOf(399871));
    assertEquals(TwoFactor.calculateVerificationCode(rawSecret, 9), String.valueOf(520489));
  }

  @Test
  public void validate() {
    String rawSecret = "These pretzels are making me thirsty.";
    long instant = 47893469;
    assertTrue(TwoFactor.validateVerificationCode(rawSecret, instant, "991696"));
  }

  private byte[] toBigEndianArray(int count) {
    return ByteBuffer.allocate(8).putLong(count).order(ByteOrder.BIG_ENDIAN).array();
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
