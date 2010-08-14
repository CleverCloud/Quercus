<?php

# quote html characters for ouput
function q($str)
{
  return htmlspecialchars($str, ENT_QUOTES, "UTF-8");
}

?>
