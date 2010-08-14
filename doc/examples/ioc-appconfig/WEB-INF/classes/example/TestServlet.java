package example;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;
import java.util.*;
import javax.inject.Inject;

public class TestServlet extends HttpServlet {
  @Inject AppConfig _appConfig;

  public void doGet(HttpServletRequest req,
                    HttpServletResponse res)
    throws IOException, ServletException
  {
    res.setContentType("text/html");
    PrintWriter out = res.getWriter();

    String inputFile = req.getParameter("inputFile");
    String outputFile = req.getParameter("outputFile");

    // stop browser from caching the page
    res.setHeader("Cache-Control","no-cache");

    out.println("<pre>");
    if (inputFile != null && inputFile.length() > 0) {
      // demonstration of reading a configuration file
      out.println("inputFile: " + inputFile);
      out.println();

      try {
        readConfigFile(inputFile, out);
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
    } else {
      // demonstration of writing a configuration file
      outputFile="testFile.txt";
      out.println("outputFile: " + outputFile);
      try {
        writeConfigFile(outputFile);
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
    }

    out.println("</pre>");
    out.println("<a href='index.jsp'>back to demo</a>");
  }

  /**
   * use the _appConfig to read a configuration file and put it out to
   * the browser
   */
  void readConfigFile(String inputFile, PrintWriter out)
    throws java.io.IOException
  {
    InputStream is = _appConfig.openConfigFileRead(inputFile);

    final int bufsz = 1024;
    char[] buf = new char[bufsz];
    int l;
    
    Reader in = new BufferedReader(new InputStreamReader(is));

    while ((l = in.read(buf,0,bufsz)) > 0) {
      out.write(buf,0,l);
    }

    in.close();
  }

  /**
   * use the _appConfig to write a configuration file
   */
  void writeConfigFile(String outputFile)
    throws java.io.IOException
  {
    OutputStream os = _appConfig.openConfigFileWrite(outputFile);
    PrintWriter ow = new PrintWriter(os);
    Date now = new Date();
    ow.print("Configuration file made from 'TestServlet' ");
    ow.println(now);
    ow.close();
    os.close();
  }
}
