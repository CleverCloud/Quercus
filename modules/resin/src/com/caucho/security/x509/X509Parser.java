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

package com.caucho.security.x509;

import com.caucho.util.*;
import com.caucho.vfs.*;
import java.io.*;
import java.util.*;

/**
 * pkcs#10 is in PEM format, DER with base64  and --- BEGIN ---
 */
public class X509Parser {
  private static final L10N L = new L10N(X509Parser.class);
  
  /**
   * Parses the certificate in pkcs#10
   */
  public String parseCertificate(Path path)
    throws IOException
  {
    ReadStream is = path.openRead();

    try {
      String line;

      while ((line = is.readLine()) != null) {

        if (line.startsWith("-----BEGIN CERTIFICATE---"))
          return parseCertificateContent(is);
      }

      throw new IOException(L.l("Can't find certificate in '{0}'",
                                path));
    } finally {
      is.close();
    }
  }

  private String parseCertificateContent(ReadStream is)
    throws IOException
  {
    TempOutputStream os = new TempOutputStream();

    String line;
    LineReader reader = new LineReader();

    while ((line = is.readLine()) != null) {
      if (line.startsWith("-----END CERTIFICATE-----")) {
        System.out.println("TOTAL-LEN: " + os.getLength());

        return parseCertificateDer(os.openRead());
      }

      reader.init(line);
      Base64.decode(reader, os);
    }


    throw new IOException(L.l("Can't find end certificate"));
  }

  /**
   * Certificate ::= SEQUENCE {
   *   tbsCertificate
   *   signatureAlgorithm
   *   signature
   * }
   */
  private String parseCertificateDer(ReadStream is)
    throws IOException
  {
    int len = parseSequenceHeader(is);

    parseTbsCertificate(is);
    parseSignatureAlgorithm(is);
    parseSignature(is);
    
    return "ok";
  }

  /**
   * tbsCertificate ::= SEQUENCE {
   * version          [ 0 ]  Version DEFAULT v1(0),
   * serialNumber            CertificateSerialNumber,
   * signature               AlgorithmIdentifier,
   * issuer                  Name,
   * validity                Validity,
   * subject                 Name,
   * subjectPublicKeyInfo    SubjectPublicKeyInfo,
   * issuerUniqueID    [ 1 ] IMPLICIT UniqueIdentifier OPTIONAL,
   * subjectUniqueID   [ 2 ] IMPLICIT UniqueIdentifier OPTIONAL,
   * extensions        [ 3 ] Extensions OPTIONAL
   * }
   */
  private void parseTbsCertificate(ReadStream is)
    throws IOException
  {
    int len = parseSequenceHeader(is);

    int field = parseFieldHeader(is);
    int version = 0;
    if (field >= 0) {
      version = (int) parseInteger(is);
    }

    System.out.println("Version: " + version);
    // actually an integer
    byte []serial = parseBlob(is, 0x02);

    Oid algorithmOid = parseAlgorithmIdentifier(is);
    System.out.println("ALG: " + algorithmOid);

    System.out.println("ISSUER:");
    String issuer = parseName(is);

    parseValidity(is);

    System.out.println("SUBJECT:");
    String subject = parseName(is);

    parseSubjectPublicKeyInfo(is);
  }

  /**
   * signatureAlgorithm ::=  {
   * }
   */
  private void parseSignatureAlgorithm(ReadStream is)
    throws IOException
  {
  }

  /**
   * signature ::=  {
   * }
   */
  private void parseSignature(ReadStream is)
    throws IOException
  {
  }

  private Oid parseAlgorithmIdentifier(ReadStream is)
    throws IOException
  {
    int len = parseSequenceHeader(is);

    Oid oid = parseOid(is);

    while (parseOid(is) != null) {
    }

    return oid;
  }

  /**
   *
   */
  private void parseSubjectPublicKeyInfo(ReadStream is)
    throws IOException
  {
    int len = parseSequenceHeader(is);
    long end = is.getPosition() + len;

    while (is.getPosition() < end) {
      Object value = parseAny(is);

      System.out.println("PK: " + value);
    }
  }

  /**
   * Name :: SEQUENCE OF RelativeDistinguishedName
   *
   * RelativeDistinguidhedName ::= SET OF AttributeValueAssertion
   *
   * AttributeValueAssertion ::= SEQUENCE {
   *   attributeType OID,
   *   attributeValue ANY
   * }
   */
  private String parseName(ReadStream is)
    throws IOException
  {
    int length = parseSequenceHeader(is);
    System.out.println("LEN: " + length);

    while (length > 0) {
      int code = is.read();
      if (code != 0x31)
        throw new IOException(L.l("expected 0x31 at {0}",
                                  Integer.toHexString(code)));

      int len = parseLength(is);
      length -= 2 + len;

      int sublen = parseSequenceHeader(is);
      Oid oid = parseOid(is);
      Object value = parseAny(is);

      System.out.println("NAME: " + oid + " " + value);
    }

    return "name";
  }

