<html>
  <head>
    <title>Frontend</title>
  </head>
  <body>
    <?php
      if ($_POST["add"]) {
        bam_send_message("backend@localhost", "foo");
        bam_send_message("backend@localhost", "bar");
        bam_send_message("backend@localhost", "baz");
        bam_send_message("backend@localhost", "bam");
        bam_send_message("backend@localhost", "bop");
      }
    ?>
    <form action="frontend.php" method="POST">
      <input type="submit" name="add" value="Add 5 Tasks"/>
    </form>

    <iframe src="rss.xml" width="100%" height="100%"/>
  </body>
</html>

