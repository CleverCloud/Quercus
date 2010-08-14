<?php
/**
 * Redirect to the summary page
 */

if ($_REQUEST['logout'] == 'true') {
  quercus_servlet_request()->getSession()->invalidate();
  header("Location: index.php");
}
else {
  require "WEB-INF/php/inc.php";

  $g_pages = load_pages("rest");

  $g_page = $_GET['q'];

  if (! $g_pages[$g_page]) {
    $g_page = "index";
  }

  include_once($g_pages[$g_page]);
}

?>
