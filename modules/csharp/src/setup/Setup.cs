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
using System.Collections;
using System.Collections.Generic;
using System.Configuration.Install;
using System.DirectoryServices;
using System.IO;
using System.Runtime.Serialization.Formatters.Binary;
using System.ServiceProcess;
using System.Text;
using System.Windows.Forms;
using Microsoft.Win32;
using System.Diagnostics;

namespace Caucho
{
  public class Setup
  {
    static EventLog log = new EventLog();

    static Setup()
    {
      ((System.ComponentModel.ISupportInitialize)(log)).BeginInit();
      log.Log = "Application";
      log.Source = "caucho/Setup.cs";
    }

    public static String REG_SERVICES = "SYSTEM\\CurrentControlSet\\Services";

    private String _resinHome;
    private String _apacheHome;
    private Resin _resin;
    public Resin Resin
    {
      get { return _resin; }
      set
      {
        if (_resin != null)
          throw new ApplicationException();
        _resin = value;
      }
    }
    private List<Resin> _resinList = new List<Resin>();
    private List<ResinService> _resinServices = new List<ResinService>();
    private List<String> _users = null;

    private ArrayList _apacheHomeSet;

    public String ResinHome
    {
      get { return _resinHome; }
      set { _resinHome = value; }
    }

    public String ApacheHome
    {
      get { return _apacheHome; }
      set { _apacheHome = value; }
    }

    public ArrayList ApacheHomeSet
    {
      get { return _apacheHomeSet; }
    }

    public Setup()
    {
      String path = System.Reflection.Assembly.GetExecutingAssembly().Location;
      this.ResinHome = Util.GetResinHome(null, path);

      this._apacheHomeSet = new DirSet();

      FindResinServices();
      FindResin();

      Apache.FindApache(_apacheHomeSet);
    }

    public void FindResin()
    {
      DriveInfo[] drives = DriveInfo.GetDrives();
      foreach (DriveInfo drive in drives) {
        if (DriveType.Fixed != drive.DriveType && DriveType.Ram != drive.DriveType)
          continue;
        DirectoryInfo root = drive.RootDirectory;
        DirectoryInfo[] directories = root.GetDirectories();
        foreach (DirectoryInfo directory in directories) {
          if (directory.Name.StartsWith("resin", StringComparison.CurrentCultureIgnoreCase)
            && Util.IsResinHome(directory.FullName)) {
            Resin resin = new Resin(Util.Canonicalize(directory.FullName));
            if (!HasResin(resin))
              AddResin(resin);
          } else if (directory.Name.Contains("appservers")) {
            DirectoryInfo[] appserverDirectories = directory.GetDirectories();
            foreach (DirectoryInfo appserverDir in appserverDirectories) {
              if (Util.IsResinHome(appserverDir.FullName)) {
                String home = Util.Canonicalize(appserverDir.FullName);
                Resin resin = new Resin(home);
                if (!HasResin(resin))
                  AddResin(resin);
              }
            }
          }
        }
      }

      String currentResin = Util.GetCurrentResinFromRegistry();
      if (currentResin != null) {
        currentResin = Util.Canonicalize(currentResin);
        Resin resin = new Resin(currentResin);

        Resin = resin;

        if (!HasResin(resin))
          AddResin(resin);
      }

      RegistryKey services = Registry.LocalMachine.OpenSubKey(Setup.REG_SERVICES);
      foreach (String name in services.GetSubKeyNames()) {
        RegistryKey key = services.OpenSubKey(name);
        Object imagePathObj = key.GetValue("ImagePath");
        if (imagePathObj == null && !"".Equals(imagePathObj))
          continue;

        String imagePath = (String)imagePathObj;
        String lowerCaseImagePath = imagePath.ToLower();

        if (imagePath.IndexOf("resin.exe") != -1) {
          ResinArgs resinArgs = new ResinArgs(imagePath);
          Resin resin = null;
          if (resinArgs.Home != null) {
            resin = new Resin(resinArgs.Home);
          } else if (resinArgs.Exe != null) {
            String exe = resinArgs.Exe;
            String home = exe.Substring(0, exe.Length - 10);
            if (Util.IsResinHome(home))
              resin = new Resin(home);
          }

          if (resin != null && !HasResin(resin))
            AddResin(resin);
        }

        key.Close();
      }

      services.Close();

      String path = Util.Canonicalize(System.Reflection.Assembly.GetExecutingAssembly().Location);
      while (path.LastIndexOf('\\') > 0) {
        path = path.Substring(0, path.LastIndexOf('\\'));
        if (Util.IsResinHome(path)) {
          Resin resin = new Resin(path);
          if (Resin == null) {
            Resin = resin;
          }

          if (!HasResin(resin)) {
            AddResin(resin);
          }

          break;
        };
      }
    }

