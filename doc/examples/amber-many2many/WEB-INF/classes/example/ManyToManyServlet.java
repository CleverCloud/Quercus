package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import javax.persistence.*;

/**
 * A client to illustrate the many-to-many relation.
 */
public class ManyToManyServlet extends HttpServlet {
  @PersistenceContext(name="example")
  private EntityManager _entityManager;

  public void init()
  {
    Student student = null;

    try {
      student = _entityManager.find(Student.class, new Long(1));
    } catch (Throwable e) {
    }

    if (student == null) {
      _entityManager.getTransaction().begin();

      try {
        Student harry = new Student("Harry Potter");
        _entityManager.persist(harry);

        Student ron = new Student("Ron Weasley");
        _entityManager.persist(ron);

        Student hermione = new Student("Hermione Granger");
        _entityManager.persist(hermione);

        Course darkArts = new Course("Defense Against the Dark Arts");
        _entityManager.persist(darkArts);

        Course potions = new Course("Potions");
        _entityManager.persist(potions);

        Course divination = new Course("Divination");
        _entityManager.persist(divination);

        Course arithmancy = new Course("Arithmancy");
        _entityManager.persist(arithmancy);

        Course transfiguration = new Course("Transfiguration");
        _entityManager.persist(transfiguration);

        Grade grade;

        _entityManager.persist(new Grade(harry, darkArts, "A"));
        _entityManager.persist(new Grade(harry, potions, "C-"));
        _entityManager.persist(new Grade(harry, transfiguration, "B+"));
        _entityManager.persist(new Grade(harry, divination, "B"));

        _entityManager.persist(new Grade(ron, darkArts, "A-"));
        _entityManager.persist(new Grade(ron, potions, "C+"));
        _entityManager.persist(new Grade(ron, transfiguration, "B"));
        _entityManager.persist(new Grade(ron, divination, "B+"));

        _entityManager.persist(new Grade(hermione, darkArts, "A+"));
        _entityManager.persist(new Grade(hermione, potions, "A-"));
        _entityManager.persist(new Grade(hermione, transfiguration, "A+"));
        _entityManager.persist(new Grade(hermione, arithmancy, "A+"));
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

      Query allStudent = _entityManager.createQuery("SELECT o FROM Student o");

      List students = allStudent.getResultList();

      for (int i = 0; i < students.size(); i++) {
        Student student = (Student) students.get(i);

        out.println("<h3>" + student.getName() + "</h3>");

        Collection courses = student.getCourses();

        out.println("<ul>");
        Iterator iter = courses.iterator();
        while (iter.hasNext()) {
          Course course = (Course) iter.next();

          out.println("<li>" + course.getName());
        }
        out.println("</ul>");
      }
    } finally {
      _entityManager.getTransaction().commit();
    }
  }
}
