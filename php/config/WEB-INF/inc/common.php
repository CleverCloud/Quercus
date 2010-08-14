<?php

function common_print()
{
  echo "\n";
  echo "  <class-loader>\n"
  echo "    <tree-loader path=\"\${resin.root}/lib\"/>\n";
  echo "  </class-loader>\n";
  echo "\n";
  echo "  <cluster-default>\n";
  echo "    <resin:import path=\"\${__DIR__}/app-default.xml\"/>\n";
  echo "  </cluster-default>\n";
}

?>