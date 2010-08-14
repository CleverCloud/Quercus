package example.data;

import java.util.*;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="photos")
public class FlickrPhotos implements FlickrPayload {
  @XmlAttribute public int page;
  @XmlAttribute public int pages;
  @XmlAttribute public int perpage;
  @XmlAttribute public int total;

  @XmlElement(name="photo") public List<Photo> photos = new ArrayList<Photo>();

  public static class Photo {
    @XmlAttribute public String id;
    @XmlAttribute public String owner;
    @XmlAttribute public String secret;
    @XmlAttribute public int server;
    @XmlAttribute public String title;
    @XmlAttribute public int ispublic;
    @XmlAttribute public int isfriend;
    @XmlAttribute public int isfamily;

    public String toString()
    {
      return "Photo[id=" + id + ", " +
                   "owner=" + owner + ", " +
                   "secret=" + secret + ", " +
                   "server=" + server + ", " +
                   "title=" + title + ", " +
                   "ispublic=" + ispublic + ", " +
                   "isfriend =" + isfriend + ", " +
                   "isfamily=" + isfamily + "]";
    }
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("FlickrPhotos[page=" + page + ", ");
    sb.append(             "pages=" + pages + ", ");
    sb.append(             "perpage=" + perpage + ", ");
    sb.append(             "total=" + total + ", ");
    sb.append(             "photos=(");

    for (Photo photo : photos) {
      sb.append(photo.toString());
      sb.append(' ');
    }

    sb.append(")]");

    return sb.toString();
  }
}
