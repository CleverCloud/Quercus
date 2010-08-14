<?php

// saturated primary/secondary
// 0    #ff0000  - red
// 30   #ff8000  - orange
// 60   #ffff00  - yellow
// 90   #80ff00  - chartreuse
// 120  #00ff00  - green
// 150  #00ff80  - spring green
// 180  #00ffff  - cyan
// 210  #0080ff  - azure
// 240  #0000ff  - blue
// 270  #8000ff  - indigo
// 300  #ff00ff  - magenta
// 330  #ff0080  - rose

$g_colors = array("#ff3030", // red
                  "#30b0ff", // azure
                  "#906000", // brown
                  "#ff9030", // orange
                  "#3030ff", // blue
                  "#000000", // black
                  "#50b000", // green
                  "#d030d0", // magenta
                  "#008080", // cyan
                  "#b03060", // rose
                  "#e090ff", // indigo
                  "#c0c0c0", // gray
                  "#408040"); // forest green

$g_label_width = 180;

function stat_graph_regexp($canvas, $width, $height,
                           $start, $end, $pattern, $expand_height = true)
{
  global $g_mbean_server;
  global $g_label_width;

  if (! $g_mbean_server)
    return;

  $stat = $g_mbean_server->lookup("resin:type=StatService");

  if (! $stat)
    return;

  $full_names = $stat->statisticsNames();

  $names = preg_grep($pattern, $full_names);
  sort($names);

  stat_graph($canvas, $width, $height, $start, $end, $names, $expand_height);
}

function stat_graph($canvas, $width, $height,
                    $start, $end, $names, $expand_height = true)
{
  global $g_mbean_server;
  global $g_label_width;

  if (! $g_mbean_server)
    return;

  $stat = $g_mbean_server->lookup("resin:type=StatService");

  if (! $stat)
    return;

  $step = calculate_step($end - $start, $width);

  graph_draw_header();

  $index = null;

  foreach ($names as $name) {
    $values = $stat->statisticsData($name, $start * 1000, $end * 1000,
                                    $step * 1000);

    if ($index === null && preg_match("/^(\d+)\|/", $name, $name_values)) {
      $index = $name_values[1];
    }

    $bounds = calculate_bounds($bounds, $values);

    $value_set[$name] = $values;
  }

  if ($index !== null) {
    $start_times = $stat->getStartTimes($index, $start * 1000, ($end + 2 * $step) * 1000);
  }

  $l_margin = 50;
  $r_margin = 20;
  $graph_width = $width - $l_margin - $r_margin;

  $col = floor($graph_width / $g_label_width);
  if ($col <= 0)
    $col = 1;

  $label_height = 10 * floor((count($value_set) + $col - 1) / $col);

  if ($expand_height)
    $height += $label_height;

  graph_write_canvas($canvas, $width, $height);

  graph_draw_impl($stat, $canvas, $width, $height,
                  $names, $value_set, $start, $end, $step,
                  $start_times);
}

function calculate_step($range, $width)
{

  return $range / $width * 3;
  /*
  if ($period <= 60) {
    return 0;
  }
  else if ($period <= 6 * 60) {
    return 2 * 60;
  }
  else if ($period <= 24 * 60) {
    return 10 * 60;
  }
  else if ($period <= 7 * 24 * 60) {
    return 60 * 60;
  }
  else {
    return 4 * 60 * 60;
  }
  */
}

function graph_write_canvas($canvas, $width, $height)
{
  echo "<canvas id='$canvas' width='$width' height='$height'>\n";
  echo "HTML 5 &lt;canvas> is not supported in this browser\n";
  echo "</canvas>\n";
}

function graph_draw_header()
{
  static $is_header;

  if ($is_header)
    return;

  $is_header = 1;
?>
<script type="application/x-javascript">
  function select_name(src, dst)
  {
    var source_list = document.getElementById(src);

    if (source_list.selectedIndex <= 0)
      return;

    var selected_value = source_list.options[source_list.selectedIndex];

    if (! selected_value)
      return;
    
    var dest_list = document.getElementById(dst);
    dest_list.add(selected_value);
  }
  
  function start_bounds(canvas_name, c_width, c_height, x1, y1)
  {
    var canvas = document.getElementById(canvas_name);
    if (canvas.getContext) {
      var ctx = canvas.getContext("2d");
    }

    if (! ctx)
      return;

    ctx.save();
    
    ctx.fillStyle = "#fcf8e5";
    ctx.rect(0, 0, c_width, c_height);
    ctx.fill();

    ctx.translate(x1, y1);
    // ctx.scale(width / 100, height / 100);

    ctx.strokeStyle = "#000000";
    
    ctx.strokeStyle = "#ff00ee";
    ctx.fillStyle = "#ff00ee";

    ctx.beginPath();    
    ctx.moveTo(0, 0);

    return ctx;
  }

  function rl(c, x, y)
  {
    c.lineTo(x, y);
    c.stroke();
  }

  function rline(c, x0, y0, x1, y1)
  {
    c.moveTo(x0, y0);
    c.lineTo(x1, y1);
    c.stroke();
  }
</script>
<?php
}

