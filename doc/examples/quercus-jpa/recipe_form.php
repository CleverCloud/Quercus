<?php
  import java.util.Calendar;

  require_once "view_inc.php";


  function year_option($year, $date) {

    echo "<option value='$year'";

    if ($year == $date->get(Calendar::YEAR))
      echo ' selected="selected"';

    echo ">$year</option>\n";
  }

  function month_option($month, $date) {

    echo "<option value='$month'";

    if ($month == $date->get(Calendar::MONTH))
      echo ' selected="selected"';

    switch ($month) {
    case 1:  echo ">January"; break;
    case 2:  echo ">February"; break;
    case 3:  echo ">March"; break;
    case 4:  echo ">April"; break;
    case 5:  echo ">May"; break;
    case 6:  echo ">June"; break;
    case 7:  echo ">July"; break;
    case 8:  echo ">August"; break;
    case 9:  echo ">September"; break;
    case 10: echo ">October"; break;
    case 11: echo ">November"; break;
    case 12: echo ">December"; break;
    }

    echo "</option>\n";
  }

  function dayofmonth_option($dayofmonth, $date) {
    echo "<option value='$dayofmonth'";

    if ($dayofmonth == $date->get(Calendar::DAY_OF_MONTH))
      echo ' selected="selected"';

    echo ">$dayofmonth</option>\n";
  }

?>

<form action="recipe.php?action=submit&id=<?= $recipe->id ?>" method="post">

<p>
<label for="recipe_title">Title</label><br/>
<input id="recipe_title" name="recipe[title]" size="30" type="text" value="<?= q($recipe->title) ?>" />
</p>

<p>
<label for="recipe_category_id">Category</label><br/>
<select id="recipe_category_id" name="recipe[category_id]">
<? foreach ($categories as $category) { ?>
  <option value="<?= $category->id ?>"><?= q($category->name) ?></option>
<? } ?>
</select></p>

<p>
<label for="recipe_description">Description</label><br/>
<input id="recipe_description" name="recipe[description]" size="30" type="text" value="<?= q($recipe->description) ?>" />
</p>

<p>
<label for="recipe_date">Date</label><br/>

<select name="recipe[year]">
<?
  for ($year = 2002; $year <= 2012; $year++) {
    year_option($year, $recipe->date);
  }
?>
</select>

<select name="recipe[month]">
<?
  for ($month = 1; $month <= 12; $month++) {
    month_option($month, $recipe->date);
  }
?>
</select>

<select name="recipe[day]">
<?
  for ($day = 1; $day <= 31; $day++) {
    dayofmonth_option($day, $recipe->date);
  }
?>
</select>

</p>

<p>
<label for="recipe_instructions">Instructions</label><br/>
<textarea cols="40" id="recipe_instructions" name="recipe[instructions]" rows="20">
<?= $recipe->instructions ?>
</textarea>
</p>

<input name="commit" type="submit" value="<?= empty($recipe->id) ? 'Create' : 'Edit' ?>" />

</form>

<a href="recipe.php">Back</a>

