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

package com.caucho.jws;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *  A bogus SOAP null-call for testing purposes
 */
public class NullCallServlet extends HttpServlet {

  public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
    doIt(req, resp);
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
    doIt(req, resp);
  }

  public void doIt(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (req.getParameter("wsdl")==null)
      sendResponse(req, resp);
    else
      sendWsdl(req, resp);
  }

  public void sendResponse(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    PrintWriter out = resp.getWriter();
    resp.setContentType("text/xml;charset=utf-8");
    out.println("<?xml version=\"1.0\" ?>");
    out.println("  <Envelope");
    out.println(" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"");
    out.println(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
    out.println(" xmlns:ns1=\"http://endpoint.nullservice/\">");
    out.println("    <soapenv:Body>");
    out.println("      <ns1:nullCallResponse>");
    out.println("      </ns1:nullCallResponse>");
    out.println("    </soapenv:Body>");
    out.println("  </Envelope>");
    out.flush();
    out.close();
  }

  public void sendWsdl(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    PrintWriter out = resp.getWriter();
    resp.setContentType("text/xml");
    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    out.println("<definitions xmlns=\"http://schemas.xmlsoap.org/wsdl/\"");
    out.println("             xmlns:tns=\"http://endpoint.nullservice/\"");
    out.println("             xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
    out.println("       xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"");
    out.println("       targetNamespace=\"http://endpoint.nullservice/\"");
    out.println("       name=\"NullService\">");
    out.println("<types>");
    out.println("  <xsd:schema>");
    out.println("  <xsd:import namespace=\"http://endpoint.nullservice/\"");

    String xsd = "http://127.0.0.1:49106/nullservice/NullService/" +
      "__container$publishing$subctx/WEB-INF/wsdl/NullService_schema1.xsd";
    out.println("    schemaLocation=\""+xsd+"\"");
    out.println("    xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" ");
    out.println("  xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\"/>");
    out.println("  </xsd:schema>");
    out.println("</types>");
    out.println("<message name=\"nullCall\">");
    out.println("  <part name=\"parameters\" element=\"tns:nullCall\"/>");
    out.println("</message>");
    out.println("<message name=\"nullCallResponse\">");
    out.println("<part name=\"parameters\" element=\"tns:nullCallResponse\"/>");
    out.println("</message>");
    out.println("<portType name=\"Null\">");
    out.println("  <operation name=\"nullCall\">");
    out.println("    <input message=\"tns:nullCall\"/>");
    out.println("    <output message=\"tns:nullCallResponse\"/>");
    out.println("  </operation>");
    out.println("</portType>");
    out.println("<binding name=\"NullPortBinding\" type=\"tns:Null\">");
    out.println("  <soap:binding ");
    out.println("  transport=\"http://schemas.xmlsoap.org/soap/http\" ");
    out.println("   style=\"document\"/>");
    out.println("  <operation name=\"nullCall\">");
    out.println("  <soap:operation soapAction=\"\"/>");
    out.println("    <input>");
    out.println("  <soap:body use=\"literal\"/>");
    out.println("    </input>");
    out.println("    <output>");
    out.println("  <soap:body use=\"literal\"/>");
    out.println("    </output>");
    out.println("  </operation>");
    out.println("</binding>");
    out.println("<service name=\"NullService\">");
    out.println("  <port name=\"NullPort\" binding=\"tns:NullPortBinding\">");

    String service = "http://127.0.0.1:49106/nullservice/NullService";
    out.println("  <soap:address location=\""+service+"\" ");
    out.println("      xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"");
    out.println("  xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\"/>");
    out.println("    </port>");
    out.println("</service>");
    out.println("</definitions>");
    out.flush();
    out.close();
  }
}
