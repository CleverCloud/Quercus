package example.taglib;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.io.*;

public class MessageTag extends BodyTagSupport {

  /* tag attributes fields:
   *
   * member variables to store the value of tag attributes are treated
   * as read-only.  Resin will set the values using setXXXX() based on
   * the values passed as attributes to the tag, and the code in the
   * class will never change the values.
   */
  private String _attrTitle;

  /* internal tag fields
   *
   * Since an instance of this class can be reused, initialization of
   * internal member variables happens in the init() method, which is
   * called from doStartTag()
   */
  private String _title;
  private StringBuffer _msg;
  
  public void setTitle(String title) 
  {
    // this is the only place where _attrTitle is ever set
    _attrTitle = title;
  }
  
  public int doStartTag() 
    throws JspException 
  {
    // initialize internal member variables
    init();
 
    return EVAL_BODY_BUFFERED;
  }

  public int doEndTag()
    throws JspException 
  {
    // initialize internal member variables
    init();
    
    try {
      // print the message out
      JspWriter out = pageContext.getOut();
      out.println("<p>");
      out.println("<table border=1>");
      out.println("<tr><td>");
      out.println("instance: " + this);
      out.println("<tr><td>");
      out.println(_title);
      out.println("<tr><td>");
      out.println(_msg.toString());
      out.println("</table>");

    } catch (Exception ex) {
      throw new JspException(ex);
    }

    return EVAL_PAGE;
  }

  /**
   * Set defaults for attributes and initialize internal member
   * variables.
   */
  protected void init()
  {
    // default value for _title is "Default Title"
    _title = _attrTitle;
    if (_title == null)
      _title = "Default Title";

    // internal member variables
    _msg = new StringBuffer();

  }

  /** 
   * called by nested children to add to the message.  This happens
   * after doStartTag() and before doEndTag() 
   */
  void addToMessage(String text)
    throws JspException
  {
    _msg.append(text);
    _msg.append("<br>");
  }
}

