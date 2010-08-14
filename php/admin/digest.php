<?php
/**
 * Provides a form for calculating a digest.
 * Usable as an include file or as a standalone page.
 *
 * @author Sam
 */

require_once "WEB-INF/php/inc.php";

display_header("digest.php", "Calculate a password digest", "");

$digest = NULL;
$validation_error = NULL;
$digest_password1 = NULL;
$digest_password2 = NULL;

$digest_attempt = $_REQUEST["digest_attempt"];

if (! empty($_REQUEST["digest_username"]))

if (! empty($digest_attempt)) {
  $digest_username = htmlspecialchars($_REQUEST["digest_username"]);
  $digest_password1 = $_REQUEST["digest_password1"];
  $digest_password2 = $_REQUEST["digest_password2"];
  $digest_realm = htmlspecialchars($_REQUEST["digest_realm"]);

  if (! empty($digest_password1) || ! empty($digest_password2)) {
    if ($digest_password1 !== $digest_password2)
      $validation_error = "Passwords do not match";
    else if (empty($digest_username))
      $validation_error = "Username is required";
    else {
      $passwordDigest = new Java("com.caucho.server.security.PasswordDigest");
      $digest = $passwordDigest->getPasswordDigest($digest_username, $digest_password1, $digest_realm);
  }
}
else {
  if (! empty($digest_username))
    $validation_error = "Password is required";
}
}

if (empty($digest_realm)) 
  $digest_realm = "resin";

?>

<?php
  $show_form = true;

  if (! empty($digest)) {
?>

  <!--
<p>
The digest for user <b><i><?= $digest_username ?></i></b>
in realm <b><i><?= $digest_realm ?></i></b>
is <b><i><?= $digest ?></i></b>
</p>
-->

<?php
    if (basename($_SERVER['PHP_SELF']) !== "calculate-digest.php")
      $show_form = false;
    else
      $digest_username = NULL;

  }

  if ($show_form) {

    if (empty($validation_error) ) {
?>

<h3>Don't have a login yet?</h3>
<p>
To access the Resin administration console, you'll need to generate
a configuration file containing your username and password.  Complete
the form below to create a configuration file automatically with
your login information.  The next page will provide you instructions 
to install the configuration file.
</p>

<?php
    }
?>

<form method="post">
<table>
<?php
  if (! empty($validation_error) ) {
?>

<tr><td colspan='2' class='error'><?= $validation_error ?></td></tr>

<?php
  }
?>

<tr>
<th>Username:</th>
<td><input name="digest_username" size="50" value="<?= $digest_username ?>"></td>
</tr>

<tr>
<th>Password:</th>
<td><input name="digest_password1" type="password" size="50" value=""></td>
</tr>

<tr>
<th>Re-enter password:</th>
<td><input name="digest_password2" type="password" size="50" value=""></td>
</tr>

<tr>
<th>Realm:</th>
<td><input name="digest_realm" size="50" value="<?= $digest_realm ?>"></td>
</tr>

<tr><td colspan='2' class='buttons'><input type="submit" value="Create Configuration File"></td></tr>
</table>

  <input name="digest_attempt" type="hidden" value="true">
</form>

<?php
  }
?>

<?php
  display_footer("digest.php");
?>

