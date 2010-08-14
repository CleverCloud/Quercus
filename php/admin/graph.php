<?php
/**
 * Provides the most important status information about the Resin server.
 */

// require_once "WEB-INF/php/inc.php";

//echo "<pre>";
$mbean_server = new MBeanServer();

$server = $mbean_server->lookup("resin:type=Server");
$stat_service = $mbean_server->lookup("resin:type=StatService");

if (! $stat_service) {
  echo "stat service not available";
  return;
}

$now = time() * 1000;
$hour = 3600 * 1000;

$full_width = $_GET['width'];
$full_height = $_GET['height'];

if (! $full_width)
  $full_width = 300;
  
if (! $full_height)
  $full_height = 200;

$items = $_GET['g'];

//echo "<pre>";
//var_dump($items);

$graph = new StatGraph();

//$value[0].attr = "resin:type=CpuLoadAvg";

$test_server_id = null;
$g_server_unique = true;

foreach ($items as $item) {
  $stat_data = new StatData();
  $stat_data->name = $item[0];
  // $stat_data->attr = $item[1];

  $server_id = $item[2];
  if (! $server_id)
    $mbean_server = new MBeanServer();
  else if ($server_id == "default") {
    $mbean_server = new MBeanServer("");
    $stat_data->server = $server_id;
  }
  else {
    $mbean_server = new MBeanServer($server_id);
    $stat_data->server = $server_id;
  }
  
  if (isset($test_server) && $test_server != $server_id)
    $g_server_unique = false;
    
  $test_server = $server_id;

  $stat_service = $mbean_server->lookup("resin:type=StatService");

  if (! $stat_service)
    continue;
    
//  resin_var_dump($stat_data);

  $stat = find_stat($stat_service, $stat_data->name);

  if (! $stat) {
//  resin_var_dump($stat_data);
    echo "$stat_data->name is an unknown statistic\n";
    return;
  }

  $stat_data->desc = $stat->description;
  $val = $stat_service->statisticsData($stat_data->name,
                                       $now - 24 * $hour, $now, 1);

  $stat_data->val = $val;
  calculate_data_bounds($stat_data);
  $graph->add($stat_data);
}

$min_x = $graph->min_x;
$min_y = $graph->min_y;
$max_x = $graph->max_x;
$max_y = $graph->max_y;

if ($min_y > 0)
  $min_y = 0;

$base_y = pow(10, floor(log10($max_y)));
$max_y = ceil($max_y / $base_y) * $base_y;

$data_width = $max_x - $min_x;
if ($data_width == 0)
  $data_width = 1;
$data_height = $max_y - $min_y;  
if ($data_height == 0)
  $data_height = 1;

$y0 = 13 * (count($graph->stat_list) + 1) / 2 + 7;

$width = $full_width - 50;
$height = $full_height - ($y0 + 20);
$x0 = 10;

//echo $width . " " . $height . "\n";
//echo $data_width . " " . $data_height. "\n";

$im = imagecreate($full_width, $full_height);

$background = imagecolorallocate($im, 0xf8, 0xf8, 0xf0);
imagefilledrectangle($im, 0, 0, $full_width, $full_height, $background);

//$black = imagecolorallocate($im, 0, 0, 0);
$grid_color = imagecolorallocate($im, 0xb0, 0xb0, 0xb0);
$grid_low_color = imagecolorallocate($im, 0xe0, 0xe0, 0xe0);

$label_color = imagecolorallocate($im, 0x60, 0x60, 0x60);

draw_grid($im, $min_x, $min_y, $max_x, $max_y,
           $x0, $y0, $width, $height, $grid_low_color, $grid_color);

imagerectangle($im, $x0, $y0, $x0 + $width, $y0 + $height, $grid_color);

// x, y labels

$s = date("H:i", $min_x / 1000);
imagestring($im, 2, $x0, $y0 + $height + 2, $s, $label_color);	   

$s = date("H:i", $max_x / 1000);
imagestring($im, 2, $x0 + $width - 25, $y0 + $height + 2, $s, $label_color);

$s = y_label($min_y);
imagestring($im, 2, $x0 + $width + 2, $y0 + $height - 10, $s, $label_color);

$s = y_label($max_y);
imagestring($im, 2, $x0 + $width + 2, $y0, $s, $label_color);

$colors[] = array(0xff, 0x00, 0x00); // red
$colors[] = array(0x40, 0x40, 0xff); // blue
$colors[] = array(0xff, 0xb0, 0x00); // orange
$colors[] = array(0x00, 0xc0, 0xc0); // cyan
$colors[] = array(0x00, 0x00, 0x00); // black
$colors[] = array(0x80, 0x00, 0x80); // magenta
$colors[] = array(0xa0, 0xa0, 0xf0); // gray
$colors[] = array(0x80, 0x80, 0x00); // brown
$colors[] = array(0x10, 0x80, 0x10); // green
$colors[] = array(0xff, 0x00, 0xff); // magenta
$colors[] = array(0x00, 0xd0, 0x00); // green
$colors[] = array(0xff, 0xa0, 0xff); // magenta
$colors[] = array(0x80, 0xf0, 0x80); // green

