package example;

import javax.persistence.*;

import javax.naming.InitialContext;
import javax.naming.Context;

/**
 * Bean to handle a user.
 *
 * <code><pre>
 * CREATE TABLE amber_session_user (
 *   id INTEGER
 *   name VARCHAR(255),
 *   quest VARCHAR(255),
 *   color VARCHAR(255),
 * 
 *   PRIMARY KEY(id)
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_session_user")
public class User {
  @Id
  @Column(name="id")
  @GeneratedValue
  private int _id;
  
  @Basic
  @Column(name="name")
  private String _name;
  
  @Basic
  @Column(name="quest")
  private String _quest;
  
  @Basic
  @Column(name="color")
  private String _color;

  public User()
  {
  }

  public User(String name, String quest, String color)
  {
    _name = name;
    _quest = quest;
    _color = color;
  }
  
  /**
   * Returns the ID of the user.
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
   * Returns the user's name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the user's quest
   */
  public String getQuest()
  {
    return _quest;
  }

  /**
   * Returns the user's favorite color
   */
  public String getColor()
  {
    return _color;
  }

  /**
   * When serializing, replace with the UserHandle instead.
   */
  private Object writeReplace()
  {
    return new UserHandle(_id);
  }

  static class UserHandle {
    private int _id;

    UserHandle(int id)
    {
      _id = id;
    }

    /**
     * When deserializing return the User.
     */
    private Object readResolve()
    {
      try {
	// get Amber's EntityManager from JNDI
	Context ic = new InitialContext();
	
	EntityManager manager
	  = (EntityManager) ic.lookup("java:comp/env/persistence/PersistenceContext/example");

	// find the user object
	return manager.find(User.class, _id);
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }
}
