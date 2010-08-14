package example;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface UserService {
  @WebMethod
  public List<User> getUsers(int groupId)
    throws InvalidGroupIdException;
}
