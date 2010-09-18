
$(document).ready(function(){
    $('.more a').live('click',function(){
	$('.datas',$(this).parent().parent()).slideToggle('fast');
	$(this).toggleClass('active');

	if ($(this).hasClass('active'))
	    $(this).html('Hide details');
	else
	    $(this).html('Show details');
    });

    getImpl('functions');
    getImpl('classes');
    getImpl('methods');
    getImpl('constants');

    getVer();
});

function getVer() {
    $.getJSON('engine.php?ver', function(data) {

	$('h1 span').html(data[0]);
    });
}

function getImpl(type) {
    $.getJSON('engine.php?impl&t='+type, function(data) {
	
	var container = $("#"+type);
	var inner = $('.Inner',container);

	var inner2 = $('<div />');
	$(inner2).addClass('Inner2').appendTo($(inner));
	
	// Show stats
	var _bt = $('<div />');
	$(_bt).addClass('total').html('Referenced '+type+' : ');
	$('<span />').appendTo($(_bt));
	$('<strong />').html(data.stats.t).appendTo($('span',_bt));
	$(inner2).append(_bt);

	var _bi = $('<div />');
	$(_bi).addClass('ignore').html('Ignored '+type+' : ');
	$('<span />').html('<strong>'+data.stats.i+'</strong> (<strong>'+data.stats.pi+'%</strong>)').appendTo($(_bi));
	$(inner2).append(_bi);

	var _bo = $('<div />');
	$(_bo).addClass('impl').html('Implemented '+type+' : ');
	$('<span />').html('<strong>'+data.stats.o+'</strong> (<strong>'+data.stats.po+'%</strong>)').appendTo($(_bo));
	$(inner2).append(_bo);

	// Create tinybox
	var tbI = $('<div />');
	$(tbI).addClass('tb-box').addClass(data.tb).html(getTb(data.tb));
	$(inner).append(tbI);

	$('<div />').addClass('clear').appendTo($(inner));

	// Create link for details
	var more = $('<div />');
	$(more).addClass('more');
	$('<a />').html('Show details').attr('href','javascript:void(0)').appendTo($(more));
	$(inner).append(more);

	// Create details div
	var datas = $('<div />');
	$(datas).addClass('datas');
	$('<h3 />').html('Missing elements').appendTo($(datas));
	$('<ul />').appendTo($(datas));
	$('<div />').addClass('clear').appendTo($(datas));
	// Foreach
	for (i in data.data) {
	    $('<li />').html(data.data[i]).appendTo($('ul',datas));
	}
	$(inner).append(datas);

	// Show result
	$(inner).show();
	
	// Remove loading image
	$('.Loading',container).remove();

    });
}

function getTb(tb) {

    var html = null;

    if (tb == 'error') {
	html = "Implementation statement is very low";
    } else if (tb == 'warn') {
	html = "Implementation statement is low";
    } else {
	html = "Implementation statement seems to be good";
    }

    return html;

}