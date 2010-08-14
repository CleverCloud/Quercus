<?php
/**
 * General include file for admin.
 *
 * @author Sam
 */

require_once "WEB-INF/php/graph.php";

global $g_server_id;
global $g_mbean_server;
global $g_resin;
global $g_server;

// kill the cache, all pages are uncached and private
header("Expires: 01 Dec 1994 16:00:00 GMT"); 
header("Cache-Control: max-age=0,private"); 
header("Pragma: No-Cache");

function admin_init($query="", $is_refresh=false)
{
  global $g_server_id;
  global $g_server_index;
  global $g_mbean_server;
  global $g_resin;
  global $g_server;
  global $g_page;

  mbean_init();
  
  if (! $g_mbean_server) {
    if ($g_server_id)
      $title = "Resin: $g_page for server $g_server_id";
    else
      $title = "Resin: $g_page for server default";

    display_header("thread.php", $title, $g_server, $query, $is_refresh);

    echo "<h3 class='fail'>Can't contact $g_server_id</h3>";
    
    return false;
  }

  if ($g_server_id)
    $title = "Resin: $g_page";
  else
    $title = "Resin: $g_page";

  return display_header("thread.php", $title, $g_server, $query,
                        $is_refresh, true);
}  

function mbean_init()
{
  global $g_server_id;
  global $g_server_index;
  global $g_mbean_server;
  global $g_resin;
  global $g_server;

  $g_server_index = $_GET["s"];

  if (isset($_REQUEST["new_s"])) {
    $g_server_index = $_REQUEST["new_s"];
  }

  if (! isset($g_server_index)) {
    $g_mbean_server = new MBeanServer();
    $g_server = $g_mbean_server->lookup("resin:type=Server");
    $g_server_index = $g_server->SelfServer->ClusterIndex;
    $g_server_id = $g_server->Id;

    if (! $g_server_id)
      $g_server_id = "default";
  }
  else {
    $g_mbean_server = new MBeanServer("");
    $server = server_find_by_index($g_mbean_server, $g_server_index);

    $g_server_id = $server->Name;
    $g_mbean_server = new MBeanServer($g_server_id);

    $g_server = $g_mbean_server->lookup("resin:type=Server");
  }

  if ($g_mbean_server) {
    $g_resin = $g_mbean_server->lookup("resin:type=Resin");
    $g_server = $g_mbean_server->lookup("resin:type=Server");
  }
}

function load_pages($suffix)
{
  $pages = load_dir_pages("WEB-INF/php", $suffix);

  $config = java("com.caucho.config.Config");
  $user_path = $config->getProperty("resin_admin_ext_path");

  if ($user_path)
    $pages = array_merge($pages, load_dir_pages($user_path, $suffix));
    
  return $pages;
}

function load_dir_pages($dir_name, $suffix)
{
  $dir = opendir($dir_name);

  $pages = array();

  while (($file = readdir($dir))) {
    $values = null;

    if (preg_match("/(.*)\." . $suffix . "$/", $file, $values)) {
      $name = $values[1];
      $pages[$name] = $dir_name . "/" . $file;
    }
  }

  closedir($dir);
  
  return $pages;
}

function format_datetime($date)
{
  return strftime("%a %b %d %H:%M:%S %Z %Y", $date->time / 1000);
}

function format_memory($memory)
{
  return sprintf("%.2fMeg", $memory / (1024 * 1024))
}

function format_hit_ratio($hit, $miss)
{
  $total = $hit + $miss;

  if ($total == 0)
    return "0.00% (0 / 0)";
  else
    return sprintf("%.2f%% (%d / %d)", 100 * $hit / $total, $hit, $total);
}

function format_ago_td_pair($value, $date, $fail=3600, $warn=14400)
{
  $ago_class = format_ago_class($date);
  if ($ago_class)
    $ago_class="class='$ago_class'";

  /*
  echo "<td $ago_class style='border-right: none'>$value</td>\n";
  echo "<td $ago_class style='border-left: none'>";
  echo format_ago($date);
  echo "</td>";
  */

  echo "<td>$value</td>\n";
  echo "<td $ago_class>";
  echo format_ago($date);
  echo "</td>";
}

function format_state_class($state)
{
  if ($state == "error")
    return "fail";
  else
    return "";
}

