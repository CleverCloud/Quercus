package com.caucho.netbeans;

import org.openide.util.NbBundle;

public class PluginL10N
{
  private Class<?> _cl;

  public PluginL10N(Class<?> cl)
  {
    _cl = cl;
  }
  
  public String l(String msg, Object... args)
  {
    String message;

    try {
      return NbBundle.getMessage(_cl, msg, args);
    }
    catch (Exception ex) {
      return java.text.MessageFormat.format(msg, args);
    }
  }
}
