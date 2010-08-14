package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.persistence.*;

/**
 * The QueryServlet just displays the values.
 */
public class QueryServlet extends HttpServlet {
  @PersistenceContext(name="example")
  private EntityManager _manager;

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void init()
    throws ServletException
  {
    Student student = null;
    
    try {
      student = _manager.find(Student.class, new Long(1));
      if (student != null)
	return;
    } catch (Exception e) {
    }

    _manager.getTransaction().begin();

    try {
      _manager.persist(new Student("Harry Potter"));
      _manager.persist(new Prefect("Ron Weasley"));
      _manager.persist(new Prefect("Hermione Granger"));
    } finally {
      _manager.getTransaction().commit();
    }
  }

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");
    
    out.println("<h3>Students</h3>");

    Query query = _manager.createQuery("SELECT o FROM Student o");
    
    for (Object student : query.getResultList()) {
      out.println(student + "<br>");
    }
  }
}
