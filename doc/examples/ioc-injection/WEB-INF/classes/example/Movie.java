package example;

public class Movie {
  private String _title;
  private String _director;

  /**
   * Sets the movie title.
   */
  public void setTitle(String title)
  {
    _title = title;
  }

  /**
   * Gets the movie title.
   */
  public String getTitle()
  {
    return _title;
  }

  /**
   * Sets the director.
   */
  public void setDirector(String director)
  {
    _director = director;
  }

  /**
   * Gets the director
   */
  public String getDirector()
  {
    return _director;
  }
}
