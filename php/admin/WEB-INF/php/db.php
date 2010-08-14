<?php
require_once "WEB-INF/php/inc.php";

function print_db_pools($db_pools)
{
  ob_start();
?>

<table class="data">
  <tr>
    <th>&nbsp;</th>
    <th colspan='5'><?= info("Connections", "Database_Connections") ?></th>
    <th colspan='2'><?= info("Config", "Database_Connections") ?></th>
  </tr>
  <tr>
    <th>Name</th>
    <th>Active</th>
    <th>Idle</th>
    <th>Created</th>
    <th colspan='2'>Failed</th>
    <th>max-connections</th>
    <th>idle-time</th>
  </tr>

<?php
  $row = 0;
  foreach ($db_pools as $pool) {
?>

  <tr class='<?= row_style($row++) ?>'>
    <td><?= $pool->Name ?></td>
    <td><?= $pool->ConnectionActiveCount ?></td>
    <td><?= $pool->ConnectionIdleCount ?></td>
    <td><?= format_miss_ratio($pool->ConnectionCountTotal,
                              $pool->ConnectionCreateCountTotal) ?></td>
    <td><?= $pool->ConnectionFailCountTotal ?></td>
    <td class='<?= format_ago_class($pool->LastFailTime) ?>'>
        <?= format_ago($pool->LastFailTime) ?></td>
    <td><?= $pool->MaxConnections ?></td>
    <td><?= sprintf("%.2fs", $pool->MaxIdleTime * 0.001) ?></td>
  </tr>
<?php
  }
?>
</table>
<?php
  return ob_get_clean();
}
?>
