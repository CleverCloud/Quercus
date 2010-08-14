package example.taglib;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public class MessageAddTag extends TagSupport {
  /* tag attributes */
  String _text;

  /* internal member variables */
  MessageTag _parentTag;
  
  public void setText(String text) 
  {
    _text = text;
  }

  public int doStartTag() 
    throws JspException 
  {
    // initialize internal member variables
    init();

    _parentTag.addToMessage(_text);

    return SKIP_BODY;
  }

  private void init()
    throws JspException
  {
    _parentTag = (MessageTag) findAncestorWithClass(this,MessageTag.class);
    if (_parentTag == null)
      throw new JspException("Could not find parent MessageTag");
  }
}