function format_ago_class($date, $fail=3600, $warn=14400)
{
  if (! $date)
    return "";

  $event_time = $date->time / 1000;

  if ($event_time < 365 * 24 * 3600)
    return "";

  $now = time();
  $ago = $now - $event_time;

  if ($ago < $fail)
    return "fail";
  else if ($ago < $warn)
    return "warn";
  else
    return "";
}

function format_ago($date)
{
  if (! $date)
    return "";

  $event_time = $date->time / 1000;

  if ($event_time < 365 * 24 * 3600)
    return "";

  $now = time();
  $ago = $now - $event_time;

  return sprintf("%dh%02d", $ago / 3600, $ago / 60 % 60);
}

function format_miss_ratio($hit, $miss)
{
  $total = $hit + $miss;

  if ($total == 0)
    return "0.00% (0 / 0)";
  else
    return sprintf("%.2f%% (%s / %s)", 100 * $miss / $total,
                   format_count($miss),
                   format_count($total));
}

function format_count($count)
{
  if ($count < 100 * 1000)
    return sprintf("%d", $count);
  if ($count < 1000 * 1000)
    return sprintf("%.1fk", $count / 1000.0);
  if ($count < 1000 * 1000 * 1000)
    return sprintf("%.1fM", $count / (1000.0 * 1000.0));
  else
    return sprintf("%.1fG", $count / (1000.0 * 1000.0 * 1000.0));
}

function indent($string, $count = 2)
{
  $lines = explode("\n", $string);
  $output = "";
  $indent = str_repeat(" ", $count);

  foreach ($lines as $line) {
    $output .= $indent . $line . "\n";
  }

  return $output;
}

function uri($path)
{
  global $home_uri;

  if (is_null($home_uri))
    $home_uri = rtrim(dirname($_SERVER['PHP_SELF']), '/\\'); 

  if (strncmp($path, "/", 1) === 0)
    return $path;
  else
    return $home_uri . "/" . $path;
}

function uri_nocache($path)
{
  global $home_uri;

  if (is_null($home_uri))
    $home_uri = rtrim(dirname($_SERVER['PHP_SELF']), '/\\'); 

  /*
  if (strstr($path, "?") === FALSE)
    $rand = "?.rand=" . mt_rand();
  else
    $rand = "&.rand=" . mt_rand();
  */

  if (strncmp($path, "/", 1) === 0)
    return $path . $rand;
  else
    return $home_uri . "/" . $path . $rand;
}

function server_names($server, $cluster)
{
  $client_names = array();

  foreach ($cluster->Servers as $client) {
    $client_names[] = $client->Name;
  }

  sort($client_names);

  return $client_names;
}

function static_server_names($server, $cluster)
{
  $client_names = array();

  foreach ($cluster->Servers as $client) {
    if (! $client->isDynamicServer())
      $client_names[] = $client->Name;
  }

  sort($client_names);

  return $client_names;
}

function dynamic_server_names($server, $cluster)
{
  $client_names = array();

  foreach ($cluster->Servers as $client) {
    if ($client->isDynamicServer())
      $client_names[] = $client->Name;
  }

  sort($client_names);

  return $client_names;
}

function server_by_name($name, $cluster)
{
  foreach ($cluster->Servers as $client) {
    if ($client->Name == $name)
      return $client;
  }

  return null;
}

function redirect($relative_url)
{
  $uri = uri($relative_url);

  $scheme = $_SERVER['HTTPS'] ? "https" : "http";

  header("Location: $scheme://" . $_SERVER['HTTP_HOST'] . $uri);
}

function redirect_nocache($relative_url)
{
  $uri = uri_nocache($relative_url);

  $scheme = $_SERVER['HTTPS'] ? "https" : "http";

  header("Location: $scheme://" . $_SERVER['HTTP_HOST'] . $uri);
}

if (is_null($target_uri))
  $target_uri = $_SERVER['PHP_SELF'];

$is_read_role = quercus_servlet_request()->isUserInRole("read");
$is_write_role = quercus_servlet_request()->isUserInRole("write");

$display_header_script = NULL;
$display_header_title = NULL;
$is_display_footer = false;

