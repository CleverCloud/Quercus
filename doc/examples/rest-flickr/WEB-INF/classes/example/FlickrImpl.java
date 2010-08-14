package example;

import java.util.*;
import java.util.logging.*;
import javax.jws.*;
import javax.xml.bind.*;
import example.data.*;

@WebService(endpointInterface="example.FlickrAPI")
public class FlickrImpl implements FlickrAPI {
  private static final Logger log = 
    Logger.getLogger(FlickrImpl.class.getName());

  private HashMap<String,FlickrUser> _emailToUserMap
    = new HashMap<String,FlickrUser>();

  private HashMap<String,FlickrUser> _usernameToUserMap
    = new HashMap<String,FlickrUser>();

  private HashMap<String,FlickrPerson> _infoMap
    = new HashMap<String,FlickrPerson>();

  private HashMap<String,FlickrGroups> _groupsMap
    = new HashMap<String,FlickrGroups>();

  private HashMap<String,ArrayList<FlickrPhotos.Photo>> _photoMap
    = new HashMap<String,ArrayList<FlickrPhotos.Photo>>();

  public FlickrImpl()
  {
    FlickrUser user1 = new FlickrUser();
    user1.nsid = "12345678901@N01";
    user1.username = "resin-caucho";

    _emailToUserMap.put("resin@caucho.com", user1);
    _usernameToUserMap.put("resin-caucho", user1);

    FlickrPerson person1 = new FlickrPerson();
    person1.nsid = "12345678901@N01";
    person1.isadmin = 0;
    person1.ispro = 0;
    person1.iconserver = 2;
    person1.realname = "Resin Caucho";
    person1.mbox_sha1sum = "a11fc34be47a7ad1da8f670a26fa2b29f293c9fd";
    person1.location = "La Jolla, California";
    person1.photosurl = "http://www.flickr.com/photos/resin-caucho/";
    person1.profileurl = "http://www.flickr.com/people/resin-caucho/";
    person1.photos = new FlickrPerson.Photos();
    person1.photos.firstdate = 1053200573;
    person1.photos.firstdatetaken = "2003-10-08 17:32:04";
    person1.photos.count = 342;

    _infoMap.put("12345678901@N01", person1);

    FlickrGroups groups1 = new FlickrGroups();

    FlickrGroups.Group group1 = new FlickrGroups.Group();
    group1.nsid = "23456789012@N01";
    group1.name = "Flowers";
    group1.admin = 0;
    group1.eighteenplus = 0;
    groups1.groups.add(group1);

    FlickrGroups.Group group2 = new FlickrGroups.Group();
    group2.nsid = "34567890123@N01";
    group2.name = "Architecture";
    group2.admin = 0;
    group2.eighteenplus = 0;
    groups1.groups.add(group2);

    _groupsMap.put("12345678901@N01", groups1);

    ArrayList<FlickrPhotos.Photo> photoList1 = 
      new ArrayList<FlickrPhotos.Photo>();

    FlickrPhotos.Photo photo1 = new FlickrPhotos.Photo();
    photo1.id = "3041";
    photo1.owner = "12345678901@N01";
    photo1.secret = "x123456";
    photo1.server = 2;
    photo1.title = "Our wedding";
    photo1.ispublic = 1;
    photo1.isfriend = 0;
    photo1.isfamily = 0;
    photoList1.add(photo1);

    FlickrPhotos.Photo photo2 = new FlickrPhotos.Photo();
    photo2.id = "3042";
    photo2.owner = "12345678901@N01";
    photo2.secret = "y123456";
    photo2.server = 1;
    photo2.title = "Best friends";
    photo2.ispublic = 0;
    photo2.isfriend = 1;
    photo2.isfamily = 0;
    photoList1.add(photo2);

    _photoMap.put("12345678901@N01", photoList1);
  }

  @WebMethod(operationName="flickr.people.findByEmail")
  public FlickrResponse
  findByEmail(@WebParam(name="api_key") String api_key,
              @WebParam(name="find_email") String find_email)
  {
    FlickrResponse response = new FlickrResponse();
    response.payload = _emailToUserMap.get(find_email);

    if (response.payload == null) {
      response.stat = "fail";
      response.payload = new FlickrError();
    }
   
    return response;
  }

  @WebMethod(operationName="flickr.people.findByUsername")
  public FlickrResponse
  findByUsername(@WebParam(name="api_key") String api_key,
                 @WebParam(name="username") String username)
  {
    FlickrResponse response = new FlickrResponse();
    response.payload = _usernameToUserMap.get(username);
   
    if (response.payload == null) {
      response.stat = "fail";
      response.payload = new FlickrError();
    }
   
    return response;
  }

  @WebMethod(operationName="flickr.people.getInfo")
  public FlickrResponse
  getInfo(@WebParam(name="api_key") String api_key,
          @WebParam(name="user_id") String user_id)
  {
    FlickrResponse response = new FlickrResponse();
    response.payload = _infoMap.get(user_id);

    if (response.payload == null) {
      response.stat = "fail";
      response.payload = new FlickrError();
    }
   
    return response;
  }

  @WebMethod(operationName="flickr.people.getPublicGroups")
  public FlickrResponse
  getPublicGroups(@WebParam(name="api_key") String api_key,
                  @WebParam(name="user_id") String user_id)
  {
    FlickrResponse response = new FlickrResponse();
    response.payload = _groupsMap.get(user_id);

    if (response.payload == null) {
      response.stat = "fail";
      response.payload = new FlickrError();
    }
   
    return response;
  }

  @WebMethod(operationName="flickr.people.getPublicPhotos")
  public FlickrResponse
  getPublicPhotos(@WebParam(name="api_key") String api_key,
                  @WebParam(name="user_id") String user_id,
                  @WebParam(name="extras") String extras,
                  @WebParam(name="per_page") int per_page,
                  @WebParam(name="page") int page)
  {
    FlickrResponse response = new FlickrResponse();

    ArrayList<FlickrPhotos.Photo> photoList = _photoMap.get(user_id);

    if (photoList != null) {
      FlickrPhotos photos = new FlickrPhotos();
      photos.page = page;
      photos.pages = (photoList.size() / per_page) +
                     (photoList.size() % per_page > 0 ? 1 : 0);
      photos.perpage = per_page;
      photos.total = photoList.size();

      int startPhoto = (page - 1) * per_page;
      int endPhoto = Math.min(page * per_page, photos.total);

      for (int i = startPhoto; i < endPhoto; i++) 
        photos.photos.add(photoList.get(i));

      response.payload = photos;
    }

    if (response.payload == null) {
      response.stat = "fail";
      response.payload = new FlickrError();
    }
   
    return response;
  }
}
