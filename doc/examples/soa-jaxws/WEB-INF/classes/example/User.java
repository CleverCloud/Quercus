package example;

public class User {
  private String _name;
  private int _id;
  private int _groupId;

  // 
  // JAXB requires a zero-argument constructor
  //
  public User() {}

  public User(int id, int groupId, String name) 
  {
    _id = id;
    _groupId = groupId;
    _name = name;
  }
    
  public String getName() 
  { 
    return _name; 
  }

  public void setName(String name) 
  {
    _name = name; 
  }
  
  public int getId() 
  { 
    return _id; 
  }

  public void setId(int id)
  {
    _id = id; 
  }

  public int getGroupId() 
  { 
    return _groupId; 
  }

  public void setGroupId(int groupId)
  {
    _groupId = groupId; 
  }

  public String toString()
  {
    return "User[id=" + _id + ", groupId=" + _groupId + ", name=" + _name + "]";
  }
}
