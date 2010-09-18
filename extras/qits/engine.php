<?php

require 'qits.php';

$qits = new QITS();

if (isset($_GET['impl'])) {

    $qits->get($_GET['t']);
    $qits->process();
    print $qits->output();

} else if (isset($_GET['ver'])) {
    print json_encode(array(quercus_version()));
}
?>