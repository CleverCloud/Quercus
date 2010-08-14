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
using System.Reflection;
using System.Collections;
using System.Text;
using System.IO;
using Microsoft.Win32;
using System.Diagnostics;
using System.Windows.Forms;
using System.ServiceProcess;
using System.Threading;
using System.Runtime.Serialization.Formatters.Binary;
using System.Security.Principal;

namespace Caucho
{
  public class Resin : ServiceBase
  {
    private static String HKEY_JRE = @"Software\JavaSoft\Java Runtime Environment";
    private static String HKEY_JDK = @"Software\JavaSoft\Java Development Kit";
    private static String CAUCHO_APP_DATA = @"Caucho Technology\Resin";

    private String _javaExe;
    private String _javaHome;
    private String _cp;
    private String _resinHome;
    private String _rootDirectory;
    private String _displayName;
    private String _resinDataDir;

    private Process _process;
    private ResinArgs ResinArgs;

    private Resin(String[] args)
    {
      ResinArgs = new ResinArgs(args);

      ServiceName = ResinArgs.ServiceName;
      if (ServiceName == null)
        ServiceName = "Resin";

      _displayName = "Resin Web Server";
    }

    public bool StartResin()
    {
      try {
        if (ResinArgs.IsService)
          ExecuteJava("start");
        else
          ExecuteJava(ResinArgs.Command);

        return true;
      } catch (ResinServiceException e) {
        throw e;
      } catch (Exception e) {
        StringBuilder message = new StringBuilder("Unable to start application. Make sure java is in your path. Use option -verbose for more detail.\n");
        message.Append(e.ToString());

        Info(message.ToString());

        return false;
      }
    }

    public void StopResin()
    {
      if (ResinArgs.IsService) {
        Info("Stopping Resin");
        ExecuteJava("stop");
      }
    }

    private int Execute()
    {
      if (ResinArgs.IsHelp) {
        Usage(ServiceName);

        return 0;
      }

      _resinHome = Util.GetResinHome(_resinHome, System.Reflection.Assembly.GetExecutingAssembly().Location);

      if (_resinHome == null) {
        Error("Can't find RESIN_HOME", null);

        return 1;
      }

      if (_rootDirectory == null)
        _rootDirectory = _resinHome;

      _javaHome = GetJavaHome(_resinHome, _javaHome);

      _cp = GetClasspath(_cp, _resinHome, _javaHome, ResinArgs.EnvClassPath);

      if (_javaExe == null && _javaHome != null)
        _javaExe = GetJavaExe(_javaHome);

      if (_javaExe == null)
        _javaExe = "java.exe";

      System.Environment.SetEnvironmentVariable("JAVA_HOME", _javaHome);

      try {
        Directory.SetCurrentDirectory(_rootDirectory);
      } catch (Exception e) {
        Error(String.Format("Can't change dir to {0} due to: {1}", _rootDirectory, e), e);

        return 1;
      }

      Environment.SetEnvironmentVariable("CLASSPATH", _cp);
      Environment.SetEnvironmentVariable("PATH",
                                         String.Format("{0};{1};\\openssl\\bin;.",
                                                       _javaHome + "\\bin",
                                                       Environment.GetEnvironmentVariable("PATH")));

      _resinDataDir = System.Reflection.Assembly.GetExecutingAssembly().Location;
      _resinDataDir = _resinDataDir.Substring(0, _resinDataDir.LastIndexOf('\\')) + "\\resin-data";

      if (!Directory.Exists(_resinDataDir))
        Directory.CreateDirectory(_resinDataDir);

      if (ResinArgs.IsService) {
        ServiceBase.Run(new ServiceBase[] { this });

        return 0;
      } else {
        if (StartResin()) {
          Join();
          if (_process != null) {
            int exitCode = _process.ExitCode;
            _process.Dispose();
            return exitCode;
          }
        }

        return 0;
      }
    }

    private static String GetResinAppDataDir()
    {
      return Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData) + '\\' + CAUCHO_APP_DATA;
    }