function display_jmx($mbean_server, $group_mbeans)
{
  $type_partition = jmx_partition($group_mbeans, array("type"));
  ksort($type_partition);
  static $data_id = 0;
  echo "<table class='data'>";
  
  foreach ($type_partition as $type_name => $type_mbeans) {
    echo "<tr><td class='group' colspan='2'>$type_name</td></tr>\n";

    foreach ($type_mbeans as $mbean) {
      $attr_list = $mbean->mbean_info->attributes;
      sort($attr_list);
      
      $attr_names = null;
      
      foreach ($attr_list as $attr) {
        $attr_names[] = $attr->name;
      }
      sort($attr_names);

      $start_id = ++$data_id;

      $s = "show('h$start_id');hide('s$start_id');";
      $h = "hide('h$start_id');show('s$start_id');";

/*
      for ($i = 0; $i < count($attr_names); $i++) {
        $s .= "show('jmx" . ($i + $start_id) . "');";
        $h .= "hide('jmx" . ($i + $start_id) . "');";
      }
*/
        $s .= "show('jmx" . ($start_id) . "');";
        $h .= "hide('jmx" . ($start_id) . "');";
      
      echo "<tr><td class='item' colspan='2'>";
      echo "<a id='s$start_id' href=\"javascript:$s\">[show]</a>\n";
      echo "<a id='h$start_id' href=\"javascript:$h\" style='display:none'>[hide]</a>\n";
      echo jmx_short_name($mbean->mbean_name, $group_array);
      echo "</td></tr>\n";

      echo "<tr><td>";
      echo "<table id='jmx${start_id}' class='data' style='display:none'>\n";
      $row = 0;

      foreach ($attr_names as $attr_name) {
        $id = "jmx" . $data_id++;
      
//        echo "<tr id='$id' style='display:none'>";
        echo "<tr>";
	echo "<td>" . $attr_name . "</td>";

        //OS X 10.6.2 JDK 1.6 fix for #3782
        try {
          echo "<td>";
          $v = $mbean->$attr_name;
          display_jmx_data($v);
        } catch (Exception $e) {
          echo "Data unavailable due to error: " . $e;
        }
        
        echo "</td>\n";
        echo "</tr>\n";
      }
      echo "</table>";
    }
  }
  
  echo "</table>";
}

function display_jmx_data($v)
{
  if (is_array($v)) {
    echo "<pre>{\n";
    foreach ($v as $k => $v) {
      echo "  ";
      if (is_string($v))
        echo "\"$v\",\n";
      else
        var_dump($v);
    }
    echo "}</pre>";
  }
  else if ($v === false)
    echo "false";
  else if ($v === true)
    echo "true";
  else if ($v === null)
    echo "null";
  else {
    $v = (string) $v;

    $v = wordwrap($v);
	  
    echo $v;
  }
}

function jmx_partition($mbean_list, $keys)
{
  $env = null;

  foreach ($mbean_list as $mbean) {
    $exp = mbean_explode($mbean->mbean_name);

    $domain = $exp[':domain:'];

    if ($domain == "JMImplementation")
      continue;

    $name = "";

    foreach ($keys as $key) {
      if ($key == ":domain:")
        continue;
      
      $value = $exp[$key];

      if ($value) {
        if (strlen($name) > 0)
	  $name .= ",";

	$name .= $value;
      }
    }

    if (in_array(":domain:", $keys)) {
      $name = "${domain}:" . $name;
    }
      
    $env[$name][] = $mbean;
  }
  
  ksort($env);

  return $env;
}

function jmx_short_name($name, $exclude_array)
{
  $exp = mbean_explode($name);

  foreach ($exclude_array as $key) {
    unset($exp[$key]);
  }

  if (count($exp) > 0) {
    ksort($exp);
    
    $name = "";
    
    foreach ($exp as $key => $value) {
      if ($key == ':domain:')
        continue;
	
      if (strlen($name) > 0)
        $name .= ",";

      $name .= $key . '=' . $value;
    }
    
    if ($exp[':domain:']) {
      $name = $exp[':domain:'] . ":" . $name;
    }
    
    return $name;
  }
  else
    return $name;
}

/**
 * Outputs an html header.
 * A header is only output if this is the first call to display_header().
 * The first call establishes the title of the page.
 * 
 * @param $script the script calling the function
 * @param $title a title to use if the header is output.
 *
 * @return true if the header was output, false if a header has already been output
 */
