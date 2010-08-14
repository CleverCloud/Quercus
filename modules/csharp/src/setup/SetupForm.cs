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
using System.ComponentModel;
using System.DirectoryServices.AccountManagement;
using System.IO;
using System.ServiceProcess;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace Caucho
{
  public partial class SetupForm : Form
  {
    private enum MODE { NEW, EXISTING, NONE };

    private ProgressDialog _progressDialog;
    private Setup _setup;
    private String _createService = "New Service ...";
    private ArrayList _userJdks = new ArrayList();
    private Environment.SpecialFolder _none;
    private Resin _resin;
    private ResinService _resinService;
    private ResinConf _resinConf;
    private MODE _mode;
    private String _resinRoot;
    private String _resinLog;
    private String _resinConfFile;
    private String _javaHome;

    public SetupForm(Setup setup)
    {
      _setup = setup;
      _resin = _setup.Resin;

      InitializeComponent();
      _none = _folderDlg.RootFolder;

      _resinCmbBox.BeginUpdate();
      _resinCmbBox.DataSource = _setup.GetResinList();
      _resinCmbBox.SelectedItem = _resin;
      _resinCmbBox.EndUpdate();

      UpdateServices(null);
    }

    private void SelectResinBtnClick(object sender, EventArgs e)
    {
      String resinHome = Util.GetResinHome(null, System.Reflection.Assembly.GetExecutingAssembly().Location);

      _folderDlg.Description = "Please locate your Resin installation";
      bool select = true;

      while (select) {
        if (resinHome == null || "".Equals(resinHome))
          _folderDlg.RootFolder = Environment.SpecialFolder.MyComputer;
        else
          _folderDlg.SelectedPath = resinHome;

        if (_folderDlg.ShowDialog() == DialogResult.OK) {
          resinHome = _folderDlg.SelectedPath;
          if (Util.IsResinHome(resinHome)) {
            SelectResin(resinHome);
            select = false;
          } else {
            String caption = "Incorrect Resin Home";
            String message = "Resin Home must contain lib\\resin.jar";

            if (MessageBox.Show(message, caption, MessageBoxButtons.RetryCancel, MessageBoxIcon.Error) == DialogResult.Cancel)
              select = false;
          }
        } else {
          select = false;
        }
      }
    }

    private void SelectResin(String resinHome)
    {
      _resinCmbBox.BeginUpdate();
      _resin = _setup.SelectResin(resinHome);
      _resinCmbBox.DataSource = _setup.GetResinList();
      _resinCmbBox.SelectedItem = _resin;
      _resinCmbBox.EndUpdate();

      UpdateServices(null);
    }

    private void ResinSelectectionCommitted(object sender, EventArgs e)
    {
      UpdateServices(null);
      ResetApacheInstallControls();
      ResetIISInstallControls();
    }

    private void UpdateServices(ResinService newResinService)
    {
      _resin = (Resin)_resinCmbBox.SelectedItem;

      if (_resin != null) {
        IList<ResinService> services = _setup.GetResinServices(_resin);
        ArrayList items = new ArrayList();

        items.Add(_createService);
        items.AddRange((ICollection)services);

        _servicesCmbBox.BeginUpdate();
        _servicesCmbBox.DataSource = items;

        if (services.Count == 0) {
          _servicesCmbBox.SelectedIndex = 0;
        } else if (services.Count > 0) {
          _servicesCmbBox.SelectedIndex = -1;
          StringBuilder builder = new StringBuilder("Select Service: [");
          for (int i = 0; i < services.Count; i++) {
            ResinService service = services[i];
            builder.Append(service.Name);
            if (i + 1 < services.Count)
              builder.Append(", ");
          }
          builder.Append(']');

          _servicesCmbBox.Text = builder.ToString();
        }
        _servicesCmbBox.EndUpdate();
      }
    }

    private void ServiceSelectionChanged(object sender, EventArgs e)
    {
      if (_createService.Equals(_servicesCmbBox.SelectedItem)) {
        _resinService = null;
        _mode = MODE.NEW;
      } else if (_servicesCmbBox.SelectedItem is ResinService) {
        _mode = MODE.EXISTING;
        _resinService = (ResinService)_servicesCmbBox.SelectedItem;
      } else {
        _mode = MODE.NONE;
      }
      bool enabled = MODE.NEW.Equals(_mode);
      _resinConfTxtBox.Enabled = enabled;
      _selectResinConfBtn.Enabled = enabled;
      _serverCmbBox.Enabled = enabled;
      _serviceNameTxtBox.Enabled = enabled;
      _serviceUserCmbBox.Enabled = enabled;
      _servicePassTxtBox.Enabled = enabled;
      enabled = !MODE.NONE.Equals(_mode);
      _javaHomeCmbBox.Enabled = enabled;
      _selectJavaHomeBtn.Enabled = enabled;
      _resinRootTxtBox.Enabled = enabled;
      _selectResinRootBtn.Enabled = enabled;
      _logDirTxtBox.Enabled = enabled;
      _selectLogDirBtn.Enabled = enabled;
      _previewCmbBox.Enabled = enabled;
      _jmxPortTxtBox.Enabled = enabled;
      _debugPortTxtBox.Enabled = enabled;
      _extraParamsTxbBox.Enabled = enabled;
      _watchdogPortTxtBox.Enabled = enabled;
      _serviceInstallBtn.Enabled = enabled;
      _serviceRemoveBtn.Enabled = MODE.EXISTING.Equals(_mode);

      UpdateDetails();
    }

    private void UpdateDetails()
    {
      _resinRoot = null;
      _resinLog = null;
      _resinConfFile = null;
      _resinConf = null;
      if (_resinService != null)
        _resinConfFile = _resinService.Conf;

      if (_resinConfFile == null)
        _resinConfFile = _setup.GetResinConfFile(_resin);

      if (_resinConfFile != null && !"".Equals(_resinConfFile) && !Util.IsAbsolutePath(_resinConfFile))
        _resinConfFile = _resin.Home + @"\" + _resinConfFile;

      if (_resinConfFile != null) {
        _resinConfTxtBox.Text = _resinConfFile;
        _resinConf = _setup.GetResinConf(_resinConfFile);
      } else
        _resinConfTxtBox.Text = "Please specify configuration file";

      _resinRoot = null;
      if (_resinService != null)
        _resinRoot = _resinService.Root;

      if ((_resinRoot == null || "".Equals(_resinRoot)) && _resinConf != null)
        _resinRoot = _resinConf.getRootDirectory();
      else
        _resinRootTxtBox.Text = "Please specify resin root";

      if (_resinRoot != null && !"".Equals(_resinRoot) && !Util.IsAbsolutePath(_resinRoot))
        _resinRoot = Util.GetCanonicalPath(_resin.Home + @"\" + _resinRoot);

      if (_resinRoot == null || "".Equals(_resinRoot))
        _resinRoot = _resin.Home;

      _resinRootTxtBox.Text = _resinRoot;

      _resinLog = null;
      if (_resinService != null)
        _resinLog = _resinService.Log;

      if (_resinLog == null || "".Equals(_resinLog))
        _resinLog = "log";

      if (!Util.IsAbsolutePath(_resinLog))
        _resinLog = _resinRoot + @"\log";

      _logDirTxtBox.Text = _resinLog;

      if (_resinService != null && _resinService.IsPreview)
        _previewCmbBox.SelectedItem = "Yes";
      else
        _previewCmbBox.SelectedItem = "No";

      String javaHome = null;
      if (_resinService != null)
        javaHome = _resinService.JavaHome;
      UpdateJavaHomes(javaHome);

      UpdateServers();

      UpdateJmxAndDebugPorts();
    }

    private void UpdateServers()
    {
      if (_resinService == null) {
        ArrayList servers = new ArrayList();

        if (_resinConf != null)
          servers.AddRange(_resinConf.getServers());

        ArrayList dynamicServers = new ArrayList();
        foreach (Object o in servers) {
          ResinConfServer server = (ResinConfServer)o;
          if (_resinConf.IsDynamicServerEnabled(server.Cluster)) {
            String dynamic = "dynamic:" + server.Cluster + ":127.0.0.1:6811";
            if (!dynamicServers.Contains(dynamic))
              dynamicServers.Add(dynamic);
          }
        }
        servers.AddRange(dynamicServers);
        _serverCmbBox.BeginUpdate();
        _serverCmbBox.DataSource = null;
        _serverCmbBox.DataSource = servers;
        _serverCmbBox.EndUpdate();

        if (servers.Count > 0) {
          String cluster = ((ResinConfServer)servers[0]).Cluster;
          String id = ((ResinConfServer)servers[0]).ID;
          if ("".Equals(id))
            _serviceNameTxtBox.Text = "Resin";
          else
            _serviceNameTxtBox.Text = "Resin-" + id;
        }
        _serviceUserCmbBox.DataSource = _setup.GetUsers();
      } else {
        _serviceNameTxtBox.Text = _resinService.Name;
        if (_resinService.DynamicServer != null)
          _serverCmbBox.Text = "dynamic:" + _resinService.DynamicServer;
        else if (_resinService.Server == null)
          _serverCmbBox.Text = "default";
        else
          _serverCmbBox.Text = _resinService.Server;

        _serviceUserCmbBox.DataSource = null;
      }

    }

    private void UpdateJmxAndDebugPorts()
    {
      String jmxPort = null;
      String debugPort = null;
      String watchDogPort = null;

      if (_resinService != null) {
        if (_resinService.JmxPort > 0)
          jmxPort = _resinService.JmxPort.ToString();

        if (_resinService.DebugPort > 0)
          debugPort = _resinService.DebugPort.ToString();

        if (_resinService.WatchdogPort > 0)
          watchDogPort = _resinService.WatchdogPort.ToString();
      }

      ResinConfServer server = null;
      if ((jmxPort == null || debugPort == null) && _serverCmbBox.SelectedItem is ResinConfServer)
        server = (ResinConfServer)_serverCmbBox.SelectedItem;

      if (jmxPort == null && server != null)
        jmxPort = _resinConf.GetJmxPort(server.Cluster, server.ID);

      if (jmxPort == null)
        jmxPort = "Not Specified";

      if (debugPort == null && server != null)
        debugPort = _resinConf.GetDebugPort(server.Cluster, server.ID);

      if (debugPort == null)
        debugPort = "Not Specified";

      if (watchDogPort == null && server != null)
        watchDogPort = _resinConf.GetWatchDogPort(server.Cluster, server.ID);

      if (watchDogPort == null)
        watchDogPort = "Not Specified";

      _jmxPortTxtBox.Text = jmxPort;
      _debugPortTxtBox.Text = debugPort;
      _watchdogPortTxtBox.Text = watchDogPort;
    }

    public void ResinConfFileChanged()
    {
      UpdateServers();
      UpdateJmxAndDebugPorts();
    }

    private void SelectJavaHome(object sender, EventArgs e)
    {
      _folderDlg.Description = "Please locate your java installation";
      bool select = true;
      String javaHome;
      while (select) {
        _folderDlg.RootFolder = Environment.SpecialFolder.MyComputer;

        if (_folderDlg.ShowDialog() == DialogResult.OK) {
          javaHome = _folderDlg.SelectedPath;
          if (Util.IsValidJavaHome(javaHome)) {
            _userJdks.Add(javaHome);

            UpdateJavaHomes(javaHome);
            select = false;
          } else {
            String caption = "Incorrect Java Home";
            String message = @"Java Home must contain bin\java.exe";

            if (MessageBox.Show(message, caption, MessageBoxButtons.RetryCancel, MessageBoxIcon.Error) == DialogResult.Cancel)
              select = false;
          }
        } else {
          select = false;
        }
      }
    }

    private void UpdateJavaHomes(String javaHome)
    {
      ArrayList jdks = new ArrayList();
      jdks.AddRange(_userJdks);

      IList foundJdks = Util.FindJava();

      if (foundJdks.Count > 0)
        jdks.Add("Default: [" + foundJdks[0] + "]");

      foreach (String jdk in foundJdks) {
        if (!jdks.Contains(jdk)) {
          jdks.Add(jdk);
        }
      }

      if (javaHome == null && jdks.Count > 0) {
        javaHome = jdks[0].ToString();
      }

      _javaHomeCmbBox.BeginUpdate();
      _javaHomeCmbBox.DataSource = jdks;
      if (javaHome == null)
        _javaHomeCmbBox.SelectedIndex = -1;
      else
        _javaHomeCmbBox.SelectedItem = javaHome;

      _javaHome = (String)_javaHomeCmbBox.SelectedItem;

      _javaHomeCmbBox.EndUpdate();
    }

    private void SelectResinRoot(object sender, EventArgs e)
    {
      _folderDlg.RootFolder = _none;
      if (_resin != null)
        _folderDlg.SelectedPath = _resin.Home;

      if (DialogResult.OK.Equals(_folderDlg.ShowDialog())) {
        String resinRoot = _folderDlg.SelectedPath;
        _resinRootTxtBox.Text = resinRoot;
        ResinRootTxtBoxLeaving(null, null);
      }
    }

    private void SelectResinConf(object sender, EventArgs e)
    {
      if (_resinConfFile != null) {
        int lastSlashIdx = _resinConfFile.LastIndexOf('\\');
        if (lastSlashIdx != -1) {
          _fileDlg.InitialDirectory = _resinConfFile.Substring(0, lastSlashIdx);
          _fileDlg.FileName = _resinConfFile.Substring(lastSlashIdx + 1, _resinConfFile.Length - lastSlashIdx - 1);
        }
      }

      if (DialogResult.OK.Equals(_fileDlg.ShowDialog())) {
        _resinConfFile = _fileDlg.FileName;
        _resinConfTxtBox.Text = _resinConfFile;
        _resinConf = _setup.GetResinConf(_resinConfFile);
        ResinConfFileChanged();
      }
    }

    private void ServerSelectionChanged(object sender, EventArgs e)
    {
      UpdateJmxAndDebugPorts();
    }

    private void SelectLogDirectory(object sender, EventArgs e)
    {
      _folderDlg.RootFolder = _none;
      String log = _resinLog;

      if (log != null) {
        while (!Directory.Exists(log)) {
          log = log.Substring(0, log.LastIndexOf('\\'));
        }

        _folderDlg.SelectedPath = log;
      }

      if (DialogResult.OK.Equals(_folderDlg.ShowDialog())) {
        _resinLog = _folderDlg.SelectedPath;
        _logDirTxtBox.Text = _resinLog;
      }
    }

    private void ResinCmbBoxLeaving(object sender, EventArgs e)
    {
      CheckResinHome();
      ResetApacheInstallControls();
      ResetIISInstallControls();
    }

    private bool CheckResinHome()
    {
      String resinHome = _resinCmbBox.Text;
      if (Util.IsResinHome(resinHome) && Util.FindResinExe(resinHome) != null) {
        SelectResin(resinHome);
        return true;
      } else {
        String message = @"Resin Home must contain lib\resin.jar and resin.exe or httpd.exe";
        _errorProvider.SetError(_resinCmbBox, message);

        _resinCmbBox.Focus();
        return false;
      }
    }

    private void JavaHomeCmbBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      if (_javaHomeCmbBox.Text.StartsWith("Default: [")
        && _javaHomeCmbBox.SelectionLength < _javaHomeCmbBox.Text.Length)
        e.Handled = true;

      if (e.KeyChar == 27)
        _javaHomeCmbBox.Text = _javaHomeCmbBox.SelectedItem.ToString();
    }

    private void JavaHomeCmbBoxLeaving(object sender, EventArgs e)
    {
      String javaHome = _javaHomeCmbBox.Text;
      if (javaHome.StartsWith("Default: [")) {
      } else if (Util.IsValidJavaHome(javaHome)) {
      } else {
        String message = @"Java Home must contain bin\java.exe";
        _errorProvider.SetError(_javaHomeCmbBox, message);
        _javaHomeCmbBox.Focus();
      }
    }

    private void ServicesCmbBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      e.Handled = true;
    }

    private void SetupFormClosing(object sender, FormClosingEventArgs e)
    {
      e.Cancel = false;
    }

    private void ResinRootTxtBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      if (e.KeyChar == 27) {
        _resinRootTxtBox.Text = _resinRoot;
        e.Handled = true;
      }
    }

    private void ResinRootTxtBoxLeaving(object sender, EventArgs e)
    {
      String resinRoot = null;

      if (Util.IsAbsolutePath(_resinRootTxtBox.Text)) {
        resinRoot = _resinRootTxtBox.Text;
      } else if (_resinConf.getRootDirectory() == null) {
        String path = Util.Canonicalize(_resinRootTxtBox.Text);
        resinRoot = _resin.Home + (path.StartsWith(@"\") ? "" : @"\") + path;
      }

      if (_resinLog.Equals(_resinRoot + @"\log")) {
        _resinLog = resinRoot + @"\log";
        _logDirTxtBox.Text = _resinLog;
      }

      _resinRoot = resinRoot;
      _resinRootTxtBox.Text = _resinRoot;
    }

    private void ServiceRefreshBtnClick(object sender, EventArgs e)
    {
      _setup.ResetResinServices();
      this.UpdateServices(null);
    }

    private void LogDirTxtBoxLeaving(object sender, EventArgs e)
    {
      String log = _logDirTxtBox.Text;
      if (Util.IsAbsolutePath(log))
        _resinLog = log;
      else
        _resinLog = _resinRoot + @"\" + log;
    }

    private void ResinConfTxtBoxLeaving(object sender, EventArgs e)
    {
      String resinConfFile = _resinConfTxtBox.Text;

      if (!Util.IsAbsolutePath(resinConfFile))
        resinConfFile = Util.GetCanonicalPath(_resin.Home + @"\" + resinConfFile);

      if (File.Exists(resinConfFile)) {
        _resinConfFile = resinConfFile;
        _resinConf = _setup.GetResinConf(resinConfFile);
        ResinConfFileChanged();
      } else {
        String message = String.Format("File `{0}' does not exist", resinConfFile);
        _errorProvider.SetError(_resinConfTxtBox, message);
        _resinConfTxtBox.Focus();
      }
    }

    private void ResinConfTxtBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      if (e.KeyChar == 27)
        _resinConfTxtBox.Text = _resinConfFile;
    }

    private void PreviewCmbBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      char c = e.KeyChar;
      switch (c) {
        case 'y':
        case 'Y':
        case 't':
        case 'T': {
            e.Handled = true;
            _previewCmbBox.SelectedItem = "Yes";
            break;
          }
        case 'n':
        case 'N':
        case 'f':
        case 'F': {
            _previewCmbBox.SelectedItem = "No";
            e.Handled = true;
            break;
          }
        default: {
            e.Handled = true;
            break;
          }
      }
    }

    private void PreviewCmbBoxKeyDown(object sender, KeyEventArgs e)
    {
      if (Keys.Left.Equals(e.KeyCode) && "Yes".Equals(_previewCmbBox.SelectedItem)) {
        _previewCmbBox.SelectedItem = "No";
        e.Handled = true;
      } else if (Keys.Right.Equals(e.KeyCode) && "No".Equals(_previewCmbBox.SelectedItem)) {
        _previewCmbBox.SelectedItem = "Yes";
        e.Handled = true;
      }

    }

    private bool CheckUserCredentials(String domainUser, String password)
    {
      try {
        PrincipalContext context = null;
        String userName = domainUser.Substring(domainUser.LastIndexOf('\\'));
        if (domainUser.StartsWith(@".\")) {
          context = new PrincipalContext(ContextType.Machine);
        } else {
          context = new PrincipalContext(ContextType.Domain);
        }
        return context.ValidateCredentials(userName, password);
      }
      catch (Exception e) {
        log.WriteEntry(e.Message + "\n" + e.StackTrace);
      }

      return false;
    }

    private bool CheckServiceName()
    {
      if (Util.ServiceExists(_serviceNameTxtBox.Text)) {
        String message = String.Format("Service Name `{0}' is already taken.", _serviceNameTxtBox.Text);
        _errorProvider.SetError(_serviceNameTxtBox, message);
        _serviceNameTxtBox.Focus();
        return false;
      } else {
        return true;
      }
    }

    private bool CheckConf()
    {
      if (_resinConfFile == null) {
        String message = "Please specify configuration file";
        _errorProvider.SetError(_resinConfTxtBox, message);
        _resinConfTxtBox.Focus();
        return false;
      } else {
        return true;
      }
    }

    private void ServiceInstallBtnClick(object sender, EventArgs eventArgs)
    {
      bool isNew = MODE.NEW.Equals(_mode);
      if (isNew && !CheckServiceName())
        return;

      if (isNew && !CheckConf())
        return;

      ResinService resinService = new ResinService();
      String resinExe = Util.FindResinExe(_resin.Home);
      resinService.Exe = resinExe;
      resinService.Home = _resin.Home;
      if (!_resinConfFile.Equals(_resin.Home + @"\" + _setup.GetResinConfFile(_resin)))
        resinService.Conf = _resinConfFile;
      if (!_javaHomeCmbBox.Text.StartsWith("Default: ["))
        resinService.JavaHome = _javaHome;

      if (!_resin.Home.Equals(_resinRoot))
        resinService.Root = _resinRoot;

      if (!_resinLog.Equals(_resinRoot + @"\log"))
        resinService.Log = _resinLog;

      resinService.Name = _serviceNameTxtBox.Text;
      if ("Local Service".Equals(_serviceUserCmbBox.Text)) {
      } else if (_resinService == null && ("".Equals(_servicePassTxtBox.Text) || _servicePassTxtBox.Text == null)) {
        MessageBox.Show("Service Password is required", "Missing Password", MessageBoxButtons.OK, MessageBoxIcon.Error);
        _servicePassTxtBox.Focus();
        return;
      } else {
        String user = _serviceUserCmbBox.Text;
        if (!(user.StartsWith(@".\") || user.StartsWith("\\")))
          user = @".\" + user;
        String password = _servicePassTxtBox.Text;
        resinService.User = user;
        resinService.Password = password;
      }

      resinService.IsPreview = "Yes".Equals(_previewCmbBox.Text);

      ResinConfServer server = null;
      if (_serverCmbBox.SelectedItem is ResinConfServer)
        server = (ResinConfServer)_serverCmbBox.SelectedItem;

      String cluster = "";
      String serverId = "";
      if (server != null) {
        cluster = server.Cluster;
        serverId = server.ID;
        resinService.Server = server.ID;
      } else {
        ResinConfServer dynamicServer = ResinConf.ParseDynamic(_serverCmbBox.Text);

        resinService.DynamicServer = dynamicServer.Cluster + ":" + dynamicServer.Address + ":" + dynamicServer.Port;
      }

      if (!"Not Specified".Equals(_jmxPortTxtBox.Text)) {
        String jmxPort = _resinConf.GetJmxPort(cluster, serverId);
        if (!_jmxPortTxtBox.Text.Equals(jmxPort))
          resinService.JmxPort = int.Parse(_jmxPortTxtBox.Text);
      }

      if (!"Not Specified".Equals(_debugPortTxtBox.Text)) {
        String debugPort = _resinConf.GetDebugPort(cluster, serverId);
        if (!_debugPortTxtBox.Text.Equals(debugPort))
          resinService.DebugPort = int.Parse(_debugPortTxtBox.Text);
      }

      if (!"Not Specified".Equals(_watchdogPortTxtBox.Text)) {
        String watchDogPort = _resinConf.GetWatchDogPort(cluster, serverId);
        if (!_watchdogPortTxtBox.Text.Equals(watchDogPort))
          resinService.WatchdogPort = int.Parse(_watchdogPortTxtBox.Text);
      }

      resinService.ExtraParams = _extraParamsTxbBox.Text;
      String checkUser = null;
      bool success = false;
      BackgroundWorker worker = new BackgroundWorker();
      worker.DoWork += delegate(object delegateSender, DoWorkEventArgs delegateEvent)
      {
        try {
          while (_progressDialog == null || !_progressDialog.Visible)
            Thread.Sleep(10);
          if (isNew) {
            ProgressDialogAddStatus("Starting installation ...");
            ProgressDialogAddStatus("Checking user ...");
          } else {
            ProgressDialogAddStatus(String.Format("Updating service `{0}'", _resinService.Name));
          }
          if (_resinService == null && resinService.User != null && !CheckUserCredentials(resinService.User, resinService.Password)) {
            checkUser = String.Format("User {0} failed to authenticate.\nPlease check user name and password", resinService.User);
            ProgressDialogError(checkUser);

            return;
          }
          _setup.InstallService(resinService, isNew);

          String message = null;
          if (isNew)
            message = String.Format("Service `{0}' is installed.", resinService.Name);
          else
            message = String.Format("Service `{0}' is updated.", resinService.Name);

          ProgressDialogSuccess(message, true);
          success = true;
        }
        catch (Exception e) {
          log.WriteEntry(e.Message + "\n" + e.StackTrace);
          String message = String.Format("Service installation failed due to message: {0}", e.Message);
          ProgressDialogError(message);
        }
      };
      worker.RunWorkerAsync();
      if (isNew)
        ProgressDialogDisplay("Installing Service " + resinService.Name, "Progress: ");
      else
        ProgressDialogDisplay("Updating Service " + resinService.Name, "Progress: ");

      if (success) {
        _setup.ResetResinServices();
        UpdateServices(null);
      } else if (checkUser != null) {
        _errorProvider.SetError(_serviceUserCmbBox, checkUser);
        _serviceUserCmbBox.Focus();
      }
    }

    private void ProgressDialogDisplay(String title, String message)
    {
      if (_progressDialog == null) {
        _progressDialog = new ProgressDialog();
        _progressDialog.Icon = this.Icon;
      }
      _progressDialog.Text = title;
      _progressDialog.Message = message;
      _progressDialog.ShowDialog(this);
    }

    private void ProgressDialogAddStatus(String status)
    {
      _progressDialog.UpdateStatus(status);
    }

    private void ProgressDialogSuccess(String message, bool resetStatus)
    {
      _progressDialog.SetSuccess(message, resetStatus);
    }

    private void ProgressDialogError(String error)
    {
      _progressDialog.SetError(error);
    }

    private void ServiceRemoveBtnClick(object sender, EventArgs eventArgs)
    {
      if (_resinService != null) {
        BackgroundWorker worker = new BackgroundWorker();
        worker.DoWork += delegate(object delegateSender, DoWorkEventArgs delegateEvent)
        {
          ServiceController serviceController = null;
          try {
            while (_progressDialog == null || !_progressDialog.Visible)
              Thread.Sleep(10);

            serviceController = new ServiceController(_resinService.Name);

            if (serviceController.Status == ServiceControllerStatus.Running) {
              String status = String.Format("Stopping Service `{0}' ...", _resinService.Name);
              ProgressDialogAddStatus(status);
              serviceController.Stop();
              serviceController.WaitForStatus(ServiceControllerStatus.Stopped);
            }
          }
          catch (Exception e) {
            log.WriteEntry(e.Message + "\n" + e.StackTrace);
          }
          finally {
            if (serviceController != null)
              serviceController.Close();
          }

          try {
            String message = String.Format("Removing Service `{0}' ...", _resinService.Name);
            ProgressDialogAddStatus(message);
            _setup.UninstallService(_resinService);
            message = String.Format("Service `{0}' is removed", _resinService.Name);
            ProgressDialogSuccess(message, true);
          }
          catch (Exception e) {
            log.WriteEntry(e.Message + "\n" + e.StackTrace);
            String error = String.Format("Failed to remove service `{0}' due to message `{1}'", _resinService.Name, e.Message);
            ProgressDialogError(error);
          }
        };
        worker.RunWorkerAsync();
        ProgressDialogDisplay("Removing Service: " + _resinService.Name, "Progress: ");
        _setup.ResetResinServices();
        UpdateServices(null);
      }
    }

    private void ServiceNameTxtBoxLeaving(object sender, EventArgs e)
    {
      CheckServiceName();
    }

    private void JavaHomeCmbBoxTextChanged(object sender, EventArgs e)
    {
      _errorProvider.SetError(_javaHomeCmbBox, null);
    }

    private void ResinCmbBoxTextChanged(object sender, EventArgs e)
    {
      _errorProvider.SetError(_resinCmbBox, null);
      if (_resinCmbBox.SelectedItem == null) {
        _installApacheBtn.Enabled = false;
        _installIISBtn.Enabled = false;
        _removeApacheBtn.Enabled = false;
        _removeIISBtn.Enabled = false;
      }
    }

    private void ResinConfTxtBoxTextChanged(object sender, EventArgs e)
    {
      _errorProvider.SetError(_resinConfTxtBox, null);
    }

    private void ServiceNameTxtBoxTextChanged(object sender, EventArgs e)
    {
      _errorProvider.SetError(_serviceNameTxtBox, null);
    }

    private void ServiceUserCmbBoxSelectionChangeCommitted(object sender, EventArgs e)
    {
      _servicePassTxtBox.Focus();
    }

    private void ServiceUserCmbBoxTextChanged(object sender, EventArgs e)
    {
      _errorProvider.SetError(_serviceUserCmbBox, null);
    }

    private void PluginsTabEnter(object sender, EventArgs e)
    {
      if (_apacheCmbBox.DataSource == null) {
        ArrayList homes = new ArrayList();
        Apache.FindApache(homes);
        _apacheCmbBox.DataSource = homes;
      }
      ResetApacheInstallControls();
      if (_iisScriptsTxtBox.Text == null || "".Equals(_iisScriptsTxtBox.Text))
        _iisScriptsTxtBox.Text = IIS.FindIIS();
      ResetIISInstallControls();
    }

    private void SelectApacheBtnClick(object sender, EventArgs e)
    {
      bool select = true;
      while (select) {
        _folderDlg.RootFolder = Environment.SpecialFolder.MyComputer;
        _folderDlg.Description = "Please locate your Apache installation";
        if (_folderDlg.ShowDialog() == DialogResult.OK) {
          String apacheHome = _folderDlg.SelectedPath;
          if (Apache.IsValidApacheHome(apacheHome)) {
            AddApacheHome(apacheHome);
            select = false;
          } else {
            String caption = "Incorrect Apache Home";
            String message = @"Apache Home must contain conf\httpd.conf";
            if (MessageBox.Show(message, caption, MessageBoxButtons.RetryCancel, MessageBoxIcon.Error) == DialogResult.Cancel)
              select = false;
          }
        } else {
          select = false;
        }
      }
    }

    private void InstallApacheBtnClick(object sender, EventArgs eventArgs)
    {
      String resinHome = _resinCmbBox.Text;
      String apacheHome = _apacheCmbBox.Text;
      bool success = false;

      BackgroundWorker worker = new BackgroundWorker();
      worker.DoWork += delegate
      {
        while (_progressDialog == null || !_progressDialog.Visible)
          Thread.Sleep(10);
        ProgressDialogAddStatus(String.Format("Adding Resin Apache Module to `{0}'..", apacheHome));
        SetupResult setupResult = Apache.SetupApache(resinHome, apacheHome);
        if (SetupResult.OK.Equals(setupResult.Status)) {
          ProgressDialogAddStatus(setupResult.Message);
          ServiceController serviceController = null;
          String apacheService = Apache.FindApacheServiceName(apacheHome);

          if (apacheService != null) {
            try {
              serviceController = new ServiceController(apacheService);
              if (serviceController.Status == ServiceControllerStatus.Running) {
                ProgressDialogAddStatus(String.Format("Restarting Service `{0}' ...", apacheService));
                serviceController.Stop();
                serviceController.WaitForStatus(ServiceControllerStatus.Stopped);
                ProgressDialogAddStatus(String.Format("Service `{0}' stopped...", apacheService));
                ProgressDialogAddStatus(String.Format("Starting Service `{0}' ...", apacheService));
                serviceController.Start();
                serviceController.WaitForStatus(ServiceControllerStatus.Running, new TimeSpan(0, 0, 30));
                if (serviceController.Status == ServiceControllerStatus.Running)
                  ProgressDialogAddStatus(String.Format("Service `{0}' started.", apacheService));
                else
                  ProgressDialogAddStatus(String.Format("Service `{0}' did not restart in 30 seconds time frame.\nPlease check event log for error messages from service {0}.", apacheService));
              }
            }
            catch (Exception e) {
              log.WriteEntry(e.Message + "\n" + e.StackTrace);
              ProgressDialogAddStatus(String.Format("Could not restart apache service `{0}'. Please restart manually.", apacheService));
            }
            finally {
              if (serviceController != null)
                serviceController.Close();
            }
          }
          ProgressDialogSuccess("Done.", false);
          success = true;
        } else if (SetupResult.ERROR.Equals(setupResult.Status)) {
          ProgressDialogError(setupResult.Message);
        } else if (SetupResult.EXCEPTION.Equals(setupResult.Status)) {
          ProgressDialogError(setupResult.Exception + "\n" + setupResult.Exception.StackTrace);
        }
      };
      worker.RunWorkerAsync();
      ProgressDialogDisplay(String.Format("Configuring", apacheHome), "Progress: ");

      _installApacheBtn.Enabled = !success;
      _removeApacheBtn.Enabled = success;
    }

    private void ApacheCmbBoxLeave(object sender, EventArgs e)
    {
      String apacheHome = _apacheCmbBox.Text;
      if (!CheckApacheHome())
        return;
      AddApacheHome(apacheHome);
      ResetApacheInstallControls();
    }

    private void AddApacheHome(String apacheHome)
    {
      ArrayList homes = (ArrayList)_apacheCmbBox.DataSource;
      if (homes.Contains(apacheHome)) {
        _apacheCmbBox.SelectedItem = apacheHome;
        return;
      }

      ArrayList newHomes = new ArrayList(homes);
      newHomes.Add(apacheHome);
      _apacheCmbBox.BeginUpdate();
      _apacheCmbBox.DataSource = null;
      _apacheCmbBox.DataSource = newHomes;
      _apacheCmbBox.SelectedItem = apacheHome;
      _apacheCmbBox.EndUpdate();
    }

    private bool CheckApacheHome()
    {
      String apacheHome = _apacheCmbBox.Text;
      if (Apache.IsValidApacheHome(apacheHome)) {
        return true;
      } else {
        String message = @"Apache Home must contain conf\httpd.conf"; ;
        _errorProvider.SetError(_apacheCmbBox, message);

        _apacheCmbBox.Focus();
        return false;
      }
    }

    private void RemoveApacheBtnClick(object sender, EventArgs eventArgs)
    {
      String resinHome = _resinCmbBox.Text;
      String apacheHome = _apacheCmbBox.Text;
      bool success = false;

      BackgroundWorker worker = new BackgroundWorker();
      worker.DoWork += delegate
      {
        while (_progressDialog == null || !_progressDialog.Visible)
          Thread.Sleep(10);
        ProgressDialogAddStatus(String.Format("Removing Resin Apache Module configuration from `{0}'...", apacheHome));
        SetupResult setupResult = Apache.RemoveApache(apacheHome);
        if (SetupResult.OK.Equals(setupResult.Status)) {
          ProgressDialogAddStatus(setupResult.Message);
          ServiceController serviceController = null;
          String apacheService = Apache.FindApacheServiceName(apacheHome);

          if (apacheService != null) {
            try {
              serviceController = new ServiceController(apacheService);
              if (serviceController.Status == ServiceControllerStatus.Running) {
                ProgressDialogAddStatus(String.Format("Restarting Service `{0}' ...", apacheService));
                serviceController.Stop();
                serviceController.WaitForStatus(ServiceControllerStatus.Stopped);
                ProgressDialogAddStatus(String.Format("Service `{0}' stopped...", apacheService));
                ProgressDialogAddStatus(String.Format("Starting Service `{0}' ...", apacheService));
                serviceController.Start();
                serviceController.WaitForStatus(ServiceControllerStatus.Running);
                ProgressDialogAddStatus(String.Format("Service `{0}' started.", apacheService));
              }
            }
            catch (Exception e) {
              log.WriteEntry(e.Message + "\n" + e.StackTrace);
              ProgressDialogAddStatus(String.Format("Could not restart apache service `{0}'. Please restart manually.", apacheService));
            }
            finally {
              if (serviceController != null)
                serviceController.Close();
            }
          }
          ProgressDialogSuccess("Done.", false);
          success = true;
        } else if (SetupResult.ERROR.Equals(setupResult.Status)) {
          ProgressDialogError(setupResult.Message);
        } else if (SetupResult.EXCEPTION.Equals(setupResult.Status)) {
          ProgressDialogError(setupResult.Exception + "\n" + setupResult.Exception.StackTrace);
        }
      };
      worker.RunWorkerAsync();
      ProgressDialogDisplay(String.Format("Configuring", apacheHome), "Progress: ");

      _installApacheBtn.Enabled = success;
      _removeApacheBtn.Enabled = !success;
    }

    private void ApacheCmbBoxTextChanged(object sender, EventArgs e)
    {
      _errorProvider.SetError(_apacheCmbBox, null);
      if (_apacheCmbBox.SelectedItem == null) {
        _installApacheBtn.Enabled = false;
        _removeApacheBtn.Enabled = false;
      }
    }

    private void ApacheCmbBoxSelectionChangeCommitted(object sender, EventArgs e)
    {
      ResetApacheInstallControls();
    }

    private void ResetApacheInstallControls()
    {
      if (!(_apacheCmbBox.SelectedItem is String))
        return;

      String apacheHome = (String)_apacheCmbBox.SelectedItem;
      bool resinValid = (_resin != null) && Util.IsResinHome(_resin.Home) && Util.HasWinDirs(_resin.Home);
      bool configured = Apache.IsConfigured(apacheHome);
      _installApacheBtn.Enabled = resinValid && !configured;
      _removeApacheBtn.Enabled = resinValid && configured;
    }

    private void ResetIISInstallControls()
    {
      bool resinValid = (_resin != null) && Util.IsResinHome(_resin.Home) && Util.HasWinDirs(_resin.Home);
      bool enabled = resinValid && _iisScriptsTxtBox.Text != null && !"".Equals(_iisScriptsTxtBox.Text);
      _installIISBtn.Enabled = enabled;
      _removeIISBtn.Enabled = enabled;
    }

    private void SelectIISBtnClick(object sender, EventArgs e)
    {
      _folderDlg.RootFolder = Environment.SpecialFolder.MyComputer;
      _folderDlg.Description = "Please locate your IIS Script directory";
      if (_folderDlg.ShowDialog() == DialogResult.OK) {
        _iisScriptsTxtBox.Text = _folderDlg.SelectedPath;

      }
    }
    private void InstallIISBtnClick(object sender, EventArgs eventArgs)
    {
      InstallOrRemoveIIS(true);
    }

    private void RemoveIISBtnClick(object sender, EventArgs e)
    {
      InstallOrRemoveIIS(false);
    }

    private void InstallOrRemoveIIS(bool install)
    {
      String resinHome = _resinCmbBox.Text;
      String iisScripts = _iisScriptsTxtBox.Text;
      bool success = false;

      BackgroundWorker worker = new BackgroundWorker();
      worker.DoWork += delegate
      {
        while (_progressDialog == null || !_progressDialog.Visible)
          Thread.Sleep(10);

        if (install)
          ProgressDialogAddStatus(String.Format("Adding IIS Resin ISAPI Filter to `{0}'..", iisScripts));
        else
          ProgressDialogAddStatus(String.Format("Removing IIS Resin ISAPI Filter `{0}'..", iisScripts));

        ServiceController serviceController = null;
        bool serviceStopped = false;
        try {
          serviceController = new ServiceController("W3SVC");
          if (serviceController.Status == ServiceControllerStatus.Running) {
            ProgressDialogAddStatus(String.Format("Stopping Service `{0}' ...", serviceController.ServiceName));
            serviceController.Stop();
            serviceController.WaitForStatus(ServiceControllerStatus.Stopped, new TimeSpan(0, 1, 0)); //wait a minute
            serviceStopped = serviceController.Status == ServiceControllerStatus.Stopped;
            if (serviceStopped) {
              ProgressDialogAddStatus(String.Format("Service `{0}' stopped...", serviceController.ServiceName));
            } else {
              ProgressDialogAddStatus(String.Format("Unable to stop service `{0}'. Please stop service manually and repeat installation", serviceController.ServiceName));
              return;
            }
          }

          SetupResult setupResult = null;

          if (install) {
            setupResult = IIS.SetupIIS(resinHome, iisScripts);
            if (SetupResult.OK == setupResult.Status) {
              ProgressDialogAddStatus("Resin ISAPI Filter is installed.");
              success = true;
            } else if (SetupResult.ERROR == setupResult.Status) {
              ProgressDialogAddStatus("Unable to install Resin ISAPI Filter: " + setupResult.Message);
              ProgressDialogAddStatus(setupResult.Message);
            } else {
              ProgressDialogAddStatus(String.Format("Unable to install Resin ISAPI Filter due to exception `{0}'", setupResult.Exception));
            }
          } else {
            setupResult = IIS.RemoveIIS(iisScripts);
            if (SetupResult.OK == setupResult.Status) {
              success = true;
              ProgressDialogAddStatus("Resin ISAPI Filter is uninstalled.");
            } else if (SetupResult.ERROR == setupResult.Status) {
              ProgressDialogAddStatus("Unable to removeResin ISAPI Filter: " + setupResult.Message);
              ProgressDialogAddStatus(setupResult.Message);
            } else {
              ProgressDialogAddStatus(String.Format("Unable to remove Resin ISAPI Filter due to exception `{0}'", setupResult.Exception));
            }
          }

          if (serviceStopped) {
            serviceController.Start();
            ProgressDialogAddStatus("Starting IIS...");
            serviceController.WaitForStatus(ServiceControllerStatus.Running, new TimeSpan(0, 1, 0));//wait a minute
            if (serviceController.Status == ServiceControllerStatus.Running)
              ProgressDialogAddStatus("IIS Started");
            else
              ProgressDialogAddStatus("IIS did not start in a given timeframe. Please check IIS event log and restart IIS manually.");
          }

          if (install) {
            if (SetupResult.OK == setupResult.Status) {
              ProgressDialogSuccess("Installation was successful.", false);
            } else {
              ProgressDialogSuccess("Installation failed.", false);
            }
          } else {
            if (SetupResult.OK == setupResult.Status) {
              ProgressDialogSuccess("Uninstallation was successful.", false);
            } else {
              ProgressDialogSuccess("Uninstallation failed.", false);
            }
          }
        }
        catch (Exception e) {
          ProgressDialogAddStatus(String.Format("Unable to stop service `{0}' due to exception `{1}'. Please stop service manually and repeat installation", serviceController.ServiceName, e.Message + "\n" + e.StackTrace));
        }
      };
      worker.RunWorkerAsync();
      ProgressDialogDisplay("Configuring IIS", "Progress: ");

      _installIISBtn.Enabled = !install;
      _removeIISBtn.Enabled = success && install;
    }

    private void IisScriptsTxtBoxTextChanged(object sender, EventArgs e)
    {
      ResetIISInstallControls();
    }

    private void ServerCmbBoxLeave(object sender, EventArgs eventArgs)
    {
      if (_resinConfFile == null)
        return;

      String server = null;
      if (_serverCmbBox.SelectedItem is String)
        server = (String)_serverCmbBox.SelectedItem;

      if (server == null)
        server = _serverCmbBox.Text;

      if (server == null || "".Equals(server)) {
        _errorProvider.SetError(_serverCmbBox, "Please select server.");
        _serverCmbBox.Focus();
        return;
      }

      if (_serverCmbBox.Text.StartsWith("dynamic")) {
        try {
          ResinConfServer r = ResinConf.ParseDynamic(_serverCmbBox.Text);
        }
        catch (Exception) {
          _errorProvider.SetError(_serverCmbBox, "Invalid dynamic server format");
          _serverCmbBox.SelectedIndex = ((ArrayList)_serverCmbBox.DataSource).Count - 1;
          _serverCmbBox.Focus();
        }
      }
    }
  }
}
