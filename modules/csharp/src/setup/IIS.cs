/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.DirectoryServices;
using System.Collections;
using System.IO;
using System.ServiceProcess;
using System.Diagnostics;

namespace Caucho
{
  class IIS
  {
    static EventLog log = new EventLog();

    static IIS()
    {
      ((System.ComponentModel.ISupportInitialize)(log)).BeginInit();
      log.Log = "Application";
      log.Source = "caucho/IIS.cs";
    }

    public static String FindIIS()
    {
      String result = null;

      DirectoryEntry entry = null;
      try {
        entry = new DirectoryEntry("IIS://localhost/W3SVC/1/Root");

        if (entry.Properties != null) {
          Object val = entry.Properties["Path"];
          if (val != null && (val is PropertyValueCollection)) {
            PropertyValueCollection collection = (PropertyValueCollection)val;
            IEnumerator enumerator = collection.GetEnumerator();

            if (enumerator.MoveNext())
              result = (String)enumerator.Current;
          }
        }
      }
      catch (Exception e) {
        log.WriteEntry(e.Message + "\n" + e.StackTrace);
      }
      finally {
        if (entry != null)
          entry.Close();
      }

      if (result != null) {
        int i = result.LastIndexOf('\\');
        if (i > -1) {
          result = result.Substring(0, i) + @"\Scripts";
          if (!Directory.Exists(result))
            Directory.CreateDirectory(result);
        }
      }

      return result;
    }

    public static void RestartIIS()
    {
      Util.RestartService("W3SVC");
    }

    public static SetupResult SetupIIS(String resinHome, String iisScripts)
    {
      try {
        DirectoryEntry filters = new DirectoryEntry("IIS://localhost/W3SVC/Filters");
        DirectoryEntry resinFilter = null;

        foreach (DirectoryEntry entry in filters.Children) {
          if ("Resin".Equals(entry.Name)) {
            resinFilter = entry;
          }
        }

        if (resinFilter == null)
          resinFilter = filters.Children.Add("Resin", "IIsFilter");

        resinFilter.Properties["FilterEnabled"][0] = true;
        //resinFilter.Properties["FilterState"][0] = 4;
        resinFilter.Properties["KeyType"][0] = "IIsFilter";
        resinFilter.Properties["FilterPath"][0] = iisScripts + @"\isapi_srun.dll";
        resinFilter.Properties["FilterDescription"][0] = "isapi_srun Extension";

        PropertyValueCollection filterOrder = (PropertyValueCollection)filters.Properties["FilterLoadOrder"];
        String val = (String)filterOrder[0];

        if (!val.Contains("Resin,"))
          filterOrder[0] = "Resin," + val;

        resinFilter.CommitChanges();
        resinFilter.Close();
        filters.CommitChanges();
        filters.Close();

        CopyIsapiFilter(resinHome, iisScripts);
        return new SetupResult("IIS Installation Complete.");
      }
      catch (Exception e) {
        log.WriteEntry(e.Message + "\n" + e.StackTrace);
        return new SetupResult(e);
      }
    }

    public static void CopyIsapiFilter(String resinHome, String iisScripts)
    {
      String filterPath = iisScripts + @"\isapi_srun.dll";
      if (File.Exists(filterPath))
        File.Delete(filterPath);

      File.Copy(resinHome + @"\win32\isapi_srun.dll", filterPath);
    }

    public static void RemoveIsapiFilter(String iisScripts)
    {
      String filterPath = iisScripts + @"\isapi_srun.dll";
      File.Delete(filterPath);
    }

    public static void StopIIS()
    {
      ServiceController sc = new ServiceController("W3SVC");

      if (sc.Status == ServiceControllerStatus.Running) {
        sc.Stop();
        sc.WaitForStatus(ServiceControllerStatus.Stopped);
      }

      sc.Close();
    }

    public static SetupResult RemoveIIS(String iisScripts)
    {
      DirectoryEntry filters = new DirectoryEntry("IIS://localhost/W3SVC/Filters");
      DirectoryEntry resinFilter = null;

      foreach (DirectoryEntry entry in filters.Children) {
        if ("Resin".Equals(entry.Name)) {
          resinFilter = entry;
        }
      }

      if (resinFilter != null) {
        filters.Children.Remove(resinFilter);
      }

      PropertyValueCollection filterOrder = (PropertyValueCollection)filters.Properties["FilterLoadOrder"];
      String val = (String)filterOrder[0];

      int index = val.IndexOf("Resin,");

      if (index != -1) {
        String newVal = val.Substring(0, index) + val.Substring(index + 6, val.Length - 6 - index);
        filterOrder[0] = newVal;
      }

      filters.CommitChanges();
      filters.Close();

      try {
        String filterPath = iisScripts + @"\isapi_srun.dll";
        if (File.Exists(filterPath))
          File.Delete(filterPath);

        return new SetupResult("IIS Resin filter is removed.");
      }
      catch (Exception e) {
        log.WriteEntry(e.Message + "\n" + e.StackTrace);
        return new SetupResult(e);
      }
    }
  }
}