$i = 0;
$row = 0;
foreach ($graph->stat_list as $data) {
  $rgb = $colors[$i % count($colors)];
  
  $color = imagecolorallocate($im, $rgb[0], $rgb[1], $rgb[2]);

  $desc = $data->desc;

  if (! $g_server_unique && $data->server)
    $desc .= " [" . $data->server . "]";

  $y = 13 * $row;

  if ($i % 2 == 0) {
    $x = $x0;
  }
  else {
    $x = $x0 + ($full_width - $x0) / 2;
    
    $row++;
  }

  imageline($im, $x, 9 + $y, $x + 15, 9 + $y, $color);
  imagestring($im, 2, $x + 20, 2 + $y, $desc, $label_color);

  draw_graph($im, $data->val,
             $min_x, $min_y, $data_width, $data_height,
             $x0, $y0, $width, $height, $color);

  $i++;
}

header("Content-Type: image/png");
imagepng($im);

function find_stat($stat_service, $name)
{
  foreach ($stat_service->statisticsNames() as $stat) {
    if ($stat == $name) {
      $value = null;
      $value->name = $name;
      $values = preg_split("/[|]/", $name);
      $index = array_shift($values);
      array_shift($values);
      array_shift($values);
      $value->description = $index . ": " . join('|', $values);
      return $value;
    }
  }

  return null;
}

class StatGraph {
  var $stat_list;

  var $min_x = 0x7fffffffffffffff;
  var $max_x = -0x7fffffffffffffff;
  var $min_y = 0x7fffffffffffffff;
  var $max_y = -0x7fffffffffffffff;

  function add($item)
  {
    if ($item->min_x < $this->min_x)
      $this->min_x = $item->min_x;
      
    if ($this->max_x < $item->max_x)
      $this->max_x = $item->max_x;
      
    if ($item->min_y < $this->min_y)
      $this->min_y = $item->min_y;
      
    if ($this->max_y < $item->max_y)
      $this->max_y = $item->max_y;

    $this->stat_list[] = $item;
  }
}

class StatData {
  var $name;
  var $attr;
  var $desc;
  var $val;

  var $min_x;
  var $min_y;

  var $max_x;
  var $max_y;
}

function y_label($value)
{
  $abs_value = $value < 0 ? - $value : $value;

  if ($abs_value > 1e12)
    return sprintf("%.2g", $value);
  else if ($abs_value >= 1e9)
    return sprintf("%.1fG", $value / 1e9);
  else if ($abs_value >= 1e6)
    return sprintf("%.1fM", $value / 1e6);
  else if ($abs_value >= 1e3)
    return sprintf("%.1fK", $value / 1e6);
  else if ($abs_value >= 0.1)
    return sprintf("%.2f", $value);
  else
    return sprintf("%.2g", $value);
}

function draw_grid($im,
                   $min_x, $min_y, $max_x, $max_y,
                   $x0, $y0, $width, $height, $low_color, $high_color)
{
  // x-grid
  $x = $min_x;

  for ($x = $min_x; $x < $max_x; $x += 3600000) {
    $hour_x = ($x + 3599999);
    $hour_x -= $hour_x % 3600000;

    $hour_time = $hour_x / 1000;
    $hour_time += date_create($hour_x)->offset;
    $day_hour = $hour_time / 3600 % 24;

    if ($hour_x < $max_x) {
      $xp = $x0 + ($hour_x - $min_x) * $width / ($max_x - $min_x);

      if ($day_hour % 6 == 0) {
        $color = $high_color;
      }
      else
        $color = $low_color;

      if ($min_x < $x && $x + 3600000 < $max_x) {
        $s = date("H", $hour_x / 1000);
        imagestring($im, 1, $xp - 2, $y0 + $height + 2, $s, $color);
      }

      imageline($im, $xp, $y0, $xp, $y0 + $height, $color);
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

  $i = 1;
  for ($y = $min_y + $delta; $y < $max_y; $y += $delta) {
    $yp = $y0 + ($y - $min_y) * $height / ($max_y - $min_y);

    $color = $i % $high_mod == 0 ? $high_color : $low_color;

    imageline($im, $x0, $yp, $x0 + $width, $yp, $color);

    $i++;
  }
}

function draw_graph($im, $val,
                    $min_x, $min_y, $data_width, $data_height,
                    $x0, $y0, $width, $height, $color)
{
  $old_xform = image_get_transform($im);

  image_transform_translate($im, $x0, $y0 + $height);
  image_transform_scale($im, 1, -1);
  
  $is_first = true;

  foreach ($val as $point) {
    $data_x = $point->time;
    $data_y = $point->value;

    $x = ($data_x - $min_x) * $width / $data_width;
    $y = ($data_y - $min_y) * $height / $data_height;

//    echo $x . ", " . $y . "\n";

    if ($is_first) {
      imagesetpixel($im, $x, $y, $color);
    }
    else
      imageline($im, $last_x, $last_y, $x, $y, $color);

//    imagearc($im, $x, $y, 3, 3, 0, 360, $color);

    $last_x = $x;
    $last_y = $y;
    $is_first = false;
  }
    
  $old_xform = image_set_transform($im, $old_xform);
}

function calculate_data_bounds($stat_data)
{
  $values = $stat_data->val;
  
  $min_x = 0x7ffffffffffffff;
  $max_x = -0x7ffffffffffffff;
  $min_y = 0x7ffffffffffffff;
  $max_y = -0x7ffffffffffffff;
  
  foreach ($values as $data) {
    $time = $data->time;
    $value = $data->value;

    if ($time < $min_x)
      $min_x = $time;
    if ($max_x < $time)
      $max_x = $time;

    if ($value < $min_y)
      $min_y = $value;
    if ($max_y < $value)
      $max_y = $value;
  }

  $stat_data->min_x = $min_x;
  $stat_data->min_y = $min_y;
  $stat_data->max_x = $max_x;
  $stat_data->max_y = $max_y;
}
