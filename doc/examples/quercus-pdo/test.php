<?php

// Resin's PDO supports the JNDI scheme "java:comp"
$pdo = new PDO("java:comp/env/jdbc/resin");

/*
 * Initialization
 */

$rs = $pdo->query("SELECT * FROM brooms WHERE id=1");
if (! $rs) {
  $pdo->exec(<<<END
CREATE TABLE brooms (
  id INTEGER PRIMARY KEY auto_increment,
  name VARCHAR(255) UNIQUE,
  price INTEGER
)
END);

  $brooms = array("cleansweep 5" => 20,
                  "cleansweep 7" => 30,
                  "shooting star" => 5);

  foreach ($brooms as $key => $value) {
    $pdo->exec("INSERT INTO brooms (name, price) VALUES ('$key', '$value')");
  }
}
else {
  $pdo->exec("DELETE FROM brooms WHERE name='firebolt'");
  $pdo->exec("DELETE FROM brooms WHERE name='nimbus 2000'");
  $pdo->exec("DELETE FROM brooms WHERE name='nimbus 2001'");
}

/*
 * $pdo->query can be used in a foreach statement
 */
echo "<h2>Retrieve all entries with \$pdo->query in a foreach</h2>\n";

echo "<table border='2'>\n";
foreach ($pdo->query("SELECT * FROM brooms") as $row) {
  echo "<tr><td>${row['name']}<td>${row['price']}\n";
}
echo "</table>\n";

/*
 * INSERT using $pdo->exec executes a string as a SQL statement
 */
echo "<h2>Create a new entry with \$pdo->exec</h2>\n";

$name = "firebolt";
$price = 4000;

$pdo->exec("INSERT INTO brooms (name, price) VALUES ('$name', $price)")
  || die("Can't add a $name broom");

echo "Inserted record for $name\n";

echo "<h2>Retrieve \$rs->fetch(PDO::FETCH_ASSOC)</h2>\n";

$rs = $pdo->query("SELECT * FROM brooms");
echo "<table border='2'>\n";
while (($row = $rs->fetch(PDO::FETCH_ASSOC))) {
  echo "<tr><td>${row['name']}<td>${row['price']}\n";
}
echo "</table>\n";

/*
 * INSERT with $pdo->prepare and bind param for list
 */
echo "<h2>Create new entries with \$pdo->prepare</h2>\n";

$brooms = array("nimbus 2000" => 100,
                "nimbus 2001" => 150);

$stmt = $pdo->prepare("INSERT INTO brooms (name, price) VALUES (?, ?)")
  or die("Can't prepare statement");

$stmt->bindParam(1, $broom);
$stmt->bindParam(2, $price);

foreach ($brooms as $broom => $price) {
  $stmt->execute() || die("can't insert $broom");

  echo "insert $broom<br>\n";
}

echo "<h2>Retrieve with PDO::FETCH_ASSOC</h2>\n";

$rs = $pdo->query("SELECT * FROM brooms");
echo "<table border='2'>\n";
while (($obj = $rs->fetch(PDO::FETCH_OBJ))) {
  echo "<tr><td>$obj->name<td>$obj->price\n";
}
echo "</table>\n";

/*
 * UPDATE with transactions and $pdo->prepare
 */
echo "<h2>Update entries with \$pdo->prepare</h2>\n";

$brooms = array("nimbus 2000" => 120,
                "nimbus 2001" => 250);

$stmt = $pdo->prepare("UPDATE brooms SET price=:price WHERE name=:broom")
  or die("Can't prepare statement");

$stmt->bindParam(":broom", $broom);
$stmt->bindParam(":price", $price);

$pdo->beginTransaction();
echo "begin transaction<br>\n";

foreach ($brooms as $broom => $price) {
  $stmt->execute() || die("can't update $broom");

  echo "update $broom<br>\n";
}

$pdo->commit();
echo "commit<br>\n";

echo "<h2>Retrieve with PDO::FETCH_BOUND</h2>\n";

$stmt = $pdo->query("SELECT name, price FROM brooms");
$stmt->bindColumn("name", $broom);
$stmt->bindColumn("price", $price);

echo "<table border='2'>\n";
while ($stmt->fetch(PDO::FETCH_BOUND)) {
  echo "<tr><td>$broom<td>$price\n";
}
echo "</table>\n";

/*
 * DELETE with $pdo->prepare
 */
echo "<h2>Delete the new entries</h2>\n";

$stmt = $pdo->prepare("DELETE FROM brooms WHERE name=:name");
$stmt->bindParam(":name", $name);

foreach (array('firebolt', 'nimbus 2000', 'nimbus 2001') as $name) {
  $stmt->execute() || die("Can't delete $name");
  echo "delete $name<br>\n";
}

echo "<h2>Retrieve with PDO::FETCH_OBJ</h2>\n";

$rs = $pdo->query("SELECT * FROM brooms");
echo "<table border='2'>\n";
while (($obj = $rs->fetch(PDO::FETCH_OBJ))) {
  echo "<tr><td>$obj->name<td>$obj->price\n";
}
echo "</table>\n";

?>
