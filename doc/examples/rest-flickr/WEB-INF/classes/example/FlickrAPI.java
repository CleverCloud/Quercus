package example;

import javax.jws.*;
import example.data.*;

public interface FlickrAPI {
  @WebMethod(operationName="flickr.people.findByEmail")
  public FlickrResponse
  findByEmail(@WebParam(name="api_key") String api_key,
              @WebParam(name="find_email") String find_email);

  @WebMethod(operationName="flickr.people.findByUsername")
  public FlickrResponse
  findByUsername(@WebParam(name="api_key") String api_key,
                 @WebParam(name="username") String username);

  @WebMethod(operationName="flickr.people.getInfo")
  public FlickrResponse
  getInfo(@WebParam(name="api_key") String api_key,
          @WebParam(name="user_id") String user_id);

  @WebMethod(operationName="flickr.people.getPublicGroups")
  public FlickrResponse
  getPublicGroups(@WebParam(name="api_key") String api_key,
                  @WebParam(name="user_id") String user_id);

  @WebMethod(operationName="flickr.people.getPublicPhotos")
  public FlickrResponse
  getPublicPhotos(@WebParam(name="api_key") String api_key,
                  @WebParam(name="user_id") String user_id,
                  @WebParam(name="extras") String extras,
                  @WebParam(name="per_page") int per_page,
                  @WebParam(name="page") int page);
}
