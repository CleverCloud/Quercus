<?php require_once "view_inc.php" ?>

<table border="1">
  <tr>
    <th>Name</th>
  </tr>
  
<? foreach ($categories as $category) { ?>
  <tr>
  <td><a href="category.php?id=<?= $category->id ?>"><?= q($category->name) ?></a>
      <a href="category.php?action=delete&id=<?= $category->id ?>">(delete)</a>
  </tr>
<? } ?>
</table>