  /**
   * Validity ::= SEQUENCE {
   *   notBefore               UTCTIME,
   *   notAfter                UTCTIME
   *  }
   */
  private void parseValidity(ReadStream is)
    throws IOException
  {
    int len = parseSequenceHeader(is);

    long notBefore = parseUtcTime(is);
    long notAfter = parseUtcTime(is);
  }

  private Object parseAny(ReadStream is)
    throws IOException
  {
    int code = is.read();

    switch (code) {
    case 0x02: // integer
      {
        is.unread();
        return parseInteger(is);
      }
      
    case 0x03: // bit string
      {
        is.unread();
        return parseBitString(is);
      }
      
    case 0x05: // null
      {
        int len = parseLength(is);
        is.skip(len);
        return null;
      }
      
    case 0x06: // oid
      {
        is.unread();

        return parseOid(is);
      }

    case 0x13: // printablestring
      {
        is.unread();
        return parsePrintableString(is);
      }

    case 0x16: // ia5string
      {
        is.unread();
        return parseIa5String(is);
      }

    case 0x30: // sequence
      {
        is.unread();
        return parseSequence(is);
      }

    default:
      {
        System.out.println(String.format("UNKNOWN: %x", code));

        int len = parseLength(is);
        is.skip(len);
        return null;
      }
    }
  }

  private ArrayList parseSequence(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x30)
      throw new IOException(L.l("expected 0x17 at '0x{0}'",
                                Integer.toHexString(ch)));

    int len = parseLength(is);
    long end = is.getPosition() + len;

    ArrayList value = new ArrayList();
    
    while (is.getPosition() < end) {
      value.add(parseAny(is));
    }

    return value;
  }

  private long parseUtcTime(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x17)
      throw new IOException(L.l("expected 0x17 at '{0}'", ch));

    int len = parseLength(is);

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      sb.append((char) is.read());
    }

    return 0;
  }

  private BitString parseBitString(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x03)
      throw new IOException(L.l("expected 0x03 at '{0}'", ch));

    int len = parseLength(is);

    int unused = is.read();
    
    byte []data = new byte[len - 1];

    is.readAll(data, 0, len - 1);

    return new BitString(data, unused);
  }

  private String parsePrintableString(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x13)
      throw new IOException(L.l("expected 0x13 at '{0}'", ch));

    int len = parseLength(is);

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      sb.append((char) is.read());
    }

    return sb.toString();
  }

  private String parseIa5String(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x16)
      throw new IOException(L.l("expected 0x13 at '{0}'", ch));

    int len = parseLength(is);

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      sb.append((char) is.read());
    }

    return sb.toString();
  }
  
  private Oid parseOid(ReadStream is)
    throws IOException
  {
    int ch = is.read();
    is.unread();

    if (ch == 0x05) {
      parseNull(is);
      return null;
    }
    else
      return new Oid(parseBlob(is, 0x06));
  }

  private int parseSequenceHeader(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x30)
      throw new IOException(L.l("expected sequence 0x30 at {0}",
                                Integer.toHexString(ch)));

    return parseLength(is);
  }

  private int parseFieldHeader(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if ((ch & 0xf0) != 0xa0) {
      is.unread();
      return -1;
    }

    int len = parseLength(is);

    return ch & 0xf;
  }

  private long parseInteger(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x02)
      throw new IOException(L.l("expected sequence 0x02 at {0}",
                                Integer.toHexString(ch)));

    int len = parseLength(is);

    long v = 0;
    for (int i = 0; i < len; i++) {
      v = 256 * v + is.read();
    }
    
    return v;
  }

  private void parseNull(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if (ch != 0x05)
      throw new IOException(L.l("expected sequence 0x05 at {0}",
                                Integer.toHexString(ch)));

    int len = parseLength(is);

    is.skip(len);
  }

  private byte []parseBlob(ReadStream is, int code)
    throws IOException
  {
    int ch = is.read();

    if (code != 0 && ch != code)
      throw new IOException(L.l("expected value {0} at {1}",
                                Integer.toHexString(code),
                                Integer.toHexString(ch)));

    int len = parseLength(is);

    byte []data = new byte[len];

    is.readAll(data, 0, data.length);
    
    return data;
  }

  private int parseLength(ReadStream is)
    throws IOException
  {
    int ch = is.read();

    if ((ch & 0x80) == 0)
      return ch & 0x7f;

    int count = ch & 0x7f;

    int len = 0;
    for (int i = 0; i < count; i++) {
      len = 256 * len + is.read();
    }

    return len;
  }

  static class LineReader extends Reader {
    String _string;
    int _offset;
    int _length;

    void init(String string)
    {
      _string = string;
      _offset = 0;
      _length = string.length();
    }

    public int read()
    {
      if (_offset < _length)
        return _string.charAt(_offset++);

      return -1;
    }

    public int read(char []buffer, int offset, int length)
    {
      if (_offset < _length) {
        buffer[offset] = _string.charAt(_offset++);
        return 1;
      }

      return -1;
    }

    public void close()
    {
    }
  }
}
