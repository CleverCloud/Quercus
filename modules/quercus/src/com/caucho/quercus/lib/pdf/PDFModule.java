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

package com.caucho.quercus.lib.pdf;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * PHP PDF routines.
 */
public class PDFModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(PDFModule.class);

  private static final Logger log =
    Logger.getLogger(PDFModule.class.getName());

  /**
   * Returns true for the mysql extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "pdf" };
  }

  /**
   * Activates a created element.
   */
  public static boolean pdf_activate_item(Env env,
                                          @NotNull PDF pdf,
                                          int id)
  {
    env.stub("pdf_activate_item");

    return false;
  }

  /**
   * Adds an annotation
   */
  public static boolean pdf_add_annotation(Env env,
                                          @NotNull PDF pdf)
  {
    env.stub("pdf_add_annotation");

    return false;
  }

  /**
   * Adds an bookmarkannotation
   */
  public static boolean pdf_add_bookmark(Env env,
                                         @NotNull PDF pdf)
  {
    env.stub("pdf_add_bookmark");

    return false;
  }

  /**
   * Adds an launchlink
   */
  public static boolean pdf_add_launchlink(Env env,
                                           @NotNull PDF pdf,
                                           double llx,
                                           double lly,
                                           double urx,
                                           double ury,
                                           String filename)
  {
    env.stub("pdf_add_launchlink");

    return false;
  }

  /**
   * Adds a locallink
   */
  public static boolean pdf_add_locallink(Env env,
                                          @NotNull PDF pdf,
                                          double llx,
                                          double lly,
                                          double urx,
                                          double ury,
                                          int page,
                                          String dest)
  {
    env.stub("pdf_add_locallink");

    return false;
  }

  /**
   * Creates a named destination
   */
  public static boolean pdf_add_nameddest(Env env,
                                          @NotNull PDF pdf,
                                          String name,
                                          @Optional String optlist)
  {
    env.stub("pdf_add_nameddest");

    return false;
  }

  /**
   * Creates an annotation
   */
  public static boolean pdf_add_note(Env env,
                                     @NotNull PDF pdf,
                                     double llx,
                                     double lly,
                                     double urx,
                                     double ury,
                                     String contents,
                                     String title,
                                     String icon,
                                     int open)
  {
    env.stub("pdf_add_note");

    return false;
  }

  /**
   * Creates an outline
   */
  public static boolean pdf_add_outline(Env env,
                                        @NotNull PDF pdf,
                                        String name,
                                        @Optional String optlist)
  {
    env.stub("pdf_add_outline");

    return false;
  }

  /**
   * Creates a file link annotation
   */
  public static boolean pdf_add_pdflink(Env env,
                                        @NotNull PDF pdf,
                                        double llx,
                                        double lly,
                                        double urx,
                                        double ury,
                                        String filename,
                                        int page,
                                        String dest)
  {
    env.stub("pdf_add_pdflink");

    return false;
  }

  /**
   * Adds a thumbnail
   */
  public static boolean pdf_add_thumbnail(Env env,
                                          @NotNull PDF pdf,
                                          @NotNull PDFImage image)
  {
    env.stub("pdf_add_thumbnail");

    return false;
  }

  /**
   * Adds a web link
   */
  public static boolean pdf_add_weblink(Env env,
                                        @NotNull PDF pdf,
                                        double llx,
                                        double lly,
                                        double urx,
                                        double ury,
                                        String url)
  {
    env.stub("pdf_add_weblink");

    return false;
  }

  /**
   * Creates a counterclockwise arc
   */
  public static boolean pdf_arc(@NotNull PDF pdf,
                                double x1, double y1,
                                double r, double a, double b)
  {
    if (pdf == null)
      return false;
      
    return pdf.arc(x1, y1, r, a, b);
  }

  /**
   * Creates a clockwise arc
   */
  public static boolean pdf_arcn(@NotNull PDF pdf,
                                double x1, double y1,
                                double r, double a, double b)
  {
    if (pdf == null)
      return false;
    
    return pdf.arcn(x1, y1, r, a, b);
  }

  /**
   * Adds a file attachment
   */
  public static boolean pdf_attach_file(Env env,
                                        @NotNull PDF pdf,
                                        double llx,
                                        double lly,
                                        double urx,
                                        double ury,
                                        String filename,
                                        String description,
                                        String author,
                                        String mimetype,
                                        String icon)
  {
    env.stub("pdf_attach_file");
    
    return false;
  }

  /**
   * Starts the document.
   */
  public static boolean pdf_begin_document(@NotNull PDF pdf,
                                           @Optional String fileName,
                                           @Optional String optList)
  {
    try {
      if (pdf == null)
        return false;
    
      return pdf.begin_document(fileName, optList);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Starts a font definition
   */
  public static boolean pdf_begin_font(Env env,
                                       @NotNull PDF pdf,
                                       String fileName,
                                       double a,
                                       double b,
                                       double c,
                                       double d,
                                       double e,
                                       double f,
                                       @Optional String optList)
  {
    env.stub("pdf_begin_font");
    
    return false;
  }

  /**
   * Starts a glyph definition
   */
  public static boolean pdf_begin_glyph(Env env,
                                        @NotNull PDF pdf,
                                        String glyphname,
                                        double wx,
                                        double llx,
                                        double lly,
                                        double urx,
                                        double ury)
  {
    env.stub("pdf_begin_glyph");
    
    return false;
  }

  /**
   * Starts a structure element
   */
  public static boolean pdf_begin_item(Env env,
                                       @NotNull PDF pdf,
                                       String tag,
                                       String optlist)
  {
    env.stub("pdf_begin_item");
    
    return false;
  }

  /**
   * Starts a pdf layer
   */
  public static boolean pdf_begin_layer(Env env,
                                        @NotNull PDF pdf,
                                        int layer)
  {
    env.stub("pdf_begin_layer");
    
    return false;
  }

  /**
   * Starts the page.
   */
  public static boolean pdf_begin_page_ext(@NotNull PDF pdf,
                                           double w, double h,
                                           @Optional String optlist)
  {
    try {
      if (pdf == null)
        return false;
    
      return pdf.begin_page_ext(w, h, optlist);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Starts the page.
   */
  public static boolean pdf_begin_page(@NotNull PDF pdf,
                                       double w, double h)
  {
    try {
      if (pdf == null)
        return false;
    
      return pdf.begin_page(w, h);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Starts a pattern
   */
  public static boolean pdf_begin_pattern(Env env,
                                          @NotNull PDF pdf,
                                          double w,
                                          double h,
                                          double xStep,
                                          double yStep,
                                          int paintType)
  {
    env.stub("pdf_begin_pattern");
    
    return false;
  }

  /**
   * Starts a template
   */
  public static boolean pdf_begin_template(Env env,
                                          @NotNull PDF pdf,
                                          double w,
                                          double h)
  {
    env.stub("pdf_begin_template");
    
    return false;
  }

  /**
   * Draws a circle
   */
  public static boolean pdf_circle(@NotNull PDF pdf,
                                   double x,
                                   double y,
                                   double r)
  {
    if (pdf == null)
      return false;
    
    return pdf.circle(x, y, r);
  }

  /**
   * Clips the path.
   */
  public static boolean pdf_clip(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.clip();
  }

  /**
   * Closes an image
   */
  public static boolean pdf_close_image(Env env,
                                        @NotNull PDF pdf,
                                        PDFImage image)
  {
    env.stub("pdf_close_image");
    
    return false;
  }

  /**
   * Closes a page
   */
  public static boolean pdf_close_pdi_page(Env env,
                                       @NotNull PDF pdf,
                                       int page)
  {
    env.stub("pdf_close_pdi_page");
    
    return false;
  }

  /**
   * Closes a document
   */
  public static boolean pdf_close_pdi(Env env,
                                      @NotNull PDF pdf,
                                      int doc)
  {
    env.stub("pdf_close_pdi");
    
    return false;
  }

  /**
   * Closes the pdf document.
   */
  public static boolean pdf_close(@NotNull PDF pdf)
  {
    try {
      if (pdf == null)
        return false;
    
      return pdf.close();
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Closes the path, fill, and stroke it.
   */
  public static boolean pdf_closepath_fill_stroke(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.closepath_fill_stroke();
  }

  /**
   * Closes the path and stroke it.
   */
  public static boolean pdf_closepath_stroke(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.closepath_stroke();
  }

  /**
   * Closes the path.
   */
  public static boolean pdf_closepath(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.closepath();
  }

  /**
   * Concatenates a transformation matrix
   */
  public static boolean pdf_concat(@NotNull PDF pdf,
                                   double a,
                                   double b,
                                   double c,
                                   double d,
                                   double e,
                                   double f)
  {
    if (pdf == null)
      return false;
    
    return pdf.concat(a, b,c, d, e, f);
  }

  /**
   * Continues text at the next line.
   */
  public static boolean pdf_continue_text(@NotNull PDF pdf,
                                          String text)
  {
    if (pdf == null)
      return false;
    
    return pdf.continue_text(text);
  }

  /**
   * Creates an action.
   */
  public static boolean pdf_create_action(Env env,
                                          @NotNull PDF pdf,
                                          String type,
                                          @Optional String optList)
  {
    env.stub("pdf_create_action");
    
    return false;
  }

  /**
   * Creates a rectangular annotation
   */
  public static boolean pdf_create_annotation(Env env,
                                              @NotNull PDF pdf,
                                              double llx,
                                              double lly,
                                              double urx,
                                              double ury,
                                              String type,
                                              @Optional String optList)
  {
    env.stub("pdf_create_annotation");
    
    return false;
  }

  /**
   * Creates a bookmark
   */
  public static boolean pdf_create_bookmark(Env env,
                                            @NotNull PDF pdf,
                                            String text,
                                            @Optional String optList)
  {
    env.stub("pdf_create_bookmark");
    
    return false;
  }

  /**
   * Creates a form field.
   */
  public static boolean pdf_create_field(Env env,
                                         @NotNull PDF pdf,
                                         double llx,
                                         double lly,
                                         double urx,
                                         double ury,
                                         String name,
                                         String type,
                                         @Optional String optList)
  {
    env.stub("pdf_create_field");
    
    return false;
  }

  /**
   * Creates a form field group.
   */
  public static boolean pdf_create_fieldgroup(Env env,
                                              @NotNull PDF pdf,
                                              String name,
                                              @Optional String optList)
  {
    env.stub("pdf_create_fieldgroup");
    
    return false;
  }

  /**
   * Creates a graphics state
   */
  public static boolean pdf_create_gstate(Env env,
                                          @NotNull PDF pdf,
                                          @Optional String optList)
  {
    env.stub("pdf_create_gstate");
    
    return false;
  }

  /**
   * Creates a virtual file
   */
  public static boolean pdf_create_pvf(Env env,
                                       @NotNull PDF pdf,
                                       String filename,
                                       String data,
                                       @Optional String optList)
  {
    env.stub("pdf_create_pvf");
    
    return false;
  }

  /**
   * Creates a textflow object
   */
  public static boolean pdf_create_textflow(Env env,
                                            @NotNull PDF pdf,
                                            String text,
                                            @Optional String optList)
  {
    env.stub("pdf_create_textflow");
    
    return false;
  }

  /**
   * Draws a bezier curve
   */
  public static boolean pdf_curveto(@NotNull PDF pdf,
                                    double x1,
                                    double y1,
                                    double x2,
                                    double y2,
                                    double x3,
                                    double y3)
  {
    if (pdf == null)
      return false;
    
    return pdf.curveto(x1, y1, x2, y2, x3, y3);
  }

  /**
   * Creates a layer
   */
  public static boolean pdf_define_layer(Env env,
                                         @NotNull PDF pdf,
                                         String name,
                                         @Optional String optList)
  {
    env.stub("pdf_define_layer");
    
    return false;
  }

  /**
   * Delete a virtual file
   */
  public static boolean pdf_delete_pvf(Env env,
                                       @NotNull PDF pdf,
                                       String name)
  {
    env.stub("pdf_delete_pvf");
    
    return false;
  }

  /**
   * Delete a textflow object
   */
  public static boolean pdf_delete_textflow(Env env,
                                            @NotNull PDF pdf,
                                            int textflow)
  {
    env.stub("pdf_delete_textflow");
    
    return false;
  }

  /**
   * Delete the pdf object.
   */
  public static boolean pdf_delete(@NotNull PDF pdf)
  {
    try {
      if (pdf == null)
        return false;
    
      return pdf.delete();
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Adds a glyph to a custom encoding.
   */
  public static boolean pdf_encoding_set_char(Env env,
                                              @NotNull PDF pdf,
                                              String encoding,
                                              int slow,
                                              String glyphname,
                                              int uv)
  {
    env.stub("pdf_encoding_set_char");

    return false;
  }

  /**
   * Completes the document.
   */
  public static boolean pdf_end_document(@NotNull PDF pdf,
                                         @Optional String optlist)
  {
    try {
      if (pdf == null)
        return false;
    
      return pdf.end_document(optlist);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Completes a font definition
   */
  public static boolean pdf_end_font(Env env,
                                     @NotNull PDF pdf)
  {
    env.stub("pdf_end_font");
    
    return false;
  }

  /**
   * Completes a glyph definition
   */
  public static boolean pdf_end_glyph(Env env,
                                     @NotNull PDF pdf)
  {
    env.stub("pdf_end_glyph");
    
    return false;
  }

  /**
   * Completes a structure element.
   */
  public static boolean pdf_end_item(Env env,
                                     @NotNull PDF pdf,
                                     int id)
  {
    env.stub("pdf_end_item");
    
    return false;
  }

  /**
   * Completes a layer
   */
  public static boolean pdf_end_layer(Env env,
                                      @NotNull PDF pdf)
  {
    env.stub("pdf_end_layer");
    
    return false;
  }

  /**
   * Completes a page
   */
  public static boolean pdf_end_page_ext(@NotNull PDF pdf,
                                         @Optional String optlist)
  {
    if (pdf == null)
      return false;
    
    return pdf.end_page_ext(optlist);
  }

  /**
   * Completes a page
   */
  public static boolean pdf_end_page(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.end_page();
  }

  /**
   * Completes a pattern
   */
  public static boolean pdf_end_pattern(Env env,
                                        @NotNull PDF pdf)
  {
    env.stub("pdf_end_pattern");

    return false;
  }

  /**
   * Completes a template
   */
  public static boolean pdf_end_template(Env env,
                                         @NotNull PDF pdf)
  {
    env.stub("pdf_end_template");

    return false;
  }

  /**
   * End the current path.
   */
  public static boolean pdf_end_path(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.endpath();
  }

  /**
   * Fill the image with data.
   */
  public static boolean pdf_fill_imageblock(Env env,
                                            @NotNull PDF pdf,
                                            int page,
                                            String blockname,
                                            int image,
                                            @Optional String optlist)
  {
    env.stub("pdf_fill_imageblock");
    
    return false;
  }

  /**
   * Fill the pdfblock with data.
   */
  public static boolean pdf_fill_pdfblock(Env env,
                                          @NotNull PDF pdf,
                                          int page,
                                          String blockname,
                                          int contents,
                                            @Optional String optlist)
  {
    env.stub("pdf_fill_pdfblock");
    
    return false;
  }

  /**
   * Fill and stroke the path.
   */
  public static boolean pdf_fill_stroke(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.fill_stroke();
  }

  /**
   * Fill the text with data.
   */
  public static boolean pdf_fill_textblock(Env env,
                                           @NotNull PDF pdf,
                                           int page,
                                           String block,
                                           String text,
                                           @Optional String optlist)
  {
    env.stub("pdf_fill_textblock");

    return false;
  }

  /**
   * Fill the path.
   */
  public static boolean pdf_fill(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.fill();
  }

  /**
   * Loads a font.
   */
  public static boolean pdf_findfont(Env env,
                                     @NotNull PDF pdf,
                                     String fontname,
                                     String encoding,
                                     int embed)
  {
    env.stub("pdf_findfont");
    
    return false;
  }

  /**
   * Place an image
   */
  public static boolean pdf_fit_image(@NotNull PDF pdf,
                                      @NotNull PDFImage image,
                                      double x,
                                      double y,
                                      @Optional String optlist)
  {
    if (pdf == null)
      return false;
    
    return pdf.fit_image(image, x, y, optlist);
  }

  /**
   * Place an embedded pdf
   */
  public static boolean pdf_fit_pdi_page(Env env,
                                         @NotNull PDF pdf,
                                         int page,
                                         double x,
                                         double y,
                                         @Optional String optlist)
  {
    env.stub("pdf_fit_pdi_page");

    return false;
  }

  /**
   * Place a textflow object
   */
  public static boolean pdf_fit_textflow(Env env,
                                         @NotNull PDF pdf,
                                         int textflow,
                                         double llx,
                                         double lly,
                                         double urx,
                                         double ury,
                                         @Optional String optlist)
  {
    env.stub("pdf_fit_textflow");

    return false;
  }

  /**
   * Place a line of text.
   */
  public static boolean pdf_fit_textline(Env env,
                                         @NotNull PDF pdf,
                                         String text,
                                         double x,
                                         double y,
                                         @Optional String optlist)
  {
    env.stub("pdf_fit_textline");

    return false;
  }

  /**
   * Returns the name of the last failing function.
   */
  public static String pdf_get_apiname(Env env,
                                        @NotNull PDF pdf)
  {
    env.stub("pdf_get_apiname");

    return "";
  }

  /**
   * Returns the buffer with the data.
   */
  public static Value pdf_get_buffer(Env env,
                                     @NotNull PDF pdf)
  {
    if (pdf == null)
      return BooleanValue.FALSE;
    
    return pdf.get_buffer(env);
  }

  /**
   * Returns the last error message
   */
  public static String pdf_get_errmsg(PDF pdf)
  {
    if (pdf != null)
      return pdf.get_errmsg();
    else
      return "";
  }

  /**
   * Returns the last error number
   */
  public static int pdf_get_errnum(PDF pdf)
  {
    if (pdf != null)
      return pdf.get_errnum();
    else
      return 0;
  }

  /**
   * Returns the height of an image.
   */
  public static double pdf_get_image_height(@NotNull PDFImage image)
  {
    if (image != null)
      return image.get_height();
    else
      return 0;
  }

  /**
   * Returns the width of an image.
   */
  public static double pdf_get_image_width(@NotNull PDFImage image)
  {
    if (image != null)
      return image.get_width();
    else
      return 0;
  }
  
  /**
   * Returns the result as a string.
   */
  /*
  public static Value pdf_get_buffer(Env env, @NotNull PDF pdf)
  {
    if (pdf != null)
      return pdf.get_buffer(env);
    else
      return BooleanValue.FALSE;
  }
  */
  
  /**
   * Returns the named parameter.
   */
  public static String pdf_get_parameter(@NotNull PDF pdf,
                                        String key,
                                        @Optional double modifier)
  {
    if (pdf != null)
      return pdf.get_parameter(key, modifier);
    else
      return "";
  }
  
  /**
   * Returns the named pdi parameter.
   */
  public static String pdf_get_pdi_parameter(Env env,
                                             @NotNull PDF pdf,
                                             String key,
                                             int doc,
                                             int page,
                                             int reserved)
  {
    env.stub("pdf_get_pdi_parameter");
    
    return "";
  }
  
  /**
   * Returns the named pdi value.
   */
  public static double pdf_get_pdi_value(Env env,
                                             @NotNull PDF pdf,
                                             String key,
                                             int doc,
                                             int page,
                                             int reserved)
  {
    env.stub("pdf_get_pdi_value");
    
    return 0;
  }
  
  /**
   * Returns the named parameter.
   */
  public static double pdf_get_value(@NotNull PDF pdf,
                                     String key,
                                     @Optional double modifier)
  {
    if (pdf != null)
      return pdf.get_value(key, modifier);
    else
      return 0;
  }
  
  /**
   * Returns the textflow state
   */
  public static double pdf_info_textflow(Env env,
                                         @NotNull PDF pdf,
                                         int textflow,
                                         String key)
  {
    env.stub("pdf_info_textflow");

    return 0;
  }
  
  /**
   * Resets the graphic state
   */
  public static boolean pdf_initgraphics(Env env,
                                         @NotNull PDF pdf)
  {
    if (pdf != null)
      return pdf.initgraphics(env);
    else
      return false;
  }
  
  /**
   * Draw a line from the current position.
   */
  public static boolean pdf_lineto(@NotNull PDF pdf,
                                   double x,
                                   double y)
  {
    if (pdf == null)
      return false;
    
    return pdf.lineto(x, y);
  }
  
  /**
   * Search for a font.
   */
  public static PDFFont pdf_load_font(@NotNull PDF pdf,
                                      String fontname,
                                      String encoding,
                                      @Optional String optlist)
  {
    try {
      if (pdf == null)
        return null;
    
      return pdf.load_font(fontname, encoding, optlist);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }
  
  /**
   * Search for an icc profile
   */
  public static boolean pdf_load_iccprofile(Env env,
                                            @NotNull PDF pdf,
                                            String profileName,
                                            @Optional String optlist)
  {
    env.stub("pdf_load_iccprofile");

    return false;
  }
  
  /**
   * Loads an image
   */
  public static PDFImage pdf_load_image(@NotNull PDF pdf,
                                        String imageType,
                                        Path path,
                                        @Optional String optlist)
  {
    try {
      if (pdf == null)
        return null;
    
      return pdf.load_image(imageType, path, optlist);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }
  
  /**
   * Finds a spot color
   */
  public static boolean pdf_makespotcolor(Env env,
                                          @NotNull PDF pdf,
                                          String spotname)
  {
    env.stub("pdf_makespotcolor");

    return false;
  }
  
  /**
   * Sets the current graphics point.
   */
  public static boolean pdf_moveto(@NotNull PDF pdf,
                                   double x,
                                   double y)
  {
    if (pdf == null)
      return false;
    
    return pdf.moveto(x, y);
  }

  /**
   * Creates a new PDF object.
   */
  public static PDF pdf_new(Env env)
  {
    return new PDF(env);
  }

  /**
   * Opens a file.
   */
  public static boolean pdf_open_file(@NotNull PDF pdf,
                                      String filename)
  {
    return pdf_begin_document(pdf, filename, "");
  }

  /**
   * Opens an image.
   */
  public static PDFImage pdf_open_image_file(@NotNull PDF pdf,
                                             String imagetype,
                                             Path filename,
                                             String stringparam,
                                             int intparam)
  {
    return pdf_load_image(pdf, imagetype, filename, "");
  }

  /**
   * Opens an image.
   */
  public static boolean pdf_open_image_data(Env env,
                                             @NotNull PDF pdf,
                                             String imagetype,
                                             String source,
                                             String data,
                                             long length,
                                             long width,
                                             long height,
                                             int components,
                                             int bpc,
                                             String params)
  {
    env.stub("pdf_open_image_data");
    
    return false;
  }

  /**
   * Opens an embedded page.
   */
  public static boolean pdf_open_pdi_page(Env env,
                                          @NotNull PDF pdf,
                                          int doc,
                                          int pagenumber,
                                          @Optional String optlist)
  {
    env.stub("pdf_open_pdi_page");

    return false;
  }

  /**
   * Opens an embedded document
   */
  public static boolean pdf_open_pdi(Env env,
                                     @NotNull PDF pdf,
                                     String filename,
                                     @Optional String optlist)
  {
    env.stub("pdf_open_pdi");

    return false;
  }

  /**
   * Place an image.
   */
  public static boolean pdf_place_image(@NotNull PDF pdf,
                                        PDFImage image,
                                        double x,
                                        double y,
                                        double scale)
  {
    return pdf_fit_image(pdf, image, x, y, "");
  }

  /**
   * Place an embedded page.
   */
  public static boolean pdf_place_pdi_page(Env env,
                                           @NotNull PDF pdf,
                                           int page,
                                           double x,
                                           double y,
                                           double scaleX,
                                           double scaleY)
  {
    return pdf_fit_pdi_page(env, pdf, page, x, y, "");
  }

  /**
   * Process an imported PDF document.
   */
  public static boolean pdf_process_pdi(Env env,
                                        @NotNull PDF pdf,
                                        int doc,
                                        int page,
                                        @Optional String optlist)
  {
    env.stub("pdf_process_pdi");
    
    return false;
  }
  
  /**
   * Creates a rectangle
   */
  public static boolean pdf_rect(@NotNull PDF pdf,
                                 double x, double y,
                                 double width, double height)
  {
    if (pdf == null)
      return false;
    
    return pdf.rect(x, y, width, height);
  }

  /**
   * Restores the graphics state.
   */
  public static boolean pdf_restore(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.restore();
  }

  /**
   * Rotate the coordinates.
   */
  public static boolean pdf_rotate(@NotNull PDF pdf,
                                   double phi)
  {
    if (pdf == null)
      return false;
    
    return pdf.rotate(phi);
  }

  /**
   * Save the graphics state.
   */
  public static boolean pdf_save(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.save();
  }

  /**
   * Scale the coordinates.
   */
  public static boolean pdf_scale(@NotNull PDF pdf,
                                  double scaleX,
                                  double scaleY)
  {
    if (pdf == null)
      return false;
    
    return pdf.scale(scaleX, scaleY);
  }

  /**
   * Sets an annotation border color.
   */
  public static boolean pdf_set_border_color(Env env,
                                             @NotNull PDF pdf,
                                             double red,
                                             double green,
                                             double blue)
  {
    env.stub("pdf_set_border_color");
    
    return false;
  }

  /**
   * Sets an annotation border dash
   */
  public static boolean pdf_set_border_dash(Env env,
                                            @NotNull PDF pdf,
                                            double black,
                                            double white)
  {
    env.stub("pdf_set_border_dash");
    
    return false;
  }

  /**
   * Sets an annotation border style
   */
  public static boolean pdf_set_border_style(Env env,
                                            @NotNull PDF pdf,
                                            String style,
                                            double width)
  {
    env.stub("pdf_set_border_style");
    
    return false;
  }

  /**
   * Activate a graphics state.
   */
  public static boolean pdf_set_gstate(Env env,
                                       @NotNull PDF pdf,
                                       int gstate)
  {
    env.stub("pdf_set_gstate");
    
    return false;
  }

  /**
   * Sets document info.
   */
  public static boolean pdf_set_info(@NotNull PDF pdf,
                                     String key,
                                     String value)
  {
    if (pdf == null)
      return false;
    
    return pdf.set_info(key, value);
  }

  /**
   * Define a relationship between layers.
   */
  public static boolean pdf_set_layer_dependency(Env env,
                                                 @NotNull PDF pdf,
                                                 String type,
                                                 @Optional String optlist)
  {
    env.stub("pdf_set_layer_dependency");
    
    return false;
  }

  /**
   * Sets a string parameter.
   */
  public static boolean pdf_set_parameter(@NotNull PDF pdf,
                                          String key,
                                          String value)
  {
    if (pdf != null)
      return false;
    
    return pdf.set_parameter(key, value);
  }

  /**
   * Sets the text position
   */
  public static boolean pdf_set_text_pos(@NotNull PDF pdf,
                                         double x,
                                         double y)
  {
    if (pdf == null)
      return false;
    
    return pdf.set_text_pos(x, y);
  }

  /**
   * Sets a double parameter.
   */
  public static boolean pdf_set_value(@NotNull PDF pdf,
                                      String key,
                                      double value)
  {
    if (pdf == null)
      return false;
    
    return pdf.set_value(key, value);
  }

  /**
   * Sets the colorspace and color
   */
  public static boolean pdf_setcolor(@NotNull PDF pdf,
                                     String type,
                                     String colorspace,
                                     double c1,
                                     @Optional double c2,
                                     @Optional double c3,
                                     @Optional double c4)
  {
    if (pdf == null)
      return false;
    
    return pdf.setcolor(type, colorspace, c1, c2, c3, c4);
  }

  /**
   * Sets the dashing
   */
  public static boolean pdf_setdash(@NotNull PDF pdf,
                                    double black,
                                    double white)
  {
    if (pdf == null)
      return false;
    
    return pdf.setdash(black, white);
  }

  /**
   * Sets the dash pattern
   */
  public static boolean pdf_setdashpattern(Env env,
                                           @NotNull PDF pdf,
                                           String optlist)
  {
    if (pdf == null)
      return false;
    
    return pdf.setdashpattern(env, optlist);
  }

  /**
   * Sets the flatness
   */
  public static boolean pdf_setflat(Env env,
                                    @NotNull PDF pdf,
                                    double flatness)
  {
    if (pdf == null)
      return false;
    
    return pdf.setflat(env, flatness);
  }

  /**
   * Sets the font size
   */
  public static boolean pdf_setfont(@NotNull PDF pdf,
                                    @NotNull PDFFont font,
                                    double size)
  {
    try {
      if (pdf == null)
        return false;
    
      return pdf.setfont(font, size);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Sets the fill color to gray
   */
  public static boolean pdf_setgray_fill(@NotNull PDF pdf,
                                         double g)
  {
    if (pdf == null)
      return false;
    
    return pdf.setgray_fill(g);
  }

  /**
   * Sets the stroke color to gray
   */
  public static boolean pdf_setgray_stroke(@NotNull PDF pdf,
                                           double g)
  {
    if (pdf == null)
      return false;
    
    return pdf.setgray_stroke(g);
  }

  /**
   * Sets the color to gray
   */
  public static boolean pdf_setgray(@NotNull PDF pdf,
                                    double g)
  {
    if (pdf == null)
      return false;
    
    return pdf.setgray(g);
  }

  /**
   * Sets the linecap param
   */
  public static boolean pdf_setlinecap(Env env,
                                       @NotNull PDF pdf,
                                       int value)
  {
    if (pdf == null)
      return false;
    
    return pdf.setlinecap(env, value);
  }

  /**
   * Sets the linejoin param
   */
  public static boolean pdf_setlinejoin(Env env,
                                        @NotNull PDF pdf,
                                        int value)
  {
    if (pdf == null)
      return false;
    
    return pdf.setlinejoin(env, value);
  }

  /**
   * Sets the line width
   */
  public static boolean pdf_setlinewidth(@NotNull PDF pdf,
                                         double width)
  {
    if (pdf == null)
      return false;
    
    return pdf.setlinewidth(width);
  }

  /**
   * Sets the current transformation matrix
   */
  public static boolean pdf_setmatrix(Env env,
                                      @NotNull PDF pdf,
                                      double a,
                                      double b,
                                      double c,
                                      double d,
                                      double e,
                                      double f)
  {
    if (pdf == null)
      return false;
    
    return pdf.setmatrix(env, a, b, c, d, e, f);
  }

  /**
   * Sets the line miter limit.
   */
  public static boolean pdf_setmiterlimit(Env env,
                                          @NotNull PDF pdf,
                                          double value)
  {
    if (pdf == null)
      return false;
    
    return pdf.setmiterlimit(env, value);
  }

  /**
   * Sets the fill in rgb
   */
  public static boolean pdf_setrgbcolor_fill(@NotNull PDF pdf,
                                             double red,
                                             double green,
                                             double blue)
  {
    if (pdf == null)
      return false;
    
    return pdf.setrgbcolor_fill(red, green, blue);
  }

  /**
   * Sets the stroke in rgb
   */
  public static boolean pdf_setrgbcolor_stroke(@NotNull PDF pdf,
                                               double red,
                                               double green,
                                               double blue)
  {
    if (pdf == null)
      return false;
    
    return pdf.setrgbcolor_stroke(red, green, blue);
  }

  /**
   * Sets the color in rgb
   */
  public static boolean pdf_setrgbcolor(@NotNull PDF pdf,
                                        double red,
                                        double green,
                                        double blue)
  {
    if (pdf == null)
      return false;
    
    return pdf.setrgbcolor(red, green, blue);
  }

  /**
   * Sets the shading pattern
   */
  public static boolean pdf_shading_pattern(Env env,
                                            @NotNull PDF pdf,
                                            int shading,
                                            @Optional String optlist)
  {
    if (pdf == null)
      return false;
    
    return pdf.shading_pattern(env, shading, optlist);
  }

  /**
   * Define a blend
   */
  public static int pdf_shading(Env env,
                                @NotNull PDF pdf,
                                String type,
                                double x1,
                                double y1,
                                double x2,
                                double y2,
                                double c1,
                                double c2,
                                double c3,
                                double c4,
                                @Optional String optlist)
  {
    if (pdf == null)
      return 0;
    
    return pdf.shading(env, type, x1, y1, x2, y2, c1, c2, c3, c4, optlist);
  }

  /**
   * Fill with a shading object.
   */
  public static boolean pdf_shfill(Env env,
                                   @NotNull PDF pdf,
                                   int shading)
  {
    if (pdf == null)
      return false;
    
    return pdf.shfill(env, shading);
  }

  /**
   * Output text in a box.
   */
  public static boolean pdf_show_boxed(Env env,
                                       @NotNull PDF pdf,
                                       String text,
                                       double x,
                                       double y,
                                       double width,
                                       double height,
                                       String mode,
                                       @Optional String feature)
  {
    if (pdf == null)
      return false;
    
    return pdf.show_boxed(text, x, y, width, height, mode, feature);
  }

  /**
   * Output text at a location
   */
  public static boolean pdf_show_xy(Env env,
                                    @NotNull PDF pdf,
                                    String text,
                                    double x,
                                    double y)
  {
    if (pdf == null)
      return false;
    
    return pdf.show_xy(text, x, y);
  }

  /**
   * Output text at the current
   */
  public static boolean pdf_show(Env env,
                                 @NotNull PDF pdf,
                                 String text)
  {
    if (pdf == null)
      return false;
    
    return pdf.show(text);
  }

  /**
   * Skew the coordinate system.
   */
  public static boolean pdf_skew(@NotNull PDF pdf,
                                 double alpha,
                                 double beta)
  {
    if (pdf == null)
      return false;
    
    return pdf.skew(alpha, beta);
  }

  /**
   * Returns the width of text in the font.
   */
  public static double pdf_stringwidth(@NotNull PDF pdf,
                                       String text,
                                       @NotNull PDFFont font,
                                       double size)
  {
    if (pdf == null)
      return 0;
    
    return pdf.stringwidth(text, font, size);
  }

  /**
   * Strokes the path
   */
  public static boolean pdf_stroke(@NotNull PDF pdf)
  {
    if (pdf == null)
      return false;
    
    return pdf.stroke();
  }

  /**
   * Suspend the page.
   */
  public static boolean pdf_suspend_page(Env env,
                                         @NotNull PDF pdf,
                                         @Optional String optlist)
  {
    env.stub("pdf_suspend_page");
    
    return false;
  }

  /**
   * Sets the coordinate system origin.
   */
  public static boolean pdf_translate(@NotNull PDF pdf,
                                      double x,
                                      double y)
  {
    if (pdf == null)
      return false;
    
    return pdf.translate(x, y);
  }

  /**
   * Convert from utf16 to utf8
   */
  public static String pdf_utf16_to_utf8(Env env,
                                          @NotNull PDF pdf,
                                          String utf16string)
  {
    env.stub("pdf_utf16_to_utf8");
    
    return utf16string;
  }

  /**
   * Convert from utf8 to utf16
   */
  public static String pdf_utf8_to_utf16(Env env,
                                         @NotNull PDF pdf,
                                          String utf8string)
  {
    env.stub("pdf_utf16_to_utf8");
    
    return utf8string;
  }
}
