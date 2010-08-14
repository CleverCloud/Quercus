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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xml2;

import com.caucho.util.IntMap;

import java.io.IOException;

class HtmlEntities extends Entities {
  private static HtmlEntities _html40;
  private static HtmlEntities _html32;

  protected char [][]_latin1;
  protected char [][]_attrLatin1;

  protected char [][]_sparseEntity = new char[8192][];
  protected char[]_sparseChar = new char[8192];

  protected IntMap _entityToChar;

  static Entities create(double version)
  {
    if (version == 0 || version >= 4.0) {
      if (_html40 == null)
        _html40 = new HtmlEntities(4.0);
      return _html40;
    }
    else {
      if (_html32 == null)
        _html32 = new HtmlEntities(3.2);
      return _html32;
    }
  }

  protected HtmlEntities(double version)
  {
    _entityToChar = new IntMap();
    initLatin1();
    if (version >= 4.0) {
      initSymbol();
      initSpecial();
    }

    _latin1 = new char[256][];
    for (int i = 0; i < 32; i++) {
      _latin1[i] = ("&#" + i + ";").toCharArray();
    }
    _latin1['\t'] = "\t".toCharArray();
    _latin1['\n'] = "\n".toCharArray();
    _latin1['\r'] = "\r".toCharArray();

    for (int i = 32; i < 127; i++)
      _latin1[i] = ("" + (char) i).toCharArray();

    _latin1['<'] = "&lt;".toCharArray();
    _latin1['>'] = "&gt;".toCharArray();
    _latin1['&'] = "&amp;".toCharArray();

    for (int i = 127; i < 256; i++) {
      char []value = getSparseEntity(i);

      if (value != null)
        _latin1[i] = value;
      else
        _latin1[i] = ("&#" + i + ";").toCharArray();
    }

    _attrLatin1 = new char[256][];
    for (int i = 0; i < _latin1.length; i++)
      _attrLatin1[i] = _latin1[i];

    // unquoted matches Xalan/Xerces
    _attrLatin1['<'] = "<".toCharArray();
    _attrLatin1['>'] = ">".toCharArray();
    _attrLatin1['"'] = "&quot;".toCharArray();
    _attrLatin1['\n'] = "&#10;".toCharArray();
    _attrLatin1['\r'] = "&#13;".toCharArray();
  }

  int getEntity(String entity)
  {
    return _entityToChar.get(entity);
  }

  /**
   * Prints escaped text.
   */
  void printText(XmlPrinter os,
                 char []text, int offset, int length,
                 boolean attr)
    throws IOException
  {
    for (int i = 0; i < length; i++) {
      char ch = text[offset + i];

      if (ch == '&') {
        if (i + 1 < length && text[offset + i + 1] == '{')
          os.print('&');
        else if (attr)
          os.print(_attrLatin1[ch]);
        else
          os.print(_latin1[ch]);
      }
      else if (ch < 256) {
        if (attr)
          os.print(_attrLatin1[ch]);
        else
          os.print(_latin1[ch]);
      }
      else {
        char []value = getSparseEntity(ch);
        if (value != null) {
          os.print(value);
        } else {
          os.print("&#");
          os.print((int) ch);
          os.print(";");
        }
      }
    }
  }
  
