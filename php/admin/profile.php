<?php
/**
 * Summary of profiling
 */

require_once "WEB-INF/php/inc.php";

$profile = @new Java("com.caucho.profile.Profile");

$mbeanServer = new MBeanServer();

$server = $mbeanServer->lookup("resin:type=Server");

$title = "Resin: Profile $server->Id";

if (! $profile)
  $title .= " - not available";

?>

<?php display_header("profile.php", $title, $server) ?>

<?php

if ($profile) {
  if ($_POST['action'] == 'stop') {
    $profile->stop();
  }
  else if ($_POST['action'] == 'start') {
    $profile->start();

    if ($_POST['period'] >= 10) {
      $profile->setPeriod($_POST['period']);
    }

    if ($_POST['depth'] >= 1) {
      $profile->setDepth($_POST['depth']);
    }
  }

  if ($profile->active) {
    echo "<h2>Profile Active</h2>";
    echo "Period: {$profile->period}ms Depth: {$profile->depth}";

    echo "<form action='profile.php' method='post' style='display:inline'>";
    echo "<input type='submit' name='action' value='stop'>";
    echo "</form>";

    echo "</h2>\n";
  }
  else {
    echo "<h2>";
    echo "Profile Stopped";
    echo "</h2>";

    echo "<form action='profile.php' method='post' style='display:inline'>";
    echo "Period: <select type='select' name='period'>";
    echo "  <option>10ms";
    echo "  <option>25ms";
    echo "  <option>100ms";
    echo "  <option>250ms";
    echo "  <option selected>1000ms";
    echo "  <option>2500ms";
    echo "  <option>10000ms";
    echo "</select>";
    echo " Depth: <select type='select' name='depth'>";
    echo "  <option>2";
    echo "  <option>4";
    echo "  <option>8";
    echo "  <option>12";
    echo "  <option selected>16";
    echo "  <option>20";
    echo "  <option>32";
    echo "  <option>250";
    echo "</select>";
    echo "<input type='submit' name='action' value='start'>";
    echo "</form>";
  }

  $results = $profile->getResults();

  $partition = do_partition_profile($results);
  $groups = array("active", "resin", "keepalive", "wait", "accept", "single");

  echo "<table class='threads'>\n";

  foreach ($groups as $name) {
    $entries = $partition[$name];

    if (sizeof($entries) <= 0)
      continue;

    //$topTicks = $entries[0]->getCount();
    $totalTicks = 0;
    for ($i = sizeof($entries) - 1; $i >= 0; $i--) {
      $totalTicks += $entries[$i]->getCount();
    }

    echo "<tr class='head'><th colspan='4' align='left'>$name (" . sizeof($entries) . ")";

    $show = "hide('s_$name');show('h_$name');";
    $hide = "show('s_$name');hide('h_$name');";

    for ($i = 0; $i < sizeof($entries); $i++) {
      $show .= "show('t_{$name}_{$i}');";
      $hide .= "hide('t_{$name}_{$i}');";
    }

    echo " <a id='s_$name' href=\"javascript:$show\">show</a> ";
    echo "<a id='h_$name' href=\"javascript:$hide\" style='display:none'>hide</a>";

    echo "</th></tr>\n";

    for ($i = 0; $i < sizeof($entries); $i++) {
      $entry = $entries[$i];

      echo "<tr>";
      echo "<td align='right'>";
      printf("%9.3f%%", 100 * $entry->getCount() / $totalTicks);
      echo "</td><td align='right'>";
      printf("%.3fs", $entry->getCount() * $profile->period * 0.001);
      echo "</td>";
      echo "<td>";
      echo "<a id='s_{$name}_{$i}' href=\"javascript:show('t_{$name}_{$i}');hide('s_{$name}_{$i}');show('h_{$name}_{$i}');\">show</a> ";
      echo "<a id='h_{$name}_{$i}' href=\"javascript:hide('t_{$name}_{$i}');show('s_{$name}_{$i}');hide('h_{$name}_{$i}');\" style='display:none'>hide</a> ";
      echo "</td>";
      echo "<td>";
      $stack = $entry->getStackTrace()[0];
      echo "{$entry->getDescription()}\n";
      echo "</td>";
      echo "</tr>\n";

      echo "<tr id='t_{$name}_{$i}' style='display:none'>";
      echo "<td colspan='2'>";
      echo "</td>";
      echo "<td colspan='2'>";

      echo "<pre>";
      foreach ($entry->getStackTrace() as $stack) {
        echo "  at {$stack->getClassName()}.{$stack->getMethodName()}({$stack->getArg()})\n";
      }
      echo "</pre>";
      echo "</td>";
      echo "</tr>\n";
    }
  }

  echo "</table>\n";
}
else {
  echo "<h2>Profiling is not available</h2>";

  echo "<p>Profiling requires Resin Professional and compiled JNI</p>";
}

