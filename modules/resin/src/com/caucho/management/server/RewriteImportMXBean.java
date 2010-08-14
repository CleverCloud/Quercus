package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Units;

/**
 * Interface for a rewrite rule.
 *
 * <pre>
 * resin:type=RewriteImport,name=...
 * </pre>
 */
@Description("A rewrite rule that imports rules from an xml file")
public interface RewriteImportMXBean
  extends ManagedObjectMXBean
{
  @Description("The configured millisecond interval between checks for the need to redeploy")
  @Units("milliseconds")
  public long getDependencyCheckInterval();

  @Description("The error that ocurred during the last redeploy, if any")
  public String getRedeployError();

  @Description("The current lifecycle state")
  public String getState();

  @Description("Enables the imported rules")
  public void start();

  @Description("Disables the imported rules")
  public void stop();

  @Description("Updates the imported rules if the file has changed")
  public void update();
}

