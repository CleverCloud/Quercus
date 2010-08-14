package example;

import java.util.Collection;

import javax.persistence.*;

/**
 * Implementation class for the Course bean.
 *
 * <code><pre>
 * CREATE TABLE amber_many2many_course (
 *   course_id INTEGER PRIMARY KEY auto_increment,
 *   name VARCHAR(250),
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_many2many_course")
public class Course {
  private long _id;
  private String _name;

  public Course()
  {
  }

  public Course(String name)
  {
    setName(name);
  }

  /**
   * Gets the id.
   */
  @Id
  @Column(name="course_id")
  @GeneratedValue
  public long getId()
  {
    return _id;
  }

  /**
   * Sets the id.
   */
  public void setId(long id)
  {
    _id = id;
  }
  
  /**
   * Returns the name of the course.
   */
  @Basic
  @Column(unique=true, nullable=false)
  public String getName()
  {
    return _name;
  }
  
  /**
   * Sets the name of the course.
   */
  public void setName(String name)
  {
    _name = name;
  }
}
