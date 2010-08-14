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
 * @author Scott Ferguson
 */

package com.caucho.jsf.application;

import com.caucho.config.Config;
import com.caucho.jsf.cfg.JsfPropertyGroup;
import com.caucho.jsf.cfg.ManagedBeanConfig;
import com.caucho.jsf.cfg.ResourceBundleConfig;
import com.caucho.jsf.context.FacesELContext;
import com.caucho.jsf.el.FacesContextELResolver;
import com.caucho.jsf.el.FacesJspELResolver;
import com.caucho.jsf.el.JsfResourceBundleELResolver;
import com.caucho.jsf.el.MethodBindingAdapter;
import com.caucho.jsf.el.ValueBindingAdapter;
import com.caucho.jsf.el.ValueExpressionAdapter;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.jsp.BundleManager;

import javax.el.*;
import javax.el.PropertyNotFoundException;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.FacesContext;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionListener;
import javax.faces.validator.DoubleRangeValidator;
import javax.faces.validator.LengthValidator;
import javax.faces.validator.LongRangeValidator;
import javax.faces.validator.Validator;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import java.beans.FeatureDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationImpl
  extends Application
{
  private static final L10N L = new L10N(ApplicationImpl.class);

  private static final Logger log
    = Logger.getLogger(ApplicationImpl.class.getName());

  private ActionListener _actionListener;
  private StateManager _stateManager;
  private ViewHandler _viewHandler;
  private NavigationHandler _navigationHandler;
  final private NavigationHandlerImpl _defaultNavigationHandler;

  private PropertyResolver _propertyResolver;
  private VariableResolver _variableResolver;

  private ExpressionFactory _jsfExpressionFactory;

  private FacesContextELResolver _elResolver;

  private JsfResourceBundleELResolver _bundleResolver
    = new JsfResourceBundleELResolver();

  private ArrayList<Locale> _locales;
  private Locale _defaultLocale;

  private ArrayList<ELContextListener> _elContextListenerList
    = new ArrayList<ELContextListener>();

  private ELContextListener []_elContextListeners;

  private HashMap<String, String> _componentClassNameMap
    = new HashMap<String, String>();

  private HashMap<String, Class> _componentClassMap
    = new HashMap<String, Class>();

  private HashMap<String, String> _validatorClassMap
    = new HashMap<String, String>();

  private HashMap<String, String> _converterIdNameMap
    = new HashMap<String, String>();

  private HashMap<String, Class> _converterIdMap
    = new HashMap<String, Class>();

  private HashMap<Class<?>, String> _converterClassNameMap
    = new HashMap<Class<?>, String>();

  private HashMap<Class<?>, Class<?>> _converterClassMap
    = new HashMap<Class<?>, Class<?>>();

  private String _defaultRenderKitId = "HTML_BASIC";

  private String _messageBundle;

  private boolean _isInit;

  private PropertyResolver _legacyPropertyResolver;
  private VariableResolver _legacyVariableResolver;

  private BundleManager _bundleManager;

  public ApplicationImpl()
  {
    WebApp webApp = WebApp.getLocal();

    JspFactory jspFactory = JspFactory.getDefaultFactory();

    JspApplicationContext appContext
      = jspFactory.getJspApplicationContext(webApp);

    _defaultNavigationHandler = new NavigationHandlerImpl();

    _bundleManager = BundleManager.create();

    _jsfExpressionFactory = appContext.getExpressionFactory();

    ELResolver []customResolvers = new ELResolver[0];
    _elResolver = new FacesContextELResolver(customResolvers,
                                             _bundleResolver);

    setViewHandler(new JspViewHandler());

    SessionStateManager stateManager = new SessionStateManager();
    
    JsfPropertyGroup jsfPropertyGroup = webApp.getJsf();

    if (jsfPropertyGroup != null)
      stateManager.setStateSerializationMethod(
        jsfPropertyGroup.getStateSerializationMethod());

    setStateManager(stateManager);

    
    appContext.addELResolver(new FacesJspELResolver(this));

    addComponent(UIColumn.COMPONENT_TYPE,
                 "javax.faces.component.UIColumn");

    addComponent(UICommand.COMPONENT_TYPE,
                 "javax.faces.component.UICommand");

    addComponent(UIData.COMPONENT_TYPE,
                 "javax.faces.component.UIData");

    addComponent(UIForm.COMPONENT_TYPE,
                 "javax.faces.component.UIForm");

    addComponent(UIGraphic.COMPONENT_TYPE,
                 "javax.faces.component.UIGraphic");

    addComponent(UIInput.COMPONENT_TYPE,
                 "javax.faces.component.UIInput");

    addComponent(UIMessage.COMPONENT_TYPE,
                 "javax.faces.component.UIMessage");

    addComponent(UIMessages.COMPONENT_TYPE,
                 "javax.faces.component.UIMessages");

    addComponent(UINamingContainer.COMPONENT_TYPE,
                 "javax.faces.component.UINamingContainer");

    addComponent(UIOutput.COMPONENT_TYPE,
                 "javax.faces.component.UIOutput");

    addComponent(UIPanel.COMPONENT_TYPE,
                  "javax.faces.component.UIPanel");

    addComponent(UIParameter.COMPONENT_TYPE,
                 "javax.faces.component.UIParameter");

    addComponent(UISelectBoolean.COMPONENT_TYPE,
                 "javax.faces.component.UISelectBoolean");

    addComponent(UISelectOne.COMPONENT_TYPE,
                 "javax.faces.component.UISelectOne");

    addComponent(UISelectMany.COMPONENT_TYPE,
                 "javax.faces.component.UISelectMany");

    addComponent(UISelectItem.COMPONENT_TYPE,
                 "javax.faces.component.UISelectItem");

    addComponent(UISelectItems.COMPONENT_TYPE,
                 "javax.faces.component.UISelectItems");

    addComponent(UIViewRoot.COMPONENT_TYPE,
                 "javax.faces.component.UIViewRoot");

    addComponent(HtmlCommandButton.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlCommandButton");

    addComponent(HtmlCommandLink.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlCommandLink");

    addComponent(HtmlDataTable.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlDataTable");

    addComponent(HtmlGraphicImage.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlGraphicImage");

    addComponent(HtmlInputHidden.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlInputHidden");

    addComponent(HtmlInputSecret.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlInputSecret");

    addComponent(HtmlInputText.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlInputText");

    addComponent(HtmlInputTextarea.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlInputTextarea");

    addComponent(HtmlMessage.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlMessage");

    addComponent(HtmlMessages.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlMessages");

    addComponent(HtmlOutputFormat.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlOutputFormat");

    addComponent(HtmlOutputLabel.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlOutputLabel");

    addComponent(HtmlOutputLink.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlOutputLink");

    addComponent(HtmlOutputText.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlOutputText");

    addComponent(HtmlPanelGrid.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlPanelGrid");

    addComponent(HtmlPanelGroup.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlPanelGroup");

    addComponent(HtmlForm.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlForm");

    addComponent(HtmlSelectBooleanCheckbox.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlSelectBooleanCheckbox");

    addComponent(HtmlSelectManyCheckbox.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlSelectManyCheckbox");

    addComponent(HtmlSelectManyListbox.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlSelectManyListbox");

    addComponent(HtmlSelectManyMenu.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlSelectManyMenu");

    addComponent(HtmlSelectOneListbox.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlSelectOneListbox");

    addComponent(HtmlSelectOneMenu.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlSelectOneMenu");

    addComponent(HtmlSelectOneRadio.COMPONENT_TYPE,
                 "javax.faces.component.html.HtmlSelectOneRadio");

    addConverter(BooleanConverter.CONVERTER_ID,
                 BooleanConverter.class.getName());
    addConverter(boolean.class, BooleanConverter.class.getName());
    addConverter(Boolean.class, BooleanConverter.class.getName());

    addConverter(CharacterConverter.CONVERTER_ID,
                 CharacterConverter.class.getName());
    addConverter(char.class, CharacterConverter.class.getName());
    addConverter(Character.class, CharacterConverter.class.getName());

    addConverter(ByteConverter.CONVERTER_ID,
                 ByteConverter.class.getName());
    addConverter(byte.class, ByteConverter.class.getName());
    addConverter(Byte.class, ByteConverter.class.getName());
    addConverter(Byte.TYPE, ByteConverter.class.getName());

    addConverter(ShortConverter.CONVERTER_ID, ShortConverter.class.getName());
    addConverter(short.class, ShortConverter.class.getName());
    addConverter(Short.class, ShortConverter.class.getName());

    addConverter(IntegerConverter.CONVERTER_ID,
                 IntegerConverter.class.getName());
    addConverter(int.class, IntegerConverter.class.getName());
    addConverter(Integer.class, IntegerConverter.class.getName());

    addConverter(LongConverter.CONVERTER_ID, LongConverter.class.getName());
    addConverter(long.class, LongConverter.class.getName());
    addConverter(Long.class, LongConverter.class.getName());

    addConverter(FloatConverter.CONVERTER_ID, FloatConverter.class.getName());
    addConverter(float.class, FloatConverter.class.getName());
    addConverter(Float.class, FloatConverter.class.getName());

    addConverter(DoubleConverter.CONVERTER_ID, DoubleConverter.class.getName());
    addConverter(double.class, DoubleConverter.class.getName());
    addConverter(Double.class, DoubleConverter.class.getName());

    addConverter(DateTimeConverter.CONVERTER_ID,
                 DateTimeConverter.class.getName());

    addConverter(NumberConverter.CONVERTER_ID,
                 NumberConverter.class.getName());

    addConverter(BigDecimalConverter.CONVERTER_ID,
                 BigDecimalConverter.class.getName());
    addConverter(java.math.BigDecimal.class,
                 BigDecimalConverter.class.getName());

    addConverter(BigIntegerConverter.CONVERTER_ID,
                 BigIntegerConverter.class.getName());
    addConverter(java.math.BigInteger.class,
                 BigIntegerConverter.class.getName());

    addConverter(EnumConverter.CONVERTER_ID, EnumConverter.class.getName());
    addConverter(Enum.class, EnumConverter.class.getName());

    addValidator(DoubleRangeValidator.VALIDATOR_ID,
                 DoubleRangeValidator.class.getName());
    addValidator(LengthValidator.VALIDATOR_ID,
                 LengthValidator.class.getName());
    addValidator(LongRangeValidator.VALIDATOR_ID,
                 LongRangeValidator.class.getName());
  }

  public void addManagedBean(String name, ManagedBeanConfig managedBean)
  {
    _elResolver.addManagedBean(name, managedBean);
  }

  public void addResourceBundle(String name, ResourceBundleConfig bundle)
  {
    _bundleResolver.addBundle(name, bundle);
  }

  public ActionListener getActionListener()
  {
    if (_actionListener == null)
      _actionListener = new ActionListenerImpl();

    return _actionListener;
  }

  public void setActionListener(ActionListener listener)
  {
    if (listener == null)
      throw new NullPointerException();

    _actionListener = listener;
  }

  public Locale getDefaultLocale()
  {
    return _defaultLocale;
  }

  public void setDefaultLocale(Locale locale)
  {
    if (locale == null)
      throw new NullPointerException();

    _defaultLocale = locale;
  }

  public String getDefaultRenderKitId()
  {
    return _defaultRenderKitId;
  }

  public void setDefaultRenderKitId(String renderKitId)
  {
    _defaultRenderKitId = renderKitId;
  }

  public String getMessageBundle()
  {
    return _messageBundle;
  }

  public void setMessageBundle(String bundle)
  {
    _messageBundle = bundle;
  }

  @Override
  public ResourceBundle getResourceBundle(FacesContext context,
                                          String name)
  {
    UIViewRoot viewRoot = context.getViewRoot();

    Locale locale = null;
    
    if (viewRoot != null)
      locale = viewRoot.getLocale();
    
    LocalizationContext l10nCtx = null;

    if (locale != null)
      l10nCtx = _bundleManager.getBundle(name, locale);

    if (l10nCtx == null)
      l10nCtx = _bundleManager.getBundle(name);

    if (l10nCtx != null)
      return l10nCtx.getResourceBundle();

    return null;
  }

  public NavigationHandler getNavigationHandler()
  {
    if (_navigationHandler == null)
      return _defaultNavigationHandler;

    return _navigationHandler;
  }

  public void setNavigationHandler(NavigationHandler handler)
  {
    if (handler == null)
      throw new NullPointerException();

    _navigationHandler = handler;
  }

  public NavigationHandlerImpl getDefaultNavigationHandler(){
    return _defaultNavigationHandler;
  }

  @Deprecated
  public PropertyResolver getPropertyResolver()
  {
    if (_propertyResolver == null)
      _propertyResolver = new PropertyResolverAdapter(getELResolver());

    return _propertyResolver;
  }

  @Deprecated
  public void setPropertyResolver(PropertyResolver resolver)
  {
    if (_legacyPropertyResolver == null ||
        _legacyPropertyResolver instanceof DummyPropertyResolver) {
       addELResolver(new PropertyResolverChainWrapper());
    }

    _legacyPropertyResolver = resolver;
  }

  public PropertyResolver getLegacyPropertyResolver()
  {
    if (_legacyPropertyResolver == null)
      _legacyPropertyResolver = new DummyPropertyResolver();
    
    return _legacyPropertyResolver;
  }

  @Deprecated
  public VariableResolver getVariableResolver()
  {
    if (_variableResolver == null)
      _variableResolver = new VariableResolverAdapter(getELResolver());

    return _variableResolver;
  }

  @Deprecated
  public void setVariableResolver(VariableResolver resolver)
  {
    if (_legacyVariableResolver == null ||
        _legacyVariableResolver instanceof DummyVariableResolver) {
      addELResolver(new VariableResolverChainWrapper());
    }

    _legacyVariableResolver = resolver;
  }

  public VariableResolver getLegacyVariableResolver(){
    if (_legacyVariableResolver == null)
      _legacyVariableResolver = new DummyVariableResolver();

    return _legacyVariableResolver;
  }
  /**
   * @Since 1.2
   */
  public void addELResolver(ELResolver resolver)
  {
    if (_isInit)
      throw new IllegalStateException(L.l(
        "Can't add ELResolver after Application has been initialized"));
    _elResolver.addELResolver(resolver);
  }

  /**
   * @Since 1.2
   */
  public void addELContextListener(ELContextListener listener)
  {
    _elContextListenerList.add(listener);
    _elContextListeners = null;
  }

  /**
   * @Since 1.2
   */
  public void removeELContextListener(ELContextListener listener)
  {
    _elContextListenerList.remove(listener);
    _elContextListeners = null;
  }

  /**
   * @Since 1.2
   */
  public ELContextListener []getELContextListeners()
  {
    synchronized (_elContextListenerList) {
      if (_elContextListeners == null) {
        _elContextListeners
          = new ELContextListener[_elContextListenerList.size()];

        _elContextListenerList.toArray(_elContextListeners);
      }
    }

    return _elContextListeners;
  }

  /**
   * @Since 1.2
   */
  public ExpressionFactory getExpressionFactory()
  {
    return _jsfExpressionFactory;
  }

  @Override
  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  public ViewHandler getViewHandler()
  {
    return _viewHandler;
  }

  public void setViewHandler(ViewHandler handler)
  {
    if (handler == null)
      throw new NullPointerException();

    _viewHandler = handler;
  }

  public StateManager getStateManager()
  {
    return _stateManager;
  }

  public void setStateManager(StateManager manager)
  {
    _stateManager = manager;
  }

  public void addComponent(String componentType,
                           String componentClass)
  {
    if (componentType == null)
      throw new NullPointerException();

    if (componentClass == null)
      throw new NullPointerException();

    synchronized (_componentClassNameMap) {
      _componentClassNameMap.put(componentType, componentClass);
    }
  }

  public UIComponent createComponent(String componentType)
    throws FacesException
  {
    if (componentType == null)
      throw new NullPointerException();

    Class cl = getComponentClass(componentType);

    if (cl == null)
      throw new FacesException(L.l(
        "'{0}' is an unknown UI componentType to create",
        componentType));

    try {
      return (UIComponent) cl.newInstance();
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new FacesException(e);
    }
  }

  private Class getComponentClass(String name)
  {
    synchronized (_componentClassMap) {
      Class cl = _componentClassMap.get(name);

      if (cl != null)
        return cl;

      String className = _componentClassNameMap.get(name);

      if (className == null)
        throw new FacesException(L.l("'{0}' is an unknown component type",
                                     name));

      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        cl = Class.forName(className, false, loader);

        Config.validate(cl, UIComponent.class);

        _componentClassMap.put(name, cl);

        return cl;
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new FacesException(e);
      }
    }
  }

  /**
   * @Since 1.2
   */
  public UIComponent createComponent(ValueExpression componentExpr,
                                     FacesContext context,
                                     String componentType)
    throws FacesException
  {
    if (componentExpr == null
        || context == null
        || componentType == null)
      throw new NullPointerException();

    Object value = componentExpr.getValue(context.getELContext());

    if (value instanceof UIComponent)
      return (UIComponent) value;

    UIComponent component = createComponent(componentType);

    componentExpr.setValue(context.getELContext(), component);

    return component;
  }

  @Deprecated
  public UIComponent createComponent(ValueBinding componentBinding,
                                     FacesContext context,
                                     String componentType)
    throws FacesException
  {
    if (componentBinding == null
        || context == null
        || componentType == null)
      throw new NullPointerException();

    return createComponent(new ValueExpressionAdapter(componentBinding,
                                                      UIComponent.class),
                           context,
                           componentType);
  }

  public Iterator<String> getComponentTypes()
  {
    return _componentClassNameMap.keySet().iterator();
  }

  public void addConverter(String converterId,
                           String converterClass)
  {
    if (converterId == null)
      throw new NullPointerException();

    synchronized (_converterIdMap) {
      _converterIdNameMap.put(converterId, converterClass);
    }
  }

  public Converter createConverter(String converterId)
    throws FacesException
  {
    if (converterId == null)
      throw new NullPointerException();

    Class cl = getConverterIdClass(converterId);

    if (cl == null)
      return null;

    try {
      return (Converter) cl.newInstance();
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new FacesException(e);
    }
  }

  private Class getConverterIdClass(String id)
  {
    synchronized (_converterIdMap) {
      Class cl = _converterIdMap.get(id);

      if (cl != null)
        return cl;

      String className = _converterIdNameMap.get(id);

      if (className == null)
        throw new FacesException(L.l("'{0}' is an unknown converter type",
                                     id));

      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        cl = Class.forName(className, false, loader);

        Config.validate(cl, Converter.class);

        _converterIdMap.put(id, cl);

        return cl;
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new FacesException(e);
      }
    }
  }

  public Iterator<String> getConverterIds()
  {
    return _converterIdNameMap.keySet().iterator();
  }

  public void addConverter(Class type,
                           String converterClass)
  {
    if (type == null)
      throw new NullPointerException();

    synchronized (_converterClassMap) {
      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class cl = Class.forName(converterClass, false, loader);

        Config.validate(cl, Converter.class);

        _converterClassMap.put(type, cl);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new FacesException(e);
      }
    }
  }

  public Converter createConverter(Class type)
    throws FacesException
  {
    if (type == null)
      throw new NullPointerException();

    Class cl = findConverter(type);

    if (cl == null)
      return null;

    try {

      try {
        Constructor constructor = cl.getConstructor(Class.class);

        return (Converter) constructor.newInstance(type);
      }
      catch (NoSuchMethodException ignore) {
      }

      return (Converter) cl.newInstance();
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new FacesException(e);
    }
  }

  private Class findConverter(Class type)
  {
    if (type == null)
      return null;

    Class cl;

    synchronized (_converterClassMap) {
      cl = _converterClassMap.get(type);
    }

    if (cl != null)
      return cl;

    Class []interfaces = type.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      cl = findConverter(interfaces[i]);

      if (cl != null)
        return cl;
    }

    return findConverter(type.getSuperclass());
  }

  public Iterator getConverterTypes()
  {
    return _converterClassMap.keySet().iterator();
  }

  @Deprecated
  public MethodBinding createMethodBinding(String ref,
                                           Class []param)
    throws ReferenceSyntaxException
  {
    ExpressionFactory factory = getExpressionFactory();

    ELResolver elResolver = getELResolver();
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = new FacesELContext(facesContext, elResolver);

    if (param == null)
      param = new Class[0];

    if (!ref.startsWith("#{") && !ref.endsWith("}"))
      throw new ReferenceSyntaxException(L.l(
        "'{0}' is an illegal MethodBinding.  MethodBindings require #{...} syntax.",
        ref));

    try {
      MethodExpression expr
        = factory.createMethodExpression(elContext, ref, Object.class, param);

      return new MethodBindingAdapter(expr, param);
    }
    catch (ELException e) {
      throw new ReferenceSyntaxException(e);
    }
  }

  public Iterator<Locale> getSupportedLocales()
  {
    if (_locales != null)
      return _locales.iterator();
    else
      return new ArrayList<Locale>().iterator();
  }

  public void setSupportedLocales(Collection<Locale> locales)
  {
    _locales = new ArrayList<Locale>(locales);
  }

  public void addValidator(String validatorId, String validatorClass)
  {
    if (validatorId == null || validatorClass == null)
      throw new NullPointerException();

    _validatorClassMap.put(validatorId, validatorClass);
  }

  public Validator createValidator(String validatorId)
    throws FacesException
  {
    if (validatorId == null)
      throw new NullPointerException();

    try {
      String validatorClass = _validatorClassMap.get(validatorId);

      if (validatorClass == null)
        throw new FacesException(L.l("'{0}' is not a known validator.",
                                     validatorId));

      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      Class cl = Class.forName(validatorClass, false, loader);

      return (Validator) cl.newInstance();
    }
    catch (FacesException e) {
      throw e;
    }
    catch (Exception e) {
      throw new FacesException(e);
    }
  }

  public Iterator<String> getValidatorIds()
  {
    return _validatorClassMap.keySet().iterator();
  }

  @Override
  public ValueBinding createValueBinding(String ref)
    throws ReferenceSyntaxException
  {
    ExpressionFactory factory = getExpressionFactory();

    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = new FacesELContext(facesContext, getELResolver());

    try {
      ValueExpression expr
        = factory.createValueExpression(elContext, ref, Object.class);

      ValueBinding binding = new ValueBindingAdapter(expr);

      return binding;
    }
    catch (ELException e) {
      throw new ReferenceSyntaxException(e);
    }
  }

  @Override
  public Object evaluateExpressionGet(FacesContext context,
                                      String expression,
                                      Class expectedType)
  {
    ExpressionFactory factory = getExpressionFactory();

    ELContext elContext = context.getELContext();

    ValueExpression expr
      = factory.createValueExpression(elContext, expression, expectedType);

    return expr.getValue(elContext);
  }

  public void initRequest()
  {
    _isInit = true;

    if (_viewHandler == null)
      _viewHandler = new JspViewHandler();

    if (_stateManager == null) {
      _stateManager = new SessionStateManager();

      JsfPropertyGroup jsfPropertyGroup = WebApp.getLocal().getJsf();

      if (jsfPropertyGroup != null)
        ((SessionStateManager) _stateManager).setStateSerializationMethod(
          jsfPropertyGroup.getStateSerializationMethod());
    }
  }

  public String toString()
  {
    return "ApplicationImpl[]";
  }

  static class PropertyResolverAdapter
    extends PropertyResolver
  {
    private ELResolver _elResolver;

    PropertyResolverAdapter(ELResolver elResolver)
    {
      _elResolver = elResolver;
    }

    public Class getType(Object base, int index)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null) {
        throw new javax.faces.el.PropertyNotFoundException(
          "base can not be null");
      }
      else if (base.getClass().isArray()) {
        try {
          Object value = Array.get(base, index);

          if (value == null)
            return null;
          else
            return value.getClass();
        }
        catch (ArrayIndexOutOfBoundsException e) {
          throw new javax.faces.el.PropertyNotFoundException("index '" +
                                                             index +
                                                             "' is out of bounds");
        }
      }
      else if (base instanceof List) {
        List list = (List) base;

        try {
          Object value = list.get(index);

          if (value == null)
            return null;
          else
            return value.getClass();
        }
        catch (IndexOutOfBoundsException e) {
          throw new javax.faces.el.PropertyNotFoundException("index '" +
                                                             index +
                                                             "' is out of bounds");
        }
      }
      else {
        throw new javax.faces.el.PropertyNotFoundException(
          "wrong type of the base '" +
          base.getClass().getName() +
          "', only java.util.List and arrays are accepted");
      }
    }

    public Class getType(Object base, Object property)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null)
        throw new javax.faces.el.PropertyNotFoundException();

      try {
        FacesContext context = FacesContext.getCurrentInstance();

        return _elResolver.getType(context.getELContext(), base, property);
      }
      catch (javax.el.PropertyNotFoundException e) {
        throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }

    public Object getValue(Object base, int index)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null)
        return null;
      else if (base.getClass().isArray()) {
        try {
          return Array.get(base, index);
        }
        catch (ArrayIndexOutOfBoundsException e) {
          return null;
        }
      }
      else if (base instanceof List) {
        List list = (List) base;

        try {
          return list.get(index);
        }
        catch (IndexOutOfBoundsException e) {
          return null;
        }
      }
      else {
        throw new javax.faces.el.PropertyNotFoundException(
          "wrong type of the base '" +
          base.getClass().getName() +
          "', only java.util.List and arrays are accepted");
      }
    }

    public Object getValue(Object base, Object property)
      throws javax.faces.el.PropertyNotFoundException
    {
      try {
        FacesContext context = FacesContext.getCurrentInstance();

        return _elResolver.getValue(context.getELContext(), base, property);
      }
      catch (javax.el.PropertyNotFoundException e) {
        throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }

    public boolean isReadOnly(Object base, int index)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null) {
        throw new javax.faces.el.PropertyNotFoundException(
          "base can not be null");
      }
      else if (base.getClass().isArray()) {
        if (index >= 0 && index < Array.getLength(base))
          return false;
        else
          throw new javax.faces.el.PropertyNotFoundException("index '" +
                                                             index +
                                                             "' is out of bounds");
      }
      else if (base instanceof List) {
        List list = (List) base;
        if (index >= 0 && index < list.size())
          return false;
        else
          throw new javax.faces.el.PropertyNotFoundException("index '" +
                                                             index +
                                                             "' is out of bounds");
      }
      else {
        throw new javax.faces.el.PropertyNotFoundException(
          "wrong type of the base '" +
          base.getClass().getName() +
          "', only java.util.List and arrays are accepted");
      }
    }

    public boolean isReadOnly(Object base, Object property)
      throws javax.faces.el.PropertyNotFoundException
    {
      try {
        FacesContext context = FacesContext.getCurrentInstance();

        return _elResolver.isReadOnly(context.getELContext(), base, property);
      }
      catch (javax.el.PropertyNotFoundException e) {
        throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }

    public void setValue(Object base, int index, Object value)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null) {
        throw new javax.faces.el.PropertyNotFoundException(
          "base can not be null");
      }
      else if (base.getClass().isArray()) {
        try {
          Array.set(base, index, value);
        }
        catch (ArrayIndexOutOfBoundsException e) {
          throw new javax.faces.el.PropertyNotFoundException("index '" +
                                                             index +
                                                             "' is out of bounds");
        }
      }
      else if (base instanceof List) {
        List list = (List) base;

        try {
          list.set(index, value);
        }
        catch (IndexOutOfBoundsException e) {
          throw new javax.faces.el.PropertyNotFoundException("index '" +
                                                             index +
                                                             "' is out of bounds");
        }
      }
      else {
        throw new javax.faces.el.PropertyNotFoundException(
          "wrong type of the base '" +
          base.getClass().getName() +
          "', only java.util.List and arrays are accepted");
      }
    }

    public void setValue(Object base, Object property, Object value)
      throws javax.faces.el.PropertyNotFoundException
    {
      try {
        FacesContext context = FacesContext.getCurrentInstance();

        _elResolver.setValue(context.getELContext(), base, property, value);
      }
      catch (javax.el.PropertyNotFoundException e) {
        throw new javax.faces.el.PropertyNotFoundException(e);
      }
      catch (javax.el.PropertyNotWritableException e) {
        throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
  }

  static class VariableResolverAdapter
    extends VariableResolver
  {
    private ELResolver _elResolver;

    VariableResolverAdapter(ELResolver elResolver)
    {
      _elResolver = elResolver;
    }

    public Object resolveVariable(FacesContext context, String value)
    {
      return _elResolver.getValue(context.getELContext(), null, value);
    }
  }


  class VariableResolverChainWrapper
    extends ELResolver {

    VariableResolverChainWrapper()
    {
    }

    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
      if (base == null)
        return String.class;
      else
        return null;
    }

    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                             Object base)
    {
      return null;
    }

    public Class<?> getType(ELContext context, Object base, Object property)
    {
      if (base == null && property == null)
        throw new PropertyNotFoundException();

       return null;
    }

    public Object getValue(ELContext context, Object base, Object property)
      throws PropertyNotFoundException, ELException
    {
      if (base != null)
        return null;

      if (property == null && base == null)
        throw new PropertyNotFoundException();

      context.setPropertyResolved(true);

      FacesContext facesContext = FacesContext.getCurrentInstance();

      try {
        return _legacyVariableResolver.resolveVariable(facesContext,
                                                       (String) property);
      }
      catch (EvaluationException e) {
        context.setPropertyResolved(false);

        throw new ELException(e);
      } catch (RuntimeException e) {
        context.setPropertyResolved(false);

        throw e;
      } catch (Exception e){
        context.setPropertyResolved(false);

        throw new ELException(e);
      }      
    }

    public boolean isReadOnly(ELContext context, Object base, Object property)
      throws PropertyNotFoundException, ELException
    {
      if (base == null && property == null)
        throw new PropertyNotFoundException();
      
      return false;
    }

    public void setValue(ELContext context,
                         Object base,
                         Object property,
                         Object value)
      throws
      PropertyNotFoundException, PropertyNotWritableException, ELException
    {
      if (base == null && property == null)
        throw new PropertyNotFoundException();
    }
  }
  
  class PropertyResolverChainWrapper
    extends ELResolver
  {

    PropertyResolverChainWrapper()
    {
    }

    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
      if (base == null)
        return null;

      return Object.class;
    }

    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                             Object base)
    {
      return null;
    }

    public Class<?> getType(ELContext context, Object base, Object property)
    {
      if (base == null || property == null)
        return null;

      try {
        if (base.getClass().isArray() || base instanceof List)
          return _legacyPropertyResolver.getType(base, ((Long) property).intValue());
        else
          return _legacyPropertyResolver.getType(base, property);
      }
      catch (PropertyNotFoundException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (EvaluationException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (RuntimeException e) {
        context.setPropertyResolved(false);

        throw e;
      }
    }

    public Object getValue(ELContext context, Object base, Object property)
      throws PropertyNotFoundException, ELException
    {
      if (base == null || property == null)
        return null;

      context.setPropertyResolved(true);

      try {
        if (base.getClass().isArray() || base instanceof List)
          return _legacyPropertyResolver.getValue(base, ((Long) property).intValue());
        else
          return _legacyPropertyResolver.getValue(base, property);
      }
      catch (PropertyNotFoundException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (EvaluationException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (RuntimeException e) {
        context.setPropertyResolved(false);

        throw e;
      }
    }

    public boolean isReadOnly(ELContext context, Object base, Object property)
      throws PropertyNotFoundException, ELException
    {
      if (base == null || property == null)
        return true;
      
      try {
        if (base.getClass().isArray() || base instanceof List)
          return _legacyPropertyResolver.isReadOnly(base, ((Long) property).intValue());
        else
          return _legacyPropertyResolver.isReadOnly(base, property);
      }
      catch (PropertyNotFoundException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (EvaluationException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (RuntimeException e) {
        context.setPropertyResolved(false);

        throw e;
      }
    }

    public void setValue(ELContext context,
                         Object base,
                         Object property,
                         Object value)
      throws
      PropertyNotFoundException, PropertyNotWritableException, ELException
    {
      if (base == null || property == null)
        return;

      try {
        if (base.getClass().isArray() || base instanceof List)
          _legacyPropertyResolver.setValue(base, ((Long) property).intValue(), value);
        else
          _legacyPropertyResolver.setValue(base, property, value);
      }
      catch (PropertyNotFoundException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (EvaluationException e) {
        context.setPropertyResolved(false);

        throw e;
      }
      catch (RuntimeException e) {
        context.setPropertyResolved(false);

        throw e;
      }
    }
  }


  static class DummyPropertyResolver
    extends PropertyResolver
  {

    public Object getValue(Object base, Object property)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);
      
      return null;
    }


    public Object getValue(Object base, int index)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);

      return null;
    }


    public void setValue(Object base, Object property, Object value)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);
    }

    public void setValue(Object base, int index, Object value)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);
    }

    public boolean isReadOnly(Object base, Object property)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);

      return false;
    }

    public boolean isReadOnly(Object base, int index)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);

      return false;
    }


    public Class getType(Object base, Object property)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);

      return null;
    }

    public Class getType(Object base, int index)
      throws EvaluationException, javax.faces.el.PropertyNotFoundException
    {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getELContext().setPropertyResolved(false);
      
      return null;
    }
  }

  static class DummyVariableResolver extends VariableResolver {
    public Object resolveVariable(FacesContext context, String name)
      throws EvaluationException
    {
      context.getELContext().setPropertyResolved(false);

      return null;
    }
  }
}
