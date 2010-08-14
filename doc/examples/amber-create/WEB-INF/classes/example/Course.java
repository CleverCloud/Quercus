package example;

import javax.persistence.*;

/**
 * Local interface for a course taught at Hogwarts, providing
 * methods to view and change it.
 *
 * <code><pre>
 * CREATE TABLE amber_basic_courses (
 *   id INTEGER PRIMARY KEY auto_increment
 *   course VARCHAR(250) UNIQUE,
 *   teacher VARCHAR(250),
 * 
 *   PRIMARY KEY(course_id)
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_create_courses")
public class Course {
  private int _id;
  private String _course;
  private String _teacher;

  /**
   * Null-arg constructor.
   */
  public Course()
  {
  }

  /**
   * Null-arg constructor.
   */
  public Course(String course, String teacher)
  {
    _course = course;
    _teacher = teacher;
  }
  
  /**
   * Returns the ID of the course.
   */
  @Id
  @Column(name="id")
  @GeneratedValue
  public int getId()
  {
    return _id;
  }
  
  public void setId(int id)
  {
    _id = id;
  }

  /**
   * Returns the course name.
   */
  @Basic
  @Column(unique=true)
  public String getCourse()
  {
    return _course;
  }

  /**
   * Sets the course name.
   */
  public void setCourse(String course)
  {
    _course = course;
  }

  /**
   * Returns the teacher name.
   */
  @Basic
  public String getTeacher()
  {
    return _teacher;
  }

  /**
   * Sets the teacher name.
   */
  public void setTeacher(String teacher)
  {
    _teacher = teacher;
  }
}
