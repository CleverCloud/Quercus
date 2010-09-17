<?php

$ok = 0;
$nok = 0;
$i = 0;

$list = file_get_contents('classes.list');

function explodeRows($data) {
  $rowsArr = explode("\n", $data);
  return $rowsArr;
}

$list = explodeRows($list);

foreach($list as $class) {

    if (class_exists($class)) {
	$impl[] = $class;
	$ok++;
    } else {
	$nimpl[] = $class;
	$nok++;
    }

    $i++;
}

print "Number of classes : ".$i."<br/>";
print "Implemented classes : ".$ok." (".number_format(($ok)/($i)*100,2)."%)<br/><br/>";
print "List of unimplemented classes :<br/><ul>";
foreach ($nimpl as $f) {
    print "<li>$f</li>";
}
print "</ul>";
?>