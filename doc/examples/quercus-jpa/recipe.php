<?php
import example.Recipe;
import javax.persistence.Persistence;

$jpa = Persistence::createEntityManagerFactory("recipes")->createEntityManager();

$controller = "recipe";

$form = $_REQUEST["recipe"];
$action = $_REQUEST["action"];
$id = $_REQUEST["id"];

if (empty($action))
  $action = empty($id) ? "list" : "show";

session_start();

$message = $_SESSION['message'];
$error = $_SESSION['error'];

# get the recipe specified by $id, or create a new one if there is no $id
# if the $id is not valid: set the $error, redirect to "list", return NULL
function get_recipe() {
  global $jpa;
  global $id;
  global $error;
  global $redirect;

  if (! empty($id) ) {
    $query = $jpa->createQuery("SELECT r from Recipe r WHERE r.id = ?1");
    $query->setParameter(1, intval($id));
    $recipe =  $query->singleResult;

    if (empty($recipe)) {
      $error = "Recipe '$id' is invalid";
      $redirect = "list";
    }

    return $recipe;
  }
  else
    return new Recipe();
}

$redirect = NULL;
$view = NULL;

switch ($action) {
  case "list":
    $view = "list";

    break;

  case "edit":
    $view = "form";

    $recipe = get_recipe();

    break;

  case "submit":
    $view = "form";

    xa_begin();

    $recipe = get_recipe();

    if (empty($recipe)) {
      xa_rollback();
      break;
    }

    $query = $jpa->createQuery("SELECT c from Category c WHERE c.id = ?1");
    $query->setParameter(1, intval($form["category_id"]));
    $category =  $query->singleResult;

    $recipe->title = $form["title"];
    $recipe->category = $category;
    $categories =  $query->resultList;
    $recipe->description = $form["description"];
    $recipe->instructions = $form["instructions"];
    $recipe->date->set($form["year"], $form["month"], $form["day"]);

    if (empty($recipe->title)) {
      $error = "'Title' is required";
      xa_rollback();
      break;
    }

    if (empty($recipe->category)) {
      $error = "'Category' is required";
      xa_rollback();
      break;
    }

    if (empty($recipe->date)) {
      $error = "'Date' is invalid";
      xa_rollback();
      break;
    }

    try {
      $jpa->persist($recipe);

      xa_commit();

      if (empty($id))
        $message = "Recipe '" . $recipe->title . "' succesfully created.";
      else
        $message = "Recipe '" . $recipe->title . "' succesfully updated.";

      $redirect = empty($id) ? "list" : "show";
    }
    catch (Exception $e) {
      $error = $e->message;
      xa_rollback();
    }

    break;

  case "delete":
    $redirect = "list";

    xa_begin();

    $recipe = get_recipe();

    if (empty($recipe))
      break;

    try {
      $message = "Recipe '" . $recipe->title . "' succesfully deleted.";
      $jpa->remove($recipe);

      xa_commit();
    }
    catch (Exception $e) {
      $error = $e->message;
      xa_rollback();
    }

    break;

  default:
    if (! empty($id) ) {
      $view = "show";
      $recipe = get_recipe();
    }
    else
      $view = "list";
}

if ($view == "form")
  $categories = $jpa->createQuery("SELECT c FROM Category c")->resultList;

if ($redirect)  {
  $_SESSION['message'] = $message;
  $_SESSION['error'] = $error;

  include("redirect.php");
}
else {
  unset($_SESSION['message']);
  unset($_SESSION['error']);

  if ($view == "list") {
    $query = $jpa->createQuery("SELECT c FROM Recipe c");
    $recipes =  $query->resultList;
  }

  include("layout.php");
}

?>
