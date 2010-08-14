package example;

public class Car implements java.io.Serializable {
  private Model model;
  private Color color;
  private int year;

  public Car()
  {
  }

  public Car(Model model, Color color, int year)
  {
    this.model = model;
    this.color = color;
    this.year = year;
  }

  public Model getModel()
  {
    return this.model;
  }

  public Color getColor()
  {
    return this.color;
  }

  public int getYear()
  {
    return this.year;
  }

  public String toString()
  {
    return "Car[" + this.year + ", " + this.color + ", " + this.model + "]";
  }
}
