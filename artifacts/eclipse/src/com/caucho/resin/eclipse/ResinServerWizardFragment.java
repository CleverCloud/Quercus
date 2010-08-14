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
 * @author Emil Ong
 */

package com.caucho.resin.eclipse;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.core.internal.GenericServerRuntime;
import org.eclipse.jst.server.generic.ui.internal.GenericServerWizardFragment;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

@SuppressWarnings("restriction")
public class ResinServerWizardFragment extends GenericServerWizardFragment 
                                       implements ResinPropertyIds
{
  public static final String SERVER_PROPERTIES_ENTERED =
    "resin.server.properties.entered";
  
  private String _resinConfType = ResinServer.RESIN_CONF_BUNDLE;
  private Text _resinHomeTextField = null;
  private Text _resinRootTextField = null;
  private Text _userConfTextField = null;
  private boolean _copyConfig = false;
  private IWizardHandle _wizard = null;
  
  @Override
  public void createContent(final Composite parent, final IWizardHandle handle)
  {
    createResinPathsContent(parent);
    super.createContent(parent, handle);
    createConfigContent(parent);

    _wizard = handle;
  }
  
  private void createResinPathsContent(final Composite parent)
  {
    // layout
    Composite composite = new Composite(parent, SWT.NONE); 
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    composite.setLayout(new GridLayout(3, false));
    
    GridData singleColumnFillGridData =
      new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    GridData indentedRowFillGridData =
      new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
    indentedRowFillGridData.horizontalIndent = 20;
    
    Label resinHomeLabel = new Label(composite, SWT.NONE);
    resinHomeLabel.setText("Resin Home");
    
    _resinHomeTextField = new Text(composite, 
                                   SWT.SINGLE 
                                   | SWT.SHADOW_IN 
                                   | SWT.BORDER);
    _resinHomeTextField.setText("/usr/share/resin");
    _resinHomeTextField.setLayoutData(singleColumnFillGridData);
    
    final Button resinHomeBrowseButton = new Button(composite, SWT.PUSH);
    resinHomeBrowseButton.setText("Browse");

    final Button resinRootButton = new Button(composite, SWT.CHECK);
    resinRootButton.setText("Use Resin home as Resin root");
    resinRootButton.setLayoutData(indentedRowFillGridData);
    resinRootButton.setSelection(true);

    final Label resinRootLabel = new Label(composite, SWT.NONE);
    resinRootLabel.setText("Resin Root");
    resinRootLabel.setEnabled(false);
  
  
    _resinRootTextField = new Text(composite, 
                                   SWT.SINGLE 
                                   | SWT.SHADOW_IN 
                                   | SWT.BORDER);
    _resinRootTextField.setText("/usr/share/resin");
    _resinRootTextField.setEnabled(false);
    _resinRootTextField.setLayoutData(singleColumnFillGridData);

    final Button resinRootBrowseButton = new Button(composite, SWT.PUSH);
    resinRootBrowseButton.setText("Browse");
    resinRootBrowseButton.setEnabled(false);
    
    // listeners
  
    _resinHomeTextField.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent event)
      {
        if (resinRootButton.getSelection())
          _resinRootTextField.setText(_resinHomeTextField.getText());
      }
    });

    resinRootButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        if (resinRootButton.getSelection()) {
          resinRootLabel.setEnabled(false);
          _resinRootTextField.setEnabled(false);
          resinRootBrowseButton.setEnabled(false);

          _resinRootTextField.setText(_resinHomeTextField.getText());
        }
        else {
          resinRootLabel.setEnabled(true);
          _resinRootTextField.setEnabled(true);
          resinRootBrowseButton.setEnabled(true);
        }       
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });

    resinRootBrowseButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
        String currentText = 
          _resinRootTextField.getText().replace('\\', '/');
        dialog.setFilterPath(currentText);
        String filename = dialog.open();

        if (filename != null)
          _resinRootTextField.setText(filename.replace('\\', '/'));
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });
    
    _resinHomeTextField.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent arg0)
      {
        setProperty(ResinPropertyIds.RESIN_HOME, _resinHomeTextField.getText());
        validate();
      }
    });
    
    resinHomeBrowseButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
        String currentText = 
          _resinHomeTextField.getText().replace('\\', '/');
        dialog.setFilterPath(currentText);
        String filename = dialog.open();

        if (filename != null)
          _resinHomeTextField.setText(filename.replace('\\', '/'));
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });
  }
    
  private void createConfigContent(final Composite parent)
  { 
    // layout
    final Composite composite = new Composite(parent, SWT.NONE); 
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    composite.setLayout(new GridLayout(3, false));
    
    GridData rowFillGridData =
      new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
    GridData indentedRowFillGridData =
      new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
    indentedRowFillGridData.horizontalIndent = 20;    

    Label label = new Label(composite, SWT.NONE);
    label.setText("Select which Resin configuration you want to use with this server:");
    label.setLayoutData(rowFillGridData);
    
    final Button bundleConfig = new Button(composite, SWT.RADIO);
    bundleConfig.setText("Copy default configuration into project");
    bundleConfig.setLayoutData(rowFillGridData);
    bundleConfig.setSelection(true);
    
    final Button resinHomeConfig = new Button(composite, SWT.RADIO);
    resinHomeConfig.setLayoutData(rowFillGridData);
    resinHomeConfig.setText("Use configuration in Resin Home"); 
    
    final Button resinHomeConfigCopy = new Button(composite, SWT.CHECK);
    resinHomeConfigCopy.setLayoutData(indentedRowFillGridData);
    resinHomeConfigCopy.setText("Copy configuration to project");
    resinHomeConfigCopy.setEnabled(false);
    resinHomeConfigCopy.setSelection(false);
    
    final Label resinHomeAppDefaultLabel = new Label(composite, 
                                                     SWT.LEFT
                                                     | SWT.WRAP
                                                     | SWT.SHADOW_IN);
    resinHomeAppDefaultLabel.setVisible(false);

    GridData resinHomeAppDefaultLabelGridData =
      new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
    resinHomeAppDefaultLabelGridData.widthHint = 
      composite.getShell().getClientArea().width;
    resinHomeAppDefaultLabelGridData.horizontalIndent = 20;
    resinHomeAppDefaultLabel.setLayoutData(resinHomeAppDefaultLabelGridData);
    
    final Button userConfig = new Button(composite, SWT.RADIO);
    userConfig.setText("Use a configuration file from another location");                               
    userConfig.setLayoutData(rowFillGridData);
    
    _userConfTextField = new Text(composite, SWT.SINGLE 
                                             | SWT.SHADOW_IN 
                                             | SWT.BORDER);
    _userConfTextField.setText("/etc/resin/resin.xml");
    _userConfTextField.setEnabled(false);
    GridData indentedTwoColumnGridData =
      new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
    indentedTwoColumnGridData.horizontalIndent = 20;
    _userConfTextField.setLayoutData(indentedTwoColumnGridData);
    
    final Button userBrowseButton = new Button(composite, SWT.PUSH);
    userBrowseButton.setText("Browse");
    userBrowseButton.setEnabled(false);

    final Button userConfigCopy = new Button(composite, SWT.CHECK);
    userConfigCopy.setLayoutData(indentedRowFillGridData);
    userConfigCopy.setText("Copy configuration to project");
    userConfigCopy.setEnabled(false);
    userConfigCopy.setSelection(false);

    final Label userAppDefaultLabel = new Label(composite, SWT.LEFT 
                                                           | SWT.WRAP
                                                           | SWT.SHADOW_IN); 
    userAppDefaultLabel.setVisible(false);

    GridData userAppDefaultLabelGridData =
      new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
    userAppDefaultLabelGridData.widthHint = 
      composite.getShell().getClientArea().width;
    userAppDefaultLabelGridData.horizontalIndent = 20;
    userAppDefaultLabel.setLayoutData(userAppDefaultLabelGridData);
    
    final Label helpLabel = new Label(composite, SWT.LEFT 
                                                 | SWT.WRAP
                                                 | SWT.SHADOW_IN); 
    helpLabel.setVisible(false);

    GridData helpLabelGridData =
      new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
    helpLabelGridData.widthHint = composite.getShell().getClientArea().width;
    helpLabel.setLayoutData(helpLabelGridData);
    
    // listeners

    bundleConfig.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        _resinConfType = ResinServer.RESIN_CONF_BUNDLE;
        _copyConfig = resinHomeConfigCopy.getSelection();
        
        if (bundleConfig.getSelection())
          setLabelText(helpLabel, "");
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });

    resinHomeConfig.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {        
        _resinConfType = ResinServer.RESIN_CONF_RESIN_HOME;
        
        if (resinHomeConfig.getSelection()) {
          resinHomeConfigCopy.setEnabled(true);
          
          setLabelText(helpLabel, getHelpText());
          
          File resinConfDir = 
            new Path(_resinHomeTextField.getText()).append("conf").toFile();
          checkAppDefault(resinConfDir, resinHomeAppDefaultLabel);
        }
        else {
          resinHomeConfigCopy.setEnabled(false);
          
          setLabelText(resinHomeAppDefaultLabel, "");
        }
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });
    
    _resinHomeTextField.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent arg0)
      {
        File resinConfDir = 
          new Path(_resinHomeTextField.getText()).append("conf").toFile();
        checkAppDefault(resinConfDir, resinHomeAppDefaultLabel);
      }
    });

    resinHomeConfigCopy.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        _copyConfig = resinHomeConfigCopy.getSelection();
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });

    userConfig.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        if (userConfig.getSelection()) {
          userBrowseButton.setEnabled(true);
          _userConfTextField.setEnabled(true);
          userConfigCopy.setEnabled(true);
    
          setLabelText(helpLabel, getHelpText());
       
          File resinConfDir = 
            new File(_userConfTextField.getText()).getParentFile();
          checkAppDefault(resinConfDir, userAppDefaultLabel);
        }
        else {
          userBrowseButton.setEnabled(false);
          _userConfTextField.setEnabled(false);
          userConfigCopy.setEnabled(false);
          
          setLabelText(userAppDefaultLabel, "");
        }
        
        _resinConfType = ResinServer.RESIN_CONF_USER;
        _copyConfig = userConfigCopy.getSelection();
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });
    
    _userConfTextField.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent arg0)
      {
        File resinConfDir = 
          new File(_userConfTextField.getText()).getParentFile();
        checkAppDefault(resinConfDir, userAppDefaultLabel);
      }
    });
    
    userBrowseButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        FileDialog dialog = new FileDialog(parent.getShell());
        dialog.setFileName(_userConfTextField.getText().replace('\\', '/'));
        String filename = dialog.open();
        
        if (filename != null)
          _userConfTextField.setText(filename.replace('\\', '/'));
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });

    userConfigCopy.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) 
      {
        _copyConfig = userConfigCopy.getSelection();
      }

      public void widgetDefaultSelected(SelectionEvent e) 
      {
        widgetSelected(e);
      }
    });
  }

  private IServerWorkingCopy getServer() 
  {
    IServerWorkingCopy server = 
      (IServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
    
    return server;
  }
 
  private ResinServer getResinServer()
  {
    return (ResinServer) getServer().loadAdapter(GenericServer.class, null);
  }
  
  private void setProperty(String key, String value)
  {
    getResinServer().getServerInstanceProperties().put(key, value);
   
    // without setting the properties in both places, certain resolutions
    // will fail, e.g. resin.home for the class path.
    GenericServerRuntime runtime = getResinServer().getRuntimeDelegate();
    runtime.getServerInstanceProperties().put(key, value);
  }
  
  private String getHelpText()
  {    
    ResinServer server = getResinServer();
    
    String helpText = "If you supply a configuration file for this server, " 
      + "you may need to use certain parameters passed as Java system "
      + "properties in your configuration file in order to see the correct "
      + "behavior with Eclipse.  These variables can be accessed in your " 
      + "configuration file using the ${system['property.name']} EL syntax. "
      + "The variables used by this server are:\n\n";
    
    String variables = server.getPropertyDefault(ResinPropertyIds.HELP_TEXT); 
    
    return helpText + variables.replace("\\n", "\n");
  }
  
  private void validate()
  {
    IStatus status = getResinServer().getRuntimeDelegate().validate();
    
    if (status != null && status.isOK()) {
      _wizard.update();
      _wizard.setMessage(null, IMessageProvider.NONE);
    }
    else {
      _wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
    }
  }
  
  private void checkAppDefault(File resinConfDir, Label label)
  {
    File appDefault = new File(resinConfDir, "app-default.xml");
    
    if (! appDefault.exists()) {
      setLabelText(label, "Warning: app-default.xml could not be found in " 
                          + resinConfDir);
    }
    else {
      setLabelText(label, "");
    }
  }
  
  private void setLabelText(Label label, String text)
  {
    if ("".equals(text))
      label.setVisible(false);
    else
      label.setVisible(true);
    
    label.setText(text);
    label.redraw();
    label.getShell().pack();      
  }
  
  @Override
  public void enter()
  {
    super.enter();
    
    setProperty(SERVER_PROPERTIES_ENTERED, "true");
    
    validate();
  }

  @Override
  public void exit()
  {
    super.exit();
    
    setProperty(ResinPropertyIds.RESIN_HOME, _resinHomeTextField.getText());
    setProperty(ResinPropertyIds.RESIN_ROOT, _resinRootTextField.getText());
    setProperty(ResinServer.RESIN_CONF_TYPE, _resinConfType);
    
    if (ResinServer.RESIN_CONF_USER.equals(_resinConfType)) {
      setProperty(ResinServer.RESIN_CONF_USER_LOCATION, 
                  _userConfTextField.getText());
    }
    
    if (_copyConfig)
      setProperty(ResinServer.RESIN_CONF_COPY, "true");         
  }
}
