package example.data;

import java.util.*;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="groups")
public class FlickrGroups implements FlickrPayload {
  @XmlElement(name="group") public List<Group> groups = new ArrayList<Group>();

  public static class Group {
    @XmlAttribute public String nsid;
    @XmlAttribute public String name;
    @XmlAttribute public int admin;
    @XmlAttribute public int eighteenplus;

    public String toString()
    {
      return "Group[nsid=" + nsid + ", " +
                   "name=" + name + ", " +
                   "admin=" + admin + ", " +
                   "eighteenplus=" + eighteenplus + "]";
    }
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("FlickrGroups[groups=(");

    for (Group group : groups) {
      sb.append(group.toString());
      sb.append(' ');
    }

    sb.append(")]");

    return sb.toString();
  }
}
