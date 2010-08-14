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
using System.Collections.Specialized;
using System.Text;

namespace Caucho
{
  public class ResinArgs
  {
    public static String USAGE = @"usage: {0} [flags] gui | console | status | start | stop | restart | kill | shutdown

    COMMANDS:
      gui - start Resin with a Graphic UI
      console - start Resin in console mode
      status - watchdog status
      start - start a Resin server
      stop - stop a Resin server
      restart - restart a Resin server
      kill - force a kill of a Resin server
      shutdown - shutdown the watchdog

    OPTIONS:
      -h                                     : this help
      -verbose                               : information on launching java
      -conf <resin.conf>                     : alternate configuration file
      -server <id>                           : select a <server> to run
      -dynamic-server <cluster:address:port> : initialize a dynamic server
      -java_home <dir>                       : sets the JAVA_HOME
      -java_exe <path>                       : path to java executable
      -classpath <dir>                       : java classpath
      -Jxxx                                  : JVM arg xxx
      -J-Dfoo=bar                            : Set JVM variable
      -Xxxx                                  : JVM -X parameter
      -resin_home <dir>                      : home of Resin
      -root-directory <dir>                  : select a root directory
      -log-directory  <dir>                  : select a logging directory
      -watchdog-port  <port>                 : override the watchdog-port
      -preview                               : run as a preview server
      -debug-port <port>                     : configure a debug port
      -jmx-port <port>                       : configure an unauthenticated jmx port
";
    public bool IsVerbose { get; private set; }
    public bool IsNoJit { get; private set; }
    public bool IsService { get; private set; }
    public bool IsInstall { get; private set; }
    public bool IsUnInstall { get; private set; }
    public bool IsHelp { get; private set; }

    //used with -install / -remove.
    //value of true indicates that the process is started
    //from resin.exe with elevated privileges
    public String User { get; private set; }
    public String Password { get; private set; }
    public String JavaExe { get; private set; }
    public String JavaHome { get; private set; }
    public String JvmArgs { get; private set; }
    public String ResinArguments { get; private set; }
    public String ClassPath { get; private set; }
    public String EnvClassPath { get; private set; }
    public String Home { get; private set; }
    public String Root { get; private set; }
    public String DisplayName { get; private set; }
    public String Command { get; private set; }
    public String ResinDataDir { get; private set; }
    public String ServiceName { get; private set; }
    public String Exe { get; private set; }
    public String Server { get; private set; }
    public String DynamicServer { get; private set; }
    public String Conf { get; private set; }
    public String Log { get; private set; }
    public String JmxPort { get; private set; }
    public String DebugPort { get; private set; }
    public String WatchDogPort { get; private set; }
    public bool IsPreview { get; private set; }

    public ResinArgs(String cmd)
    {
      int resinIdx = cmd.IndexOf("resin.exe");
      if (resinIdx == -1)
        resinIdx = cmd.IndexOf("httpd.exe");

      if (resinIdx > 0 && cmd[0] == '"')
        Exe = cmd.Substring(1, resinIdx - 1 + 9);
      else if (resinIdx > 0)
        Exe = cmd.Substring(0, resinIdx + 9);

      StringCollection arguments = new StringCollection();
      if (Exe != null)
        arguments.Add(Exe);

      StringBuilder builder = new StringBuilder();

      int startIdx = 0;
      if (resinIdx > 0)
        startIdx = resinIdx + 9;
      for (; startIdx < cmd.Length; startIdx++) {
        if (cmd[startIdx] == ' ')
          break;
      }

      bool quoted = false;
      for (int i = startIdx; i < cmd.Length; i++) {
        char c = cmd[i];
        if ('"' == c && !quoted) {
          quoted = true;
        } else if ('"' == c && quoted) {
          if (builder.Length > 0)
            arguments.Add(builder.ToString());

          builder = new StringBuilder();
          quoted = false;
        } else if (' ' == c && quoted) {
          builder.Append(c);
        } else if (' ' == c) {
          arguments.Add(builder.ToString());
          builder = new StringBuilder();

        } else if (builder != null) {
          builder.Append(c);
        } else {
          builder.Append(c);
        }
      }

      parse(arguments);
    }

    public ResinArgs(String[] args)
    {
      StringCollection arguments = new StringCollection();
      arguments.AddRange(args);

      parse(arguments);
    }

