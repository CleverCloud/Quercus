<?php
// refresh every 5 seconds
header("refresh: 5");

$mbean_server = new MBeanServer();

?>

<html>
<head><title>mbean</title></head>

<body>
<h1>mbean</h1>

This page is automatically refreshed every 5 seconds.

<?php
var_dump($mbean_server->query("resin:type=PeriodicTask,*"));

foreach ($mbean_server->query("resin:type=PeriodicTask,*") as $mbean) {
?>

<hr/>
<dl>

<dt>estimatedAverageTime
<dd><?= $mbean->EstimatedAverageTime ?>

<dt>active
<dd>${mbean.active}

<dt>estimatedTimeRemaining
<dd>${mbean.estimatedTimeRemaining}

<dt>lastActiveTime
<dd>${mbean.lastActiveTime}

<dt>totalActiveCount
<dd>${mbean.totalActiveCount}

<dt>totalActiveTime
<dd>${mbean.totalActiveTime}

<dt>averageActiveTime
<dd>${mbean.averageActiveTime}

</dl>

<?php
}
?>

<hr/>

</body>
</html>

