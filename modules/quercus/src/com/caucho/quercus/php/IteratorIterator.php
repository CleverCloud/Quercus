<?php

class IteratorIterator implements OuterIterator {
  var $it;
  
  function __construct($it)
  {
    $this->it = $it;
  }

  function getInnerIterator()
  {
    return $this->it;
  }

  function current()
  {
    return $this->it->current();
  }

  function key()
  {
    return $this->it->key();
  }

  function next()
  {
    $this->it->next();
  }

  function rewind()
  {
    $this->it->rewind();
  }

  function valid()
  {
    return $this->it->valid();
  }
}
