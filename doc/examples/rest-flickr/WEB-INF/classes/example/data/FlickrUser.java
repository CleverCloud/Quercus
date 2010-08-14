package example.data;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="user")
public class FlickrUser implements FlickrPayload {
  @XmlAttribute public String nsid;
  @XmlElement public String username;

  public String toString()
  {
    return "FlickrUser[nsid=" + nsid + ", username=" + username + "]";
  }
}
