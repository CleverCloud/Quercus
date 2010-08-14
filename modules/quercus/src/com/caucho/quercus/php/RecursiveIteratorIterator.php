<?php

class RecursiveIteratorIterator
  implements OuterIterator
{
  const LEAVES_ONLY = 0x00;
  const SELF_FIRST = 0x01;
  const CHILD_FIRST = 0x02;
  const CATCH_GET_CHILD = 0x10;

  private $ait;
  private $count;
  private $flags;
  private $mode;
  
  function __construct(RecursiveIterator $it, $mode = self::LEAVES_ONLY, $flags = 0)
  {
    $this->ait = $it;
    $this->mode = $mode;
    $this->flags = $flags;
  }

  function getInnerIterator()
  {

  }
  
  function current()
  {

  }
  
  function getDepth()
  {
    return $this->level;
  }
  
  function getMaxDepth()
  {
  
  }
  
  function setMaxDepth()
  {
  
  }
  
  function getSubIterator()
  {
  
  }
  
  function beginIteration()
  {
  
  }
  
  function callHasChildren()
  {
  
  }
  
  function callGetChildren()
  {
  
  }
  
  function beginChildren()
  {
  
  }
  
  function endChildren()
  {
  
  }
  
  function endIteration()
  {
  
  }
  
  private function callNextElement($after_move)
  {
  
  }
  
  function nextElement()
  {
  
  }

  function key()
  {
  
  }

  function next()
  {

  }

  function rewind()
  {

  }

  function valid()
  {

  }
}