  private void initLatin1()
  {
    entity("nbsp", 160);
    entity("iexcl", 161);
    entity("cent", 162);
    entity("pound", 163);
    entity("curren", 164);
    entity("yen", 165);
    entity("brvbar", 166);
    entity("sect", 167);
    entity("uml", 168);
    entity("copy", 169);
    entity("ordf", 170);
    entity("laquo", 171);
    entity("not", 172);
    entity("shy", 173);
    entity("reg", 174);
    entity("macr", 175);
    entity("deg", 176);
    entity("plusmn", 177);
    entity("sup2", 178);
    entity("sup3", 179);
    entity("acute", 180);
    entity("micro", 181);
    entity("para", 182);
    entity("middot", 183);
    entity("cedil", 184);
    entity("sup1", 185);
    entity("ordm", 186);
    entity("raquo", 187);
    entity("frac14", 188);
    entity("frac12", 189);
    entity("frac34", 190);
    entity("iquest", 191);
    entity("Agrave", 192);
    entity("Aacute", 193);
    entity("Acirc", 194);
    entity("Atilde", 195);
    entity("Auml", 196);
    entity("Aring", 197);
    entity("AElig", 198);
    entity("Ccedil", 199);
    entity("Egrave", 200);
    entity("Eacute", 201);
    entity("Ecirc", 202);
    entity("Euml", 203);
    entity("Igrave", 204);
    entity("Iacute", 205);
    entity("Icirc", 206);
    entity("Iuml", 207);
    entity("ETH", 208);
    entity("Ntilde", 209);
    entity("Ograve", 210);
    entity("Oacute", 211);
    entity("Ocirc", 212);
    entity("Otilde", 213);
    entity("Ouml", 214);
    entity("times", 215);
    entity("Oslash", 216);
    entity("Ugrave", 217);
    entity("Uacute", 218);
    entity("Ucirc", 219);
    entity("Uuml", 220);
    entity("Yacute", 221);
    entity("THORN", 222);
    entity("szlig", 223);
    entity("agrave", 224);
    entity("aacute", 225);
    entity("acirc", 226);
    entity("atilde", 227);
    entity("auml", 228);
    entity("aring", 229);
    entity("aelig", 230);
    entity("ccedil", 231);
    entity("egrave", 232);
    entity("eacute", 233);
    entity("ecirc", 234);
    entity("euml", 235);
    entity("igrave", 236);
    entity("iacute", 237);
    entity("icirc", 238);
    entity("iuml", 239);
    entity("eth", 240);
    entity("ntilde", 241);
    entity("ograve", 242);
    entity("oacute", 243);
    entity("ocirc", 244);
    entity("otilde", 245);
    entity("ouml", 246);
    entity("divide", 247);
    entity("oslash", 248);
    entity("ugrave", 249);
    entity("uacute", 250);
    entity("ucirc", 251);
    entity("uuml", 252);
    entity("yacute", 253);
    entity("thorn", 254);
    entity("yuml", 255);
  }

