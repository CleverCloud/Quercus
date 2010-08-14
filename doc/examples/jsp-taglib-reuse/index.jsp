<%@ taglib prefix="msg" uri="/WEB-INF/message.tld" %>

<html>
  <head>
    <title>Reusing Tags Example</title>
  </head>

  <body>
    <h1>Reusing Tags Example</h1>

    <msg:message title="Test Message">
      <msg:add text="Hello"/>
      <msg:add text="World"/>
    </msg:message>

    <msg:message>
      <msg:add text="Hello Again"/>
      <msg:add text="World"/>
    </msg:message>

    <msg:message title="Test Message">
      <msg:add text="Goodbye"/>
      <msg:add text="World"/>
    </msg:message>

  </body>
</html>
