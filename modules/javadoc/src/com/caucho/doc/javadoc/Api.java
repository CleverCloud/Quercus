/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits redistribution, modification and use
 * of this file in source and binary form ("the Software") under the
 * Caucho Developer Source License ("the License").  The following
 * conditions must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Redistributions of the Software in source or binary form must include
 *    an unmodified copy of the License, normally in a plain ASCII text
 *
 * 3. The names "Resin" or "Caucho" are trademarks of Caucho Technology and
 *    may not be used to endorse products derived from this software.
 *    "Resin" or "Caucho" may not appear in the names of products derived
 *    from this software.
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

package com.caucho.doc.javadoc;

import com.caucho.config.ConfigException;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.Crc64;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.io.FileNotFoundException;

/**
 * An Api is a javadoc generated api.
 */
public class Api {
  static protected final Logger log = Log.open(Api.class);
  static final L10N L = new L10N(Api.class);

  private String _id;
  private String _name;
  private String _description;
  private String _location;
  private String _indexString;
  private ArrayList<Path> _index = new ArrayList<Path>();

  private Path _locationPath;
  private boolean _isLocal;
  private boolean _isLocalAbsolute;

  /**
   * A unique id for the api, required.
   */
  public void setId(String id)
    throws ConfigException
  {
    for (int i = 0; i < id.length(); i++) {
      if (!Character.isJavaIdentifierPart(id.charAt(i)))
        throw new ConfigException(L.l("illegal character in `{0}': {1}","id",id.charAt(i)));
    }
    _id = id;
  }

  /**
   * A unique id for the api.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * A descriptive name for the api, default is to use location.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * A descriptive name for the api.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * A long descriptiion for the api, optional.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * A long description for the api, optional.
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * The location of a javadoc generated api, can be a url, required.
   * <p>Examples:
   * <ul>
   * <li>http://java.sun.com/j2se/1.4.2/docs/api
   * <li>file://usr/local/java/axis_1-1/docs/apiDocs
   * <li>resin/ 
   * </ul>
   */ 
  public void setLocation(String location)
  {
    if (!location.endsWith("/")) {
      CharBuffer cb = CharBuffer.allocate();
      cb.append(location);
      cb.append('/');
      _location = cb.close();
    }
    else
      _location = location;
  }

  /**
   * The location of a javadoc generated api.
   */ 
  public String getLocation()
  {
    return _location;
  }

  /**
   * The location of a javadoc generated api, as a vfs Path object.
   */ 
  Path getLocationPath()
  {
    return _locationPath;
  }

  /**
   * The location of a javadoc generated html index file, can be relative in
   * which case it is relative to `location'.  Default is "index-all.html".
   */
  public void setIndex(String index)
  {
    _indexString = index;
  }

  public void init()
    throws ConfigException
  {
    if (_id == null)
      throw new ConfigException(L.l("`{0}' is required","id"));

    if (_location == null)
      throw new ConfigException(L.l("`{0}' is required","location"));

    if (_name == null)
      _name = _location.toString();

    if (_indexString == null)
      _indexString = "index-all.html";

    _locationPath = Vfs.lookup(_location);

    int split = _indexString.indexOf('#');

    if (split > -1) {
      CharBuffer before = new CharBuffer(_indexString.substring(0,split));
      CharBuffer after = new CharBuffer(_indexString.substring(split + 1));
      CharBuffer index = CharBuffer.allocate();

      boolean isIndex = false;

      for (int i = 1; i <= 27; i++) {
        index.append(before);
        index.append(i);
        index.append(after);

        Path indexPath = _locationPath.lookup(index.toString());

        if (indexPath.exists())  {
          isIndex = true;
          _index.add(indexPath);
        }

        index.clear();
      }

      if (!isIndex) {
        throw new ConfigException(L.l("`{0}' not found", _locationPath.lookup(_indexString)));
      }
    }
    else
      _index.add(_locationPath.lookup(_indexString));

    if (_locationPath.getScheme().equals("file")) {
      _isLocal = true;
      Path pwd = Vfs.getPwd();

      if (!_locationPath.getPath().startsWith(pwd.getPath()))
        _isLocalAbsolute = true;
    }
  }

  long generateCrc64(long crc)
  {
    crc = Crc64.generate(crc,_location);
    return Crc64.generate(crc,_index.toString());
  }

  /**
   * The location of all javadoc generated html index files.
   */ 
  public ArrayList<Path> getIndexes()
  {
    return _index;
  }


  /**
   * An api that is local to the server.
   */
  public boolean isLocal()
  {
    return _isLocal;
  }

  /**
   * An api that is local to the server, but somewhere on the filesystem
   * outside of the context of the web application.
   */
  public boolean isLocalAbsolute()
  {
    return _isLocalAbsolute;
  }

  /**
   * A location href, relative to the web-app root, appropriately rewritten to
   * handle remote locations and locations that are local absolute.
   */

  String getLocationHref(String file)
  {
    CharBuffer cb = CharBuffer.allocate();

    if (_isLocalAbsolute) {
      cb.append(_id);
      cb.append('/');
    }
    else {
      cb.append(_location);
    }

    cb.append(file);

    return cb.close();
  }
}


