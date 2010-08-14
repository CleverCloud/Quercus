package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import javax.persistence.*;

/**
 * A client to illustrate the query.
 */
public class OneToManyServlet extends HttpServlet {
  @PersistenceContext
  private EntityManager _entityManager;

  public void init()
  {
    House house = null;

    try {
      house = _entityManager.find(House.class, new Long(1));
    } catch (Throwable e) {
    }

    if (house == null) {
      _entityManager.getTransaction().begin();

      try {
        House gryffindor = new House("Gryffindor");
        _entityManager.persist(gryffindor);

        House slytherin = new House("Slytherin");
        _entityManager.persist(slytherin);

        House ravenclaw = new House("Ravenclaw");
        _entityManager.persist(ravenclaw);

        House hufflepuff = new House("Hufflepuff");
        _entityManager.persist(hufflepuff);

        Student student;

        student = new Student("Harry Potter", "M", gryffindor);
        _entityManager.persist(student);

        student = new Student("Ron Weasley", "M", gryffindor);
        _entityManager.persist(student);

        student = new Student("Hermione Granger", "F", gryffindor);
        _entityManager.persist(student);

        student = new Student("Draco Malfoy", "M", slytherin);
        _entityManager.persist(student);

        student = new Student("Millicent Bulstrode", "F", slytherin);
        _entityManager.persist(student);

        student = new Student("Penelope Clearwater", "F", ravenclaw);
        _entityManager.persist(student);
      } finally {
        _entityManager.getTransaction().commit();
      }
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    try {
      _entityManager.getTransaction().begin();

      String sql = "SELECT h FROM House h";

      Query allHouse = _entityManager.createQuery("SELECT o FROM House o");

      List houses = allHouse.getResultList();

      for (int i = 0; i < houses.size(); i++) {
        House house = (House) houses.get(i);

        out.println("<h3>" + house.getName() + "</h3>");

        for (Student student : house.getStudents()) {
          out.println(student.getName() + "<br>");
        }
      }
    } finally {
      _entityManager.getTransaction().commit();
    }
  }
}
