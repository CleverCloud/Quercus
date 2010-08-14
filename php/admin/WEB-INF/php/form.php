<?php
/**
 * Form processor
 *
 * @author Emil
 */

interface FormItem {
  public function get_root_form();
  public function set_root_form($form);
  public function process_input();
  public function render();
}

class FormContent implements FormItem {
  private $_root_form;
  private $_content;

  public function __construct($content)
  {
    $this->_content = $content;
  }

  public function get_root_form()
  {
    return $this->_root_form;
  }

  public function set_root_form($form)
  {
    $this->_root_form = $form;
  }

  public function process_input()
  {
    return true;
  }

  public function render()
  {
    echo "<tr><td colspan='3'>\n";
    echo $this->_content;
    echo "</td></tr>\n";
  }
}

class FormOutput implements FormItem {
  private $_root_form;
  private $_callback;
  private $_userdata;

  public function __construct($callback, $userdata)
  {
    $this->_callback = $callback;
    $this->_userdata = $userdata;
  }

  public function get_root_form()
  {
    return $this->_root_form;
  }

  public function set_root_form($form)
  {
    $this->_root_form = $form;
  }

  public function process_input()
  {
    return true;
  }

  public function render()
  {
    echo "<tr><td colspan='3'>\n";
    echo "<div class='subform'>\n";
    call_user_func($this->_callback, 
                   $this->_root_form,
                   $this->_userdata);
    echo "</div>\n";
    echo "</td></tr>\n";
  }
}

class FormSubmit implements FormItem {
  private $_root_form;
  private $_label;

  public function __construct($label)
  {
    $this->_label = $label;
  }

  public function get_root_form()
  {
    return $this->_root_form;
  }

  public function set_root_form($form)
  {
    $this->_root_form = $form;
  }

  public function process_input()
  {
    return true;
  }

  public function render()
  {
    echo "<tr><td colspan='3'>\n";
    echo "<div class='form-submit'>\n";
    echo "* indicates a required field";
    echo "<input type='submit' value='{$this->_label}'/>";
    echo "</div>\n";
    echo "</td></tr>\n";
  }
}

abstract class FormField implements FormItem {
  private $_name;
  private $_label;
  private $_required;
  private $_value;
  private $_form;
  private $_default;
  private $_root_form;

  public function __construct($name, $label, $required, $default)
  {
    $this->_name = $name;
    $this->_label = $label;
    $this->_required = $required;
    $this->_default = $default;
  }

  public function get_default()
  {
    return $this->_default;
  }

  public function get_root_form()
  {
    return $this->_root_form;
  }

  public function set_root_form($form)
  {
    $this->_root_form = $form;
  }

  public function get_name()
  {
    return $this->_name;
  }

  public function get_label()
  {
    return $this->_label;
  }

  public function is_required()
  {
    return $this->_required;
  }

  public function set_value($value)
  {
    $this->_value = $value;
  }

  public function get_value()
  {
    return $this->_value;
  }

  public function process_input()
  {
    $valid = true;

    if ($_POST[$this->get_name()] != NULL) {
      $this->set_value($_POST[$this->get_name()]);

      if (! $this->validate())
        $valid = false;
      else 
        $this->_root_form->set_field($this->get_name(), $this);
    }
    elseif ($this->is_required()) {
      $valid = false;
    }

    return $valid;
  }

  public function render()
  {
    $class = 'form';

    if ($this->is_required()) {
      $label = $this->_label . '*';

      if ($this->get_root_form()->is_submitted() && ! $this->get_value())
        $class = 'missing-value';
    }
    else 
      $label = $this->_label;

    echo "<tr>\n";
    echo "<th class='{$class}'>{$label}</th>\n";

    if ($this->get_root_form()->is_submitted() 
        && $this->get_value() 
        && ! $this->validate()) {
      echo "<td class='invalid-input'>" . $this->generate_input_tag() "</td>\n";
    }
    else {
      echo "<td>" . $this->generate_input_tag() "</td>\n";
    }

    echo "<td>(default: " . $this->get_default() . ")</td>\n";
    echo "</tr>\n";
  }

  protected function validate()
  {
    return true;
  }

  abstract protected function generate_input_tag();
}

class TextField extends FormField {
  private $_max_length;
  private $_initial_value;

  public function __construct($name, $label, $required, $default = 'N/A')
  {
    parent::__construct($name, $label, $required, $default);
  }

  public function set_initial_value($initial_value)
  {
    $this->_initial_value = $initial_value;
  }

  public function set_max_length($max_length)
  {
    $this->_max_length = $max_length;
  }

  protected function get_initial_value()
  {
    return $this->_initial_value;
  }

  protected function validate() 
  {
    if ($this->_max_length == 0)
      return true;

    if (strlen($this->_value) > $this->_max_length)
      return false;

    return true;
  }

  public function generate_input_tag()
  {
    $value = $this->_initial_value;

    if ($this->get_root_form()->is_submitted() && $this->get_value())
      $value = $this->get_value();

    $name = $this->get_name();
    $root = $this->get_root_form()->get_name();

    echo "<input name='{$name}' id='{$root}_{$name}_text' type='text'";

    if ($value != NULL)
      echo " value='{$value}'/>";
    else
      echo "/>";
  }
}

