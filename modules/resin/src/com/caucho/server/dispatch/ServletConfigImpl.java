/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 * @author Scott Ferguson
 */

package com.caucho.server.dispatch;

import com.caucho.config.*;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.config.annotation.DisableConfig;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.InitParam;
import com.caucho.config.types.CronType;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.jmx.Jmx;
import com.caucho.jsp.Page;
import com.caucho.jsp.QServlet;
import com.caucho.naming.Jndi;
import com.caucho.remote.server.*;
import com.caucho.security.BasicPrincipal;
import com.caucho.server.http.StubServletRequest;
import com.caucho.server.http.StubServletResponse;
import com.caucho.server.webapp.WebApp;
import com.caucho.servlet.comet.CometServlet;
import com.caucho.util.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.faces.*;
import javax.faces.application.*;
import javax.naming.NamingException;
import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for a servlet.
 */
public class ServletConfigImpl
  implements ServletConfig, ServletRegistration.Dynamic, AlarmListener
{
  public enum FRAGMENT_MODE {IN_FRAGMENT, IN_WEBXML};

  static L10N L = new L10N(ServletConfigImpl.class);
  protected static final Logger log
    = Logger.getLogger(ServletConfigImpl.class.getName());

  private String _location;

  private String _jndiName;
  private String _var;

  private String _servletName;
  private String _servletNameDefault;

  private String _servletClassName;
  private Class _servletClass;
  private Bean _bean;
  private String _jspFile;
  private String _displayName;
  private int _loadOnStartup = Integer.MIN_VALUE;
  private boolean _asyncSupported;

  private Servlet _singletonServlet;

  private boolean _allowEL = true;
  private HashMap<String,String> _initParams = new HashMap<String,String>();
  // used for params defined prior to applying fragments.
  private Set<String> _paramNames = new HashSet();

  private HashMap<String,String> _roleMap;

  private ContainerProgram _init;

  private RunAt _runAt;
  private CronType _cron;

  private MultipartConfigElement _multipartConfigElement;

  private ServletProtocolConfig _protocolConfig;
  private ProtocolServletFactory _protocolFactory;

  private Alarm _alarm;
  private InjectionTarget _comp;

  private WebApp _webApp;
  private ServletContext _servletContext;
  private ServletManager _servletManager;
  private ServletMapper _servletMapper;

  private ServletException _initException;
  private long _nextInitTime;

  private Object _servlet;
  private FilterChain _servletChain;

  private Principal _runAs;

  private FRAGMENT_MODE _fragmentMode = FRAGMENT_MODE.IN_WEBXML;

  /**
   * Creates a new servlet configuration object.
   */
  public ServletConfigImpl()
  {
  }

  public ServletConfigImpl(FRAGMENT_MODE fragmentMode)
  {
    _fragmentMode = fragmentMode;
  }

  /**
   * Sets the config location.
   */
  public void setConfigLocation(String location, int line)
  {
    _location = location + ":" + line + ": ";
  }

  /**
   * Sets the id attribute
   */
  public void setId(String id)
  {
  }

  /**
   * Sets the servlet name.
   */
  public void setServletName(String name)
  {
    _servletName = name;
  }

  /**
   * Gets the servlet name.
   */
  public String getServletName()
  {
    return _servletName;
  }

  public String getName()
  {
    return _servletName;
  }

  public ServletConfigImpl createRegexpConfig(String servletName)
    throws ServletException
  {
    ServletConfigImpl config = new ServletConfigImpl();
    config.setServletName(servletName);
    config.setServletClass(servletName);

    config.init();

    return config;
  }

  public String getClassName()
  {
    return _servletClassName;
  }

  public boolean setInitParameter(String name, String value)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    if (_initParams.containsKey(name))
      return false;

    _initParams.put(name, value);

    return true;
  }

  public Set<String> setServletSecurity(ServletSecurityElement securityElement)
  {
    _servletManager.addSecurityElement(getServletClass(), securityElement);
    
    return new HashSet<String>();
  }

  public ServletSecurityElement getSecurityElement()
  {
    // server/10ds - servlets are allowed to be lazy loaded. It's not an
    // error in this case for a class not found.
    try {
      return _servletManager.getSecurityElement(getServletClass());
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    }
  }

  public void setMultipartConfig(MultipartConfigElement multipartConfig)
  {
    if (multipartConfig == null)
      throw new IllegalArgumentException();

    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _multipartConfigElement = multipartConfig;
  }

  public MultipartConfigElement getMultipartConfig()
  {
    if (_multipartConfigElement == null) {
      Class servletClass = getServletClass();

      if (servletClass != null) {
        MultipartConfig config
          = (MultipartConfig) servletClass.getAnnotation(MultipartConfig.class);

        if (config != null)
          _multipartConfigElement = new MultipartConfigElement(config);
      }
    }

    return _multipartConfigElement;
  }

  /**
   * Maps or exists if any of the patterns in urlPatterns already map to a
   * different servlet 
   * @param urlPatterns
   * @return a Set of patterns previously mapped to a different servlet
   */
  public Set<String> addMapping(String... urlPatterns)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    try {
      Set<String> result = new HashSet<String>();

      for (String urlPattern : urlPatterns) {
        String servletName = _servletMapper.getServletName(urlPattern);

        if (! _servletName.equals(servletName) && servletName != null)
          result.add(urlPattern);
      }

      if (result.size() > 0)
        return result;

      ServletMapping mapping = _webApp.createServletMapping();
      mapping.setIfAbsent(true);

      mapping.setServletName(getServletName());

      for (String urlPattern : urlPatterns) {
        mapping.addURLPattern(urlPattern);
      }

      _webApp.addServletMapping(mapping);

      return Collections.unmodifiableSet(result);
    }
    catch (ServletException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public Collection<String> getMappings()
  {
    Set<String> patterns = _servletMapper.getUrlPatterns(_servletName);

    return Collections.unmodifiableSet(new LinkedHashSet<String>(patterns));
  }

  public Set<String> setInitParameters(Map<String, String> initParameters)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    Set<String> conflicting = new HashSet<String>();

    for (Map.Entry<String, String> param : initParameters.entrySet()) {
      if (_initParams.containsKey(param.getKey()))
        conflicting.add(param.getKey());
      else
        _initParams.put(param.getKey(), param.getValue());
    }

    return Collections.unmodifiableSet(conflicting);
  }

  public Map<String, String> getInitParameters()
  {
    return _initParams;
  }

  public void setAsyncSupported(boolean asyncSupported)
  {
    if (_webApp != null && ! _webApp.isInitializing())
      throw new IllegalStateException();

    _asyncSupported = asyncSupported;
  }

  public boolean isAsyncSupported() {
    return _asyncSupported;
  }

  /**
   * Sets the servlet name default when not specified
   */
  public void setServletNameDefault(String name)
  {
    _servletNameDefault = name;
  }

  /**
   * Gets the servlet name default.
   */
  public String getServletNameDefault()
  {
    return _servletNameDefault;
  }

  /**
   * Gets the servlet name.
   */
  public String getServletClassName()
  {
    return _servletClassName;
  }

  /**
   * Set the bean
   */
  @Configurable
  public void setBean(Bean bean)
  {
    _bean = bean;
  }

  public Bean getBean()
  {
    return _bean;
  }

  public boolean isServletConfig()
  {
    return _bean != null || _servletClassName != null;
  }

  /**
   * Sets the servlet class.
   */
  @Configurable
  public void setServletClass(String servletClassName)
  {
    _servletClassName = servletClassName;

    // JSF is special
    if (isFacesServlet()) {
      // ioc/0566

      if (_loadOnStartup < 0)
        _loadOnStartup = 1;

      if (_servletContext instanceof WebApp)
        ((WebApp) _servletContext).createJsp().setLoadTldOnInit(true);
    }

    InjectManager beanManager = InjectManager.create();
    beanManager.addConfiguredBean(servletClassName);
  }
  
  private boolean isFacesServlet()
  {
    return "javax.faces.webapp.FacesServlet".equals(_servletClassName);
  }

  @DisableConfig
  public void setServletClass(Class<? extends Servlet> servletClass)
  {
    if (_servletClass != null)
      throw new IllegalStateException();

    _servletClass = servletClass;
  }

  /**
   * Gets the servlet class.
   */
  public Class getServletClass()
  {
    if (_bean != null)
      return _bean.getBeanClass();

    if (_servletClassName == null)
      return null;

    if (_servletClass == null) {
      try {
        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();

        _servletClass = Class.forName(_servletClassName, false, loader);
      } catch (Exception e) {
        throw error(L.l("'{0}' is not a known servlet class.  Servlets belong in the classpath, for example WEB-INF/classes.",
                        _servletClassName),
                    e);
      }
    }

    return _servletClass;
  }

  public void setServlet(Servlet servlet)
  {
    _singletonServlet = servlet;
  }

  /**
   * Sets the JSP file
   */
  public void setJspFile(String jspFile)
  {
    _jspFile = jspFile;
  }

  /**
   * Gets the JSP file
   */
  public String getJspFile()
  {
    return _jspFile;
  }

  /**
   * Sets the allow value.
   */
  public void setAllowEL(boolean allowEL)
  {
    _allowEL = allowEL;
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(String param, String value)
  {
    _initParams.put(param, value);
  }

  /**
   * Sets an init-param
   */
  public InitParam createInitParam()
  {
    InitParam initParam = new InitParam();

    initParam.setAllowEL(_allowEL);

    return initParam;
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(InitParam initParam)
  {
    if (_webApp.isAllowInitParamOverride())
      _initParams.putAll(initParam.getParameters());
    else {
      for (Map.Entry<String, String> param : initParam.getParameters()
        .entrySet()) {
        if (! _initParams.containsKey(param.getKey())) {
          _initParams.put(param.getKey(), param.getValue());
          _paramNames.add(param.getKey());
        }
      }
    }
  }

  /**
   * Gets the init params
   */
  public Map getInitParamMap()
  {
    return _initParams;
  }

  /**
   * Gets the init params
   */
  public String getInitParameter(String name)
  {
    return _initParams.get(name);
  }

  /**
   * Gets the init params
   */
  public Enumeration getInitParameterNames()
  {
    return Collections.enumeration(_initParams.keySet());
  }

  /**
   * Sets the run-as
   */
  public void setRunAs(RunAs runAs)
  {
    String roleName = runAs.getRoleName();

    if (roleName != null)
      _runAs = new BasicPrincipal(roleName);
  }

  public String getRunAsRole()
  {
    if (_runAs != null)
      return _runAs.getName();

    return null;
  }

  public void setRunAsRole(String roleName)
  {
    if (roleName == null)
      throw new IllegalArgumentException();

    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _runAs = new BasicPrincipal(roleName);
  }

  /**
   * Returns the servlet context.
   */
  public ServletContext getServletContext()
  {
    return _servletContext;
  }

  /**
   * Sets the servlet context.
   */
  public void setServletContext(ServletContext app)
  {
    _servletContext = app;
  }

  public void setWebApp(WebApp webApp)
  {
    _webApp = webApp;
  }

  /**
   * Returns the servlet manager.
   */
  public ServletManager getServletManager()
  {
    return _servletManager;
  }

  /**
   * Sets the servlet manager.
   */
  public void setServletManager(ServletManager manager)
  {
    _servletManager = manager;
  }

  public void setServletMapper(ServletMapper servletMapper)
  {
    _servletMapper = servletMapper;
  }

  /**
   * Sets the init block
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init block
   */
  public ContainerProgram getInit()
  {
    return _init;
  }

  /**
   * Sets the load-on-startup
   */
  public void setLoadOnStartup(int loadOnStartup)
  {
    _loadOnStartup = loadOnStartup;
  }

  /**
   * Gets the load-on-startup value.
   */
  public int getLoadOnStartup()
  {
    if (_loadOnStartup > Integer.MIN_VALUE)
      return _loadOnStartup;
    else if (_runAt != null || _cron != null)
      return 0;
    else
      return Integer.MIN_VALUE;
  }

  /**
   * Creates the run-at configuration.
   */
  public RunAt createRunAt()
  {
    if (_runAt == null)
      _runAt = new RunAt();

    return _runAt;
  }

  public void setCron(CronType cron)
  {
    _cron = cron;
  }

  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Returns the run-at configuration.
   */
  public RunAt getRunAt()
  {
    return _runAt;
  }

  /**
   * Returns the cron configuration.
   */
  public CronType getCron()
  {
    return _cron;
  }

  /**
   * Adds a security role reference.
   */
  public void addSecurityRoleRef(SecurityRoleRef ref)
  {
    if (_roleMap == null)
      _roleMap = new HashMap<String,String>(8);

    // server/12h2
    // server/12m0
    _roleMap.put(ref.getRoleName(), ref.getRoleLink());
  }

  /**
   * Adds a security role reference.
   */
  public HashMap<String,String> getRoleMap()
  {
    return _roleMap;
  }

  /**
   * Sets the display name
   */
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  /**
   * Gets the display name
   */
  public String getDisplayName()
  {
    return _displayName;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the icon
   */
  public void setIcon(com.caucho.config.types.Icon icon)
  {
  }

  /**
   * Sets the web service protocol.
   */
  public void setProtocol(ServletProtocolConfig protocol)
  {
    _protocolConfig = protocol;
  }

  /**
   * Sets the web service protocol.
   */
  public void setProtocolFactory(ProtocolServletFactory factory)
  {
    _protocolFactory = factory;
  }

  /**
   * Sets the init exception
   */
  public void setInitException(ServletException exn)
  {
    _initException = exn;

    _nextInitTime = Long.MAX_VALUE / 2;

    if (exn instanceof UnavailableException) {
      UnavailableException unExn = (UnavailableException) exn;

      if (! unExn.isPermanent())
        _nextInitTime = (Alarm.getCurrentTime() +
                         1000L * unExn.getUnavailableSeconds());
    }
  }

  public void setInFragmentMode()
  {
    _fragmentMode = FRAGMENT_MODE.IN_FRAGMENT;
  }

  public boolean isInFragmentMode()
  {
    return _fragmentMode == FRAGMENT_MODE.IN_FRAGMENT;
  }

  /**
   * Returns the servlet.
   */
  public Object getServlet()
  {
    return _servlet;
  }

  public void merge(ServletConfigImpl config) 
  {
    if (_loadOnStartup == Integer.MIN_VALUE)
      _loadOnStartup = config._loadOnStartup;

    if (! getClassName().equals(config.getClassName()))
      throw new ConfigException(L.l(
        "Illegal attempt to specify different servlet-class '{0}' for servlet '{1}'. Servlet '{1}' has already been defined with servlet-class '{2}'. Consider using <absolute-ordering> to exclude conflicting web-fragment.",
        config.getClassName(),
        _servletName,
        _servletClassName));

    for (Map.Entry<String, String> param
           : config._initParams.entrySet()) {
      if (_paramNames.contains(param.getKey())) {
      }
      else if (! _initParams.containsKey(param.getKey()))
        _initParams.put(param.getKey(), param.getValue());
      else if (! _initParams.get(param.getKey()).equals(param.getValue())) {
        throw new ConfigException(L.l(
          "Illegal attempt to specify different param-value of '{0}' for parameter '{1}'. This error indicates that two web-fragments use different values. Consider defining the parameter in web.xml to override definitions in web-fragment.",
          param.getValue(),
          param.getKey()));
      }
    }
  }

  /**
   * Initialize the servlet config.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_runAt != null || _cron != null) {
      _alarm = new Alarm(this);
    }

    if (_servletName != null) {
    }
    else if (getServletNameDefault() != null) {
      // server/13f4
      _servletName = getServletNameDefault();
    }
    else if (_protocolConfig != null) {
      String protocolName = _protocolConfig.getUri();

      setServletName(_servletClassName + "-" + protocolName);
    }
    else
      setServletName(_servletClassName);

    // XXX: should only be for web services
    if (_jndiName != null) {
      validateClass(true);

      Object servlet = createServlet(false);

      try {
        Jndi.bindDeepShort(_jndiName, servlet);
      } catch (NamingException e) {
        throw new ServletException(e);
      }
    }

    InjectManager webBeans = InjectManager.create();

    if (_var != null) {
      validateClass(true);

      Object servlet = createServlet(false);

      BeanBuilder factory = webBeans.createBeanFactory(servlet.getClass());
      factory.name(_var);

      webBeans.addBean(factory.singleton(servlet));
    }
  }

  protected void validateClass(boolean requireClass)
    throws ServletException
  {
    if (_runAt != null || _cron != null || _loadOnStartup >= 0)
      requireClass = true;

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    if (_servletClassName == null) {
    }
    else if (_servletClassName.equals("invoker")) {
    }
    else {
      try {
        _servletClass = Class.forName(_servletClassName, false, loader);
      } catch (ClassNotFoundException e) {
        if (e instanceof CompileException)
          throw error(e);

        log.log(Level.FINER, e.toString(), e);
      }

      if (_servletClass != null) {
      }
      else if (requireClass) {
        throw error(L.l("'{0}' is not a known servlet.  Servlets belong in the classpath, often in WEB-INF/classes.", _servletClassName));
      }
      else {
        String location = _location != null ? _location : "";

        log.warning(L.l(location + "'{0}' is not a known servlet.  Servlets belong in the classpath, often in WEB-INF/classes.", _servletClassName));
        return;
      }

      Config.checkCanInstantiate(_servletClass);

      if (Servlet.class.isAssignableFrom(_servletClass)) {
      }
      else if (_protocolConfig != null || _protocolFactory != null) {
      }
      else {
        // XXX: should allow soap
        throw error(L.l("'{0}' must implement javax.servlet.Servlet or have a <protocol>.  All servlets must implement the Servlet interface.", _servletClassName));
      }
      /*
      else if (_servletClass.isAnnotationPresent(WebService.class)) {
        // update protocol for "soap"?
      }
      else if (_servletClass.isAnnotationPresent(WebServiceProvider.class)) {
        // update protocol for "soap"?
      }
      */

      /*
      if (Modifier.isAbstract(_servletClass.getModifiers()))
        throw error(L.l("'{0}' must not be abstract.  Servlets must be fully-implemented classes.", _servletClassName));

      if (! Modifier.isPublic(_servletClass.getModifiers()))
        throw error(L.l("'{0}' must be public.  Servlets must be public classes.", _servletClassName));

      checkConstructor();
      */
    }
  }

  /**
   * Checks the class constructor for the public-zero arg.
   */
  public void checkConstructor()
    throws ServletException
  {
    Constructor []constructors = _servletClass.getDeclaredConstructors();

    Constructor zeroArg = null;
    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == 0) {
        zeroArg = constructors[i];
        break;
      }
    }

    if (zeroArg == null)
      throw error(L.l("'{0}' must have a zero arg constructor.  Servlets must have public zero-arg constructors.\n{1} is not a valid constructor.", _servletClassName, constructors != null ? constructors[0] : null));


    if (! Modifier.isPublic(zeroArg.getModifiers()))
        throw error(L.l("'{0}' must be public.  '{1}' must have a public, zero-arg constructor.",
                        zeroArg,
                        _servletClassName));
  }

  /**
   * Handles a cron alarm callback.
   */
  public void handleAlarm(Alarm alarm)
  {
    try {
      log.fine(this + " cron");

      FilterChain chain = createServletChain();

      ServletRequest req = new StubServletRequest();
      ServletResponse res = new StubServletResponse();

      chain.doFilter(req, res);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      long now = Alarm.getCurrentTime();
      long nextTime = nextTimeout(now);

      Alarm nextAlarm = _alarm;
      if (nextAlarm != null)
        alarm.queue(nextTime - now);
    }
  }

  private long nextTimeout(long now)
  {
    if (_cron != null)
      return _cron.nextTime(now);
    else
      return _runAt.getNextTimeout(now);
  }

  public FilterChain createServletChain()
    throws ServletException
  {
    synchronized (this) {
      // JSP files need to have separate chains created for each JSP

      if (_servletChain != null)
        return _servletChain;
      else
        return createServletChainImpl();
    }
  }

  private FilterChain createServletChainImpl()
    throws ServletException
  {
    String jspFile = getJspFile();
    FilterChain servletChain = null;

    if (jspFile != null) {
      QServlet jsp = (QServlet) _servletManager.createServlet("resin-jsp");

      servletChain = new PageFilterChain(_servletContext, jsp, jspFile, this);

      return servletChain;
    }
    else if (_singletonServlet != null) {
      servletChain = new ServletFilterChain(this);

      return servletChain;
    }

    validateClass(true);

    Class servletClass = getServletClass();

    if (servletClass == null) {
      throw new IllegalStateException(L.l("servlet class for {0} can't be null",
                                          getServletName()));
    }
    else if (QServlet.class.isAssignableFrom(servletClass)) {
      servletChain = new PageFilterChain(_servletContext, (QServlet) createServlet(false));
    }
    else if (SingleThreadModel.class.isAssignableFrom(servletClass)) {
      servletChain = new SingleThreadServletFilterChain(this);
    }
    else if (_protocolConfig != null || _protocolFactory != null) {
      servletChain = new WebServiceFilterChain(this);
    }
    else if (CometServlet.class.isAssignableFrom(servletClass))
      servletChain = new CometServletFilterChain(this);
    else {
      servletChain = new ServletFilterChain(this);
    }

    /*
    if (_roleMap != null)
      servletChain = new SecurityRoleMapFilterChain(servletChain, _roleMap);
    */

    // server/10a8.  JSP pages need a fresh PageFilterChain
    // XXX: lock contention issues with JSPs?
    /*
    if (! QServlet.class.isAssignableFrom(servletClass))
      _servletChain = servletChain;
    */

    return servletChain;
  }

  /**
   * Instantiates a web service.
   *
   * @return the initialized servlet.
   */
  /*
  ProtocolServlet createWebServiceSkeleton()
    throws ServletException
  {
    try {
      Object service = createServlet(false);

      ProtocolServlet skeleton
        = (ProtocolServlet) _protocolClass.newInstance();

      skeleton.setService(service);

      if (_protocolInit != null) {
        _protocolInit.configure(skeleton);
      }

      skeleton.init(this);

      return skeleton;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
  */

  /**
   * Instantiates a servlet given its configuration.
   *
   * @param isNew
   *
   * @return the initialized servlet.
   */
  Object createServlet(boolean isNew)
    throws ServletException
  {
    // server/102e
    if (_servlet != null && ! isNew)
      return _servlet;
    else if (_singletonServlet != null) {
      // server/1p19
      _servlet = _singletonServlet;

      _singletonServlet.init(this);

      return _singletonServlet;
    }

    Object servlet = null;

    if (Alarm.getCurrentTime() < _nextInitTime)
      throw _initException;

    /*
    if ("javax.faces.webapp.FacesServlet".equals(_servletClassName)) {
      addFacesResolvers();
    }
    */

    try {
      synchronized (this) {
        if (! isNew && _servlet != null)
          return _servlet;

        // XXX: this was outside of the sync block
        servlet = createServletImpl();

        if (! isNew)
          _servlet = servlet;
      }

      if (log.isLoggable(Level.FINE))
        log.finer("Servlet[" + _servletName + "] active");

      //J2EEManagedObject.register(new com.caucho.management.j2ee.Servlet(this));

      if (! isNew) {
        // If the servlet has an MBean, register it
        try {
          Hashtable<String,String> props = new Hashtable<String,String>();

          props.put("type", _servlet.getClass().getSimpleName());
          props.put("name", _servletName);
          Jmx.register(_servlet, props);
        } catch (Exception e) {
          log.finest(e.toString());
        }

        if ((_runAt != null || _cron != null) && _alarm != null) {
          long nextTime = nextTimeout(Alarm.getCurrentTime());
          _alarm.queue(nextTime - Alarm.getCurrentTime());
        }
      }

      if (log.isLoggable(Level.FINE))
        log.finer("Servlet[" + _servletName + "] active");

      return servlet;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  /*
  private void addFacesResolvers()
  {
    ApplicationFactory appFactory = (ApplicationFactory)
      FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);

    if (appFactory != null) {
      Application app = appFactory.getApplication();

      if (app != null) {
        InjectManager beanManager = InjectManager.create();

        app.addELResolver(beanManager.getELResolver());
      }
    }
  }
  */

  Servlet createProtocolServlet()
    throws ServletException
  {
    try {
      Object service = createServletImpl();

      if (_protocolFactory == null)
        _protocolFactory = _protocolConfig.createFactory();

      if (_protocolFactory == null)
        throw new IllegalStateException(L.l("unknown protocol factory for '{0}'",
                                            this));

      Servlet servlet
        = _protocolFactory.createServlet(getServletClass(), service);

      servlet.init(this);

      return servlet;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private Object createServletImpl()
    throws Exception
  {
    if (_bean != null) {
      // XXX: need to ask manager?
      CreationalContextImpl<?> env = new OwnerCreationalContext(_bean);
      
      return _bean.create(env);
    }

    Class<?> servletClass = getServletClass();

    Object servlet;
    if (_jspFile != null) {
      servlet = createJspServlet(_servletName, _jspFile);

      if (servlet == null)
        throw new ServletException(L.l("'{0}' is a missing JSP file.",
                                       _jspFile));
    }

    else if (servletClass != null) {
      InjectManager inject = InjectManager.create();

      _comp = inject.createInjectionTarget(servletClass);

      CreationalContextImpl env = new OwnerCreationalContext(null);

      try {
        // server/1b40
        if (_comp != null) {
          servlet = _comp.produce(env);
          _comp.inject(servlet, env);
        }
        else
          servlet = servletClass.newInstance();
      } catch (InjectionException e) {
        throw ConfigException.createConfig(e);
      }
    }
    else
      throw new ServletException(L.l("Null servlet class for '{0}'.",
                                     _servletName));

    configureServlet(servlet);

    try {
      if (servlet instanceof Page) {
        // server/102i
        // page already configured
      }
      else if (servlet instanceof Servlet) {
        Servlet servletObj = (Servlet) servlet;

        servletObj.init(this);
      }
    } catch (UnavailableException e) {
      setInitException(e);
      throw e;
    }

    return servlet;
  }

  /**
   *  Configure the servlet (everything that is done after
   *  instantiation but before servlet.init()
   */
  void configureServlet(Object servlet)
  {
    //InjectIntrospector.configure(servlet);

    // Initialize bean properties
    ConfigProgram init = getInit();

    if (init != null)
      init.configure(servlet);

    Config.init(servlet);
  }

  /**
   * Instantiates a servlet given its configuration.
   *
   * @param servletName the servlet
   *
   * @return the initialized servlet.
   */
  private Servlet createJspServlet(String servletName, String jspFile)
    throws ServletException
  {
    try {
      ServletConfigImpl jspConfig = _servletManager.getServlet("resin-jsp");

      QServlet jsp = (QServlet) jspConfig.createServlet(false);

      // server/105o
      Page page = jsp.getPage(servletName, jspFile, this);

      return page;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  void killServlet()
  {
    Object servlet = _servlet;
    _servlet = null;

    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();

    if (_comp != null)
      _comp.preDestroy(servlet);

    if (servlet instanceof Servlet) {
      ((Servlet) servlet).destroy();
    }
  }

  public void close()
  {
    killServlet();

    _alarm = null;
  }

  protected ConfigException error(String msg)
  {
    ConfigException e;

    if (_location != null)
      e = new LineConfigException(_location + msg);
    else
      e = new ConfigException(msg);

    log.warning(e.getMessage());

    return e;
  }

  protected ConfigException error(String msg, Throwable e)
  {
    ConfigException e1;

    if (_location != null)
      e1 = new LineConfigException(_location + msg, e);
    else
      e1 = new ConfigException(msg, e);

    log.warning(e1.getMessage());

    return e1;
  }

  protected RuntimeException error(Throwable e)
  {
    RuntimeException e1;

    if (_location != null)
      e1 = new LineConfigException(_location + e.getMessage(), e);
    else
      e1 = ConfigException.create(e);

    log.warning(e1.toString());

    return e1;
  }

  /**
   * Returns a printable representation of the servlet config object.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[name=" + _servletName + ",class=" + _servletClass + "]";
  }

  public static class RunAs {
    private String _roleName;

    public void setRoleName(String roleName)
    {
      _roleName = roleName;
    }

    public String getRoleName()
    {
      return _roleName;
    }
  }
}
