package example.data;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="person")
public class FlickrPerson implements FlickrPayload {
  @XmlAttribute public String nsid;
  @XmlAttribute public int isadmin;
  @XmlAttribute public int ispro;
  @XmlAttribute public int iconserver;

  @XmlElement public String username;
  @XmlElement public String realname;
  @XmlElement public String mbox_sha1sum;
  @XmlElement public String location;
  @XmlElement public String photosurl;
  @XmlElement public String profileurl;
  @XmlElement public Photos photos;

  public static class Photos {
    @XmlElement public long firstdate;
    @XmlElement public String firstdatetaken;
    @XmlElement public int count;

    public String toString()
    {
      return "Photos[firstdate=" + firstdate + ", " +
                    "firstdatetaken=" + firstdatetaken + ", " +
                    "count=" + count + "]";
    }
  }

  public String toString() 
  {
    return "FlickrPerson[nsid=" + nsid + ", " +
                        "isadmin=" + isadmin + ", " +
                        "ispro=" + ispro + ", " +
                        "iconserver=" + iconserver + ", " +
                        "username=" + username + ", " +
                        "realname=" + realname + ", " +
                        "mbox_sha1sum=" + mbox_sha1sum + ", " +
                        "location=" + location + ", " +
                        "photosurl=" + photosurl + ", " +
                        "profileurl=" + profileurl + ", " +
                        "photos=" + photos + "]";
  }
}