function display_header($script, $title, $server,
                        $query = "",
                        $is_refresh = false,
                        $allow_remote = false)
{
  global $g_server_id;
  global $g_server_index;
  global $g_page;
  global $g_next_url;

  $title = $title . " for server " . $g_server_id;

  $server_id = $server->Id;

  global $display_header_script, $display_header_title;

  if (! empty($display_header_script))
    return;

  $g_next_url = "?q=" . $g_page . "&s=" . $g_server_index . $query;

  if (isset($_REQUEST["new_s"]) && $_REQUEST["new_s"] != $_GET["s"]) {
    header("Location: " . $g_next_url);
    return false;
  }

  $display_header_script = $script;
  $display_header_title = $title;

  $logout_uri = uri("logout.php");
?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Strict//EN" "http://www.w3.org/TR/html4/strict.dtd">

<html>
<head>
  <title><?= $title ?></title>
  <link rel='stylesheet' href='<?= uri("default.css") ?>' type='text/css' />
<?php
if ($is_refresh) {
  echo "<meta http-equiv=\"refresh\" content=\"60\" />\n";
}
?>

  <script language='javascript' type='text/javascript'>
    function hide(id) { document.getElementById(id).style.display = 'none'; }
    function show(id) { document.getElementById(id).style.display = 'block'; }
    function show_n(id) { document.getElementById(id).style.display = ''; }
    function show_i(id) { document.getElementById(id).style.display = 'inline'; }
    function sel(id) { document.getElementById(id).className = 'selected'; }
    function unsel(id) { document.getElementById(id).className = ''; }
    function showInline(id) { document.getElementById(id).style.display = 'inline'; }
    function setValue(id, v) { document.getElementById(id).value = v; }
    function selectChoice(root, name)
    {
      var textInput = document.getElementById(root + "_" + name + "_text");
      var choice = document.getElementById(root + "_" + name + "_choice");
      var infoId = root + "_" + name + "_" + textInput.value + "_info";
      infoId = infoId.replace(/\./g, "_");

      if (textInput.value != "")
        hide(infoId);

      textInput.value = choice.options[choice.selectedIndex].value;

      infoId = root + "_" + name + "_" + textInput.value + "_info";
      infoId = infoId.replace(/\./g, "_");
      show(infoId);
    }
  </script>
</head>

<body>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
<tr>
  <td width="160" align="center">
  </td>

  <td width="10">
  </td>

  <td valign="top">
   <ul class="status">
<?
if (! empty($server)) {
  $server_name = $server->Id ? $server->Id : "default";

?>
   <li class="server"><?php display_servers($server) ?></li>
<? }  ?>
<!--
   <li>Last Refreshed: <?= strftime("%Y-%m-%d %H:%M:%S", time()) ?></li>
   -->
   <li><a href="<?= $g_next_url ?>">refresh</a></li>
   <li><a href="?q=index.php&logout=true">logout</a></li>
   </ul>

  </td>

  <td align='right'>
   <img src='<?= uri("images/caucho-logo.png") ?>' width='300'></tr>
  </td>

<tr>
  <td width="150">
   <img src='<?= uri("images/pixel.gif") ?>' height="14">
  </td>

  <td width="10">
  </td>

  <td>
  </td>

  <td>
  </td>
</tr>

<tr>
<td width="150">
<hr>
</td>
</tr>

<tr>
  <td class="leftnav" valign="top">
    <ul class="leftnav">
    <?php
      display_pages();
/*
         if ($allow_remote)
           display_left_navigation($server);
*/
    ?>
    </ul>
  </td>

  <td width="10">
  </td>
  <td valign='top' colspan='2'>

<?php
  if (! $server && $g_server_id) {
    echo "<h3 class='fail'>Can't contact $g_server_id</h3>";
    return false;
  }
  
  return true;
}

function display_pages()
{
  global $g_pages;
  global $g_page;
  global $g_server_index;

  $names = array_keys($g_pages);
  sort($names);

  $names = array_diff($names, array('index', 'summary'));
  array_unshift($names, 'summary');
  array_unshift($names, 'index');

  foreach ($names as $name) {
    if ($g_page == $name) {
      echo "<li class='selected'>$name</li>";
    } else {
      echo "<li><a href='?q=$name&s=$g_server_index'>$name</a></li>";
    }
  }
}

