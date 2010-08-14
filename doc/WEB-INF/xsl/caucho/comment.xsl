$output(disable-output-escaping=>true);


comment:table <<
<table width="100%" border=0 cellspacing=0 cellpadding=0>
<tr><td bgcolor="silver">
<table width="100%" border=0 cellspacing=3 cellpadding=3 bgcolor="#f0f0f0">
$if(@title) << <tr><td bgcolor="silver"><b>$(@title)</b></td></tr> >>
  $apply-templates();
</table>

</td></tr>
</table>
>>

comment:user <<
<tr><td bgcolor="#ddddff">
      <font face="tahoma, verdana, arial, helvetica,sans-serif" size=-1>
  $apply-templates();
</font></td></tr>
>>

comment:body <<
<tr><td>
  $apply-templates();
</td></tr>
>>
