package example;

import java.util.List;
import java.util.ArrayList;

public class MovieFinderImpl implements MovieFinder {
  private ArrayList<Movie> _list = new ArrayList<Movie>();

  /**
   * Adds a movie to the list.
   */
  public void addMovie(Movie movie)
  {
    _list.add(movie);
  }
  
  /**
   * Returns all the movies.
   */
  public List<Movie> findAll()
  {
    return new ArrayList<Movie>(_list);
  }
}
