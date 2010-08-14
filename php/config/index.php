<html>
<head>
  <title>Resin Configuration</title>
  <link rel='stylesheet' href='default.css' type='text/css' />
</head>
<body>

<?php

$templates = load_templates();

$current_template = $templates[0];

foreach ($templates as $template) {
  if ($_REQUEST["type"] == $template) {
    $current_template = $template;
  }
}

echo "<table border='0'>";
echo "<tr><td width='75%' valign='top'>";

echo "<h2>Template</h2>\n";

echo "<form id='form' method='GET'>";
echo "<table>";

echo "<tr><th></th><th>template</th><th>description</th></tr>";

foreach ($templates as $template) {
  echo "<tr>\n";
  echo "<td>\n";
  echo "  <input type='radio' name='type'";

  echo " value='$template'";

  echo " onclick='javascript:submit()'";

  if ($template == $current_template) {
    echo " checked";
  }
  
  echo ">\n";
  echo "</td>\n";
  echo "<td>" . $template . "</td>";

  $fn = $template . "_template_description";
  echo "<td>";
  echo $fn();
  echo "</td>";
  
  echo "</tr>";
}

echo "</table>";

$fn = $current_template . "_template_form";

$fn();

echo "<h2>Save</h2>\n";

echo "<input type='submit' name='save' value='save'>\n";

echo "Save to \${resin.root}/generated-resin.xml\n";

echo "</form>";

echo "</td>";
echo "<td>";

$print_fun = $current_template . "_template_print";

ob_start();
$print_fun();

$v = ob_get_contents();

ob_end_clean();

echo "<pre>";
echo htmlspecialchars($v);
echo "</pre>";

echo "</td></tr></table>";

function load_templates()
{
  $dir = opendir("WEB-INF/inc");

  $templates = null;

  while (($file = readdir($dir))) {
    $values = null;
    if (preg_match("/(.*)\.template$/", $file, $values)) {
      include_once("WEB-INF/inc/" . $file);

      $templates[] = $values[1];
    }
  }

  closedir($dir);
  
  return $templates;
}

?>