class NumberField extends FormField {
  protected function validate() 
  {
    return is_numeric($this->get_value());
  }

  public function generate_input_tag()
  {
    $value = $this->_initial_value;

    if ($this->get_root_form()->is_submitted() && $this->get_value())
      $value = $this->get_value();

    if ($value != NULL) {
      echo "<input name='{$this->get_name()}' type='text' value='{$value}'/>";
    }
    else {
      echo "<input name='{$this->get_name()}' type='text'/>";
    }
  }
}

class PeriodField extends FormField {
  protected function validate() 
  {
    $this->set_value(trim($this->get_value()));
    return ereg("^([0-9]+)(ms|s|m|h|D|W|M|Y)$", $this->get_value());
  }

  public function generate_input_tag()
  {
    $value = $this->_initial_value;

    if ($this->get_root_form()->is_submitted() && $this->get_value())
      $value = $this->get_value();

    if ($value != NULL) {
      echo "<input name='{$this->get_name()}' type='text' value='{$value}'/>";
    }
    else {
      echo "<input name='{$this->get_name()}' type='text'/>";
    }
  }
}

class BooleanField extends FormField {
  public function __construct($name, $label, $required, $default = 'false')
  {
    parent::__construct($name, $label, $required, $default);
  }

  protected function validate() 
  {
    return ($this->get_value() == "true") || ($this->get_value() == "false");
  }

  public function generate_input_tag()
  {
    $value = "";

    if ($this->get_root_form()->is_submitted() && $this->get_value())
      $value = $this->get_value();

    echo "<select name='{$this->get_name()}' type='text'>\n";

    if ($value == "true") {
      echo "  <option value=''>---</option>\n";
      echo "  <option selected value='true'>true</option>\n";
      echo "  <option value='false'>false</option>\n";
    }
    elseif ($value == 'false') {
      echo "  <option value=''>---</option>\n";
      echo "  <option value='true'>true</option>\n";
      echo "  <option selected value='false'>false</option>\n";
    }
    else {
      echo "  <option selected value=''>---</option>\n";
      echo "  <option value='true'>true</option>\n";
      echo "  <option value='false'>false</option>\n";
    }

    echo "</select>\n";
  }
}

class FileUploadField extends FormField {
  public function generate_input_tag()
  {
    echo "<input name='{$this->get_name()}' type='file'/>";
  }
}

class Choice {
  private $_name;
  private $_info;
  private $_doc_url;

  public function __construct($value, $info, $doc_url) 
  {
    $this->_value = $value;
    $this->_info = $info;
    $this->_doc_url = $doc_url;
  }

  public function get_value()
  {
    return $this->_value;
  }

  public function get_info()
  {
    return $this->_info;
  }

  public function get_doc_url()
  {
    return $this->_doc_url;
  }
}

class ChoiceField extends TextField {
  private $_title = NULL;
  private $_choices = array();

  public function set_title($title)
  {
    $this->_title = $title;
  }

  public function add_choice($choice)
  {
    $this->_choices[] = $choice; 
  }

  public function generate_input_tag()
  {
    parent::generate_input_tag();

    $name = $this->get_name();
    $root = $this->get_root_form()->get_name();

    echo "<br/>\n";
    echo "<select name='{$name}_choice' type='text'";
    echo "  id='{$root}_{$name}_choice'";
    echo "  onChange='selectChoice(\"{$root}\", \"{$name}\")'>\n";

    if ($this->_title != NULL) {
      if ($this->get_value() == NULL)
        echo "  <option selected value=''>{$this->_title}</option>\n";
      else
        echo "  <option value=''>{$this->_title}</option>\n";
    }

    foreach ($this->_choices as $choice) {
      $choice_value = $choice->get_value();
      if ($this->get_value() == $choice_value)
        echo "  <option selected value='$choice_value'>$choice_value</option>\n";
      else
        echo "  <option value='$choice_value'>$choice_value</option>\n";
    }
    echo "</select>\n";

    foreach ($this->_choices as $choice) {
      $id = $root . "_" . $name . "_" . $choice->get_value() . "_info";
      $info = $choice->get_info();
      $id = str_replace(".", "_", $id);

      if ($this->get_value() == $choice->get_value())
        echo "<div id='{$id}' style='display: block'>{$info} ";
      else
        echo "<div id='{$id}' style='display: none'>{$info} ";

      echo "[<a target='_blank' href='";
      echo $choice->get_doc_url();
      echo "'>documentation</a>]</div>";
    }
  }
}

class SubForm implements FormItem {
  private $_title;
  private $_name;
  private $_root_form;
  private $_validator;
  private $_children;
  private $_field_map;

  public function __construct($name, $title)
  {
    $this->_name = $name;
    $this->_title = $title;
    $this->_field_map = array();
    $this->_children = array();
  }

  public function set_validator($validator)
  {
    $this->_validator = $validator;
  }

