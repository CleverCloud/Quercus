<h3>Recent Messages (PHP)</h3>

<?php

$messages = java_bean("exampleMessages");

echo "<ol>\n";

foreach ($messages->getMessages() as $msg) {
  echo "<li>$msg\n";
}

echo "</ol>\n";

$color1 = $_REQUEST['color1'];
$noun1 = $_REQUEST['noun1'];
$adj1 = $_REQUEST['adj1'];

if ($color1 && $noun1 && $adj1) {
  $msg = "The " . $color1 . " " . $noun1 . " is " . $adj1 . ".";

  bam_send_message("bam-java-queue@localhost", $msg);
}

?>

<h3>Compose new message:</h3>

<form method="POST">
The
<select name='color1'>
<option>red</option>
<option>chartreuse</option>
<option>magenta</option>
<option>olive</option>
<option>avocado</option>
</select>

<select name='noun1'>
<option>roadster</option>
<option>stratocaster</option>
<option>sneaker</option>
<option>spoon</option>
</select>

is

<select name='adj1'>
<option>indulgent</option>
<option>karmically blessed</option>
<option>pensive</option>
<option>fast</option>
</select>.

<p><input type='submit'>

</form>

<a href="index.xtp">tutorial</a> <a href="demo.jsp">demo.jsp</a>
