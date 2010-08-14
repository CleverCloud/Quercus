package example;

import java.util.Collection;

import javax.persistence.*;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE amber_one2many_student (
 *   student_id INTEGER PRIMARY KEY auto_increment,
 *   name VARCHAR(250),
 *   gender VARCHAR(1),
 *   house INTEGER
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_one2many_student")
public class Student {
  @Id
  @Column(name="student_id")
  @GeneratedValue
  private long _id;
  
  @Basic
  @Column(name="name", unique=true)
  private String _name;
  
  @Basic
  @Column(name="gender", length=1)
  private String _gender;
  
  @ManyToOne
  @JoinColumn(name="house_id")
  private House _house;

  public Student()
  {
  }

  public Student(String name, String gender, House house)
  {
    setName(name);
    setGender(gender);
    setHouse(house);
  }

  /**
   * Gets the id.
   */
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
   * Returns the name of the student.
   */
  public String getName()
  {
    return _name;
  }
  
  /**
   * Sets the name of the student.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the gender of the student.
   */
  public String getGender()
  {
    return _gender;
  }

  /**
   * Sets the gender of the student.
   */
  public void setGender(String gender)
  {
    _gender = gender;
  }

  /**
   * Returns the <code>House</code> that this Student belongs to.
   */
  public House getHouse()
  {
    return _house;
  }

  /**
   * Sets the <code>House</code> this Student belongs to.
   */
  public void setHouse(House house)
  {
    _house = house;
  }
}
