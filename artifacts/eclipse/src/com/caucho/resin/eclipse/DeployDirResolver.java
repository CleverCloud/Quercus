package com.caucho.resin.eclipse;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.internal.core.util.FileUtil;

@SuppressWarnings("restriction")
public class DeployDirResolver implements IDynamicVariableResolver {
  private String _tempDirectory = null;
  
  public String resolveValue(IDynamicVariable variable, String argument)
    throws CoreException
  {
    if (_tempDirectory == null) {
      // create a webapp deploy directory in case we're doing hot deploy
      String dir = CorePlugin.getDefault().getStateLocation().toOSString(); 
      File tempFile = FileUtil.createTempFile("webapps", dir);
      
      _tempDirectory = tempFile.toString();
    }
    
    return _tempDirectory;
  }

}
