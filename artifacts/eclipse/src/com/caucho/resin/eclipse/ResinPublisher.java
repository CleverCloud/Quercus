package com.caucho.resin.eclipse;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.publishers.AntPublisher;
import org.eclipse.wst.server.core.IModuleArtifact;

@SuppressWarnings("restriction")
public class ResinPublisher extends AntPublisher
                            implements ResinPropertyIds
{
  public static final String PUBLISHER_ID = 
    "org.eclipse.jst.server.generic.resin.resinpublisher";
  
  @Override
  public IStatus[] publish(IModuleArtifact[] resource, IProgressMonitor monitor)
  {
    try {
      ResinServer resinServer = (ResinServer) getServer();
      Map properties = resinServer.getServerInstanceProperties();
      String configLocation = (String) properties.get(CONFIG_FILE_NAME);
    
      VariableUtil.setVariable(CONFIG_FILE_NAME, configLocation);
      
      String resinHome = (String) properties.get(RESIN_HOME);
      VariableUtil.setVariable(RESIN_HOME, resinHome);
    }
    catch (CoreException e) {
      IStatus s = new Status(IStatus.ERROR, 
                             CorePlugin.PLUGIN_ID, 0, 
                             "Resin publish failed",
                             e);
      CorePlugin.getDefault().getLog().log(s);
      
      return new IStatus[] { s };
    }

    // Have ant create the .war file
    return super.publish(resource, monitor);
 }

  @Override
  public IStatus[] unpublish(IProgressMonitor monitor)
  {
    return null;
  }
}
