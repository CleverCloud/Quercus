<html>
<head>
  <title>Online Cookbook</title>
</head>
<body>

<h1>Online Cookbook</h1>

<? if (!empty($message)) { ?>
<p style="color: green"><?= $message ?></p>
<? } ?>

<? if (!empty($error)) { ?>
<p style="color: red"><?= $error ?></p>
<? } ?>

<? include("$controller" . "_" . "$view.php"); ?>

<p>
<? if ($controller == "recipe") { ?>
  <a href="recipe.php?action=edit">Create new recipe</a>
<? } else { ?>
  <a href="category.php?action=edit">Create new category</a>
<? } ?>
 
<a href="recipe.php">Show all recipes</a>
<a href="category.php">Show all categories</a>

</p>

</body>
</html>