function graph_draw_impl($stat, $canvas, $c_width, $c_height,
                         $names, $value_set, $start, $end, $step,
                         $start_times)
{
  global $g_colors;
  global $g_label_width;

  $l_margin = 50;
  $r_margin = 20;
  $width = $c_width - $l_margin - $r_margin;

  $col = floor($width / $g_label_width);
  if ($col == 0)
    $col = 1;

  $label_height = 10 * floor((count($value_set) + $col - 1) / $col);

  $b_margin = 30 + $label_height;
  $t_margin = 10;

  $height = $c_height - $b_margin - $t_margin;

  foreach ($value_set as $name => $values) {
    $bounds = calculate_bounds($bounds, $values);
  }

  list($x1, $y1, $x2, $y2) = $bounds;
    
  $x1 = $start * 1000;
  $x0 = $x1;
  $x2 = $end * 1000;

  $dx = ($x2 - $x1) / $width;
  $dy = ($y2 - $y1) / $height;

  if ($dx == 0)
    $dx = 1;
  
  if ($dy == 0)
    $dy = 1;

  echo "<script type='application/x-javascript'>\n";
  echo "c = start_bounds('$canvas', $c_width, $c_height, $l_margin, $t_margin);\n";

  draw_grid($width, $height,
            $x1, $y1, $x2, $y2,
            0, 0, $dx, $dy,
            "#c0c0c0", "#c0c0c0");

  foreach ($start_times as $time) {
    echo "c.save();";
    echo "c.strokeStyle = '#60c000';\n";
    echo "c.lineWidth = 2;\n";
    echo "c.beginPath();\n";
    echo "c.moveTo(" . ($time - $x0) / $dx . "," . 0 . ");\n";
    echo "c.lineTo(" . ($time - $x0) / $dx . "," . $height . ");\n";
    echo "c.stroke();\n";
    echo "c.restore();";
  }

  $i = 0;
  foreach ($value_set as $name => $values) {
    echo "c.save();";
    echo "c.strokeStyle = '" . $g_colors[$i] . "';\n";
    echo "c.lineWidth = 1;\n";
    echo "c.beginPath();\n";
    echo "c.moveTo(" . ($values[0]->time - $x0) / $dx
          . "," . (($dy - $values[0]->value) / $dy + $height) . ");\n";

    foreach ($values as $v) {
      printf("rl(c,%.2f,%.2f);\n",
             ($v->time - $x0) / $dx,
             (($dy - $v->value) / $dy + $height));
    }

    $l_margin = 40;
    echo "c.translate("
         . ($g_label_width * floor($i % $col) - $l_margin)
         . ", " . ($height + 25 + 10 * floor($i / $col)) . ");";
  
    echo "c.beginPath();";
    //echo "c.strokeStyle = '" . $colors[$i] . "';";
    echo "c.lineWidth = 2;";
    echo "c.moveTo(0, 10);";
    echo "c.lineTo(15, 10);";
    echo "c.stroke();";
    echo "c.beginPath();";
    echo "c.font = '10px Times';\n";
    echo "c.fillStyle = '#000000';\n";

    $name_seg = preg_split('/[|]/', $name);
    $server = array_shift($name_seg);
    array_shift($name_seg);
    array_shift($name_seg);
    $name = join('|', $name_seg);

    $text = sprintf("%s - %s", $server, $name);
    $text = substr($text, 0, 40);

    echo "c.fillText('" . $text . "', 20, 13);";
    echo "c.restore();";

    $i++;
  }

  echo "c.beginPath();";
  echo "c.strokeStyle ='#000000';\n";
  echo "c.font = '10px Monotype';\n";
  echo "c.fillStyle ='#000000';\n";

  echo "c.fillText('" . print_value($y1) . "', -45, " . ($height) . ");\n";
  echo "c.fillText('" . print_value($y2) . "', -45, " . (10) . ");\n";

  echo "</script>\n";
}

function print_value($value)
{
  if ($value > 1e11)
    return sprintf("%.2g", $value);
  else if ($value >= 1e9)
    return sprintf("%.1fG", $value / 1e9);
  else if ($value >= 1e6)
    return sprintf("%.1fM", $value / 1e6);
  else if ($value >= 1e3)
    return sprintf("%.1fk", $value / 1e3);
  else if ($value >= 1)
    return sprintf("%.1f", $value);
  else
    return sprintf("%.3g", $value);
}