    private void ExecuteJava(String command)
    {
      if (ResinArgs.IsVerbose) {
        StringBuilder info = new StringBuilder();

        info.Append("java        : ").Append(_javaExe).Append('\n');
        info.Append("JAVA_HOME   : ").Append(_javaHome).Append('\n');
        info.Append("RESIN_HOME  : ").Append(_resinHome).Append('\n');
        info.Append("SERVER_ROOT : ").Append(_rootDirectory).Append('\n');
        info.Append("CLASSPATH   : ").Append(_cp).Append('\n');
        info.Append("PATH        : ").Append(Environment.GetEnvironmentVariable("PATH"));

        Info(info.ToString());
      }

      ProcessStartInfo startInfo = new ProcessStartInfo();
      startInfo.FileName = _javaExe;

      StringBuilder arguments = new StringBuilder(ResinArgs.JvmArgs).Append(' ');

      if (ResinArgs.IsNoJit)
        arguments.Append("-Djava.compiler=NONE ");

      arguments.Append("-Xrs -jar ");
      arguments.Append("\"" + _resinHome + "\\lib\\resin.jar\" ");
      arguments.Append("-resin-home \"").Append(_resinHome).Append("\" ");
      arguments.Append("-root-directory \"").Append(_rootDirectory).Append("\" ");
      if (ResinArgs.Server != null)
        arguments.Append("-server ").Append(ResinArgs.Server).Append(' ');
      else if (ResinArgs.DynamicServer != null)
        arguments.Append("-dynamic-server ").Append(ResinArgs.DynamicServer).Append(' ');

      arguments.Append(ResinArgs.ResinArguments).Append(' ');

      if (command != null)
        arguments.Append(command);

      startInfo.Arguments = arguments.ToString();

      if (ResinArgs.IsVerbose)
        Info("Using Command Line: " + _javaExe + ' ' + startInfo.Arguments);

      startInfo.UseShellExecute = false;
      startInfo.WorkingDirectory = _rootDirectory;

      if (ResinArgs.IsService) {
        startInfo.RedirectStandardError = true;
        startInfo.RedirectStandardOutput = true;

        Process process = null;
        try {
          process = Process.Start(startInfo);
        } catch (Exception e) {
          Error(e.Message, e);

          return;
        }

        StringBuilder error = new StringBuilder();
        StringBuilder output = new StringBuilder();
        process.ErrorDataReceived += delegate(Object sendingProcess, DataReceivedEventArgs err)
        {
          error.Append(err.Data).Append('\n');
        };
        process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs err)
        {
          output.Append(err.Data).Append('\n');
        };
        process.BeginErrorReadLine();
        process.BeginOutputReadLine();

        while (!process.HasExited)
          process.WaitForExit(500);

        process.CancelErrorRead();
        process.CancelOutputRead();

        if (process.HasExited && process.ExitCode != 0) {
          StringBuilder messageBuilder = new StringBuilder("Error Executing Resin Using: ");
          messageBuilder.Append(startInfo.FileName).Append(' ').Append(startInfo.Arguments);

          if (output.Length > 0)
            messageBuilder.Append('\n').Append(output);

          if (error.Length > 0)
            messageBuilder.Append('\n').Append(error);

          String message = messageBuilder.ToString();

          Info(message, true);

          throw new ResinServiceException(message);
        }
      } else {
        _process = Process.Start(startInfo);
      }
    }

    protected override void OnStart(string[] args)
    {
      StartResin();
      base.OnStart(args);
    }

    protected override void OnStop()
    {
      StopResin();
      base.OnStop();
    }

    private void Join()
    {
      if (_process != null && !_process.HasExited)
        _process.WaitForExit();
    }

    public void Error(String message, Exception e)
    {
      Error(message, e, null);
    }

    public void Error(String message, Exception e, TextWriter writer)
    {
      StringBuilder data = new StringBuilder(message);

      if (e != null)
        data.Append('\n').Append(e.ToString());

      if (writer != null)
        writer.WriteLine(data.ToString());
      else if (ResinArgs.IsService && EventLog != null) {
        EventLog.WriteEntry(this.ServiceName, data.ToString(), EventLogEntryType.Error);
      } else
        Console.WriteLine(data.ToString());
    }

    private void Info(String message)
    {
      Info(message, null, true);
    }

    private void Info(String message, bool newLine)
    {
      Info(message, null, newLine);
    }

    private void Info(String message, TextWriter writer, bool newLine)
    {
      if (writer != null && newLine)
        writer.WriteLine(message);
      else if (writer != null && !newLine)
        writer.Write(message);
      else if (ResinArgs.IsService && EventLog != null) {
        EventLog.WriteEntry(this.ServiceName, message, EventLogEntryType.Information);
      } else if (newLine)
        Console.WriteLine(message);
      else
        Console.Write(message);
    }

    public static int Main(String[] args)
    {
      Resin resin = new Resin(Environment.GetCommandLineArgs());
      if (resin.ResinArgs.IsServiceCommand()) {
        resin.Info("Please use setup.exe to install or remove the service.");
        return -1;
      } else if (!resin.ResinArgs.IsValid()) {
        resin.Usage(resin.ServiceName);
        return -1;
      } else {
        return resin.Execute();
      }
    }

    private static String GetJavaExe(String javaHome)
    {
      if (File.Exists(javaHome + @"\bin\java.exe"))
        return javaHome + @"\bin\java.exe";
      else if (File.Exists(javaHome + @"\jrockit.exe"))
        return javaHome + @"\jrockit.exe";
      else
        return null;
    }

    private static String GetClasspath(String cp, String resinHome, String javaHome, String envCp)
    {
      StringBuilder buffer = new StringBuilder();

      if (cp != null && !"".Equals(cp))
        buffer.Append(cp).Append(';');

      if (javaHome != null) {

        if (File.Exists(javaHome + @"\lib\tools.jar"))
          buffer.Append(javaHome + @"\lib\tools.jar;");

        if (File.Exists(javaHome + @"\jre\lib\rt.jar"))
          buffer.Append(javaHome + @"\jre\lib\rt.jar;");
      }

      //add zip files ommitted.

      if (envCp != null && !"".Equals(envCp)) {
        buffer.Append(envCp);
      }

      return buffer.ToString();
    }

    private static String FindJdkInRegistry(String key)
    {
      RegistryKey regKey
        = Registry.LocalMachine.OpenSubKey(key);

      if (regKey == null)
        return null;

      RegistryKey java = regKey.OpenSubKey("CurrentVersion");

      if (java == null)
        java = regKey.OpenSubKey("1.6");

      if (java == null)
        java = regKey.OpenSubKey("1.5");

      if (java == null)
        return null;

      String result = java.GetValue("JavaHome").ToString();

      java.Close();
      regKey.Close();

      return result;
    }

    private static String FindPath(String exe)
    {
      String[] paths = Environment.GetEnvironmentVariable("PATH").Split(';');

      foreach (String path in paths) {
        String testPath;

        if (path.ToLower().EndsWith("bin") && File.Exists(testPath = path + "\\java.exe"))
          return testPath;
      }

      return null;
    }

    private static String GetJavaHome(String resinHome, String javaHome)
    {
      String path = null;

      if (javaHome != null) {
      } else if (Environment.GetEnvironmentVariable("JAVA_HOME") != null &&
                 !"".Equals(Environment.GetEnvironmentVariable("JAVA_HOME"))) {
        javaHome = Environment.GetEnvironmentVariable("JAVA_HOME").Replace('/', '\\');
      } else if ((javaHome = FindJdkInRegistry(HKEY_JDK)) != null) {
      } else if ((javaHome = FindJdkInRegistry(HKEY_JRE)) != null) {
      } else if (File.Exists(resinHome + "\\jre\\bin\\java.exe")) {
        javaHome = resinHome + "\\jre";
      } else if ((path = FindPath("java.exe")) != null) {
        javaHome = Util.GetParent(path, 2);
      }

      if (javaHome == null && Directory.Exists("\\java\\lib"))
        javaHome = Directory.GetCurrentDirectory()[0] + ":\\java";

      if (javaHome == null && Directory.Exists("\\jre\\lib"))
        javaHome = Directory.GetCurrentDirectory()[0] + ":\\jre";

      if (javaHome == null) {
        String[] dirs = Directory.GetDirectories("\\", "jdk*");

        foreach (String dir in dirs) {
          if (File.Exists(dir + "\\bin\\java.exe"))
            javaHome = Directory.GetCurrentDirectory().Substring(0, 2) + dir;
        }

        String programFilesJava
          = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles)
          + "\\java";

        if (Directory.Exists(programFilesJava)) {
          dirs = Directory.GetDirectories(programFilesJava, "jdk*");
          foreach (String dir in dirs) {
            if (File.Exists(dir + "\\bin\\java.exe"))
              javaHome = dir;
          }
        }
      }

      if (javaHome == null) {
        String[] dirs = Directory.GetDirectories("\\", "jre*");

        foreach (String dir in dirs) {
          if (File.Exists(dir + "\\bin\\java.exe"))
            javaHome = Directory.GetCurrentDirectory().Substring(0, 2) + dir;
        }
      }

      return javaHome;
    }

    private void Usage(String name)
    {
      Info(String.Format(ResinArgs.USAGE, name));
    }
  }
}

class ResinServiceException : Exception
{
  public ResinServiceException(String message)
    : base(message)
  {
  }
}