<html>
<head>
<title>
Quercus&#153; Start Page
</title>

<!--
<?php

  function quercus_test()
  {
    return function_exists("quercus_version");
  }

?>
-->

<style type="text/css">
.message {
  margin: 10px;
  padding: 10px;
  border: 1px solid blue;
  background: #CCCCCC;
}

.footer {
  font-size: small;
  font-style: italic;
}

#failure {
    <?php echo "display: none;"; ?> 
}

#failure_default_interpreter {
    display: none;
    <?php if (! quercus_test()) echo "display: block;"; ?> 
}

#success_pro {
    display: none;
    <?php if (quercus_is_pro() && quercus_test()) echo "display: block;"; ?> 
}

#success_open_source {
    display: none;
    <?php if (! quercus_is_pro() && quercus_test()) echo "display: block;"; ?> 
}
</style>
</head>

<body>
<a href="http://www.caucho.com"><img border="0" src="images/caucho-white.jpg"></a>

<p>
Testing for Quercus&#153;...
</p>

<div class="message" id="failure">
PHP files are not being interpreted by Quercus&#153;.
</div>

<div class="message" id="failure_default_interpreter">
PHP is being interpreted, but not by Quercus&#153;!  Please check your configuration.
</div>

<div class="message" id="success_pro">
<img src="images/dragonfly-tiny.png">Congratulations!  Quercus&#153; <?php if (quercus_test()) echo quercus_version(); ?> is compiling PHP pages.  Have fun!
</div>

<div class="message" id="success_open_source">
<img src="images/dragonfly-tiny.png">Congratulations!  Quercus&#153; <?php if (quercus_test()) echo quercus_version(); ?> is interpreting PHP pages.  Have fun!
</div>

<div>
Documentation is available at <a href="http://www.caucho.com">http://www.caucho.com</a>
</div>

<div>
The README is available <a href="README">here</a>.
</div>

<hr/>

<div class="footer">
Copyright &copy; 1998-2009
<a href="http://www.caucho.com">Caucho Technology, Inc</a>. 
All rights reserved.<br/>

Resin <sup><font size="-1">&#174;</font></sup> is a registered trademark,
and Quercus<sup>tm</sup>, Amber<sup>tm</sup>, and Hessian<sup>tm</sup>
are trademarks of Caucho Technology.
</div>
</body>

</html>
