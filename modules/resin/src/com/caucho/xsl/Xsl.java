/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xsl;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.ExceptionWrapper;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.Html;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlParser;
import com.caucho.loader.*;

import org.w3c.dom.Document;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Public facade for creating stylesheets.  The Xsl factory
 * creates standard XSL stylesheets.  A Stylesheet object represents
 * a compiled stylesheet.  You'll need to create a Transformer to
 * actually perform any transformations.
 *
 * <code><pre>
 * import java.io.*;
 * import javax.xml.transform.*;
 * import javax.xml.transform.stream.*;
 * import org.xml.sax.*;
 *
 * import com.caucho.xsl.*;
 *
 * ...
 *
 * TransformerFactory factory = new Xsl();
 * StreamSource xslSource = new StreamSource("mystyle.xsl");
 * Transformer transformer = factory.newTransformer(xslSource);
 *
 * StreamSource xmlSource = new StreamSource("test.xml");
 * StreamResult htmlResult = new StreamResult("test.html");
 *
 * transformer.transform(xmlSource, htmlResult);
 * </pre></code>
 */
public class Xsl extends AbstractStylesheetFactory {
  private static final Logger log
    = Logger.getLogger(Xsl.class.getName());

  public Xsl() {}

  /**
   * Parses the XSL into a DOM document.
   *
   * @param rs the input stream.
   */
  protected Document parseXSL(ReadStream rs)
    throws TransformerConfigurationException
  {
    try {
      Xml parser = new Xml();

      return parser.parseDocument(rs);
    } catch (Exception e) {
      throw new XslParseException(e);
    }
  }

