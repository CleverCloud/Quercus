package com.caucho.netbeans;

import junit.framework.TestCase;

public class ResinConfigurationTest extends TestCase
{
  public void testParseURI()
  {
    uriTest("resin:home=/bar", "resin:home=/bar");

    uriTest("resin:home=/resin/home:conf=/conf/resin.conf:server-id=:server-i:server-port=9999:server-address=127.0.0.2",
            "resin:home=/resin/home:conf=/conf/resin.conf:server-id=:server-i:server-port=9999:server-address=127.0.0.2");

    uriTest("resin:home=/resin/home:conf=/conf/resin.conf",
            "resin:home=/resin/home:conf=/conf/resin.conf");

    uriTest("resin:home=/resin/home:conf=conf/resin.conf",
            "resin:home=/resin/home:conf=/resin/home/conf/resin.conf");

    boolean isException = false;
    try {
      uriTest("resin:home=:conf=conf/resin.conf",
              "resin:home=:conf=conf/resin.conf");
    }
    catch (IllegalArgumentException ex) {
      isException = true;
      assertEquals("problem parsing URI 'resin:home=:conf=conf/resin.conf': java.lang.IllegalArgumentException: no resin-home set for relative conf conf/resin.conf",
                   ex.getMessage());

    }

    assertTrue(isException);
  }

  private static void uriTest(String uri, String compare)
  {
    ResinConfiguration configuration = new ResinConfiguration();
    configuration.parseURI(uri);

    assertEquals(configuration.getURI(), compare);
  }
}