function do_partition_profile($entries)
{
  $partition = array();
  foreach ($entries as $info) {
    $stackTrace = $info->stackTrace;

    if (! $stackTrace) {
    }
    else if ($info->getCount() < 2) {
      $partition["single"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.lang.Object"
        && $stackTrace[0]->methodName == "wait") {
      $partition["wait"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.lang.Thread"
        && $stackTrace[0]->methodName == "sleep") {
      $partition["wait"][] = $info;
    }
    else if ($stackTrace[0]->className == "com.caucho.vfs.JniServerSocketImpl"
             && $stackTrace[0]->methodName == "nativeAccept") {
      $partition["accept"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.net.PlainSocketImpl"
             && $stackTrace[0]->methodName == "socketAccept") {
      $partition["accept"][] = $info;
    }
    else if ($stackTrace[0]->className == "com.caucho.profile.Profile"
             && $stackTrace[0]->methodName == "nativeProfile") {
      $partition["resin"][] = $info;
    }
    else if ($stackTrace[0]->className == "com.caucho.server.port.JniSelectManager"
             && $stackTrace[0]->methodName == "selectNative") {
      $partition["resin"][] = $info;
    }
    else if (is_resin_main($stackTrace)) {
      $partition["resin"][] = $info;
    }
    else if (is_keepalive($stackTrace)) {
      $partition["keepalive"][] = $info;
    }
    else if (is_accept($stackTrace)) {
      $partition["accept"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.lang.ref.ReferenceQueue") {
    }
    else {
      $partition["active"][] = $info;
    }
  }

  return $partition;
}

function is_resin_main($stackTrace)
{
  for ($i = 0; $i < sizeof($stackTrace); $i++) {
    if ($stackTrace[$i]->className == "com.caucho.server.resin.Resin"
        && $stackTrace[$i]->methodName == "waitForExit") {
      return true;
    }
  }

  return false;
}

function is_accept($stackTrace)
{
  if ($stackTrace[0]->className == "com.caucho.vfs.JniServerSocketImpl"
      && $stackTrace[0]->methodName == "nativeAccept") {
    return true;
  }
  else if ($stackTrace[0]->className == "java.net.PlainSocketImpl"
           && $stackTrace[0]->methodName == "socketAccept") {
    return true;
  }
  else if ($stackTrace[0]->className == "java.net.PlainSocketImpl"
           && $stackTrace[0]->methodName == "accept") {
    return true;
  }

  return false;
}

function is_keepalive($stackTrace)
{
  for ($i = 0; $i < sizeof($stackTrace); $i++) {
    if ($stackTrace[$i]->className != "com.caucho.server.port.TcpConnection"
        || $stackTrace[$i]->methodName != "handleConnection") {
      continue;
    }
    else if ($stackTrace[$i - 1]->className == "com.caucho.server.port.TcpConnection"
             && $stackTrace[$i - 1]->methodName == "keepaliveRead") {
      return true;
    }
    else if ($stackTrace[$i - 1]->className == "com.caucho.vfs.ReadStream"
             && $stackTrace[$i - 1]->methodName == "waitForRead") {
      return true;
    }
  }

  return false;
}

display_footer("profile.php");

?>
