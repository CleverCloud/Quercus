package example;

import com.caucho.util.L10N;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.Random;
import java.util.HashMap;

/**
 * Professor Trelawney's magic 8 ball.
 * The magic 8 ball will return a prophecy with getProphecy().
 *
 * <p>The default prophet is Professor Trelawney (of course).   If you wish to
 * change the prophet that provides the answer, use
 * setProphet(name), where name is one of "trelawney", "classic", or "kingwu".  
 */
public class Magic8Ball {
  static protected final Logger log = 
    Logger.getLogger(Magic8Ball.class.getName());
  static final L10N L = new L10N(Magic8Ball.class);

  private final static String DEFAULT_PROPHET = "trelawney";

  private Random _random = new Random();

  private String _prophet = DEFAULT_PROPHET;

  private HashMap _prophets = new HashMap();

  public Magic8Ball()
  {
    _prophets.put("trelawney",new String[] {
      "The future for you is very dire",
      "Within a fortnight you will surely die",
      "Grave trouble awaits you",
      "You must not leave the room",
      "The stars are aligned for grave injury",
    });
    _prophets.put("classic",new String[] {

      "Signs point to yes",
      "Yes",
      "Reply hazy, try again",
      "Without a doubt",
      "My sources say no",
      "As I see it, yes",
      "You may rely on it",
      "Concentrate and ask again",
      "Outlook not so good",
      "It is decidedly so",
      "Better not tell you now",
      "Very doubtful",
      "Yes - definitely",
      "It is certain",
      "Cannot predict now",
      "Most likely",
      "Ask again later",
      "My reply is no",
      "Outlook good",
      "Don't count on it",
    });
    _prophets.put("kingwu",new String[] {
"THE CREATIVE works sublime success, Furthering through perseverance.",
"THE RECEPTIVE brings about sublime success, Furthering through the perseverance of a mare.  If the superior man undertakes something and tries to lead, He goes astray; But if he follows, he finds guidance.  It is favorable to find friends in the west and south, To forego friends in the east and north.  Quiet perseverance brings good fortune.",
"DIFFICULTY AT THE BEGINNING works supreme success, Furthering through perseverance.  Nothing should be undertaken.  It furthers one to appoint helpers.",
"YOUTHFUL FOLLY has success.  It is not I who seek the young fool; The young fool seeks me.  At the first oracle I inform him.  If he asks two or three times, it is importunity.  If he importunes, I give him no information.  Perseverance furthers.",
"WAITING. If you are sincere, You have light and success.  Perseverance brings good fortune.  It furthers one to cross the great water.",
"CONFLICT. You are sincere And are being obstructed.  A cautious halt halfway brings good fortune.  Going through to the end brings misfortune.  It furthers one to see the great man.  It does not further one to cross the great water.",
"THE ARMY. The army needs perseverance And a strong man.  Good fortune without blame.",
"HOLDING TOGETHER brings good fortune.  Inquire of the oracle once again Whether you possess sublimity, constancy, and perseverance; Then there is no blame.  Those who are uncertain gradually join.  Whoever come too late Meets with misfortune.",
"THE TAMING POWER OF THE SMALL Has success.  Dense clouds, no rain from our western region.  ",
"TREADING. Treading upon the tail of the tiger.  It does not bite the man. Success.",
"PEACE. The small departs, The great approaches.  Good fortune. Success.",
"STANDSTILL. Evil people do not further The perseverance of the superior man.  The great departs; the small approaches.",
"FELLOWSHIP WITH MEN in the open.  Success.  It furthers one to cross the great water.  The perseverance of the superior man furthers.",
"POSSESSION IN GREAT MEASURE.  Supreme success.",
"MODESTY creates success.  The superior man carries things through.",
"ENTHUSIASM. It furthers one to install helpers And to set armies marching.",
"FOLLOWING has supreme success.  Perseverance furthers. No blame.",
"WORK ON WHAT HAS BEEN SPOILED Has supreme success.  It furthers one to cross the great water.  Before the starting point, three days.  After the starting point, three days.",
"APPROACH has supreme success.  Perseverance furthers.  When the eighth month comes, There will be misfortune.",
"CONTEMPLATION. The ablution has been made, But not yet the offering.  Full of trust they look up to him.",
"BITING THROUGH has success.  It is favorable to let justice be administered.",
"GRACE has success.  In small matters It is favorable to undertake something.",
"SPLITTING APART. IT does not further one To go anywhere.",
"RETURN. Success.  Going out and coming in without error.  Friends come without blame.  To and fro goes the way.  On the seventh day comes return.  It furthers one to have somewhere to go.",
"INNOCENCE. Supreme success.  Perseverance furthers.  If someone is not as he should be, He has misfortune, And it does not further him To undertake anything.",
"THE TAMING POWER OF THE GREAT.  Perseverance furthers.  Not eating at home brings good fortune.  It furthers one to cross the great water.",
"THE CORNERS OF THE MOUTH.  Perseverance brings good fortune.  Pay heed to the providing of nourishment And to what a man seeks To fill his own mouth with.",
"PREPONDERANCE OF THE GREAT.  The ridgepole sags to the breaking point.  It furthers one to have somewhere to go.  Success.",
"The Abysmal repeated.  If you are sincere, you have success in your heart, And whatever you do succeeds.",
"THE CLINGING. Perseverance furthers.  It brings success.  Care of the cow brings good fortune.",
"Influence. Success.  Perseverance furthers.  To take a maiden to wife brings good fortune.",
"DURATION. Success. No blame.  Perseverance furthers.  It furthers one to have somewhere to go.",
"RETREAT. Success.  In what is small, perseverance furthers.",
"THE POWER OF THE GREAT. Perseverance furthers.  ",
"PROGRESS. The powerful prince Is honored with horses in large numbers.  In a single day he is granted audience three times.",
"DARKENING OF THE LIGHT. In adversity It furthers one to be persevering.",
"THE FAMILY. The perseverance of the woman furthers.  ",
"OPPOSITION. In small matters, good fortune.",
"OBSTRUCTION. The southwest furthers.  The northeast does not further.  It furthers one to see the great man.  Perseverance brings good fortune.",
"DELIVERANCE. The southwest furthers.  If there is no longer anything where one has to go, Return brings good fortune.  If there is still something where one has to go, Hastening brings good fortune.",
"DECREASE combined with sincerity Brings about supreme good fortune Without blame.  One may be persevering in this.  It furthers one to undertake something.  How is this to be carried out?  One may use two small bowls for the sacrifice.",
"INCREASE. It furthers one To undertake something.  It furthers one to cross the great water.",
"BREAK-THROUGH. One must resolutely make the matter known At the court of the king.  It must be announced truthfully. Danger.  It is necessary to notify one's own city.  It does not further to resort to arms.  It furthers one to undertake something.",
"COMING TO MEET. The maiden is powerful.  One should not marry such a maiden.",
"GATHERING TOGETHER. Success.  The king approaches his temple.  It furthers one to see the great man.  This brings success. Perseverance furthers.  To bring great offerings creates good fortune.  It furthers one to undertake something.",
"PUSHING UPWARD has supreme success.  One must see the great man.  Fear not.  Departure toward the south Brings good fortune.",
"OPPRESSION. Success. Perseverance.  The great man brings about good fortune.  No blame.  When one has something to say, It is not believed.",
"THE WELL. The town may be changed, But the well cannot be changed.  It neither decreases nor increases.  They come and go and draw from the well.  If one gets down almost to the water And the rope does not go all the way, Or the jug breaks, it brings misfortune.",
"REVOLUTION. On your own day You are believed.  Supreme success, Furthering through perseverance.  Remorse disappears.",
"THE CALDRON. Supreme good fortune.  Success.",
"SHOCK brings success.  Shock comes-oh, oh!  Laughing words -ha, ha!  The shock terrifies for a hundred miles, And he does not let fall the sacrificial spoon and chalice.",
"KEEPING STILL. Keeping his back still So that he no longer feels his body.  He goes into his courtyard And does not see his people.  No blame.",
"DEVELOPMENT. The maiden Is given in marriage.  Good fortune.  Perseverance furthers.",
"THE MARRYING MAIDEN.  Undertakings bring misfortune.  Nothing that would further.",
"ABUNDANCE has success.  The king attains abundance.  Be not sad.  Be like the sun at midday.",
"THE WANDERER. Success through smallness.  Perseverance brings good fortune To the wanderer.",
"THE GENTLE. Success through what is small.  It furthers one to have somewhere to go.  It furthers one to see the great man.",
"THE JOYOUS. Success.  Perseverance is favorable.",
"DISPERSION. Success.  The king approaches his temple.  It furthers one to cross the great water.  Perseverance furthers.",
"LIMITATION. Success.  Galling limitation must not be persevered in.",
"INNER TRUTH. Pigs and fishes.  Good fortune.  It furthers one to cross the great water.  Perseverance furthers.",
"PREPONDERANCE OF THE SMALL. Success.  Perseverance furthers.  Small things may be done; great things should not be done.  The flying bird brings the message: It is not well to strive upward, It is well to remain below.  Great good fortune.",
"AFTER COMPLETION. Success in small matters.  Perseverance furthers.  At the beginning good fortune.  At the end disorder.",
"BEFORE COMPLETION. Success.  But if the little fox, after nearly completing the crossing, Gets his tail in the water, There is nothing that would further.",
    });
  }

  /**
   * Set the prophet to use.
   */
  public void setProphet(String prophet)
  {
    if (prophet == null)
      _prophet = DEFAULT_PROPHET;
    else
      _prophet = prophet;
  }

  /**
   * Get the prophet that will be used.
   */
  public String getProphet()
  {
    return _prophet;
  }

  /**
   * Get a prophecy.
   */
  public String getProphecy()
  {
    String[] prophecies = (String[]) _prophets.get(_prophet);

    if (prophecies == null) {
      return "a false prophet is never wise";
    }
    else {
      int i = _random.nextInt(prophecies.length);
      return prophecies[i];
    }
  }
}