  public function add_child($child)
  {
    $this->_children[] = $child;
    $child->set_root_form($this);
  }

  public function add_children($children)
  {
    foreach ($children as $child)
      $this->add_child($child);
  }

  public function get_title()
  {
    return $this->_title;
  }

  public function get_name()
  {
    return $this->_name;
  }

  public function get_root_form()
  {
    return $this->_root_form;
  }

  public function set_root_form($form)
  {
    $this->_root_form = $form;
    
    foreach ($this->_children as $child) {
      $child->set_root_form($form);
    }
  }

  protected function get_children()
  {
    return $this->_children;
  }

  protected function is_submitted()
  {
    return $_POST['form-name'] == $this->_name;
  }

  protected function set_field($name, $field)
  {
    $this->_field_map[$name] = $field;
  }

  public function get_field($name)
  {
    return $this->_field_map[$name];
  }

  protected function process_input()
  {
    $valid = true;

    foreach ($this->_children as $child) {
      if (! $child->process_input())
        $valid = false;
    }

    return $valid;
  }

  protected function render()
  {
    echo "<tr><td colspan='3'>";
    echo "<div class='subform'>\n";

    echo "<div class='form-title'>{$this->get_title()}</div>\n";
    echo "<table class='form'>\n";
    foreach ($this->_children as $child) {
      $child->render();
    }
    echo "</table>\n";

    echo "</div>\n";
    echo "</td></tr>";
  }
}

class RootForm extends SubForm {
  public function __construct($name, $title)
  {
    parent::__construct($name, $title);
  }

  public function get_root_form()
  {
    return $this;
  }

  public function set_root_form($form)
  {
    // this is always the root form
  }

  public function process($callback, $userdata = NULL)
  {
    $valid = false;

    if ($this->is_submitted()) {
      $valid = $this->process_input($this->get_root_form());

      if ($valid && $this->_validator) {
        if (! call_user_func($this->_validator, $this->_field_map))
          $valid = false;
      }
    }

    if ($valid) {
      $this->add_output($callback, $userdata);
    }

    $this->render();
  }

  protected function add_output($callback, $userdata)
  {
    $output_item = new FormOutput($callback, $userdata);
    $this->add_child($output_item);
  }

  protected function render()
  {
    $this->add_submit();

    echo "<form id='{$this->_name}' name='{$this->_name}' method='POST'>";
    echo "<input type='hidden' name='form-name' value='{$this->_name}'/>\n";

    echo "<table class='form'>\n";
    parent::render();
    echo "</table>\n";

    echo "</form>\n";
  }

  protected function add_submit()
  {
    $this->add_child(new FormSubmit("Submit"));
  }
}

class OptionalForm extends SubForm {
  private $_display = false;

  public function set_default_display($display)
  {
    $this->_display = $display;
  }

  protected function render()
  {
    echo "<tr><td colspan='3'>";
    echo "  <div class='subform'>\n";

    if ($_POST[$this->get_name() . "_display"] == NULL) 
      $display = $this->_display;
    else if ($_POST[$this->get_name() . "_display"] == "true")
      $display = true;
    else
      $display = false;

    if ($display) 
      echo "<input type='hidden' " .
                  "id='{$this->get_name()}_display' " .
                  "name='{$this->get_name()}_display' " .
                  "value='true'/>\n";
    else
      echo "<input type='hidden' " .
                  "id='{$this->get_name()}_display' " .
                  "name='{$this->get_name()}_display' " .
                  "value='false'/>\n";

    echo "  <div class='form-title'>{$this->get_title()}";

    if ($display) 
      echo "<span style='display: none' id='{$this->get_name()}_show'>\n";
    else
      echo "<span style='display: inline' id='{$this->get_name()}_show'>\n";

    echo "<a href='javascript:show(\"{$this->get_name()}_hidden\")," . 
                             "showInline(\"{$this->get_name()}_hide\")," .
                             "hide(\"{$this->get_name()}_show\")," .
                             "setValue(\"{$this->get_name()}_display\", \"true\")'>" .
                             "(show)</a>\n";
    echo "</span>\n";

    if ($display) 
      echo "<span style='display: inline' id='{$this->get_name()}_hide'>\n";
    else
      echo "<span style='display: none' id='{$this->get_name()}_hide'>\n";

    echo "<a href='javascript:hide(\"{$this->get_name()}_hidden\")," . 
                             "hide(\"{$this->get_name()}_hide\")," .
                             "showInline(\"{$this->get_name()}_show\")," .
                             "setValue(\"{$this->get_name()}_display\", \"false\")'>" .
                             "(hide)</a>\n";
    echo "</span>\n";
    echo "</div>\n";

    if ($display) 
      echo "<div style='display: block' id='{$this->get_name()}_hidden'>\n";
    else
      echo "<div style='display: none' id='{$this->get_name()}_hidden'>\n";

    echo "  <table class='form'>\n";
    foreach ($this->_children as $child) {
      $child->render();
    }
    echo "  </table>\n";
    echo "  </div>\n";

    echo "</div>\n";
    echo "</td></tr>";
  }
}