  private void initSymbol()
  {
    entity("fnof", 402);
    entity("Alpha", 913);
    entity("Beta", 914);
    entity("Gamma", 915);
    entity("Delta", 916);
    entity("Epsilon", 917);
    entity("Zeta", 918);
    entity("Eta", 919);
    entity("Theta", 920);
    entity("Iota", 921);
    entity("Kappa", 922);
    entity("Lambda", 923);
    entity("Mu", 924);
    entity("Nu", 925);
    entity("Xi", 926);
    entity("Omicron", 927);
    entity("Pi", 928);
    entity("Rho", 929);
    entity("Sigma", 931);
    entity("Tau", 932);
    entity("Upsilon", 933);
    entity("Phi", 934);
    entity("Chi", 935);
    entity("Psi", 936);
    entity("Omega", 937);
    entity("alpha", 945);
    entity("beta", 946);
    entity("gamma", 947);
    entity("delta", 948);
    entity("epsilon", 949);
    entity("zeta", 950);
    entity("eta", 951);
    entity("theta", 952);
    entity("iota", 953);
    entity("kappa", 954);
    entity("lambda", 955);
    entity("mu", 956);
    entity("nu", 957);
    entity("xi", 958);
    entity("omicron", 959);
    entity("pi", 960);
    entity("rho", 961);
    entity("sigmaf", 962);
    entity("sigma", 963);
    entity("tau", 964);
    entity("upsilon", 965);
    entity("phi", 966);
    entity("chi", 967);
    entity("psi", 968);
    entity("omega", 969);
    entity("thetasym", 977);
    entity("upsih", 978);
    entity("piv", 982);

    entity("bull", 8226);
    entity("hellip", 8230);
    entity("prime", 8242);
    entity("Prime", 8243);
    entity("oline", 8254);
    entity("frasl", 8260);
    entity("weirp", 8472);
    entity("image", 8465);
    entity("real", 8476);
    entity("trade", 8482);
    entity("alefsym", 8501);

    entity("larr", 8592);
    entity("uarr", 8593);
    entity("rarr", 8594);
    entity("darr", 8595);
    entity("harr", 8596);
    entity("crarr", 8629);
    entity("lArr", 8656);
    entity("uArr", 8657);
    entity("rArr", 8658);
    entity("dArr", 8659);
    entity("hArr", 8660);

    entity("forall", 8704);
    entity("part", 8706);
    entity("exist", 8707);
    entity("empty", 8709);
    entity("nabla", 8711);
    entity("isin", 8712);
    entity("ni", 8715);
    entity("prod", 8719);
    entity("sum", 8721);
    entity("minus", 8722);
    entity("lowas", 8727);
    entity("radic", 8730);
    entity("prop", 8733);
    entity("infin", 8734);
    entity("ang", 8736);
    entity("and", 8743);
    entity("or", 8744);
    entity("cap", 8745);
    entity("cup", 8746);
    entity("int", 8747);
    entity("there4", 8756);
    entity("sim", 8764);
    entity("cong", 8773);
    entity("asymp", 8776);
    entity("ne", 8800);
    entity("equiv", 8801);
    entity("le", 8804);
    entity("ge", 8805);
    entity("sub", 8834);
    entity("sup", 8835);
    entity("nsub", 8836);
    entity("sube", 8838);
    entity("supe", 8839);
    entity("oplus", 8853);
    entity("otimes", 8855);
    entity("perp", 8869);
    entity("sdot", 8901);
    entity("lceil", 8968);
    entity("rceil", 8969);
    entity("lfloor", 8970);
    entity("rfloor", 8971);
    entity("lang", 9001);
    entity("rang", 9002);

    entity("loz", 9674);
    entity("spades", 9824);
    entity("clubs", 9827);
    entity("hearts", 9829);
    entity("diams", 9830);
  }

  private void initSpecial()
  {
    entity("quot", 34);
    entity("amp", 38);
    entity("lt", 60);
    entity("gt", 62);
    entity("apos", '\'');
    entity("OElig", 338);
    entity("oelig", 339);
    entity("Scaron", 352);
    entity("scaron", 353);
    entity("Yuml", 376);
    entity("circ", 710);
    entity("tilde", 732);
    entity("ensp", 8194);
    entity("emsp", 8195);
    entity("thinsp", 8201);
    entity("zwnj", 8204);
    entity("zwj", 8205);
    entity("lrm", 8206);
    entity("rlm", 8207);
    entity("ndash", 8211);
    entity("mdash", 8212);
    entity("lsquo", 8216);
    entity("rsquo", 8217);
    entity("sbquo", 8218);
    entity("ldquo", 8220);
    entity("rdquo", 8221);
    entity("bdquo", 8222);
    entity("dagger", 8224);
    entity("Dagger", 8225);
    entity("permil", 8240);
    entity("lsaquo", 8249);
    entity("rsaquo", 8250);
    entity("euro", 8364);
  }

  /**
   * Returns the character entity for the given character.  The
   * map is sparse.
   */
  protected char []getSparseEntity(int ch)
  {
    int size = _sparseChar.length;
    
    int i = (ch * 65521) % size;
    if (i < 0)
      i = -i;
    for (;
         _sparseChar[i] != ch && _sparseEntity[i] != null;
         i = (i + 1) % size) {
    }

    return _sparseEntity[i];
  }

  private void entity(String name, int ch)
  {
    _entityToChar.put(name, ch);

    int size = _sparseChar.length;
    
    int i = (ch * 65521) % size;
    if (i < 0)
      i = -i;
    for (;
         _sparseChar[i] != ch && _sparseEntity[i] != null;
         i = (i + 1) % size) {
    }

    _sparseChar[i] = (char) ch;
    _sparseEntity[i] = ("&" + name + ";").toCharArray();
  }
}
