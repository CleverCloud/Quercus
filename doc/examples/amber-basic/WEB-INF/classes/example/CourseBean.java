package example;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Local interface for a course taught at Hogwarts, providing
 * methods to view and change it.
 *
 * <code><pre>
 * CREATE TABLE ejb3_basic_courses (
 *   id INTEGER
 *   course VARCHAR(250),
 *   teacher VARCHAR(250),
 * 
 *   PRIMARY KEY(course_id)
 * );
 * </pre></code>
 */
@Entity
@Table(name="ejb3_basic_courses")
public class CourseBean {
  private int _id;
  private String _course;
  private String _teacher;
  
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
