package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

import javax.inject.Inject;

public class MovieServlet extends HttpServlet {
  // Inject the MovieLister service
  @Inject private MovieLister _movieLister;
  
  /**
   * Returns movies by a particular director.
   */
  public void doGet(HttpServletRequest request,
		    HttpServletResponse response)
    throws IOException, ServletException
  {
    PrintWriter out = response.getWriter();

    response.setContentType("text/html");

    String director = request.getParameter("director");

    if (director == null || director.indexOf('<') >= 0) {
      out.println("No director specified");
      return;
    }

    out.println("<h1>Director: " + director + "</h1>");

    for (Movie movie : _movieLister.moviesDirectedBy(director)) {
      out.println(movie.getTitle() + "<br>");
    }
  }
}
