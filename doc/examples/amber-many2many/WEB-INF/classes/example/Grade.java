package example;

import java.util.Collection;

import javax.persistence.*;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE amber_many2many_map (
 *   grade_id INTEGER PRIMARY KEY auto_increment,
 *
 *   student_id INTEGER REFERENCES Student(student_id)
 *   course_id INTEGER REFERENCES Course(course_id)
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_many2many_map")
public class Grade {
  private long _id;
  private Student _student;
  private Course _course;
  private String _grade;

  public Grade()
  {
  }

  public Grade(Student student, Course course, String grade)
  {
    setStudent(student);
    setCourse(course);
    setGrade(grade);
  }

  /**
   * Gets the id.
   */
  @Id
  @Column(name="grade_id")
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
   * Gets the grade.
   */
  @Basic
  public String getGrade()
  {
    return _grade;
  }

  /**
   * Sets the grade.
   */
  public void setGrade(String grade)
  {
    _grade = grade;
  }
  
  /**
   * Returns the student.
   */
  @ManyToOne
  @JoinColumn(name="student_id", nullable=false, updatable=false)
  public Student getStudent()
  {
    return _student;
  }
  
  /**
   * Sets the student.
   */
  public void setStudent(Student student)
  {
    _student = student;
  }
  
  /**
   * Returns the course.
   */
  @ManyToOne
  @JoinColumn(name="course_id", nullable=false, updatable=false)
  public Course getCourse()
  {
    return _course;
  }
  
  /**
   * Sets the course.
   */
  public void setCourse(Course course)
  {
    _course = course;
  }
}
