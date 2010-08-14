<html>
<head>
<title>Internationalization example</title>
<body>

<table border='1' cellpadding='5'>
<tr>
<th>English</th>
<th>Translation</th>
</tr>

<tr>
<td>Good morning</td>


<td>
<?php

  setlocale(LC_MESSAGES, $_GET['language']);

  bindtextdomain('messages', 'locale');

  echo _("Good morning");

?>
</td>
</tr>

<tr>
<td>
10/31/2006
</td>
<td>
<?php echo _("[_0]/[_1]/[_2]", 10, 31, 2006); ?>
</td>
</tr>

</table>

<form>

<p>
Change locale to:
<select name='language'>
<option>none</option>
<option value='de_DE'>de_DE (German)</option>
<option value='en_AU'>en_AU (English)</option>
<option value='fr_CA'>fr_CA (French)</option>

</select>
<input type='submit' value='Update'></input>
</p>

</form>
</body>
</html>
