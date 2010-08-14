package example;

import java.util.ArrayList;

import javax.inject.Inject;

public class MovieLister {
  @Inject private MovieFinder _finder;
  
  /**
   * Returns movies by a particular director.
   */
  public Movie []moviesDirectedBy(String director)
  {
    ArrayList<Movie> movieList = new ArrayList<Movie>();
    
    for (Movie movie : _finder.findAll()) {
      if (director.equals(movie.getDirector()))
	movieList.add(movie);
    }

    return (Movie []) movieList.toArray(new Movie[movieList.size()]);
  }
}
