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
public class ManyToOneServlet extends HttpServlet {
  @PersistenceContext(name="example")
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

    Query allStudent = _entityManager.createQuery("SELECT o FROM Student o");
    
    List students = allStudent.getResultList();

    for (int i = 0; i < students.size(); i++) {
      Student student = (Student) students.get(i);

      out.println(student.getName() + " lives in " +
		  student.getHouse().getName() + "<br>");
    }

    String sql = "SELECT s FROM Student s WHERE s.house.name=?1";
    
    Query houseStudents = _entityManager.createQuery(sql);
    houseStudents.setParameter(1, "Gryffindor");

    students = houseStudents.getResultList();

    out.println("<h3>Gryffindor Students</h3>");
    

    for (int i = 0; i < students.size(); i++) {
      Student student = (Student) students.get(i);

      out.println(student.getName() + "<br>");
    }
  }
}
