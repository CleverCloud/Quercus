<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<f:view>
  <h:messages/>
  <h:form>
    <h:inputText value="#{calc.a}" size="4"/>
     + <h:inputText value="#{calc.b}" size="4"/>
     = <h:outputText value="#{calc.sum}" style="color:red"/>
    <br>
    <h:commandButton value="Add"/>
  </h:form>
</f:view>
