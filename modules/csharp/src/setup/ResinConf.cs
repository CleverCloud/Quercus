using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.XPath;

namespace Caucho
{
  public class ResinConf
  {
    private XPathDocument _xPathDoc;
    private XPathNavigator _docNavigator;
    private XmlNamespaceManager _xmlnsMgr;

    public ResinConf(String file)
    {
      _xPathDoc = new XPathDocument(file);
      _docNavigator = _xPathDoc.CreateNavigator();
      _xmlnsMgr = new XmlNamespaceManager(_docNavigator.NameTable);
      _xmlnsMgr.AddNamespace("caucho", "http://caucho.com/ns/resin");
    }

    public IList getServers()
    {
      IList result = new List<ResinConfServer>();

      XPathNodeIterator ids = _docNavigator.Select("caucho:resin/caucho:cluster/caucho:server/@id", _xmlnsMgr);
      while (ids.MoveNext()) {
        ResinConfServer server = new ResinConfServer();
        server.ID = ids.Current.Value;
        //cluster@id
        XPathNodeIterator it = ids.Current.SelectAncestors("cluster", "http://caucho.com/ns/resin", false);
        it.MoveNext();
        server.Cluster = it.Current.GetAttribute("id", "");

        result.Add(server);
      }


      return result;
    }

    public String getRootDirectory()
    {
      String rootDirectory = null;

      XPathNavigator nav = _docNavigator.SelectSingleNode("caucho:resin/@root-directory", _xmlnsMgr);
      if (nav != null)
        rootDirectory = nav.Value;

      if (null == rootDirectory || "".Equals(rootDirectory)) {
        nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:root-directory/text()", _xmlnsMgr);
        if (nav != null)
          rootDirectory = nav.Value;
      }

      return rootDirectory;
    }

    public String GetJmxPort(String cluster, String server)
    {
      XPathNodeIterator jvmArgs
        = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/caucho:jvm-arg/text()", _xmlnsMgr);
      while (jvmArgs.MoveNext()) {
        String value = jvmArgs.Current.Value;
        if (value.StartsWith("-Dcom.sun.management.jmxremote.port="))
          return value.Substring(36);
      }

      jvmArgs = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/caucho:jvm-arg/text()", _xmlnsMgr);
      while (jvmArgs.MoveNext()) {
        String value = jvmArgs.Current.Value;
        if (value.StartsWith("-Dcom.sun.management.jmxremote.port="))
          return value.Substring(36);
      }

      return null;
    }

    public String GetWatchDogPort(String cluster, String server)
    {
      XPathNavigator nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/caucho:watchdog-port/text()", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/@watchdog-port", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/caucho:watchdog-port/text()", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/@watchdog-port", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      return null;
    }

    public bool IsDynamicServerEnabled(String cluster)
    {
      XPathNavigator nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/@dynamic-server-enable", _xmlnsMgr);
      if (nav != null)
        return !"false".Equals(nav.Value);
      
      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:dynamic-server-enable/text()", _xmlnsMgr);
      if (nav != null)
        return !"false".Equals(nav.Value);

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster-default/@dynamic-server-enable", _xmlnsMgr);
      if (nav != null)
        return !"false".Equals(nav.Value);

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster-default/caucho:dynamic-server-enable/text()", _xmlnsMgr);
      if (nav != null)
        return !"false".Equals(nav.Value);

      return false;
    }

    public String GetDebugPort(String cluster, String server)
    {
      XPathNodeIterator jvmArgs
        = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/caucho:jvm-arg/text()", _xmlnsMgr);
      String debug = null;
      int addressIndex = -1;
      while (jvmArgs.MoveNext()) {
        String value = jvmArgs.Current.Value;
        addressIndex = value.IndexOf("address=");
        if (addressIndex > -1)
          debug = value;
      }

      if (debug == null) {
        jvmArgs = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/caucho:jvm-arg/text()", _xmlnsMgr);
        while (jvmArgs.MoveNext()) {
          String value = jvmArgs.Current.Value;
          addressIndex = value.IndexOf("address=");
          if (addressIndex > -1)
            debug = value;
        }
      }
      if (debug == null)
        return debug;

      StringBuilder sb = new StringBuilder();
      for (int i = addressIndex + 8; i < debug.Length; i++) {
        if (Char.IsDigit(debug[i]))
          sb.Append(debug[i]);
        else if (sb.Length > 0)
          break;
      }

      if (sb.Length > 0)
        return sb.ToString();
      else
        return null;
    }

    static public ResinConfServer ParseDynamic(String value)
    { //dynamic:app-tier:ip:port
      int lastColumn = value.LastIndexOf(':');
      int port = int.Parse(value.Substring(lastColumn + 1));
      int clusterEnd = value.IndexOf(':', 8);
      String cluster = value.Substring(8, clusterEnd - 8);
      String address = value.Substring(clusterEnd + 1, lastColumn - clusterEnd - 1);
      ResinConfServer server = new ResinConfServer();
      server.IsDynamic = true;
      server.Cluster = cluster;
      server.Address = address;
      server.Port = port;
      return server;
    }
  }

  public class ResinConfServer
  {
    public String ID { get; set; }
    public String Cluster { get; set; }
    public Boolean IsDynamic { get; set; }
    public String Address { get; set; }
    public int Port { get; set; }

    public ResinConfServer()
    {
      IsDynamic = false;
    }

    public override string ToString()
    {
      if (IsDynamic) {
        return "-dynamic " + Address + ":" + Port;
      } else {

        String id = "".Equals(ID) ? "'default'" : ID;
        String cluster = "".Equals(Cluster) ? "'default'" : Cluster;
        return cluster + ':' + id + "  [cluster@id='" + Cluster + "', server@id='" + ID + "')";
      }
    }
  }
}

