<?php require_once "view_inc.php" ?>

<p>
  <b>Name:</b> <?= q($category->name) ?>
</p>

<a href="category.php?action=edit&id=<?= $category->id ?>">Edit</a>
| <a href="category.php">Back</a>

