package example;

import javax.persistence.*;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE amber_inherit_student (
 *   id INTEGER PRIMARY KEY auto_increment,
 *   type VARCHAR(10),
 *   name VARCHAR(250),
 * );
 * </pre></code>
 */
@Entity
@DiscriminatorValue("prefect")
public class Prefect extends Student {
  public Prefect()
  {
  }

  public Prefect(String name)
  {
    super(name);
  }

  public String toString()
  {
    return "Prefect[" + getName() + "]";
  }
}
