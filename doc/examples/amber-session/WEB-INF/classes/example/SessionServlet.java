package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.persistence.*;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SessionServlet extends HttpServlet {
  @PersistenceContext(name="example")
  private EntityManager _manager;

  /**
   * Sets the manager during initialization.
   */
  public void setEntityManager(EntityManager manager)
  {
    _manager = manager;
  }

  /**
   * Handle the request.
   */
  public void service(HttpServletRequest request,
		      HttpServletResponse response)
    throws IOException, ServletException
  {
    response.setContentType("text/html");
    
    PrintWriter out = response.getWriter();

    HttpSession session = request.getSession();

    User user = (User) session.getAttribute("user");

    if (user != null) {
      out.println("User: " + user.getName() + "<br>");
      out.println("Quest: " + user.getQuest() + "<br>");
      out.println("Favorite color: " + user.getColor() + "<br>");
      return;
    }

    String name = request.getParameter("name");
    String quest = request.getParameter("quest");
    String color = request.getParameter("color");

    if (name == null || name.equals("")) {
      out.println("<form action='session'>");
      out.println("What is your name? <input name='name'></input><br>");
      out.println("What is your quest? <input name='quest'></input><br>");
      out.println("What is your favorite color? <input name='color'></input><br>");
      out.println("</form>");
      return;
    }

    user = new User(name, quest, color);

    _manager.persist(user);

    session.setAttribute("user", user);

    out.println("<a href='session'>You may pass.</a>");
  }
}
