<?php require_once "view_inc.php" ?>

<p>
  <b>Title:</b> <?= q($recipe->title) ?>
</p>

<p>
  <b>Description:</b> <?= q($recipe->description) ?>
</p>

<p>
  <b>Date:</b> <?= q($recipe->date) ?>
</p>

<p>
  <b>Instructions:</b> <?= q($recipe->instructions) ?>
</p>

<a href="recipe.php?action=edit&id=<?= $recipe->id ?>">Edit</a>
| <a href="recipe.php">Back</a>

