<?php

$ok = 0;
$nok = 0;
$i = 0;

$list = file_get_contents('functions.list');

function explodeRows($data) {
  $rowsArr = explode("\n", $data);
  return $rowsArr;
}

$list = explodeRows($list);

foreach($list as $function) {

    if (function_exists($function)) {
	$impl[] = $function;
	$ok++;
    } else {
	$nimpl[] = $function;
	$nok++;
    }

    $i++;
}

print "Number of functions : ".$i."<br/>";
print "Implemented functions : ".$ok." (".number_format(($ok)/($i)*100,2)."%)<br/><br/>";
print "List of unimplemented functions :<br/><ul>";
foreach ($nimpl as $f) {
    print "<li>$f()</li>";
}
print "</ul>";
?>