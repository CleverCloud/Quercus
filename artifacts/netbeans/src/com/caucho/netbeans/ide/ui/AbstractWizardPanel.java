package com.caucho.netbeans.ide.ui;

import com.caucho.netbeans.ide.AddInstanceIterator;
import java.awt.Component;
import java.util.HashSet;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

abstract public class AbstractWizardPanel
  implements WizardDescriptor.Panel, ChangeListener
{
  private HashSet<ChangeListener> _listeners = new HashSet<ChangeListener>(1);
  protected WizardDescriptor _wizard;
  
  public void removeChangeListener(ChangeListener listener)
  {
    synchronized (_listeners) {
      _listeners.remove(listener);
    }
  }

  public void addChangeListener(ChangeListener listener)
  {
    synchronized (_listeners) {
      _listeners.add(listener);
    }
  }
    
  public void stateChanged(ChangeEvent ev)
  {        
    HashSet<ChangeListener> listeners;
    
    synchronized (_listeners) {
      listeners = new HashSet<ChangeListener>(_listeners);
    }

    for (ChangeListener listener : listeners)
      listener.stateChanged(ev);
  }

  public void storeSettings(Object settings)
  {
  }

  public void readSettings(Object settings)
  {
    if (_wizard == null)
      _wizard = (WizardDescriptor) settings;
  }

  protected WizardDescriptor getWizard()
  {
    return _wizard;
  }

  public HelpCtx getHelp()
  {
    return HelpCtx.DEFAULT_HELP;
  }
}
