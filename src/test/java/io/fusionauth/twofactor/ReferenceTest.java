/*
 * Copyright (c) 2018, FusionAuth, All Rights Reserved
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

/**
 * @author Daniel DeGroff
 */
public class ReferenceTest {
  @Test
  public void test() {
    String asciiSeed = "12345678901234567890";
    assertEquals(HexUtils.fromBytes(asciiSeed.getBytes()), "3132333435363738393031323334353637383930");

    // Seed for HMAC-SHA1 - 20 bytes
    String seed = "3132333435363738393031323334353637383930";
    // Seed for HMAC-SHA256 - 32 bytes
    String seed32 = "3132333435363738393031323334353637383930" + "313233343536373839303132";
    // Seed for HMAC-SHA512 - 64 bytes
    String seed64 = "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930" + "31323334";
    long T0 = 0;
    long X = 30;
    long[] testTime = {59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L};

    String steps;
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));

    try {
      System.out.println("+---------------+-----------------------+------------------+--------+--------+");
      System.out.println("|  Time(sec)    |   Time (UTC format)   | Value of T(Hex)  |  TOTP  | Mode   |");
      System.out.println("+---------------+-----------------------+------------------+--------+--------+");

      for (int i = 0; i < testTime.length; i++) {
        long T = (testTime[i] - T0) / X;
        steps = Long.toHexString(T).toUpperCase();
        while (steps.length() < 16) {
          steps = "0" + steps;
        }

        // 1111111109L --> 00000000023523EC -->
        String fmtTime = String.format("%1$-11s", testTime[i]);
        String utcTime = df.format(new Date(testTime[i] * 1000));
        System.out.println("|  " + fmtTime + "  |  " + utcTime + "  | " + steps + " |" + Reference.generateTOTP(seed, steps, "8", "HmacSHA1") + "| SHA1   |");
        System.out.println("|  " + fmtTime + "  |  " + utcTime + "  | " + steps + " |" + Reference.generateTOTP(seed32, steps, "8", "HmacSHA256") + "| SHA256 |");
        System.out.println("|  " + fmtTime + "  |  " + utcTime + "  | " + steps + " |" + Reference.generateTOTP(seed64, steps, "8", "HmacSHA512") + "| SHA512 |");
        System.out.println("+---------------+-----------------------+------------------+--------+--------+");
      }
    } catch (final Exception e) {
      System.out.println("Error : " + e);
    }
  }
}
