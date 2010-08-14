<?php
 /* Redirect to a different page in the current directory that was requested */
 $host  = $_SERVER['HTTP_HOST'];
 $uri   = rtrim(dirname($_SERVER['PHP_SELF']), '/\\');

 if ($redirect == "list")
   $extra = "$controller.php";
 else
   $extra = "$controller.php?action=$redirect";

 header("Location: http://$host$uri/$extra");

 exit;
?>
