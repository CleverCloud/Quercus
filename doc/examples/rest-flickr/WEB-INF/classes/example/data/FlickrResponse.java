package example.data;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="rsp")
public class FlickrResponse {
  @XmlAttribute public String stat = "ok";
  @XmlAnyElement(lax=true) public FlickrPayload payload;

  public String toString()
  {
    return "FlickrResponse[stat=" + stat + ", payload=" + payload + "]";
  }
}
