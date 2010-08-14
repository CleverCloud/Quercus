package example.data;

import java.util.*;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="err")
public class FlickrError implements FlickrPayload {
  @XmlAttribute public int code = 1;
  @XmlAttribute public String msg = "User not found";

  public String toString()
  {
    return "FlickrError[code=" + code + ", msg=" + msg + "]";
  }
}
