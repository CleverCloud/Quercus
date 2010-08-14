package example;

import javax.persistence.*;

import java.util.Collection;

/**
 * Implementation class for the House bean.
 *
 * <code><pre>
 * CREATE TABLE amber_many2one_house (
 *   house_id BIGINT PRIMARY KEY auto_increment,
 *   name VARCHAR(250) UNIQUE NOT NULL
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_many2one_house")
public class House {
  private long _id;
  private String _name;

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
  @Id
  @Column(name="house_id")
  @GeneratedValue
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
  @Basic
  @Column(unique=true)
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
}
