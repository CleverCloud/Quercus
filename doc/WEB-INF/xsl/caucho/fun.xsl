<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet>

  <!-- define some useful XPath functions

       installed(string)
         return a path to the root of the feature if the feature named
         by string is installed
       evaluate(string)
         evaluates the passed xpath expression as an object result
       doc-should-display()
         evaluates true if the current node (or node specified with
         optional arg1) should be displayed, based in the settings of
         product and cond
       doc-is-section()
         evaluates true if the current node (or node specified with
         optional arg1) is a node that starts a new section, such as
         'section' or 's1' or 'defun' etc.
       doc-config()
         returns the root node of the config.xml file
       file-basename(string[,string])
         return the basename (part after last /) of the file
         optional arg 2 is the basename to return if arg 1 does not contain a basename
       file-dirname(string)
         return the dirname (everything up to and including last /) of the file
       note the difference from unix dirname, a trailing '/' _is_ included
       file-removeext(string)
         return everything but the ext (everything up to last .)
       file-mergepaths(string,string[,string])
         return the path resulting from merging a parent and child path
         (basically just handles '../' in the path)
         The optional third arg will be appended if the second arg ends in '/'
       href-parse(string,position[,default])
         parse's an href string, which is up to three components seperated by '|'.
         the default is used if the component is empty; if there are less than three
         components than it is component 1 that is empty.
       javadoc-package(string)
         get just the package from a javadoc package.Class style string
       javadoc-class(string)
         get just the class from a javadoc package.Class style string (may return 
         null if the string is only a package)
       
       // the following functions need the XslWriter, so they are
       // defined each time  the xsl is run
       file-exists(string)
         return true if the file exists (note that files are relative to
         the current servlet context)

       file-realpath(string)
         treats the passed string as a path specification for a
         resource in the current context, and returns a real path for
         it.

       file-realpath-absolute(string)
         treats the passed string as a path specification for a
         resource in the web server (i.e. the path contains at the beginning a
         servlet context path), and returns a real path for it.

       file-find(string[,string2,[... stringN]])
         finds a file, starting in the real directory for the current
         url, and if not found in the parent directory. Continues
         until there are no more parent directories left.  Multiple
         args cause each file in turn to be looked for before looking
         in the parent directory.

       file-find-list(string[,string2,[... stringN]])
         the same as file-find(string[,string2,[... stringN]])
         but instead of returning a string of the first file found
         a list of all files found is returned, like:
         <files>
           <file>
             <url-path>path-1</url-path>
             <real-path>path-1</real-path>
           </file>
           <file>
             <url-path>path-2</url-path>
             <real-path>path-2</real-path>
           </file>
           ...
         </files>
        
          where `path-2' would be in a parent directory of `path-1'

         <file>file-1</file>
         <file>file-2</file>
         <file>file-3</file>


       file-find(basenode, string[,string2,[... stringN]])
         finds a file, starting in the directory that contains the document
         identified by basenode, and if not found, in the parent
         directory. Continues until there are no more parent
         directories left. Multiple string args cause each file in
         turn to be looked for before looking in the parent directory.


       request-servletpath()
         request.getServletPath()

       debug-print(node)
         print's a node to stdout

       -->

  <xtp:directive.page import='org.xml.sax.* org.w3c.dom.* java.io.* com.caucho.vfs.* javax.xml.transform.* java.util.* java.net.*'/>

  <xtp:declaration><![CDATA[
    boolean _initfun = false;
    synchronized protected void initFun()
    {
      if (!_initfun) {
        addFunction("evaluate",  new EvaluateFun());
        addFunction("doc-should-display",  new DocShouldDisplayFun());
        addFunction("doc-is-section",  new DocIsSectionFun());
        addFunction("file-basename",  new FileBasenameFun());
        addFunction("file-dirname",  new FileDirnameFun());
        addFunction("file-removeext",  new FileRemoveextFun());
        addFunction("file-mergepaths",  new FileMergepathsFun());
        addFunction("href-parse",  new HrefParseFun());
        addFunction("javadoc-package",  new JavadocPackageFun());
        addFunction("javadoc-class",  new JavadocClassFun());
        addFunction("debug-print",  new DebugPrintFun());

        // this is replaced when the request comes in
        addFunction("file-find",  new FileFindFun());

        _initfun = true;
      }
    }

    /** a bit of a hack, cannot implement a custom init() so 
        intercept transform() and add custom functions */
    public void transform(Node xml,
                          XMLWriter writer,
                          TransformerImpl transformer)
      throws SAXException, IOException, TransformerException
    {
      initFun();
      super.transform(xml,writer,transformer);
    }

   /** 
    * Find a file, relative to current request, by walking up the
    * directory structure.  
    *
    * @param realPath if true return a real path, otherwise an
    * appropriate url
    *
    * @param files the file names to look for.  Each file name in the
    * list is checked before moving on to the parent directory.
    *
    * @return the url or real path, as appropriate, or null if not
    * found
    */
    public static String fileFind(XslWriter out, boolean realPath, ArrayList files)
    throws XPathException
    {
      if (out == null) return null;
      PageContext page = (PageContext) out.getProperty("caucho.page.context");
      if (page == null)
        return null;
      ServletContext app = page.getServletContext();
      HttpServletRequest req = (HttpServletRequest) page.getRequest();

      String url = req.getRequestURI();
      int p = url.lastIndexOf('/');
    
      while (p >= 0) {
        String prefix = url.substring(0, p + 1);

        Application subapp = (Application) app.getContext(prefix);
        String rel = prefix.substring(subapp.getContextPath().length());

        String rp = subapp.getRealPath(rel);
        Path path = Vfs.lookupNative(rp);

        for (Iterator i = files.iterator(); i.hasNext(); ) {
          Object n = i.next();
          String l = n instanceof Node ? Expr.toString(n) : (String) n;
          Path chkpath = path.lookup(l);
          if (chkpath.exists()) {
            return realPath ? chkpath.getNativePath() : prefix + l;
          }
        }

        p = url.lastIndexOf('/', p - 1);
      }
      return null;
    }

    public static String fileFind(XslWriter out, boolean realPath, String file)
      throws XPathException
    {
      ArrayList a = new ArrayList(1);
      a.add(file);
      return fileFind(out,realPath,a);
    }

   /** 
    * Find all files, relative to current request, by walking up the
    * directory structure.   
    *
    * the list returned is like:
    * <files>
    *   <file>
    *     <url-path>path-1</url-path>
    *     <real-path>path-1</real-path>
    *   </file>
    *   <file>
    *     <url-path>path-2</url-path>
    *     <real-path>path-2</real-path>
    *   </file>
    *   ...
    * </files>
    *
    *  where `path-2' would be in a parent directory of `path-1'
    *
    * @param files the file names to look for.  Each file name in the
    * list is checked before moving on to the parent directory.
    *
    * @return the xml-document, as described above
    */
    public static Node fileFindList(XslWriter out, ArrayList files)
      throws ParserConfigurationException, XPathException
    {
      PageContext page = (PageContext) out.getProperty("caucho.page.context");
      if (page == null)
        return null;
      ServletContext app = page.getServletContext();
      HttpServletRequest req = (HttpServletRequest) page.getRequest();

      String url = req.getRequestURI();
      int p = url.lastIndexOf('/');
    
      Document doc = null;
      Element top = null;

      while (p >= 0) {
        String prefix = url.substring(0, p + 1);

        Application subapp = (Application) app.getContext(prefix);
        String rel = prefix.substring(subapp.getContextPath().length());

        String rp = subapp.getRealPath(rel);
        Path path = Vfs.lookupNative(rp);

        for (Iterator i = files.iterator(); i.hasNext(); ) {
          Object n = i.next();
          String l = n instanceof Node ? Expr.toString(n) : (String) n;
          Path chkpath = path.lookup(l);
          if (chkpath.exists()) {
            if (doc == null) {
              // Create a new parser using the JAXP API (javax.xml.parser)
              DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
              DocumentBuilder builder = factory.newDocumentBuilder();
              doc = builder.newDocument();
              top = doc.createElement("files");
              doc.appendChild(top);
            }

            Element file = doc.createElement("file");
            top.appendChild(file);

            Element urlpath = doc.createElement("url-path");
            urlpath.appendChild(doc.createTextNode(rel + "/" + l));
            file.appendChild(urlpath);

            Element realpath = doc.createElement("real-path");
            realpath.appendChild(doc.createTextNode(chkpath.toString()));
            file.appendChild(realpath);
          }
        }

        p = url.lastIndexOf('/', p - 1);
      }

      return doc;
    }

    private final static String _configattr = "com.caucho.resin.doc.configxmlnode";
    /** get the root node of the config.xml file */
    public static Node getConfig(XslWriter out)
      throws java.io.IOException, SAXException, XPathException
    {
      PageContext page = (PageContext) out.getProperty("caucho.page.context");
      if (page == null)
        return null;
      ServletContext app = page.getServletContext();
      HttpServletRequest req = (HttpServletRequest) page.getRequest();

      Node node;
      synchronized (app) {
        node = (Node) app.getAttribute(_configattr);
        if (node == null) {
          // find the config.xml file by travelling up the directory structure
          String path = fileFind(out,true,"config.xml");

          if (path != null)  {
            node = new LooseXml().parseDocument(path);
            app.setAttribute(_configattr, node);
          }
        }
      }
      return node;
    }

   /** 
    * Find a file, relative to document basenode, by walking up the
    * directory structure.  The file returned is always a real path.
    *
    * @return the real path, or null if not found
    */
    public static String fileFind(ExprEnvironment env, Node basenode, ArrayList files)
    {
      Path stylesheetPath = env.getStylesheetEnv().getPath();
      Path pwd = stylesheetPath.getParent();

      Document owner = null;
      if (basenode.getOwnerDocument() != null) {
        owner = basenode.getOwnerDocument();
      }
      else if (basenode instanceof Document) {
        owner = (Document) basenode;
      }

      String ownerFile = null;

      if (owner != null && owner instanceof QDocument) {
        ownerFile = ((QDocument) owner).getRootFilename();
      }

      if (ownerFile != null)
        pwd = pwd.lookup(ownerFile).getParent();


      Path parent = pwd;

      do {
        pwd = parent;
        // check this directory for each file
        for (Iterator i = files.iterator(); i.hasNext(); ) {
          String l = (String) i.next();
          Path path = pwd.lookup(l);

          if (path.exists()) {
            return path.getNativePath();
          }
        }
        // loop with parent directory
        parent = pwd.getParent();
      } while (!parent.equals(pwd));

      return null;
    }

    public static String fileFind(ExprEnvironment env, Node basenode, String file)
    {
      ArrayList a = new ArrayList(1);
      a.add(file);
      return fileFind(env,basenode,a);
    }

    public static String mergepaths(ArrayList args)
    {
      if (args.size() < 1)
        return null;
        
      String parent = (String) args.get(0);
      if (args.size() < 2)
        return parent;
      String child = (String) args.get(1); 
      if (child == null)
        return parent;

      if (args.size() > 2 && child.endsWith("/"))
        child = child + (String) args.get(2);

      if (child.startsWith("/"))
        parent = null;

      if (parent == null)
        return child;

      StringBuffer merged = new StringBuffer(parent);
      if (!parent.endsWith("/"))
        merged.append('/');
      if (child.length() > 0 && child.charAt(0) == '/') {
        merged.append(child.substring(1));
      } else {
        merged.append(child);
      }

      String merge = merged.toString();
      if (merge.indexOf("./") > -1) {
        LinkedList l = new LinkedList();
        StringTokenizer st = new StringTokenizer(merge,"/",true);
        boolean skip = false;
        while (st.hasMoreTokens()) {
          String t = st.nextToken();
          if (skip) {
            skip = false;
            continue;
          }
          if ("..".equals(t) && l.size() > 1) {
            l.removeLast();
            l.removeLast();
            skip = true;
            continue;
          } 
          if (!(".".equals(t))) {
            l.add(t);
          }
        }
        merged.setLength(0);
        Iterator i = l.iterator();
        while (i.hasNext()) {
          merged.append(i.next());
        }
        return merged.toString();
      } else {
        return merge;
      }
    }

    public static String mergepaths(String path1, String path2)
    {
      ArrayList args = new ArrayList(2);
      args.add(path1);
      args.add(path2);
      return mergepaths(args);
    }

    /** helpful function to allow static variables of parsed XPath
     *  expressions 
     */
    static Expr noexception_parseExpr(String expr) {
      try {
        return XPath.parseExpr(expr);
      } catch (Exception ex) {
        return null;
      }
    }

    /** evaluate the passed xpath expression as an Object result **/
    public class EvaluateFun extends XPathFun {
      /**
       * Evaluate the function.
       *
       * @param pattern The context pattern.
       * @param args The evaluated arguments
       */
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        String arg = Expr.toString(args.get(0));
        if (arg == null || arg.length() == 0)
          return null;
        Expr expr = XPath.parseExpr(arg);
        return expr.evalObject(node,env);
      }
    }

    static Expr docshoulddisplay_expr1 = noexception_parseExpr("if(@product,@product,if(header/product,header/product,product))");
    static Expr docshoulddisplay_expr2 = noexception_parseExpr("@cond");

    /** true if current node should be displayed **/
    public class DocShouldDisplayFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args != null && args.size() > 0)
          node = Expr.toNode(args.get(0)); 

        // check if product specified
        String p = docshoulddisplay_expr1.evalString(node,env);
        if (p != null && p.length() > 0) {
          /** see if the product is installed */
          XPathFun installed = env.getFunction("installed");
          ArrayList iargs = new ArrayList(1);
          iargs.add(p);
          boolean isinstalled = installed == null ? true : Expr.toBoolean(installed.eval(node,env,pattern,iargs));
          if (!isinstalled)
            return Boolean.FALSE;
        }
        // if there is an @cond, make sure it eval's true
        String cond = docshoulddisplay_expr2.evalString(node,env);
        if (cond != null && cond.length() > 0) {
          try {
            if (!XPath.parseExpr(cond).evalBoolean(node,env))
              return Boolean.FALSE;
          } catch (XPathParseException ex) {
            throw new RuntimeException("error parsing @cond '" + cond + "'",ex);
          }
        }

        return Boolean.TRUE;
      }
    }

    /** **/
    private static String[] _isSectionNames =
      { "document", "section", "defun", "faq", "s1", "s2", "s3", "s4", "s5", "s6" };

    public class DocIsSectionFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args != null && args.size() > 0)
          node = Expr.toNode(args.get(0)); 

        if (node != null) {
          for (int i = 0; i < _isSectionNames.length; i++) {
            if (_isSectionNames[i].equals(node.getNodeName()))
              return Boolean.TRUE;
          }
        }

        return Boolean.FALSE;
      }
    }

    /** **/
    public class DocConfigFun extends XPathFun {
      XslWriter out;
      public DocConfigFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        Node config;
        try {
          config = getConfig(out);
        } catch (Exception ex) {
          throw new XPathException(ex);
        }
        return config;
      }
    }


    /** return the basename of a file (everything after last '/') **/
    public class FileBasenameFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        String path = Expr.toString(args.get(0));
        if (path == null)
          path = "";
        if (path.endsWith("/"))
          path = path.substring(0,path.length() - 1);
        int i = path.lastIndexOf('/');
    
        if (i > -1) {
          if (++i < path.length() ) {
            path =  path.substring(i);
          } else {
            path = null;
          }
        }

        return path == null ? Expr.toString(args.get(1)) : path;
      }
    }

    /** return the dirname of a file (everything up to and including last '/') **/
    public class FileDirnameFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        String path = Expr.toString(args.get(0));
        int i = path == null ? -1 : path.lastIndexOf('/');
    
        return  (i > -1) ? path.substring(0,i+1) : null;
      }
    }

    /** return the full path, without the ext */
    public class FileRemoveextFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        String path = Expr.toString(args.get(0));
        int i = path == null ? -1 : path.lastIndexOf('/');
        int d = path == null ? -1 : path.lastIndexOf('.');
    
        return  (d > i) ? path.substring(0,d) : path;
      }
    }

    /** return the path resulting from merging a parent and child path
     *  (basically just handles '../' in the path
     */
    public class FileMergepathsFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        ArrayList sargs = new ArrayList(args.size());
        for (Iterator i = args.iterator(); i.hasNext(); ) {
          sargs.add(Expr.toString(i.next()));
        }

        return mergepaths(sargs);
      }
    }

    /** parse components of an href */
    public class HrefParseFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 2)
          return null;
        String href = Expr.toString(args.get(0));
        int pos = Integer.parseInt(Expr.toString(args.get(1)));
        String ret = args.size() < 3 ? "" : Expr.toString(args.get(2));
        // work backwards
        pos = 4 - pos;
        int nd = href.length() - 1;
        int cnt = 0;
        int st;
        while (nd > 0) {
          st = href.lastIndexOf('|',nd);
          if (++cnt == pos) {
            if (++st < nd)
              ret = href.substring(st,nd + 1);
            nd = -1;
          } else {
            nd = st - 1;
          }
        }
        return ret;
      }
    }

    /** */
    public class JavadocPackageFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        String s = Expr.toString(args.get(0));
        int pos = s.lastIndexOf('.');
        int pos2 = pos + 1;
        if (pos < 0 || pos2 >= s.length())
          return s;
        char c = s.charAt(pos2);
        return Character.isUpperCase(c) ? s.substring(0,pos) :  s;
      }
    }

    /** */
    public class JavadocClassFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        String s = Expr.toString(args.get(0));
        int pos = s.lastIndexOf('.');
        int pos2 = pos + 1;
        if (pos < 0 || pos2 >= s.length())
          return null;
        char c = s.charAt(pos2);
        return Character.isUpperCase(c) ? s.substring(pos2) : null;
      }
    }

    /** print a node and it's children out to Stdout
    */
    public class DebugPrintFun extends XPathFun {
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        XmlPrinter printer = new XmlPrinter(System.out);
        try {
          printer.printXml(Expr.toNode(args.get(0)));
        } catch (IOException e) {
          throw new XPathException(e);
        }
        return Boolean.TRUE;
      }
    }

    /** return true if the file exists 
    */
    public class FileExistsFun extends XPathFun {
      XslWriter out;
      public FileExistsFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;
        PageContext page = (PageContext) out.getProperty("caucho.page.context");
        if (page == null)
          return null;
        ServletContext app = page.getServletContext();
        String path = Expr.toString(args.get(0));
        return new Boolean(path == null ? false : (new File(app.getRealPath(path))).exists());
      }
    }

    /**
    */
    public class FileRealpathFun extends XPathFun {
      XslWriter out;
      public FileRealpathFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        PageContext page = (PageContext) out.getProperty("caucho.page.context");
        if (page == null)
          return null;
        ServletContext app = page.getServletContext();

        if (args.size() < 1)
          return null;

        String url = Expr.toString(args.get(0));
        String rp = app.getRealPath(url);

        Path path = Vfs.lookupNative(rp);
        return path.getNativePath();
      }
    }

    /**
    */
    public class FileRealpathAbsoluteFun extends XPathFun {
      XslWriter out;
      public FileRealpathAbsoluteFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        if (args.size() < 1)
          return null;

        String url = Expr.toString(args.get(0));
        if (url == null || url.length() == 0)
          return null;

        PageContext page = (PageContext) out.getProperty("caucho.page.context");
        if (page == null)
          return null;

        ServletContext app = page.getServletContext();

        // take the ServletContext path off the beginning of the
        // url before asking the ServletContext for the real path

        app = app.getContext(url);
        if (app == null)
          return null;
        if (!(app instanceof com.caucho.server.webapp.Application))
          return null;

        url = url.substring(((com.caucho.server.webapp.Application)app).getContextPath().length());

        String rp = app.getRealPath(url);

        Path path = Vfs.lookupNative(rp);
        return path.getNativePath();
      }
    }

    /**
    */
    public class FileFindFun extends XPathFun {
      XslWriter out;
      public FileFindFun()
      {
      }

      public FileFindFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {

        Node arg1 = args.size() > 0 ? Expr.toNode(args.get(0)) : null;

        if (arg1 != null) {
          args.remove(0);
          return fileFind(env, arg1, args);
        } else {
          return fileFind(out, false, args);
        }
      }
    }

    /**
    */
    public class FileFindListFun extends XPathFun {
      XslWriter out;

      public FileFindListFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {

        if (args.size() <= 0)
         return null;
        try {
          return fileFindList(out, args);
        } catch (ParserConfigurationException e) {
          throw new XPathException(e);
        }
      }
    }

    /**
    */
    public class InstalledFun extends XPathFun {
      XslWriter out;
      public InstalledFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        int c = args.size();
        if (c < 1)
          return null;

        String arg = Expr.toString(args.get(0));
        if (arg == null || arg.length() == 0)
          return null;
        Node config;
        try {
          config = getConfig(out);
        } catch (Exception ex) {
          throw new XPathException(ex);
        }
        Expr expr = XPath.parseExpr("/config/installed/*[name() = '" + arg + "']");
        String p = expr.evalString(config,env);
        if (p != null && p.length() > 0) {
          String cfg = fileFind(out,false,"config.xml");
          int i = cfg.lastIndexOf('/');
          cfg = (i > -1) ? cfg.substring(0,i+1) : "/";
          p = mergepaths(cfg,p.startsWith("/") ? p.substring(1) : p);
        } else {
          // not in the config.xml file, see if it exists as a file
          // this is for things like 'javadoc' which get installed as
          // subdirectories  off of the main root
          p = fileFind(out,false,arg);
        }
        if (p != null && p.length() == 0)  // avoid xpath false on empty string
          p = "./";
        return p;
      }
    }

    /**
    */
    public class RequestServletpathFun extends XPathFun {
      XslWriter out;
      public RequestServletpathFun(XslWriter out)
      {
        this.out = out;
      }
      public Object eval(Node node, ExprEnvironment env, 
		         AbstractPattern pattern, ArrayList args)
        throws XPathException
      {
        PageContext page = (PageContext) out.getProperty("caucho.page.context");
        if (page == null)
          return null;

        HttpServletRequest req = (HttpServletRequest) page.getRequest();
        return req.getServletPath();
      }
    }


]]>
  </xtp:declaration>

  <xsl:template name="fun-init">
    <xtp:scriptlet>
      addFunction("installed",  new InstalledFun(out));
      addFunction("file-exists",  new FileExistsFun(out));
      addFunction("file-realpath",  new FileRealpathFun(out));
      addFunction("file-realpath-absolute",  new FileRealpathAbsoluteFun(out));
      addFunction("file-find",  new FileFindFun(out));
      addFunction("file-find-list",  new FileFindListFun(out));
      addFunction("request-servletpath",  new RequestServletpathFun(out));
      addFunction("doc-config",  new DocConfigFun(out));
    </xtp:scriptlet>
  </xsl:template>

  <xsl:template match="fun-file-find-list-test">
    <xsl:copy-of select="file-find-list(@file)"/>
  </xsl:template>
</xsl:stylesheet>