    public void FindResinServices()
    {
      RegistryKey services = Registry.LocalMachine.OpenSubKey(Setup.REG_SERVICES);
      foreach (String name in services.GetSubKeyNames()) {
        RegistryKey key = services.OpenSubKey(name);
        Object imagePathObj = key.GetValue("ImagePath");
        if (imagePathObj == null && !"".Equals(imagePathObj))
          continue;

        String imagePath = (String)imagePathObj;
        String lowerCaseImagePath = imagePath.ToLower();

        if ((imagePath.IndexOf("resin.exe") > 0 || imagePath.IndexOf("httpd.exe") > 0) && imagePath.IndexOf("-service") > 0) {
          ResinArgs resinArgs = new ResinArgs(imagePath);

          ResinService resin = null;
          if (resinArgs.Home != null) {
            resin = new ResinService();
            resin.Home = resinArgs.Home;
          } else if (resinArgs.Exe != null) {
            String exe = resinArgs.Exe;
            String home = exe.Substring(0, exe.Length - 10);
            if (Util.IsResinHome(home)) {
              resin = new ResinService();
              resin.Home = home;
            }
          } else {
            continue;
          }

          resin.Exe = resinArgs.Exe;

          if (resin == null)
            continue;

          resin.Name = name;
          resin.Server = resinArgs.Server;
          resin.DynamicServer = resinArgs.DynamicServer;
          resin.Root = resinArgs.Root;
          resin.Conf = resinArgs.Conf;
          resin.Log = resinArgs.Log;
          resin.User = resinArgs.User;
          resin.JavaHome = resinArgs.JavaHome;
          if (resinArgs.JmxPort != null && !"".Equals(resinArgs.JmxPort))
            resin.JmxPort = int.Parse(resinArgs.JmxPort);

          if (resinArgs.DebugPort != null && !"".Equals(resinArgs.DebugPort))
            resin.DebugPort = int.Parse(resinArgs.DebugPort);

          if (resinArgs.WatchDogPort != null && !"".Equals(resinArgs.WatchDogPort))
            resin.WatchdogPort = int.Parse(resinArgs.WatchDogPort);

          resin.IsPreview = resinArgs.IsPreview;

          resin.ExtraParams = resinArgs.ResinArguments;

          AddResinService(resin);
        }

        key.Close();
      }

      services.Close();
    }

    public ResinConf GetResinConf(String conf)
    {
      return new ResinConf(conf);
    }

    public bool HasResin(Resin resin)
    {
      return _resinList.Contains(resin);
    }

    public void AddResin(Resin resin)
    {
      _resinList.Add(resin);
    }

    public IList GetResinList()
    {
      IList result = new List<Resin>(_resinList);
      return result;
    }

    public Resin SelectResin(String home)
    {
      home = Util.Canonicalize(home);

      Resin result = null;

      foreach (Resin resin in _resinList) {
        if (home.Equals(resin.Home))
          result = resin;
      }

      if (result == null) {
        result = new Resin(home);
        AddResin(result);
      }

      return result;
    }

    public bool HasResinService(ResinService service)
    {
      return _resinServices.Contains(service);
    }

    public void AddResinService(ResinService service)
    {
      _resinServices.Add(service);
    }

    public IList<ResinService> GetResinServices(Resin resin)
    {
      IList<ResinService> result = new List<ResinService>();
      foreach (ResinService resinService in _resinServices) {
        if (resin.Home.Equals(resinService.Home))
          result.Add(resinService);
      }

      return result;
    }

    public void ResetResinServices()
    {
      _resinServices.Clear();
      FindResinServices();
    }

    public IList<ResinService> GetResinServices()
    {
      return _resinServices;
    }

    public String GetResinConfFile(Resin resin)
    {
      if (File.Exists(resin.Home + @"\conf\resin.xml"))
        return @"conf\resin.xml";
      else if (File.Exists(resin.Home + @"\conf\resin.conf"))
        return @"conf\resin.conf";
      else
        return null;
    }

    public List<String> GetUsers()
    {
      if (_users == null) {
        List<String> users = new List<String>();
        users.Add("Local Service");
        DirectoryEntry groupEntry = new DirectoryEntry("WinNT://.");
        groupEntry.Children.SchemaFilter.Add("User");
        IEnumerator e = groupEntry.Children.GetEnumerator();
        while (e.MoveNext()) {
          String name = ((DirectoryEntry)e.Current).Name;
          if (!"Guest".Equals(name))
            users.Add(name);
        }

        _users = users;
      }

      return _users;
    }

