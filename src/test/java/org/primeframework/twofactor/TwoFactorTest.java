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

  /**
   * Assert known secrets and their SHA1 HMAC values to ensure the hash calculation is correct.
   *
   * @throws Exception
   */
  @Test
  public void control() throws Exception {
    byte[] hash = TwoFactor.generateSha1HMAC("Inversoft", "These pretzels are making me thirsty".getBytes("UTF-8"));

    Formatter formatter = new Formatter();
    for (byte b : hash) {
      formatter.format("%02x", b);
    }
    assertEquals(formatter.toString(), "f086f43907fa796d0a0ec74ae7180b7f83c61db6");
  }

  @Test
  public void generateRawSecret() {
    String rawSecret = TwoFactor.generateRawSecret();
    assertNotNull(rawSecret);
    System.out.println(rawSecret);

    String code = TwoFactor.calculateVerificationCode(rawSecret, TwoFactor.getCurrentWindowInstant());
    assertTrue(TwoFactor.validateVerificationCode(rawSecret, TwoFactor.getCurrentWindowInstant(), code));
  }

  @Test(enabled = false)
  public void test_code_generations() throws Exception {
    AtomicReference<String> last = new AtomicReference<>();
    IntStream.range(0, 100).forEach((n) -> {
      try {
        long instant = TwoFactor.getCurrentWindowInstant();
        String oneTimePassword = TwoFactor.calculateVerificationCode("HELLO", instant);
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
  public void validate() throws Exception {
    String rawSecret = "These pretzels are making me thirsty.";
    long instant = 47893469;
    assertTrue(TwoFactor.validateVerificationCode(rawSecret, instant, "991696"));
  }
}
