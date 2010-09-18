<?php

class QITS {

    private $_datas;
    private $_list;
    private $_ignore;
    private $_stats;
    private $_nok;
    private $_type;

    public function __construct() {
	$this->init();
    }

    public function process() {

	switch ($this->_type) {
	    case 'functions':
		$func = 'function_exists';
		break;
	    case 'classes':
		$func = 'class_exists';
		break;
	    case 'methods':
		$func = 'method_exists';
		break;
	    case 'constants':
		$func = 'defined';
		break;
	    default:
		$func = NULL;
		break;
	}

	foreach ($this->_list as $function) {
	    if (in_array($function, $this->_ignore))
		$this->_stats['i']++;
	    else {
		if ($this->_type == 'methods') {
		    list($class,$method) = split('::',$function);

		    if ($func($class,$method))
			  $this->_stats['o']++;
		    else {
			$this->_nok[] = $function;
			$this->_stats['u']++;
		    }
		} else {
		    if ($func($function))
			$this->_stats['o']++;
		    else {
			$this->_nok[] = $function;
			$this->_stats['u']++;
		    }
		}
	    }

	    $this->_stats['t']++;
	}
    }

    public function get($type) {
	$content = file_get_contents('datas/' . $type . '.list');
	$ignorecnt = file_get_contents('datas/' . $type . '.ignore');

	$this->init();
	$this->_type = $type;
	$this->_datas = $content;
	$this->_list = $this->explode($content);
	$this->_ignore = $this->explode($ignorecnt);
    }

    private function explode($data) {
	return explode("\n", $data);
    }

    public function output() {
	$buffer = array();

	$this->_stats['po'] = number_format($this->_stats['o'] / ($this->_stats['t'] - $this->_stats['i']) * 100, 2);
	$this->_stats['pi'] = number_format($this->_stats['i'] / $this->_stats['t'] * 100, 2);

	$buffer['stats'] = $this->_stats;
	$buffer['stats']['i'] = number_format($buffer['stats']['i'], 0, '.', ',');
	$buffer['stats']['o'] = number_format($buffer['stats']['o'], 0, '.', ',');
	$buffer['stats']['u'] = number_format($buffer['stats']['u'], 0, '.', ',');
	$buffer['stats']['t'] = number_format($buffer['stats']['t'], 0, '.', ',');

	foreach ($this->_nok as $item) {
	    // Generate links
	    switch ($this->_type) {
		case 'functions':
		    $repl = sprintf('<a href="http://php.net/manual/en/function.%s.php">%s()</a>', strtr($item, array('_' => '-')), $item);
		    break;
		case 'classes':
		    $repl = sprintf('<a href="http://php.net/manual/en/class.%s.php">%s::</a>', strtr($item, array('_' => '-')), $item);
		    break;
		case 'methods':
		    $repl = sprintf('<a href="http://php.net/manual/en/function.%s.php">%s()</a>', strtr($item, array('_' => '-','::'=>'-')), $item);
		    break;
		default:
		    $repl = $item;
		    break;
	    }

	    $buffer['data'][] = $repl;
	}

	// Generate a tinybox with message : good, low imp' or very low imp'
	if ($this->_stats['po'] > 65)
	    $buffer['tb'] = 'ok';
	else if ($this->_stats['po'] > 30)
	    $buffer['tb'] = 'warn';
	else
	    $buffer['tb'] = 'error';

	return json_encode($buffer);
    }

    public function init() {
	$this->_stats = array();
	$this->_nok = array();

	unset($this->_datas);
	unset($this->_list);
	unset($this->_ignore);
	unset($this->_type);
    }

}

?>