    public void InstallService(ResinService resinService, bool isNew)
    {
      if (isNew) {
        Installer installer = InitInstaller(resinService);
        Hashtable installState = new Hashtable();
        installer.Install(installState);
        StoreState(installState, resinService.Name);
      }

      RegistryKey system = Registry.LocalMachine.OpenSubKey("System");
      RegistryKey currentControlSet = system.OpenSubKey("CurrentControlSet");
      RegistryKey servicesKey = currentControlSet.OpenSubKey("Services");
      RegistryKey serviceKey = servicesKey.OpenSubKey(resinService.Name, true);
      String imagePath = (String)serviceKey.GetValue("ImagePath");
      if (imagePath.Contains(".exe\""))
        imagePath = imagePath.Substring(0, imagePath.IndexOf(".exe\"") + 5);
      else if (imagePath.Contains(".exe"))
        imagePath = imagePath.Substring(0, imagePath.IndexOf(".exe\"") + 4);

      StringBuilder builder = new StringBuilder(imagePath);
      builder.Append(' ').Append(resinService.GetServiceArgs());

      serviceKey.SetValue("ImagePath", builder.ToString());
    }

    public void UninstallService(ResinService resinService)
    {
      Hashtable state = LoadState(resinService.Name);

      Installer installer = InitInstaller(resinService);

      installer.Uninstall(state);
    }

    private Installer InitInstaller(ResinService resinService)
    {
      TransactedInstaller txInst = new TransactedInstaller();
      txInst.Context = new InstallContext(null, new String[] { });
      txInst.Context.Parameters["assemblypath"] = resinService.Exe;

      ServiceProcessInstaller spInst = new ServiceProcessInstaller();
      if (resinService.User != null) {
        spInst.Username = resinService.User;
        spInst.Password = resinService.Password;
        spInst.Account = ServiceAccount.User;
      } else {
        spInst.Account = ServiceAccount.LocalSystem;
      }

      txInst.Installers.Add(spInst);

      ServiceInstaller srvInst = new ServiceInstaller();
      srvInst.ServiceName = resinService.Name;
      srvInst.DisplayName = resinService.Name;
      srvInst.StartType = ServiceStartMode.Automatic;

      txInst.Installers.Add(srvInst);

      return txInst;
    }

    private String GetStateDirectory()
    {
      String dir = Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData) + @"\Caucho\services";
      return dir;
    }

    private void StoreState(Hashtable state, String serviceName)
    {
      String dir = GetStateDirectory();
      DirectoryInfo info;
      if ((info = Directory.CreateDirectory(dir)) != null && info.Exists) {
        String file = dir + @"\" + serviceName + ".srv";

        FileStream fs = new FileStream(file, FileMode.Create, FileAccess.Write);
        BinaryFormatter serializer = new BinaryFormatter();
        serializer.Serialize(fs, state);
        fs.Flush();
        fs.Close();
      }
    }

    private Hashtable LoadState(String serviceName)
    {
      String file = GetStateDirectory() + '\\' + serviceName + ".srv";
      if (File.Exists(file) && false) {
        Hashtable state = null;
        FileStream fs = new FileStream(file, FileMode.Open, FileAccess.Read);
        BinaryFormatter serializer = new BinaryFormatter();
        state = (Hashtable)serializer.Deserialize(fs);
        fs.Close();
        return state;
      } else {
        return FakeState();
      }
    }

    [STAThread]
    public static void Main(String[] args)
    {
      try {
        Util.TestDotNetCapability();
      } catch (Exception) {
        MessageBox.Show(".NET Version 3.5 is required.");
        Environment.Exit(1);
      }
      Application.EnableVisualStyles();
      Application.SetCompatibleTextRenderingDefault(false);
      SetupForm setupForm = new SetupForm(new Setup());
      Application.Run(setupForm);
    }

