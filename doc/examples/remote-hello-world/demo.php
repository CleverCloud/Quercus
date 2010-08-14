<?php
$hessian = java_bean("hessian");
$burlap = java_bean("burlap");
$vm = java_bean("vm");
?>

From Hessian: <?= $hessian->hello() ?><br>
From Burlap: <?= $burlap->hello() ?><br>
From VM: <?= $vm->hello() ?><br>

<ul>
<li><a href="demo.jsp">JSP</a></li>
<li><a href="index.xtp">Tutorial</a></li>
<ul>