  public static void main(String []args)
  {
    String xslName = "default.xsl";
    String dest = null;
    String suffix = null;
    String toc = "";
    int i = 0;
    String conf = CauchoSystem.getResinConfig();
    boolean isStrict = true;
    ArrayList<String> argList = new ArrayList<String>();
    ArrayList<String> keyList = new ArrayList<String>();
    ArrayList<String> valueList = new ArrayList<String>();

    while (i < args.length) {
      if (args[i].equals("-xsl")) {
        xslName = args[i + 1];
        i += 2;
      }
      else if (args[i].equals("-lite") ||
               args[i].equals("-stylescript")) {
        isStrict = false;
        i += 1;
      }
      else if (args[i].equals("-o")) {
        dest = args[i + 1];
        i += 2;
      }
      else if (args[i].equals("-suffix")) {
        suffix = args[i + 1];
        i += 2;
      }
      else if (args[i].startsWith("-A")) {
        argList.add(args[i].substring(2));
        i += 1;
      }
      else if (args[i].startsWith("-P")) {
        String v = args[i].substring(2);
        int p = v.indexOf('=');
        String key;
        String value;

        if (p >= 0) {
          key = v.substring(0, p);
          value = v.substring(p + 1);
        }
        else {
          key = v;
          value = "";
        }

        keyList.add(key);
        valueList.add(value);

        i += 1;
      }
      else if (args[i].equals("-conf")) {
        conf = args[i + 1];
        i += 2;
      }
      else if (args[i].equals("-h") || args[i].equals("-help")) {
        usage();
        return;
      } else
        break;
    }

    /*
    Path cfg = CauchoSystem.getResinHome().lookup(conf);
    if (cfg.exists()) {
      try {
        Registry.setRegistry(Registry.parse(cfg));
      } catch (IOException e) {
      } catch (SAXException e) {
      }
    }
    */

    Path destDir = null;
    if (dest != null)
      destDir = Vfs.lookup(dest);
    else if (suffix == null)
      destDir = Vfs.lookup("stdout:");

    if (args.length - i  > 1 && (dest == null || destDir.isFile()) &&
        suffix == null) {
      System.err.println("multiple sources require a destination directory");
      System.exit(1);
    }

    try {
      MergePath stylePath = new MergePath();
      stylePath.addMergePath(Vfs.lookup(xslName).getParent());
      stylePath.addMergePath(Vfs.lookup());
      stylePath.addMergePath(CauchoSystem.getResinHome().lookup("xsl"));

      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      if (loader instanceof DynamicClassLoader) {
        DynamicClassLoader dynLoader
          = (DynamicClassLoader) loader;
        String resourcePath = dynLoader.getResourcePathSpecificFirst();
        stylePath.addClassPath(resourcePath);
      }

      // stylePath.addClassPath(

      /*
        Path []stylePath = new Path[] {
        Pwd.lookup(xslName).getParent(),
        Pwd.lookup(),
        CauchoSystem.getResinHome().lookup("xsl")};
      */
      Path []scriptPath = new Path[] {
        Vfs.lookup(),
        Vfs.lookup(xslName).getParent(),
        CauchoSystem.getResinHome().lookup("scripts")
      };

      Path xslPath = stylePath.lookup(xslName);
      if (xslPath == null) {
        System.out.println("can't find `" + xslName + "'");
        System.exit(1);
      }

      AbstractStylesheetFactory xsl;

      if (isStrict)
        xsl = new Xsl();
      else
        xsl = new StyleScript();

      xsl.setStylePath(stylePath);

      Templates stylesheet;

      stylesheet = xsl.newTemplates(xslName);

      for (; i < args.length; i++) {
        String name = args[i];

        Path xmlPath = Vfs.lookup(name);

        HashMap<String,Object> argMap = new HashMap<String,Object>();

        String []childArgs = new String[argList.size() + 1];
        argList.toArray(childArgs);
        childArgs[childArgs.length - 1] = name;

        argMap.put("arguments", childArgs);
        argMap.put("File", Vfs.lookup());

        ReadStream is = xmlPath.openRead();
        Document doc = null;
        try {
          if (isStrict)
            doc = new Xml().parseDocument(is);
          else {
            XmlParser parser = new Html();
            parser.setEntitiesAsText(true);
            doc = parser.parseDocument(is);
          }
        } finally {
          is.close();
        }

        //Document result = xsl.transform(doc, argMap);
        Document result = null;

        Path destPath = null;
        if (dest != null)
          destPath = Vfs.lookup(dest);
        else if (suffix != null)
          destPath = xmlPath.getParent();
        else
          destPath = Vfs.lookup("stdout:");

        if (suffix != null) {
          int p = name.lastIndexOf('.');
          if (p == -1) {
            System.err.println("suffix missing for `" + name + "'");
            System.exit(1);
          }

          String destName = name.substring(0, p);
          if (dest == null) {
            p = destName.lastIndexOf('/');
            if (p >= 0)
              destName = destName.substring(p + 1);
          }

          if (! destPath.isFile())
            destPath = destPath.lookup(destName + '.' + suffix);
          else {
            System.err.println("illegal output combination");
            System.exit(1);
          }
        }
        else if (destPath.isDirectory())
          destPath = destPath.lookup(name);

        try {
          destPath.getParent().mkdirs();
        } catch (IOException e) {
        }

        WriteStream os = destPath.openWrite();

        try {
          Properties output = stylesheet.getOutputProperties();

          String encoding = (String) output.get("encoding");
          String mimeType = (String) output.get("mime-type");
          String method = (String) output.get("method");

          if (encoding == null && (method == null || ! method.equals("html")))
            encoding = "UTF-8";

          TransformerImpl transformer =
            (TransformerImpl) stylesheet.newTransformer();

          if (encoding != null)
            os.setEncoding(encoding);

          transformer.setProperty("caucho.pwd", Vfs.lookup());

          for (int j = 0; j < keyList.size(); j++) {
            String key = (String) keyList.get(j);
            String value = (String) valueList.get(j);

            transformer.setParameter(key, value);
          }

          transformer.transform(doc, os);
        } finally {
          os.close();
        }
      }
    } catch (Throwable e) {
      while ((e instanceof ExceptionWrapper) &&
             ((ExceptionWrapper) e).getRootCause() != null)
        e = ((ExceptionWrapper) e).getRootCause();

      e.printStackTrace();
    } finally {
      System.exit(0);
    }
  }

  private static void usage()
  {
    System.err.println("xsl [-xsl stylesheet] file1 file2 ...");
    System.err.println(" -xsl stylesheet : select a stylesheet");
    System.err.println(" -o filename     : output filename/directory");
    System.err.println(" -suffix suffix  : replacement suffix");
    System.err.println(" -stylescript    : StyleScript");
    System.err.println(" -Pkey=value     : template parameter");
    System.err.println(" -h              : this help message");
  }
}
