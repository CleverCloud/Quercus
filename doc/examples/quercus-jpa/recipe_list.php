<?php require_once "view_inc.php" ?>

<table border="1">
  <tr>
    <th>Recipe</th>
    <th>Category</th>
    <th>Date</th>
  </tr>
  
<? foreach ($recipes as $recipe) { ?>
  <tr>
    <td>
      <a href="recipe.php?id=<?= $recipe->id ?>"><?= q($recipe->title) ?></a>
      <a href="recipe.php?action=delete&id=<?= $recipe->id ?>">(delete)</a>
    </td>
    <td>
      <a href="recipe.php?action=list&category_id=<?= $category->id ?>"><?= q($recipe->category->name) ?></a>
    </td>
    <td>
      <?= q($recipe->date) ?>
    </td>
  </tr>
<? } ?>
</table>

