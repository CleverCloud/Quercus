package example;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Column;
import javax.ejb.GeneratorType;
import javax.ejb.Basic;
import static javax.ejb.GeneratorType.AUTO;
import static javax.ejb.AccessType.FIELD;

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
@Entity(access=FIELD)
@Table(name="ejb3_xa_courses")
public class Course {
  @Id(generate=AUTO)
  @Column(name="id")
  private int _id;
  
  @Basic
  @Column(name="course")
  private String _course;
  
  @Basic
  @Column(name="teacher")
  private String _teacher;

  /**
   * Returns the ID of the course.
   */
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
