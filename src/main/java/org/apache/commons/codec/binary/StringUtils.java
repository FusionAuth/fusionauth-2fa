package org.apache.commons.codec.binary;

import java.io.UnsupportedEncodingException;

/**
 * Converts String to and from bytes using the encodings required by the Java specification. These encodings are
 * specified in <a href="http://download.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">
 * Standard charsets</a>.
 *
 * <p>This class is immutable and thread-safe.</p>
 *
 * @see <code>CharEncoding</code>
 * @see <a href="http://download.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
 * @version $Id: StringUtils.java 1378740 2012-08-29 21:18:47Z tn $
 * @since 1.4
 */
public class StringUtils {

  public static String newStringUtf8(byte[] bytes) {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] getBytesUtf8(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

}
