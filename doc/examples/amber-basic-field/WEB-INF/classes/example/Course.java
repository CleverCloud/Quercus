package example;

import javax.persistence.*;

/**
 * Local interface for a course taught at Hogwarts, providing
 * methods to view and change it.
 *
 * <code><pre>
 * CREATE TABLE amber_field_courses (
 *   id INTEGER
 *   course VARCHAR(250),
 *   teacher VARCHAR(250),
 * 
 *   PRIMARY KEY(course_id)
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_field_courses")
public class Course {
  @Id
  @Column(name="id")
  @GeneratedValue
  private int _id;
  
  @Basic
  @Column(name="course")
  private String _course;
  
  @Basic
  @Column(name="teacher")
  private String _teacher;

  /**
   * Entities need a zero-arg constructor.
   */
  public Course()
  {
  }

  /**
   * Constructor for the init servlet.
   */
  public Course(String course, String teacher)
  {
    _course = course;
    _teacher = teacher;
  }

  /**
   * Returns the generated database id.
   */
  public int getId()
  {
    return _id;
  }
  
  /**
   * Returns the course name.
   */
  public String course()
  {
    return _course;
  }

  /**
   * Returns the teacher name.
   */
  public String teacher()
  {
    return _teacher;
  }
}
