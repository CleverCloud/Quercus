package example;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.BlockingQueue;

import javax.jms.*;
import javax.inject.Inject;
import javax.inject.Named;

public class AdProducer implements MessageListener {

  private static final Logger log =
    Logger.getLogger(AdProducer.class.getName());

  private Random _random = new Random();
  
  @Inject @Named("AdQueue")
  private BlockingQueue _producer;

  private static final String[] _ads = {
    "Buy widgets",
    "Watch this movie",
    "Eat at Joe's",
    "Learn a new trade",
    "Find your mate"
  };

  public void ejbCreate()
  {
    try {
      String ad = _ads[_random.nextInt(_ads.length)];

      _producer.put(ad);
    } catch (Exception e) {
      log.fine(e.toString());
    }
  }

  public void onMessage(Message incomingMessage)
  {
    try {
      String ad = _ads[_random.nextInt(_ads.length)];

      _producer.put(ad);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

