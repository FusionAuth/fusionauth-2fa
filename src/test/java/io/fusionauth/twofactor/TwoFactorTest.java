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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
  public void control() {
    byte[] hash = TwoFactor.generateSha1HMAC("FusionAuth".getBytes(StandardCharsets.UTF_8), "These pretzels are making me thirsty".getBytes(StandardCharsets.UTF_8));

    Formatter formatter = new Formatter();
    for (byte b : hash) {
      formatter.format("%02x", b);
    }
    assertEquals(formatter.toString(), "b8618ace38a6202a0cd4469e18d29c83ec7c6b43");

    assertTrue(TwoFactor.validateVerificationCode("These pretzels are making me thirsty.".getBytes(StandardCharsets.UTF_8), 47893469, "991696"));
  }

  @Test
  public void generateRawSecret() {
    String base64EncodedSecret = TwoFactor.generateBase64EncodedSecret();
    byte[] bytes = base64EncodedSecret.getBytes(StandardCharsets.UTF_8);
    assertNotNull(base64EncodedSecret);

    String code = TwoFactor.calculateVerificationCode(bytes, TwoFactor.getCurrentWindowInstant());
    assertTrue(TwoFactor.validateVerificationCode(bytes, TwoFactor.getCurrentWindowInstant(), code));
  }

  /**
   * @See <a href="https://stackoverflow.com/questions/42546493/generate-a-10-digit-totp-password-with-a-certain-key/"> https://stackoverflow.com/questions/42546493/generate-a-10-digit-totp-password-with-a-certain-key/ </a>
   */
  @Test
  public void stackOverflow_42546493() {
    //  Mon, 17 Mar 2014 15:20:51 GMT
    ZonedDateTime date = ZonedDateTime.of(2014, 3, 17, 15, 20, 51, 0, ZoneId.of("GMT"));
    long seconds = date.toEpochSecond();
    long timeStep = seconds / 30;
    assert seconds == 1395069651L;

    String rawSecret = "ninja@example.comHDECHALLENGE003"; // 32 bytes
    String rawSecret64Bytes = rawSecret + rawSecret; // 64 bytes
    byte[] bytes = rawSecret64Bytes.getBytes(StandardCharsets.UTF_8);

    // User is expecting 1773133250, but receives 0490867067
    // - As far as I can tell 0490867067 is the correct code.

    String code = TwoFactor.calculateVerificationCode(bytes, timeStep, Algorithm.HmacSHA512, 10);
    assertEquals(code, "0490867067");
  }

  /**
   * RFC 4226 in section 4 suggests a minimum of 128 bits with a recommended length of 160 bits.
   *
   * https://tools.ietf.org/html/rfc4226
   */
  @Test
  public void test_bitLengths() {
    assertEquals(TwoFactor.generateBase64EncodedSecret(10).getBytes().length * 8, 80);
    assertEquals(TwoFactor.generateBase64EncodedSecret(20).getBytes().length * 8, 160);
    assertEquals(TwoFactor.generateBase64EncodedSecret(32).getBytes().length * 8, 256);
    assertEquals(TwoFactor.generateBase64EncodedSecret(64).getBytes().length * 8, 512);

    // Default for this library is 160
    assertEquals(TwoFactor.generateBase64EncodedSecret().getBytes().length * 8, 160);
  }

  @Test(enabled = false)
  public void test_code_generations() {
    String base64EncodedSecret = TwoFactor.generateBase64EncodedSecret(); // Set your secret here for testing purposes
    byte[] bytes = base64EncodedSecret.getBytes(StandardCharsets.UTF_8);
    AtomicReference<String> last = new AtomicReference<>();
    IntStream.range(0, 100).forEach((n) -> {
      try {
        long instant = TwoFactor.getCurrentWindowInstant();
        String oneTimePassword = TwoFactor.calculateVerificationCode(bytes, instant);
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
    String secret = "12345678901234567890";
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    String message = "These pretzels are making me thirsty.";
    byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, messageBytes)),
                 "9ed7cd3845e219e7b07543af595d624b777d182d");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha256HMAC(bytes, messageBytes)),
                 "b51f2fb3c5b70e83c251f17ee6d3dec4ea41b507bfe74a88725b5222b93fe589");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha512HMAC(bytes, messageBytes)),
                 "12dba679b1b0ad794852b15ea5d0baf3c342211e4f4aa12eb1e053cdec0bef6d2e5990e67b5532b598f50f9de3415b357eec78eb990d61135616cfe311600262");
  }

  /**
   * Test values from <a href="https://tools.ietf.org/html/rfc4226">RFC 4226</a>
   *
   * Appendix D - HOTP Algorithm: Test Values
   */
  @Test
  public void test_rfc4226_appendixD_HOTP_TestValues() {
    String secret = "12345678901234567890";
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

    String hexSecret = HexUtils.fromBytes(bytes);
    assertEquals(hexSecret, "3132333435363738393031323334353637383930");

    // Table 1 details for each count, the intermediate HMAC value.
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(0))), "cc93cf18508d94934c64b65d8ba7667fb7cde4b0");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(1))), "75a48a19d4cbe100644e8ac1397eea747a2d33ab");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(2))), "0bacb7fa082fef30782211938bc1c5e70416ff44");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(3))), "66c28227d03a2d5529262ff016a1e6ef76557ece");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(4))), "a904c900a64b35909874b33e61c5938a8e15ed1c");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(5))), "a37e783d7b7233c083d4f62926c7a25f238d0316");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(6))), "bc9cd28561042c83f219324d3c607256c03272ae");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(7))), "a4fb960c0bc06e1eabb804e5b397cdc4b45596fa");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(8))), "1b3c89f65e6c9e883012052823443f048b4332db");
    assertEquals(HexUtils.fromBytes(TwoFactor.generateSha1HMAC(bytes, toBigEndianArray(9))), "1637409809a679dc698207310c8c7fc07290d9e5");

    // Table 2 details for each count the truncated values (both in hexadecimal and decimal) and then the HOTP value.
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 0), String.valueOf(755224));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 1), String.valueOf(287082));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 2), String.valueOf(359152));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 3), String.valueOf(969429));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 4), String.valueOf(338314));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 5), String.valueOf(254676));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 6), String.valueOf(287922));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 7), String.valueOf(162583));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 8), String.valueOf(399871));
    assertEquals(TwoFactor.calculateVerificationCode(bytes, 9), String.valueOf(520489));
  }

  /**
   * Test values from <a href="https://tools.ietf.org/html/rfc6238">RFC 6238</a>
   *
   * Appendix B - Appendix B.  Test Vectors
   */
  @Test
  public void test_rfc_6238_HOTP_TestValues() {
    String secret = "12345678901234567890";
    String secret32 = secret + secret.substring(0, 12);
    String secret64 = secret + secret + secret + secret.substring(0, 4);

    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    byte[] bytes32 = secret32.getBytes(StandardCharsets.UTF_8);
    byte[] bytes64 = secret64.getBytes(StandardCharsets.UTF_8);

    // 20 Byte seed for SHA-1
    String seed20 = HexUtils.fromBytes(bytes);
    assertEquals(seed20, "3132333435363738393031323334353637383930");

    // 32 byte seed for SHA-256
    String seed32 = HexUtils.fromBytes(bytes) + HexUtils.fromBytes(bytes).substring(0, 24);
    assertEquals(seed32, "3132333435363738393031323334353637383930" + "313233343536373839303132");

    // 64 byte seed for SHA-512
    String seed64 = HexUtils.fromBytes(bytes) + HexUtils.fromBytes(bytes) + HexUtils.fromBytes(bytes) + HexUtils.fromBytes(bytes).substring(0, 8);
    assertEquals(seed64, "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930" + "31323334");

    // 59 seconds - Window 1
    assertEquals((59L / 30), 1);
    assertEquals(Long.toHexString((59L / 30)).toUpperCase(), "1");
    assertEquals(TwoFactor.calculateVerificationCode(bytes, (59L / 30), Algorithm.HmacSHA1, 8), "94287082");
    assertEquals(TwoFactor.calculateVerificationCode(bytes32, (59L / 30), Algorithm.HmacSHA256, 8), "46119246");
    assertEquals(TwoFactor.calculateVerificationCode(bytes64, (59L / 30), Algorithm.HmacSHA512, 8), "90693936");

    // 1111111109 seconds - Window 37037036
    assertEquals((1111111109L / 30), 37037036);
    assertEquals(Long.toHexString((1111111109L / 30)).toUpperCase(), "23523EC");
    assertEquals(TwoFactor.calculateVerificationCode(bytes, (1111111109L / 30), Algorithm.HmacSHA1, 8), "07081804");
    assertEquals(TwoFactor.calculateVerificationCode(bytes32, (1111111109L / 30), Algorithm.HmacSHA256, 8), "68084774");
    assertEquals(TwoFactor.calculateVerificationCode(bytes64, (1111111109L / 30), Algorithm.HmacSHA512, 8), "25091201");

    // 1111111111 seconds - Window
    assertEquals((1111111111L / 30), 37037037);
    assertEquals(Long.toHexString((1111111111L / 30)).toUpperCase(), "23523ED");
    assertEquals(TwoFactor.calculateVerificationCode(bytes, (1111111111L / 30), Algorithm.HmacSHA1, 8), "14050471");
    assertEquals(TwoFactor.calculateVerificationCode(bytes32, (1111111111L / 30), Algorithm.HmacSHA256, 8), "67062674");
    assertEquals(TwoFactor.calculateVerificationCode(bytes64, (1111111111L / 30), Algorithm.HmacSHA512, 8), "99943326");

    // 1234567890 seconds - Window
    assertEquals((1234567890L / 30), 41152263);
    assertEquals(Long.toHexString((1234567890L / 30)).toUpperCase(), "273EF07");
    assertEquals(TwoFactor.calculateVerificationCode(bytes, (1234567890L / 30), Algorithm.HmacSHA1, 8), "89005924");
    assertEquals(TwoFactor.calculateVerificationCode(bytes32, (1234567890L / 30), Algorithm.HmacSHA256, 8), "91819424");
    assertEquals(TwoFactor.calculateVerificationCode(bytes64, (1234567890L / 30), Algorithm.HmacSHA512, 8), "93441116");

    // 2000000000 seconds - Window
    assertEquals((2000000000L / 30), 66666666);
    assertEquals(Long.toHexString((2000000000L / 30)).toUpperCase(), "3F940AA");
    assertEquals(TwoFactor.calculateVerificationCode(bytes, (2000000000L / 30), Algorithm.HmacSHA1, 8), "69279037");
    assertEquals(TwoFactor.calculateVerificationCode(bytes32, (2000000000L / 30), Algorithm.HmacSHA256, 8), "90698825");
    assertEquals(TwoFactor.calculateVerificationCode(bytes64, (2000000000L / 30), Algorithm.HmacSHA512, 8), "38618901");

    // 20000000000 seconds - Window
    assertEquals((20000000000L / 30), 666666666);
    assertEquals(Long.toHexString((20000000000L / 30)).toUpperCase(), "27BC86AA");
    assertEquals(TwoFactor.calculateVerificationCode(bytes, (20000000000L / 30), Algorithm.HmacSHA1, 8), "65353130");
    assertEquals(TwoFactor.calculateVerificationCode(bytes32, (20000000000L / 30), Algorithm.HmacSHA256, 8), "77737706");
    assertEquals(TwoFactor.calculateVerificationCode(bytes64, (20000000000L / 30), Algorithm.HmacSHA512, 8), "47863826");
  }

  @Test
  public void test_time_steps() {
    // https://tools.ietf.org/html/rfc6238#appendix-B
    // - Appendix B 4.2 Description

    // T0 = 0 and Time Step X = 30, T = 0 if the current Unix time is 0 seconds (epoch)
    assertEquals((0 / 30), 0);
    // T0 = 0 and Time Step X = 30, T = 1 if the current Unix time is 59 seconds
    assertEquals((59 / 30), 1);
    // T0 = 0 and Time Step X = 30, T = 2 if the current Unix time is 60 seconds
    assertEquals((60 / 30), 2);
  }

  private byte[] toBigEndianArray(int count) {
    return ByteBuffer.allocate(8).putLong(count).order(ByteOrder.BIG_ENDIAN).array();
  }
}
