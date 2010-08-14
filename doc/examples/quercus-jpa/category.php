<?php
import example.Category;
import javax.persistence.Persistence;

$jpa = Persistence::createEntityManagerFactory("recipes")->createEntityManager();

$controller = "category";

$form = $_REQUEST["category"];
$action = $_REQUEST["action"];
$id = $_REQUEST["id"];

if (empty($action))
  $action = empty($id) ? "list" : "show";

session_start();

$message = $_SESSION['message'];
$error = $_SESSION['error'];

# get the category specified by $id, or create a new one if there is no $id
# if the $id is not valid: set the $error, redirect to "list", return NULL
function get_category() {
  global $jpa;
  global $id;
  global $error;
  global $redirect;

  if (! empty($id) ) {
    $query = $jpa->createQuery("SELECT c from Category c WHERE c.id = ?1");
    $query->setParameter(1, intval($id));
    $category =  $query->singleResult;

    if (empty($category)) {
      $error = "Category '$id' is invalid";
      $redirect = "list";
    }

    return $category;
  }
  else
    return new Category();
}

$redirect = NULL;
$view = NULL;

switch ($action) {
  case "list":
    $view = "list";

    break;

  case "edit":
    $view = "form";

    $category = get_category();

    break;

  case "submit":
    $view = "form";

    xa_begin();

    $category = get_category();

    if (empty($category)) {
      xa_rollback();
      break;
    }

    $category->name = $form["name"];

    if (empty($category->name)) {
      $error = "'Name' is required";
      xa_rollback();
      break;
    }

    try {
      $jpa->persist($category);

      xa_commit();

      if (empty($id))
        $message = "Category '" . $category->name . "' succesfully created.";
      else
        $message = "Category '" . $category->name . "' succesfully updated.";

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

    $category = get_category();

    if (empty($category))
      break;

    try {
      $message = "Category '" . $category->name . "' succesfully deleted.";
      $jpa->remove($category);

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
      $category = get_category();
    }
    else
      $view = "list";
}

if ($redirect)  {
  $_SESSION['message'] = $message;
  $_SESSION['error'] = $error;

  include("redirect.php");
}
else {
  unset($_SESSION['message']);
  unset($_SESSION['error']);

  if ($view == "list") {
    $query = $jpa->createQuery("SELECT c FROM Category c");
    $categories =  $query->resultList;
  }

  include("layout.php");
}

?>
