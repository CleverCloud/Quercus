package com.caucho.netbeans.ide.ui;

import com.caucho.netbeans.ide.AddInstanceIterator;
import java.awt.Component;
import java.util.logging.Logger;
import org.openide.util.HelpCtx;

public class AddServerLocationPanel extends AbstractWizardPanel
{
  private static final Logger log
    = Logger.getLogger(AddServerLocationPanel.class.getName());
  
  private final static String ERROR_MESSAGE = "WizardPanel_errorMessage";
  
  private AddInstanceIterator _addServer;
  private AddServerVisualPanel _component;
  
  public AddServerLocationPanel(AddInstanceIterator addServer)
  {
    _addServer = addServer;
    
    addChangeListener(addServer);
  }

  public boolean isValid()
  {
    getComponent();

    _addServer.setResinHome(_component.getResinHome());

    if (! _addServer.isResinHomeValid()) {
      _wizard.putProperty(ERROR_MESSAGE, "Invalid resin-home");
      return false;
    }
    
    _addServer.setPort(_component.getPort());
    
    if (1024 < _addServer.getPort() && _addServer.getPort() < 32767) {
      
    }
    else {
      _wizard.putProperty(ERROR_MESSAGE, "Invalid port");
    }

    _wizard.putProperty(ERROR_MESSAGE, null);

    return true;
  }

  @Override
  public HelpCtx getHelp()
  {
    return HelpCtx.DEFAULT_HELP;
  }

  public Component getComponent()
  {   
    if (_component == null)
      _component = new AddServerVisualPanel(_addServer, this);
    
    return _component;
  }
}
