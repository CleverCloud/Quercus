package example;

import javax.persistence.*;

import java.util.Set;

/**
 * Implementation class for the House bean.
 *
 * <code><pre>
 * CREATE TABLE amber_one2many_house (
 *   house BIGINT PRIMARY KEY auto_increment,
 *   name VARCHAR(250) UNIQUE NOT NULL
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_one2many_house")
public class House {
  @Id
  @Column(name="house_id")
  @GeneratedValue
  private long _id;
  
  @Basic
  @Column(name="name",unique=true)
  private String _name;
  
  @OneToMany(mappedBy="_house")
  private Set<Student> _students;

  public House()
  {
  }

  public House(String name)
  {
    _name = name;
  }
  
  /**
   * Returns the id of the house.
   */
  public long getId()
  {
    return _id;
  }
  
  /**
   * Sets the id of the house.
   */
  public void setId(long id)
  {
    _id = id;
  }
  
  /**
   * Returns the name of the house.
   */
  public String getName()
  {
    return _name;
  }
  
  /**
   * Sets the name of the house.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Get the Student's that belong to this house.
   */
  public Set<Student> getStudents()
  {
    return _students;
  }
}
