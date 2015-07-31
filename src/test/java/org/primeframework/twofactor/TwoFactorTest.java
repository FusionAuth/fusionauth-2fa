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

import org.testng.annotations.Test;

import java.util.Formatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;
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
    TwoFactor twoFactor = TwoFactor.getInstance();
    byte[] hash = twoFactor.generateSha1HMAC("Inversoft", "These pretzels are making me thirsty".getBytes("UTF-8"));

    Formatter formatter = new Formatter();
    for (byte b : hash) {
      formatter.format("%02x", b);
    }
    assertEquals(formatter.toString(), "85c27d3f088c0c8461d240c41021bc4f13d2456e");
  }

  @Test
  public void encode_decode() throws Exception {
    TwoFactor twoFactor = TwoFactor.getInstance();

    byte[] encoded = twoFactor.encode("These pretzels are making me thirsty.");
    assertEquals(new String(encoded, "UTF-8"), "KRUGK43FEBYHEZLUPJSWY4ZAMFZGKIDNMFVWS3THEBWWKIDUNBUXE43UPEXA====");

    byte[] decoded = twoFactor.decode("KRUGK43FEBYHEZLUPJSWY4ZAMFZGKIDNMFVWS3THEBWWKIDUNBUXE43UPEXA====");
    assertEquals(new String(decoded, "UTF-8"), "These pretzels are making me thirsty.");
  }

  @Test
  public void validate() throws Exception {
    TwoFactor twoFactor = TwoFactor.getInstance();
    String encodedSecret = "KRUGK43FEBYHEZLUPJSWY4ZAMFZGKIDNMFVWS3THEBWWKIDUNBUXE43UPEXA====";
    long instant = 47893469;
    assertTrue(twoFactor.validateVerificationCode(encodedSecret, instant, "991696"));
  }

  @Test(enabled = false)
  public void test_code_generations() throws Exception {
    AtomicReference<String> last = new AtomicReference<>();
    TwoFactor twoFactor = TwoFactor.getInstance();
    IntStream.range(0, 10).forEach((n) -> {
      try {
        long instant = TwoFactor.getCurrentWindowInstant();
        String oneTimePassword = twoFactor.calculateVerificationCode("JBSWY3DPEHPK3PXP", instant);
        if (!oneTimePassword.equals(last.get())) {
          System.out.print(oneTimePassword + "\n");
        }
        last.set(oneTimePassword);
        Thread.sleep(1000);
      } catch (Exception e) {
      }
    });
  }
}