function display_servers($server)
{
  global $g_next_url;
  global $g_server_index;

  echo "<form name='servers' method='POST' action='" . $g_next_url . "'>";
  echo "Server: "; 
  echo "<select name='new_s' onchange='document.forms.servers.submit();'>\n";

  $self_server = $server->SelfServer;

  foreach ($self_server->Cluster->Servers as $cluster_server) {
    $id = $cluster_server->Name;
    if (! $id)
      $id = "default";

    echo "  <option";
    if ($cluster_server->ClusterIndex == $g_server_index)
      echo " selected";

    echo " value=\"" . $cluster_server->ClusterIndex ."\">";
    printf("%02d - %s\n", $cluster_server->ClusterIndex, $id);
    echo "  </option>";
  }
  echo "</select>";
  echo "</form>";
}

/**
 * Returns the title for the page, established by the first call to display_header().
 */
function display_header_title()
{
  global $display_header_title;

  return $display_header_title;
?>

<?php
}

/**
 * Outputs an html footer if needed.
 */
function display_footer($script)
{
  global $display_header_script, $is_display_footer;

  if ($is_display_footer)
    return;

  if ($script !== $display_header_script)
    return;

  $is_display_footer = true;

?>
</td></tr></table>
<hr />
<p>
<em><?= resin_version() ?></em>
</p>

</td></tr></table>

</body>
</html>
<?php
}

function display_left_navigation($current_server)
{
  global $g_page;
  global $g_server_id;
  
  $mbean_server = new MBeanServer();

  if (! $mbean_server)
    return;

  $resin = $mbean_server->lookup("resin:type=Resin");
  $server = $mbean_server->lookup("resin:type=Server");

  if (! $current_server)
    $current_server = $server;

  foreach ($resin->Clusters as $cluster) {
    if (empty($cluster->Servers))
      continue;

    echo "<div class='nav-cluster'>$cluster->Name</div>\n";

    $client_names = array();
    /*
    if ($cluster->Name == $server->Cluster->Name) {
      $client_names[] = $server->Id;
    }*/

    foreach ($cluster->Servers as $client) {
      $client_names[] = $client->Name;
    }

    sort($client_names);

    foreach ($client_names as $client) {
      $name = $client;
      if ($name == "")
        $name = "default";

      if (! $client)
        $client = '""';
        
      $client_server = $mbean_server->lookup("resin:type=ClusterServer,name=$client");

      if ($client == $current_server->Id) {
        echo "<div class='nav-this'>$name</div>\n";
      }
      else if ($client_server && ! $client_server->ping()) {
        echo "<div class='nav-dead'>$name</div>\n";
      }
      else {
        echo "<div class='nav-server'><a href='?q=$g_page&server-id=$name'>";
        echo "$name</a></div>\n";
      }
    }
  }
}

function row_style($i)
{
  switch ($i % 2) {
  case 0: return 'ra';
  case 1: return 'rb';
  default: return '';
  }
}

function info($name,$wiki="")
{
  if (! $wiki)
    $wiki = $name;

  echo $name;
  echo "<sup><small><a href='http://wiki.caucho.com/Admin: $wiki' class='info'>?</a></small></sup>";
}

function sort_host($a, $b)
{
  return strcmp($a->URL, $b->URL);
}

function sort_webapp($a, $b)
{
  return strcmp($a->ContextPath, $b->ContextPath);
}

function display_timeout($timeout)
{
  if ($timeout == 0)
    return "0s";
  else if ($timeout % (24 * 3600 * 1000) == 0) {
    return ($timeout / (24 * 3600 * 1000)) . "d";
  }
  else if ($timeout % (3600 * 1000) == 0) {
    return ($timeout / (3600 * 1000)) . "h";
  }
  else if ($timeout % (60 * 1000) == 0) {
    return ($timeout / (60 * 1000)) . "m";
  }
  else if ($timeout % (1000) == 0) {
    return ($timeout / (1000)) . "s";
  }
  else {
    return $timeout . "ms";
  }
}


function server_find_by_index($g_mbean_server, $index)
{
  $server = $g_mbean_server->lookup("resin:type=Server");

  foreach ($server->Cluster->Servers as $cluster_server) {
    if ($cluster_server->ClusterIndex == $index) {
      return $cluster_server;
    }
  }
  
  return null;
}

?>
