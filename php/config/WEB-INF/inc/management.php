<?php

function management_form()
{
  global $users;

  $user_names = $_REQUEST['user_names'];
  $user_digests = $_REQUEST['user_digests'];

  for ($i = 0; $i < count($user_names); $i++) {
    $name = $user_names[$i];
    $user = array(name => $name,
                  digest => $user_digests[$i]);
    $users[$name] = $user;
  }

  if ($_REQUEST['admin_name'] && $_REQUEST['admin_password']) {
    $name = $_REQUEST['admin_name'];
    $password = $_REQUEST['admin_password'];

    $passwordDigest = new Java("com.caucho.server.security.PasswordDigest");
    
    $digest = $passwordDigest->getPasswordDigest($name, $password, "resin");
    
    $users[$name] = array(name=>$name, digest=>$digest);
  }

  echo "<h2>Management</h2>\n";
  echo "<table>";
  echo "<tr><th align='left'>name</th>";
  echo "<th align='left'>password</th><th></th></tr>";

  foreach ($users as $user) {
    $name = $user['name'];
    $digest = $user['digest'];

    echo "<input type='hidden' name='user_names[]' value='$name'>";
    echo "<input type='hidden' name='user_digests[]' value='$digest'>";
  
    echo "<tr>";
    echo "  <td>$name</td>";
    echo "  <td>******</td>";
    echo "  <td><input type='submit' name='admin_delete_$name' value='delete'></td>";
    echo "</tr>";
  }

  echo "<tr>";
  echo "  <td><input name='admin_name' size='20'></td>";
  echo "  <td><input name='admin_password' type='password' size='20'></td>";
  echo "  <td><input type='submit' name='admin_add' value='add'></td>";
  echo "</tr>";
  echo "</table>";
}

function management_print()
{
  global $users;
  
  echo "\n";
  echo "  <management path=\"admin\">\n";

  foreach ($users as $user) {
    $name = $user['name'];
    $digest = $user['digest'];
    
    echo "    <user name='$name' password='$digest'/>\n";
  }

  echo "\n";
  echo "    <deploy-service/>\n";
  echo "    <jmx-service/>\n";
  echo "    <stat-service/>\n";
  echo "    <xa-log-service/>\n";
  echo "  </management>\n";
}

?>