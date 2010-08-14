/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.
 *
 * @author Sam
 */

package com.caucho.j2ee;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

/**
 * The J2EE version of a configuration file.
 */
public enum J2EEVersion {
  J2EE12 {
    public boolean hasFeaturesOf(J2EEVersion version)
    {
      return version == J2EE12;
    }
  },

  J2EE13 {
    public boolean hasFeaturesOf(J2EEVersion version)
    {
      return version == J2EE12
             || version == J2EE13;
    }
  },

  J2EE14 {
    public boolean hasFeaturesOf(J2EEVersion version)
    {
      return version == J2EE12
             || version == J2EE13
             || version == J2EE14;
    }
  },

  JAVAEE5 {
    public boolean hasFeaturesOf(J2EEVersion version)
    {
      return version == J2EE12
             || version == J2EE13
             || version == J2EE14
             || version == JAVAEE5;
    }
  },

  RESIN {
    public boolean hasFeaturesOf(J2EEVersion version)
    {
      return true;
    }
  };

  private static final L10N L = new L10N(J2EEVersion.class);

  public static final String J2EE_NAMESPACE = "http://java.sun.com/xml/ns/j2ee";
  public static final String JAVAEE_NAMESPACE = "http://java.sun.com/xml/ns/javaee";
  public static final String RESIN_NAMESPACE = "http://caucho.com/ns/resin";

  /**
   * Return a J2EEVersion based on the namespace, the version attribute,
   * and the doctype.
   *
   * @param top the top level element of a configuration file.
   */
  public static J2EEVersion getJ2EEVersion(Element top)
  {
    String version = top.getAttribute("version");
    String ns = top.getNamespaceURI();

    if (version.length() > 0) {
      if (ns == null)
        throw new ConfigException(L.l("namespace is required because version is specified, either xmlns='{0}', xmlns='{1}', or xmlns='{2}'",
                                      RESIN_NAMESPACE,
                                      JAVAEE_NAMESPACE,
                                      J2EE_NAMESPACE));

      if (ns.equals(J2EE_NAMESPACE)) {
        if (version.equals("1.4"))
          return J2EE14;
        else
          throw new ConfigException(L.l("version must be '{0}' for namespace '{1}'", "1.4", ns));
      }
      else if (ns.equals(JAVAEE_NAMESPACE)) {
        if (version.equals("5") || 
            version.equals("5.0") ||
            version.equals("2.5")) // XXX For TCK emitted web.xmls
          return JAVAEE5;
        else
          throw new ConfigException(L.l("version must be '{0}' for namespace '{1}'", "5", ns));
      }
      else if (ns.equals(RESIN_NAMESPACE)) {
        return RESIN;
      }
    }
    else if (ns != null) {
      if (ns.equals(RESIN_NAMESPACE))
        return RESIN;

      throw new ConfigException(L.l("unknown namespace '{0}', namespace must be one of xmlns='{1}', xmlns='{2}', or xmlns='{3}'",
                                    ns,
                                    RESIN_NAMESPACE,
                                    JAVAEE_NAMESPACE,
                                    J2EE_NAMESPACE));
    }

    DocumentType doctype = top.getOwnerDocument().getDoctype();

    if (doctype != null) {
      String publicId = doctype.getPublicId();

      if (publicId != null) {
        if (publicId.contains("1.3"))
          return J2EE13;
      }
    }

    return J2EE12;
  }

  /**
   * Returns true if this version is equal to or more recent than the
   * passed version.
   */
  abstract public boolean hasFeaturesOf(J2EEVersion version);
}
