<?php require_once "view_inc.php" ?>

<form action="category.php?action=submit&id=<?= $category->id ?>" method="post">

<p><label for="category_name">Name</label><br/>
  <input id="category_name" name="category[name]" size="30" type="text" value="<?= q($category->name) ?>" /></p>

  <input name="commit" type="submit" value="<?= empty($category->id) ? 'Create' : 'Edit' ?>" />
</form>

<a href="category.php">Back</a>