    public static Hashtable FakeState()
    {
      Hashtable state = new Hashtable();
      IDictionary[] states = new IDictionary[2];
      states[0] = new Hashtable();
      states[0]["_reserved_nestedSavedStates"] = new IDictionary[0];
      states[0]["Account"] = ServiceAccount.LocalSystem;

      states[1] = new Hashtable();
      IDictionary[] substates = new IDictionary[1];
      substates[0] = new Hashtable();
      substates[0]["_reserved_nestedSavedStates"] = new IDictionary[0];
      substates[0]["alreadyRegistered"] = false;
      substates[0]["logExists"] = true;
      substates[0]["baseInstalledAndPlatformOK"] = true;

      states[1]["_reserved_nestedSavedStates"] = substates;
      states[1]["installed"] = true;

      state["_reserved_nestedSavedStates"] = states;

      return state;
    }
  }

  class DirSet : ArrayList
  {
    public override int Add(object value)
    {
      int index = base.IndexOf(value);

      if (index != -1)
        return index;

      return base.Add(value);
    }
  }

  public class SetupResult
  {
    public static int OK = 0;
    public static int ERROR = 1;
    public static int EXCEPTION = 2;

    public SetupResult(String message)
    {
      this.Status = OK;
      this.Message = message;
    }

    public SetupResult(int status, String message)
    {
      this.Status = status;
      this.Message = message;
    }

    public SetupResult(Exception e)
    {
      this.Status = EXCEPTION;
      this.Exception = e;
    }

    public int Status { get; set; }

    public Exception Exception { get; set; }

    public String Message { get; set; }
  }

  public class Resin : IEquatable<Resin>
  {
    public String Home { get; set; }
    public String[] Servers { get; set; }

    public Resin(String home)
    {
      Home = home;
    }

    public override int GetHashCode()
    {
      return Home.GetHashCode();
    }

    public override bool Equals(object obj)
    {
      if (this == obj)
        return true;

      if (!(obj is Resin))
        return false;

      Resin resin = (Resin)obj;

      return Home.Equals(resin.Home);
    }

    #region IEquatable Members
    public bool Equals(Resin obj)
    {
      return Home.Equals(obj.Home);
    }
    #endregion

    public override string ToString()
    {
      return Home;
    }
  }

  public class ResinService : IEquatable<ResinService>, ICloneable
  {
    public String Exe { get; set; }
    public String Home { get; set; }
    public String Root { get; set; }
    public String Log { get; set; }
    public String Conf { get; set; }
    public String Name { get; set; }
    public String User { get; set; }
    public String Password { get; set; }
    public bool IsPreview { get; set; }
    public String JavaHome { get; set; }
    public String Server { get; set; }
    public String DynamicServer { get; set; }
    public int DebugPort { get; set; }
    public int JmxPort { get; set; }
    public int WatchdogPort { get; set; }
    public String ExtraParams { get; set; }

    public ResinService()
    {
      JmxPort = -1;
      DebugPort = -1;
      WatchdogPort = -1;
      IsPreview = false;
    }

    public String GetServiceArgs()
    {
      StringBuilder sb = new StringBuilder();
      sb.Append("-service -name ").Append("\"");
      sb.Append(Name).Append("\"");
      if (Conf != null)
        sb.Append(" -conf ").Append('"').Append(Conf).Append('"');

      sb.Append(" -resin-home ").Append('"').Append(Home).Append('"');

      if (Root != null)
        sb.Append(" -root-directory ").Append('"').Append(Root).Append('"');

      if (Log != null)
        sb.Append(" -log-directory ").Append('"').Append(Log).Append('"');

      if (Server != null && !"".Equals(Server))
        sb.Append(" -server ").Append(Server);
      else if (DynamicServer != null)
        sb.Append(" -dynamic-server ").Append(DynamicServer);

      if (IsPreview)
        sb.Append(" -preview");

      if (DebugPort > 0)
        sb.Append(" -debug-port ").Append(DebugPort.ToString());

      if (JmxPort > 0)
        sb.Append(" -jmx-port ").Append(JmxPort.ToString());

      if (WatchdogPort > 0)
        sb.Append(" -watchdog-port ").Append(WatchdogPort.ToString());

      if (ExtraParams != null)
        sb.Append(" ").Append(ExtraParams);

      return sb.ToString();
    }

    public override int GetHashCode()
    {
      return Name.GetHashCode();
    }

    public bool Equals(ResinService resinService)
    {
      if (this == resinService)
        return true;

      return Name.Equals(resinService);
    }

    public override String ToString()
    {
      StringBuilder result = new StringBuilder(Name);
      result.Append(" [");

      if (Server != null)
        result.Append("-server ").Append(Server);
      else if (DynamicServer != null)
        result.Append("-dynamic:").Append(DynamicServer);
      else
        result.Append("default server");

      result.Append(']');

      return result.ToString();
    }

    #region ICloneable Members

    public object Clone()
    {
      return this.MemberwiseClone();
    }

    #endregion
  }

  class StateNofFoundException : Exception
  {
  }
}
