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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.soap.wsdl;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import javax.xml.namespace.QName;

import static com.caucho.soap.wsdl.WSDLConstants.*;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import com.caucho.xml.schema.Type;

/**
 * WSDL Definitions top level
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="definitions", namespace=WSDL_NAMESPACE)
public class WSDLDefinitions extends WSDLExtensibleDocumented {
  @XmlTransient
  private static final L10N L = new L10N(WSDLDefinitions.class);

  @XmlAttribute(name="name")
  private String _name;

  @XmlAttribute(name="targetNamespace")
  private String _targetNamespace;

  @XmlElement(name="import", namespace=WSDL_NAMESPACE)
  private List<WSDLImport> _imports;

  @XmlElement(name="types", namespace=WSDL_NAMESPACE, required=true)
  private List<WSDLTypes> _types;

  @XmlElement(name="message", namespace=WSDL_NAMESPACE, required=true)
  private List<WSDLMessage> _messages;

  @XmlElement(name="portType", namespace=WSDL_NAMESPACE, required=true)
  private List<WSDLPortType> _portTypes;

  @XmlElement(name="binding", namespace=WSDL_NAMESPACE, required=true)
  private List<WSDLBinding> _bindings;

  @XmlElement(name="service", namespace=WSDL_NAMESPACE, required=true)
  private List<WSDLService> _services;

  @XmlTransient
  private final Map<QName,WSDLMessage> _messageMap 
    = new HashMap<QName,WSDLMessage>();

  @XmlTransient
  private final Map<QName,WSDLPortType> _portTypeMap 
    = new HashMap<QName,WSDLPortType>();

  @XmlTransient
  private final Map<QName,WSDLBinding> _bindingMap 
    = new HashMap<QName,WSDLBinding>();

  @XmlTransient
  private String _endpointAddress;

  @XmlTransient
  private String _bindingId;

  /**
   * Sets the definition name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  /**
   * Sets the target namespace
   */
  public void setTargetNamespace(String uri)
  {
    _targetNamespace = uri;
  }

  /**
   * Gets the target namespace
   */
  public String getTargetNamespace()
  {
    return _targetNamespace;
  }

  public List<WSDLImport> getImports()
  {
    if (_imports == null)
      return _imports = new ArrayList<WSDLImport>();

    return _imports;
  }

  public List<WSDLTypes> getTypes()
  {
    if (_types == null)
      return _types = new ArrayList<WSDLTypes>();

    return _types;
  }

  public List<WSDLMessage> getMessages()
  {
    if (_messages == null)
      return _messages = new ArrayList<WSDLMessage>();

    return _messages;
  }

  public List<WSDLPortType> getPortTypes()
  {
    if (_portTypes == null)
      return _portTypes = new ArrayList<WSDLPortType>();

    return _portTypes;
  }

  public List<WSDLBinding> getBindings()
  {
    if (_bindings == null)
      return _bindings = new ArrayList<WSDLBinding>();

    return _bindings;
  }

  public List<WSDLService> getServices()
  {
    if (_services == null)
      return _services = new ArrayList<WSDLService>();

    return _services;
  }

  public String getBindingId(QName serviceName, QName portName)
  {
    if (_bindingId == null) {
      if (! serviceName.getNamespaceURI().equals(getTargetNamespace()))
        return null;

      // first find out which binding we're looking for
      QName bindingName = null;

      if (_services != null) {
        for (int i = 0; i < _services.size(); i++) {
          WSDLService service = _services.get(i);

          if (serviceName.getLocalPart().equals(service.getName())) {
            List<WSDLPort> ports = service.getPorts();

            for (int j = 0; j < ports.size(); j++) {
              WSDLPort port = ports.get(j);

              if (portName.getLocalPart().equals(port.getName())) {
                bindingName = port.getBinding();
                break;
              }
            }

            if (bindingName != null)
              break;
          }
        }
      }

      if (bindingName != null && _bindings != null) {
        for (int i = 0; i < _bindings.size(); i++) {
          WSDLBinding binding = _bindings.get(i);

          if (bindingName.getLocalPart().equals(binding.getName())) {
            List<Object> any = binding.getAny();

            for (int j = 0; j < any.size(); j++) {
              if (any.get(j) instanceof SOAPBinding) {
                SOAPBinding soapBinding = (SOAPBinding) any.get(j);

                _bindingId = soapBinding.getTransport();

                return _bindingId;
              }
            }
          }
        }
      }
    }

    return _bindingId;
  }

  public String getEndpointAddress(QName serviceName, QName portName)
  {
    if (_endpointAddress == null) {
      if (! serviceName.getNamespaceURI().equals(getTargetNamespace()))
        return null;

      // dig down to find the <soap:address location="..."/>
      if (_services != null) {
        for (int i = 0; i < _services.size(); i++) {
          WSDLService service = _services.get(i);

          if (serviceName.getLocalPart().equals(service.getName())) {
            List<WSDLPort> ports = service.getPorts();

            for (int j = 0; j < ports.size(); j++) {
              WSDLPort port = ports.get(j);

              if (portName.getLocalPart().equals(port.getName())) {
                List<Object> any = port.getAny();

                for (int k = 0; k < any.size(); k++) {
                  if (any.get(k) instanceof SOAPAddress) {
                    SOAPAddress address = (SOAPAddress) any.get(k);

                    _endpointAddress = address.getLocation();

                    return _endpointAddress;
                  }
                }
              }
            }
          }
        }
      }
    }

    return _endpointAddress;
  }

  /**
   * Sets up the cached, transient data.
   **/
  public void afterUnmarshal(Unmarshaller u, Object o)
    throws JAXBException, WSDLValidationException
  {
    if (_messages != null) {
      for (WSDLMessage message : _messages) {
        QName name = null;

        if (_targetNamespace != null)
          name = new QName(_targetNamespace, message.getName());
        else
          name = new QName(message.getName());

        _messageMap.put(name, message);
      }
    }

    if (_portTypes != null) {
      for (WSDLPortType portType : _portTypes) {
        QName name = null;

        if (_targetNamespace != null)
          name = new QName(_targetNamespace, portType.getName());
        else
          name = new QName(portType.getName());

        _portTypeMap.put(name, portType);
      }
    }

    if (_bindings != null) { 
      for (WSDLBinding binding : _bindings) {
        QName name = null;

        if (_targetNamespace != null)
          name = new QName(_targetNamespace, binding.getName());
        else
          name = new QName(binding.getName());

        _bindingMap.put(name, binding);
      }
    }

    resolveImports(u);

    for (WSDLMessage message : getMessages()) {
      for (WSDLPart part : message.getParts()) {
        Type type = getType(part.getElement());

        if (type == null)
          throw new WSDLValidationException(L.l("Element type {0} for part {1} of message {2} is not defined in this WSDL's schema", part.getElement(), part.getName(), message.getName()));

        part.setType(type);
      }
    }

    // check that all the messages referenced by operations are defined
    for (WSDLPortType portType : getPortTypes()) {
      portType.setDefinitions(this);

      for (WSDLOperation operation : portType.getOperations()) {
        operation.setPortType(portType);

        for (WSDLOperationInput input : operation.getInputs()) {
          WSDLMessage message = getMessage(input.getMessageName());

          if (message == null)
            throw new WSDLValidationException(L.l("Input message {0} for operation {1} is not defined in this WSDL", input.getMessageName(), operation.getName()));

          input.setMessage(message);
        }

        for (WSDLOperationOutput output : operation.getOutputs()) {
          WSDLMessage message = getMessage(output.getMessageName());

          if (message == null)
            throw new WSDLValidationException(L.l("Output message {0} for operation {1} is not defined in this WSDL", output.getMessageName(), operation.getName()));

          output.setMessage(message);
        }

        for (WSDLOperationFault fault : operation.getFaults()) {
          WSDLMessage message = getMessage(fault.getMessageName());

          if (message == null)
            throw new WSDLValidationException(L.l("Fault message {0} for operation {1} is not defined in this WSDL", fault.getMessageName(), operation.getName()));

          fault.setMessage(message);
        }
      }
    }

    // assign the binding to a portType and check that all 
    // the operations are defined
    for (WSDLBinding binding : getBindings()) { 
      WSDLPortType portType = getPortType(binding.getType());

      if (portType == null)
        throw new WSDLValidationException(L.l("PortType {0} for binding {1} is not defined in this WSDL", binding.getType(), binding.getName()));

      binding.setPortType(portType);

      for (WSDLBindingOperation bindingOp : binding.getOperations()) {
        WSDLOperation operation = portType.getOperation(bindingOp.getName());

        if (operation == null)
          throw new WSDLValidationException(L.l("PortType {0} has no operation {1} for binding {2}", portType.getName(), bindingOp.getName(), binding.getName()));

        bindingOp.setOperation(operation);
      }
    }
  }

  public WSDLMessage getMessage(QName name)
  {
    return _messageMap.get(name);
  }

  public WSDLPortType getPortType(QName name)
  {
    return _portTypeMap.get(name);
  }

  public WSDLBinding getBinding(QName name)
  {
    return _bindingMap.get(name);
  }

  public Type getType(QName typeName)
  {
    if (_types == null)
      return null;

    for (int i = 0; i < _types.size(); i++) {
      WSDLTypes types = _types.get(i);

      Type type = types.getType(typeName);
       
      if (type != null)
        return type;
    }

    return null;
  }

  public void resolveImports(Unmarshaller u)
    throws JAXBException
  {
    if (_types == null)
      return;

    for (int i = 0; i < _types.size(); i++) {
      WSDLTypes types = _types.get(i);
      types.resolveImports(u);
    }
  }

  public void writeJAXBClasses(File outputDirectory, String pkg)
    throws IOException
  {
    if (_types == null)
      return;

    for (int i = 0; i < _types.size(); i++) {
      WSDLTypes types = _types.get(i);
      types.writeJAXBClasses(outputDirectory, pkg);
    }
  }
  
  public void generateJava(Unmarshaller u, 
                           File sourceDir, File classDir, 
                           String pkg)
    throws WSDLValidationException, JAXBException, IOException
  {
    for (WSDLService service : getServices()) { 
      for (WSDLPort port : service.getPorts()) {
        WSDLBinding binding = getBinding(port.getBinding());

        if (binding == null)
          throw new WSDLValidationException(L.l("Binding {0} for port {1} not defined in this WSDL", port.getBinding(), port.getName()));

        WSDLPortType portType = binding.getPortType();

        File dir = new File(sourceDir, pkg.replace(".", File.separator));

        dir.mkdirs();

        File output = new File(dir, portType.getName() + ".java");
        WriteStream os = null;

        try {
          os = Vfs.openWrite(output.toString());
          JavaWriter out = new JavaWriter(os);

          out.println("package " + pkg + ";");
          out.println();
          out.println("import java.math.BigDecimal;");
          out.println("import java.math.BigInteger;");
          out.println("import java.util.List;");
          out.println("import javax.jws.WebMethod;");
          out.println("import javax.jws.WebParam;");
          out.println("import javax.jws.WebResult;");
          out.println("import javax.jws.WebService;");
          out.println("import javax.xml.datatype.XMLGregorianCalendar;");
          out.println("import javax.xml.ws.RequestWrapper;");
          out.println("import javax.xml.ws.ResponseWrapper;");
          out.println();
          out.print("@WebService(name=\"" + portType.getName() + "\",");
          out.println("targetNamespace=\"" + getTargetNamespace() + "\")");
          out.println("public interface " + portType.getName() + "{");

          out.pushDepth();

          for (WSDLBindingOperation bindingOp : binding.getOperations()) {
            bindingOp.generateJava(out);
            out.println();
            out.println();
          }

          out.popDepth();

          out.println("}");
        }
        finally {
          if (os != null)
            os.close();
        }

        output = new File(dir, portType.getName() + "Service.java");
        os = null;

        try {
          os = Vfs.openWrite(output.toString());
          JavaWriter out = new JavaWriter(os);

          out.println("package " + pkg + ";");
          out.println();
          out.println("import java.net.URL;");
          out.println("import javax.xml.namespace.QName;");
          out.println("import javax.xml.ws.Service;");
          out.println();
          out.println("public class " + portType.getName() + "Service");
          out.println("  extends Service");
          out.println("{");

          out.pushDepth();

          out.print("public " + portType.getName() + "Service");
          out.println("(URL wsdlDocumentLocation, QName serviceName)");
          out.println("{");
          out.pushDepth();
          out.println("super(wsdlDocumentLocation, serviceName);");
          out.popDepth();
          out.println("}");

          out.popDepth();

          out.println("}");
        }
        finally {
          if (os != null)
            os.close();
        }

      }
    }

    writeJAXBClasses(sourceDir, pkg);

    File dir = new File(sourceDir, pkg.replace(".", File.separator));
    File[] sources = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) 
      {
        return name.endsWith(".java");
      }
    });

    if (sources.length == 0) {
      // XXX Warning message?
      System.out.println(" No sources found in " + dir + "!!!!!!");
      return;
    }

    com.sun.tools.javac.Main javac = new com.sun.tools.javac.Main();

    String[] args = new String[2 + sources.length];

    args[0] = "-d";
    args[1] = classDir.getAbsolutePath();

    for (int i = 0; i < sources.length; i++)
      args[i + 2] = sources[i].getAbsolutePath();

    javac.compile(args);
  }
}
