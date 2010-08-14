<?php

class FilterIterator extends IteratorIterator {
  var $_key = NULL;
  var $_current = NULL;

  function __construct($iter)
  {
    parent::__construct($iter);
  }

  function current()
  {
    return $this->_current;
  }

  function fetch()
  {
    $this->_key = $this->it->key();
    $this->_current = $this->it->current();

    for (; $this->it->valid() && ! $this->accept(); $this->it->next()) {
      $this->_key = $this->it->key();
      $this->_current = $this->it->current();
    }
  }

  function key()
  {
    return $this->_key;
  }

  function next()
  {
    parent::next();
    $this->fetch();
  }    

  function rewind()
  {
    parent::rewind();
    $this->fetch();
  }

  function __call($fun, $param)
  {
    return call_user_func_array(array($this->it, $fun), $param);
  }
}
