<head>
  <link rel='STYLESHEET' type='text/css' href='../../css/default.css'/>
</head>

<div class='breadcrumb'>
  <a href='../../'>docs</a>
 / <a href='../'>examples</a>
 / <a href='index.xtp'>CanDI pattern tutorial</a>
</div>

<h2>Service Pattern</h2>

<?php
$myService = java_bean("myService")
?>

<table class='deftable'>
<tr>
  <th>PHP code
  <th>MyService instance
<tr>
  <td>java_bean("myService")
  <td><?= java_bean("myService") ?>
<tr>
  <td>$myService->getMessage()
  <td><?= $myService->getMessage() ?>
</table>

<h2>Architecture</h2>

<img src='../../images/ioc-binding.png'>

<ul>
<li><a href='index.jsp'>index.jsp</a></li>
<li><a href='index.php'>index.php</a></li>
<li><a href='set?color=blue'>Set the service message blue (/set?color=blue)</a></li>
<li><a href='set?color=red'>Set the service message red (/set?color=red)</a></li>
<li><a href='get'>Get the service message (/get)</a></li>
</ul>
