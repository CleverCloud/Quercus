package example;

import java.util.Collection;

import javax.persistence.*;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE amber_query_student (
 *   id INTEGER PRIMARY KEY auto_increment,
 *   name VARCHAR(250),
 *   gender VARCHAR(1),
 *   house INTEGER
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_query_student")
public class Student {
  private long _id;
  private String _name;
  private String _gender;
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
  @Id
  @Column(name="id")
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
   * Returns the name of the student.
   */
  @Basic
  @Column(unique=true)
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
  @Basic
  @Column(length=1)
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
  @ManyToOne
  @JoinColumn(name="house")
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
