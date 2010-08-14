package example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="categories")
public class Category {
  @Id
  @GeneratedValue
  private int id;

  @Column(unique = true, nullable = false)
  private String name;

  public void setTitle(String name)
  {
    this.name = name;
  }

  public int getId()
  {
    return id;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + id + " name=" + name + "]";
  }
}