function calculate_bounds($bounds, $values)
{
  if ($bounds) {
    list($min_x, $min_y, $max_x, $max_y) = $bounds;
  }
  else {
    $min_x = 1e50;
    $min_y = 0;//1e50;
    $max_x = -1e50;
    $max_y = -1e50;
  }
  
  foreach ($values as $v) {
    if ($v->getTime() < $min_x)
      $min_x = $v->getTime();
      
    if ($max_x < $v->getTime())
      $max_x = $v->getTime();
      
    if ($v->getValue() < $min_y)
      $min_y = $v->getValue();
      
    if ($max_y < $v->getValue())
      $max_y = $v->getValue();
  }

  return array($min_x, $min_y, $max_x, $max_y);
}

function draw_line($x_0, $y_0, $x_1, $y_1,
                   $x1, $dx, $dy, $height)
{
  // global $x1, $dx, $dy, $height;

  printf("rline(c,%.2f,%.2f,%.2f,%.2f)\n",
           floor(($x_0 - $x1) / $dx) + 0.5,
           floor(($dy - $y_0) / $dy * $height) + 0.5,
           floor(($x_1 - $x1) / $dx) + 0.5,
           floor(($dy - $y_1) / $dy * $height) + 0.5);
}

function draw_grid($width, $height,
                   $min_x, $min_y, $max_x, $max_y,
                   $x0, $y0, $dx, $dy,
                   $low_color, $high_color)
{
  // x-grid
  echo "c.save();\n";
  echo "c.strokeStyle = '" . $high_color . "';\n";
  echo "c.lineWidth = 1;\n";
  echo "c.fillStyle ='#000000';\n";
  echo "c.font = '10px Monotype';\n";
  echo "c.beginPath();\n";

  $x_step = grid_delta_x($min_x, $max_x, $width);

  for ($x = $min_x; $x <= $max_x; $x += $x_step) {
    draw_line($x, 0, $x, $dy,
              $min_x, $dx, $dy, $height);

    if ($x == $min_x || $max_x < $x + $x_step
        || (floor(($x - $x_step) / (3600 * 1000))
            != floor(($x) / (3600 * 1000)))) {
      echo "c.fillText('" . date("H:i", $x / 1000) . "', " . floor(($x - $min_x) / $dx - 13) . ", " . ($height + 10) . ");\n";
    }
    else {
      echo "c.fillText('" . date("  :i", $x / 1000) . "', " . floor(($x - $min_x) / $dx - 13) . ", " . ($height + 10) . ");\n";
    }

    if ($x == $min_x || $max_x < $x + $x_step
        || (floor(($x - $x_step) / (24 * 3600 * 1000))
            != floor(($x) / (24 * 3600 * 1000)))) {
      echo "c.fillText('" . date("m-d", $x / 1000) . "', " . floor(($x - $min_x) / $dx - 13) . ", " . ($height + 20) . ");\n";
    }
  }
  
  // y-grid
  $y = $min_y;

  $digit = (int) ($max_y / pow(10, floor(log10($max_y - $min_y))) + 0.5);
  $high_mod = 2;
  
  if ($digit == 1)
    $digit = 10;
  else if ($digit == 2) {
    $high_mod = 5;
    $digit = 10;
  }
  else if ($digit == 3)
    $digit = 6;

  $delta = ($max_y - $min_y) / $digit;

  if ($delta <= 0)
    $delta = 1;

  $i = 1;
  for ($y = $min_y + $delta; $y < $max_y; $y += $delta) {
    $yp = $y0 + ($y - $min_y) * $height / ($max_y - $min_y);

    $color = $i % $high_mod == 0 ? $high_color : $low_color;

    $i++;
  }
  echo "c.restore();\n";
}

function grid_delta_x($min_x, $max_x, $width)
{
  $delta = ($max_x - $min_x) / 60000;

  if ($delta <= 60) {
    return 15 * 60000;
  }
  else if ($delta <= 3 * 60) {
    if ($width < 300)
      return 60 * 60000;
    else
      return 30 * 60000;
  }
  else if ($delta <= 12 * 60) {
    if ($width < 300)
      return 3 * 60 * 60000;
    else
      return 60 * 60000;
  }
  else if ($delta <= 24 * 60) {
    if ($width < 300)
      return 6 * 60 * 60000;
    else
      return 3 * 60 * 60000;
  }
  else if ($delta <= 7 * 24 * 60) {
    return 24 * 60 * 60000;
  }
  else {
    return 7 * 24 * 60 * 60000;
  }
}
