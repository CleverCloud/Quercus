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
@Table(name="amber_inherit_student")
@Inheritance
@DiscriminatorValue("student")
@DiscriminatorColumn(name="type")  
public class Student {
  @Id
  @Column(name="id")
  @GeneratedValue
  private long _id;
  
  @Basic
  @Column(unique=true, nullable=false)
  private String _name;

  public Student()
  {
  }

  public Student(String name)
  {
    _name = name;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  public String toString()
  {
    return "Student[" + _name + "]";
  }
}
