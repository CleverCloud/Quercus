<?php
/**
 * Include file that guards access to all admin php.
 *
 * If user is not in some role, make them login
 * or let them determine a password that can be
 * added to the WEB-INF/password.xml file.
 *
 * @author Sam
 */

require_once "WEB-INF/php/inc.php";

if (! ($is_read_role || $is_write_role) ) {
  display_header("restricted.php", "Resin Administration", "");

  $login_uri = uri("login.php?target=$target_uri");
?>
<h2>Login</h2>
<p>
  <a href="<?= $login_uri ?>">Login is required</a>
</p>
<?php
  $request = quercus_servlet_request()

  if (! $is_localhost)
    $is_localhost = $request->remoteHost == $request->serverName;

  if (! $is_localhost)
    $is_localhost = $request->remoteAddr == $request->localAddr;

  if (! $is_localhost) {
    display_footer("restricted.php");
    exit;
  }

?>

<h2>Create password</h2>

<?php
  $authenticator = jndi_lookup("java:comp/env/caucho/admin/auth");

  $password_file = $authenticator->path;

  $is_virgin_password = $authenticator->userCount === 0;

  if (empty($_REQUEST["digest_attempt"])) {

    if ($is_virgin_password) {
?>

<p>
The following form can be used to establish an administration
password for Resin.
</p>

<?php
      } else {
?>

<p>
If you have forgotten your administration password,
the following form can be used to establish a new one.
</p>

<?php
      }
    }

    $digest_username = "";

    include "digest.php";

    if (empty($digest)) {
      display_footer("restricted.php");
      exit
    }
?>

<?php
  /** XXX:
<p>
The following can now be added to the file
<code><b><?= $password_file ?></b></code>
to enable administration functionality. 
</p>

<pre>
&lt;authenticator>
 &lt;user name='<?= $digest_username ?>' password='<?= $digest ?>' roles='read,write'/>
&lt;/authenticator>
</pre>
  */
?>

<p>
The following can now be set in the resin.conf file
to enable administration functionality. 
</p>

<pre>
  &lt;resin:set var="resin_admin_user"  value="<?= $digest_username ?>"/&gt;
  &lt;resin:set var="resin_admin_password"  value="<?= $digest ?>"/&gt;
</pre>

<p>
By default, access to the administration application is limited
to the localhost.  The default behaviour can be changed in the 
resin.conf file.  To enable access to clients other than localhost:
</p>

<pre>
  &lt;resin:set var="resin_admin_external" value="true"/&gt;
</pre>

<p>
Once the file has been updated, you can
<a href="<?= $login_uri ?>">continue to the administration area</a>.
</p>

<p>
When prompted, use the username and password you provided.
</p>

<?php
  display_footer("restricted.php");
  exit
}
?>
