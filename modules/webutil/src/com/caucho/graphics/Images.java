/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 *
 * $Id: Images.java,v 1.6 2005/01/10 23:25:42 cvs Exp $
 */

package com.caucho.graphics;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.util.Hashtable;

public class Images implements ImageConsumer {
  private static Toolkit toolkit;
  private int width;
  private int height;

  public static Images getImage(String filename)
  {
    try {
      if (toolkit == null)
        toolkit = Toolkit.getDefaultToolkit();
      
      Image img = toolkit.getImage(filename);

      if (img == null)
        return null;

      Images image = new Images();

      img.getSource().startProduction(image);

      synchronized (image) {
        image.wait(100);
      }

      if (image.width > 0 && image.height > 0)
        return image;
      else
        return null;
    } catch (Throwable e) {
      return null;
    }
  }

  public int getWidth()
  {
    return width;
  }

  public int getHeight()
  {
    return height;
  }

  public void imageComplete(int status)
  {
    synchronized (this) {
      this.notifyAll();
    }
  }
  public void setColorModel(ColorModel model) {}

  public void setDimensions(int width, int height)
  {
    this.width = width;
    this.height = height;
  }

  public void setHints(int hintflags) {}
  public void setPixels(int x, int y, int w, int h, ColorModel mode,
    byte []pixels, int off, int scansize) {}
  public void setPixels(int x, int y, int w, int h, ColorModel mode,
      int []pixels, int off, int scansize) {}

  public void setProperties(Hashtable<?,?> props) {}
}


