<?php
/**
 * Wizard processor
 *
 * Takes form input and outputs XML.
 *
 * @author Emil
 */

require_once "WEB-INF/php/form.php";

interface WizardItem extends FormItem {
  public function generate_xml();
}

// mixin for generating the xml from a field
function generate_field_xml($xml, $value)
{
  if ($value != NULL)
    return "<{$xml}>{$value}</{$xml}>";

  return "";
}

class WizardFormOutput 
  extends FormOutput 
  implements WizardItem 
{
  public function __construct($callback, $userdata)
  {
    parent::__construct($callback, $userdata);
  }

  public function generate_xml()
  {
  }
}

class WizardFormSubmit
  extends FormSubmit
  implements WizardItem 
{
  public function __construct($label)
  {
    parent::__construct($label);
  }

  public function generate_xml()
  {
  }
}

class WizardTextField 
  extends TextField 
  implements WizardItem 
{
  private $_xml;

  public function __construct($name, $label, $required, 
                              $default = 'N/A', $xml = NULL, 
                              $doc_url = NULL)
  {
    parent::__construct($name, $label, $required, $default);
    $this->_xml = $xml;

    if ($doc_url != NULL)
      $this->_label = "<a target='_blank' href='{$doc_url}'>{$this->_label}</a>";
  }

  public function generate_xml()
  {
    return generate_field_xml($this->_xml, $this->get_value());
  }
}

class WizardNumberField 
  extends NumberField 
  implements WizardItem 
{
  private $_xml;

  public function __construct($name, $label, $required, 
                              $default = 'N/A', $xml = NULL,
                              $doc_url = NULL)
  {
    parent::__construct($name, $label, $required, $default);
    $this->_xml = $xml;

    if ($doc_url != NULL)
      $this->_label = "<a target='_blank' href='{$doc_url}'>{$this->_label}</a>";
  }

  public function generate_xml()
  {
    return generate_field_xml($this->_xml, $this->get_value());
  }
}

class WizardPeriodField 
  extends PeriodField 
  implements WizardItem 
{
  private $_xml;

  public function __construct($name, $label, $required, 
                              $default = 'N/A', $xml = NULL,
                              $doc_url = NULL)
  {
    parent::__construct($name, $label, $required, $default);
    $this->_xml = $xml;

    if ($doc_url != NULL)
      $this->_label = "<a target='_blank' href='{$doc_url}'>{$this->_label}</a>";
  }

  public function generate_xml()
  {
    return generate_field_xml($this->_xml, $this->get_value());
  }
}

class WizardBooleanField 
  extends BooleanField 
  implements WizardItem 
{
  private $_xml;

  public function __construct($name, $label, $required, 
                              $default = 'false', $xml = NULL, 
                              $doc_url = NULL)
  {
    parent::__construct($name, $label, $required, $default);
    $this->_xml = $xml;

    if ($doc_url != NULL)
      $this->_label = "<a target='_blank' href='{$doc_url}'>{$this->_label}</a>";
  }

  public function generate_xml()
  {
    return generate_field_xml($this->_xml, $this->get_value());
  }
}

class WizardChoiceField extends ChoiceField {
  private $_xml;

  public function __construct($name, $label, $required, 
                              $default = 'N/A', $xml = NULL,
                              $doc_url = NULL)
  {
    parent::__construct($name, $label, $required, $default);
    $this->_xml = $xml;

    if ($doc_url != NULL)
      $this->_label = "<a target='_blank' href='{$doc_url}'>{$this->_label}</a>";
  }

  public function generate_xml()
  {
    return generate_field_xml($this->_xml, $this->get_value());
  }
}

// mixin for generating the xml from a form
function generate_form_xml($xml, $children)
{
  $output = "";

  if ($xml)
    $output .= "<{$xml}>\n";
  
  $child_output = "";
  foreach ($children as $child) {
    $child_xml = $child->generate_xml();
    
    if ($child_xml)
      $child_output .= $child_xml . "\n";
  }

  if ($child_output) {
    if ($xml) {
      $output .= "  ";
      $child_output = substr($child_output, 0, -1);
      $output .= str_replace("\n", "\n  ", $child_output);
    }
    else {
      $output .= $child_output;
    }
  }

  if ($xml)
    $output .= "\n</{$xml}>";
  else
    $output = rtrim($output);

  return $output;
}

class WizardSubForm 
  extends SubForm 
  implements WizardItem 
{
  private $_xml;

  public function __construct($name, $title, $xml)
  {
    parent::__construct($name, $title);
    $this->_xml = $xml;
  }

  public function generate_xml()
  {
    return generate_form_xml($this->_xml, $this->get_children());
  }
}

class WizardRootForm 
  extends RootForm 
  implements WizardItem
{
  private $_xml;

  public function __construct($name, $title, $xml)
  {
    parent::__construct($name, $title);
    $this->_xml = $xml;
  }

  protected function add_output($callback, $userdata)
  {
    $output_item = new WizardFormOutput($callback, $userdata);
    $this->add_child($output_item);
  }

  public function generate_xml()
  {
    return generate_form_xml($this->_xml, $this->get_children());
  }

  protected function add_submit()
  {
    $this->add_child(new WizardFormSubmit("Generate XML"));
  }
}

class WizardOptionalForm 
  extends OptionalForm 
  implements WizardItem
{
  private $_xml;

  public function __construct($name, $title, $xml = NULL)
  {
    parent::__construct($name, $title);
    $this->_xml = $xml;
  }

  public function generate_xml()
  {
    return generate_form_xml($this->_xml, $this->get_children());
  }
}
