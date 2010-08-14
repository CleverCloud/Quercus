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

import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * A Javadoc generated HTML file.
 */
public class JavadocFile {
  static protected final Logger log = Log.open(JavadocFile.class);
  static final L10N L = new L10N(JavadocFile.class);

  private Api _api;
  private int _id = Integer.MAX_VALUE;
  private String _path;

  private String _href;

  /**
   *
   */
  JavadocFile(Api api, int id, String path)
  {
    _api = api;
    _id = id;
    _path = path;
    init();
  }

  JavadocFile(Api api, String path)
  {
    _api = api;
    _path = path;
    init();
  }

  private void init()
  {
    CharBuffer cb = CharBuffer.allocate();
    Api api = getApi();
    _href = api.getLocationHref(_path);
  }

  public Api getApi()
  {
    return _api;
  }

  int getId()
  {
    if (_id == Integer.MAX_VALUE)
      throw new RuntimeException("_id not init in JavadocFile");
    return _id;
  }

  public String getPath()
  {
    return _path;
  }

  public String getHref()
  {
    return _href;
  }

}