    private void parse(StringCollection arguments)
    {
      StringBuilder jvmArgs = new StringBuilder();
      StringBuilder resinArgs = new StringBuilder();
     
      int argsIdx = 1;
      while (argsIdx < arguments.Count) {
        if ("-verbose".Equals(arguments[argsIdx])) {
          IsVerbose = true;

          resinArgs.Append(' ').Append(arguments[argsIdx++]);
        } else if (arguments[argsIdx].StartsWith("-J")) {
          argsIdx++;
        } else if (arguments[argsIdx].StartsWith("-D")) {
          jvmArgs.Append(' ').Append(arguments[argsIdx]);

          argsIdx++;
        } else if (arguments[argsIdx].StartsWith("-X")) {
          jvmArgs.Append(' ').Append(arguments[argsIdx]);

          argsIdx++;
        } else if (arguments[argsIdx].StartsWith("-E")) {
          String envVar = arguments[argsIdx];
          int equalsIdx;
          if ((equalsIdx = envVar.IndexOf('=')) > -1) {
            String variable = envVar.Substring(2, equalsIdx - 2);
            String val = envVar.Substring(equalsIdx + 1);
            Environment.SetEnvironmentVariable(variable, val, EnvironmentVariableTarget.Process);
          } else {
            String variable = envVar.Substring(2);
            Environment.SetEnvironmentVariable(variable, "", EnvironmentVariableTarget.Process);
          }

          argsIdx++;
        } else if ("-user".Equals(arguments[argsIdx])) {
          User = arguments[argsIdx + 1];

          if (!User.StartsWith(".\\") && User.IndexOf('\\') < 0)
            User = ".\\" + User;

          argsIdx += 2;
        } else if ("-password".Equals(arguments[argsIdx])) {
          Password = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-name".Equals(arguments[argsIdx])) {
          ServiceName = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-display-name".Equals(arguments[argsIdx])) {
          DisplayName = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-install".Equals(arguments[argsIdx])) {
          IsInstall = true;
          IsService = false;

          argsIdx++;
        } else if ("-install-as".Equals(arguments[argsIdx]) ||
                   "-install_as".Equals(arguments[argsIdx])) {
          IsInstall = true;
          IsService = false;

          ServiceName = arguments[argsIdx + 1];
          DisplayName = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-remove".Equals(arguments[argsIdx])) {
          IsUnInstall = true;
          IsService = false;

          argsIdx++;
        } else if ("-remove-as".Equals(arguments[argsIdx]) ||
                   "-remove_as".Equals(arguments[argsIdx])) {
          IsUnInstall = true;
          IsService = false;

          ServiceName = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-java_home".Equals(arguments[argsIdx]) ||
                  "-java-home".Equals(arguments[argsIdx])) {
          JavaHome = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-java_exe".Equals(arguments[argsIdx]) ||
                   "-java-exe".Equals(arguments[argsIdx])) {
          JavaExe = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-msjava".Equals(arguments[argsIdx])) {
          //msJava = true; XXX no longer supported

          argsIdx++;
        } else if ("-resin_home".Equals(arguments[argsIdx]) ||
                   "-resin-home".Equals(arguments[argsIdx]) ||
                   "--resin-home".Equals(arguments[argsIdx])) {
          Home = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-server-root".Equals(arguments[argsIdx]) ||
                   "-server_root".Equals(arguments[argsIdx]) ||
                   "--root-directory".Equals(arguments[argsIdx]) ||
                   "-root-directory".Equals(arguments[argsIdx])) {
          Root = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-classpath".Equals(arguments[argsIdx]) ||
                   "-cp".Equals(arguments[argsIdx])) {
          ClassPath += arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-env-classpath".Equals(arguments[argsIdx])) {
          EnvClassPath = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-stdout".Equals(arguments[argsIdx])) {
          //stdOutFile = args[argsIdx + 1]; XXX not supported

          argsIdx += 2;
        } else if ("-stderr".Equals(arguments[argsIdx])) {
          //stdErrFile = args[argsIdx + 1]; XXX not supported

          argsIdx += 2;
        } else if ("-jvm-log".Equals(arguments[argsIdx])) {
          //jvmFile = args[argsIdx + 1]; XXX not supported

          argsIdx += 2;
        } else if ("-main".Equals(arguments[argsIdx])) {
          //main = args[argsIdx + 1]; XXX not supported - was used with jview

          argsIdx += 2;
        } else if ("-help".Equals(arguments[argsIdx]) ||
                  "-h".Equals(arguments[argsIdx])) {
          IsHelp = true;

          argsIdx++;
        } else if ("-service".Equals(arguments[argsIdx])) {
          IsService = true;

          argsIdx++;
        } else if ("-console".Equals(arguments[argsIdx])) {
          //make -console be a command
          Command = "console";

          argsIdx++;
        } else if ("-nojit".Equals(arguments[argsIdx])) {
          IsNoJit = true;

          argsIdx++;
        } else if ("-standalone".Equals(arguments[argsIdx])) {
          argsIdx++;
        } else if ("-server".Equals(arguments[argsIdx]) ||
                   "--server".Equals(arguments[argsIdx])) {
          Server = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-dynamic-server".Equals(arguments[argsIdx]) ||
                   "--dynamic-server".Equals(arguments[argsIdx])) {
          DynamicServer = arguments[argsIdx + 1];

          argsIdx += 2;
        } else if ("-conf".Equals(arguments[argsIdx]) ||
                   "--conf".Equals(arguments[argsIdx])) {
          Conf = arguments[argsIdx + 1];

          resinArgs.Append("-conf \"").Append(Conf).Append("\" ");

          argsIdx += 2;
        } else if ("-log-directory".Equals(arguments[argsIdx]) ||
                   "--log-directory".Equals(arguments[argsIdx])) {
          Log = arguments[argsIdx + 1];

          resinArgs.Append("-log-directory \"").Append(Log).Append("\" ");

          argsIdx += 2;
        } else if ("-jmx-port".Equals(arguments[argsIdx]) ||
                   "--jmx-port".Equals(arguments[argsIdx])) {
          JmxPort = arguments[argsIdx + 1];

          resinArgs.Append("-jmx-port ").Append(JmxPort).Append(' ');

          argsIdx += 2;
        } else if ("-debug-port".Equals(arguments[argsIdx]) ||
                   "--debug-port".Equals(arguments[argsIdx])) {
          DebugPort = arguments[argsIdx + 1];

          resinArgs.Append("-debug-port ").Append(DebugPort).Append(' ');

          argsIdx += 2;
        } else if ("-watchdog-port".Equals(arguments[argsIdx]) ||
                   "--watchdog-port".Equals(arguments[argsIdx])) {
          WatchDogPort = arguments[argsIdx + 1];

          resinArgs.Append("-watchdog-port ").Append(WatchDogPort).Append(' ');

          argsIdx += 2;
        } else if ("-e".Equals(arguments[argsIdx]) ||
                  "-compile".Equals(arguments[argsIdx])) {
          argsIdx++;
        } else if ("-preview".Equals(arguments[argsIdx]) ||
                  "--preview".Equals(arguments[argsIdx])) {
          IsPreview = true;
          argsIdx++;
        } else if ("gui".Equals(arguments[argsIdx])) {
          Command = arguments[argsIdx];

          argsIdx++;
        } else if ("console".Equals(arguments[argsIdx]) ||
                   "status".Equals(arguments[argsIdx]) ||
                   "start".Equals(arguments[argsIdx]) ||
                   "gui".Equals(arguments[argsIdx]) ||
                   "stop".Equals(arguments[argsIdx]) ||
                   "restart".Equals(arguments[argsIdx]) ||
                   "kill".Equals(arguments[argsIdx]) ||
                   "shutdown".Equals(arguments[argsIdx])) {
          Command = arguments[argsIdx];

          argsIdx++;
        } else {
          resinArgs.Append(' ').Append(arguments[argsIdx++]).Append(' ');
        }
      }

      if (ServiceName == null || "".Equals(ServiceName))
        ServiceName = "Resin";

      JvmArgs = jvmArgs.ToString();
      ResinArguments = resinArgs.ToString();
    }

    public bool IsValid()
    {
      return Command != null || IsInstall || IsUnInstall || IsService;
    }

    public bool IsServiceCommand()
    {
      return IsInstall || IsUnInstall;
    }
  }
}