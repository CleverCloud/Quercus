<%@ page session="false" import="java.util.Iterator" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<%--

  This toplevel jsp file contains the html header and the title
  bar.  The body contents are dynamically included from either
  search_initerror.jsp or search_results.jsp.

  Because the search_results.jsp are in another jsp, the result of that jsp can
  be cached, even if this toplevel file is not cacheable.  Fragmenting
  the jsp like this is an easy and convenient way to cache results and avoid
  database hits.  
  
  --%>


<jsp:useBean id="jsputil" class="com.caucho.doc.javadoc.JspUtil" scope="request">
  <jsp:setProperty name="jsputil" property="request"
                  value="${pageContext.request}"/>
  <jsp:setProperty name="jsputil" property="response"
                  value="${pageContext.response}"/>
</jsp:useBean>

<c:set var="initError" value="${jsputil.store.initError}"/>

<html>
<head>
<title>Search Resin Javadoc @VERSION@</title>
<link rel="stylesheet" href="search.css" type="text/css">
<LINK rel="icon" href="images/dragonfly-tiny.png" type="image/png">
<LINK rel="shortcut icon" href="images/dragonfly-icon.png" type="image/png">
<script type="text/javascript"><!--
var debug = false;
var timerSetFocus = false;
var IE;
var NS;
if (document.all) {
  IE=true;
}
else if (document.layers) {
  NS=true;
}
else if (document.getElementById) {
  NS=true;  // NS6
}

function doLoad() {
  registerEventListener();
  doFocusSearch();
}

function showClassHref(href) {
<c:if test="${empty param.noframes}">
  if (href) {
     parent.classFrame.location.href=href;
     doFocusClass();
     timerSetFocus = true;
     setTimeout("doTimer()", 1500)
  }
</c:if>

  registerEventListener();
}

function registerEventListener(e) {
  
  // this frame
  if (NS) document.captureEvents(Event.KEYPRESS);
  document.onkeypress = doKey;

<c:if test="${empty param.noframes}">
  // other frame
  if (NS) parent.classFrame.window.captureEvents(Event.KEYPRESS);

  parent.classFrame.window.onkeypress = doKey;
  parent.classFrame.document.onkeypress = doKey;
</c:if>
}

function doFocusClass() {
<c:if test="${empty param.noframes}">
  parent.classFrame.window.focus();
</c:if>
}

function doTimer()
{
  if (timerSetFocus) {
    doFocusClass();
    timerSetFocus = false;
  }

  registerEventListener();
  setTimeout("doTimer()", 1000);
}

function doFocusSearch() {
  document.searchForm.query.select();
  document.searchForm.query.focus();
}

function doKey(e) {
  var ev;
  if (IE) {
    ev =  (event ? event : parent.classFrame.window.event);
  }
  var whichASC = (NS) ? e.which : ev.keyCode;
  var whichKey = String.fromCharCode(whichASC).toLowerCase();
  var altPressed = (NS) ? e.modifiers & Event.ALT_MASK : ev.altKey;

  if (debug) status = "keypress: " + whichKey;
  if (whichKey == '?') {
    doFocusSearch();
    return false;
  }
}

// -->
</script>
</head>

<body onLoad="doLoad();">
<table class="header" border="0" width="100%">
<tr>
<td align="left"><font size="6"><b>Javadoc</b></font></td>
<td align="right" valign="top"><a href="http://www.caucho.com"><img border="0" width="66" height="27" class="img" src="images/caucho-main.png"></a></td>
</table>

<c:choose>
  <c:when test="${empty initError}">
    <c:import url="search_results.jsp">
      <c:if test="${not empty param.noframes}">
        <c:param name="noframes">${param.noframes}</c:param>
      </c:if>
      <c:if test="${not empty param.query}">
        <c:param name="query">${param.query}</c:param>
      </c:if>
      <c:if test="${not empty param.offset}">
        <c:param name="offset">${param.offset}</c:param>
      </c:if>
      <c:if test="${not empty param.limit}">
        <c:param name="limit">${param.limit}</c:param>
      </c:if>
    </c:import>
  </c:when>
  <c:otherwise>
    <c:import url="search_initerror.jsp"/>
  </c:otherwise>
</c:choose>
</body>
</html>
