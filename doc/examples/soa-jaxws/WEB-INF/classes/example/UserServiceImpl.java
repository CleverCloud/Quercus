package example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jws.WebService;

@WebService(endpointInterface="example.UserService")
public class UserServiceImpl implements UserService {
  private final HashMap<Integer,List<User>> _userGroupMap
    = new HashMap<Integer,List<User>>();

  public UserServiceImpl()
  {
    List<User> group1 = new ArrayList<User>();
    group1.add(new User(1, 1, "Bruce"));
    group1.add(new User(2, 1, "Harvey"));

    List<User> group2 = new ArrayList<User>();
    group2.add(new User(1, 2, "Lois"));
    group2.add(new User(2, 2, "Lex"));

    _userGroupMap.put(1, group1);
    _userGroupMap.put(2, group2);
  }

  public List<User> getUsers(int groupId)
    throws InvalidGroupIdException 
  {
    List<User> users = _userGroupMap.get(groupId);

    if (users == null)
      throw new InvalidGroupIdException("Invalid group id");

    return users;
  }
}
