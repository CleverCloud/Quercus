package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * The basic CourseClient shows the basic flow of any Resin-CMP client.
 */
public class CourseServlet extends HttpServlet {
  @PersistenceContext
  private EntityManager _manager;

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void init()
    throws ServletException
  {
    CourseBean course = null;
    
    try {
      course = _manager.find(CourseBean.class, new Integer(1));
      if (course != null)
	return;
    } catch (Exception e) {
    }

    _manager.getTransaction().begin();
    try {
      CourseBean potions = new CourseBean();
      potions.setCourse("Potions");
      potions.setTeacher("Severus Snape");

      _manager.persist(potions);

      CourseBean xfig = new CourseBean();
      xfig.setCourse("Transfiguration");
      xfig.setTeacher("Minerva McGonagall");

      _manager.persist(xfig);
    } finally {
      _manager.getTransaction().commit();
    }
  }

  /**
   * Illustrates how to interact with the Course Bean.
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    CourseBean []course = new CourseBean[2];

    course[0] = _manager.find(CourseBean.class, new Integer(1));
    course[1] = _manager.find(CourseBean.class, new Integer(2));

    out.println("<h3>Course Details</h3>");

    for (int i = 0; i < course.length; i++) {
      out.println("course: " + course[i].getCourse() + "<br>");
      out.println("teacher: " + course[i].getTeacher() + "<br>");
      out.println("<br>");
    }
  }
}
