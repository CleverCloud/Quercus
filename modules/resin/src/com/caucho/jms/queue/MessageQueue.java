package com.caucho.jms.queue;


/**
 * Provides abstract API for a queue.
 */
public interface MessageQueue<E>
{
  /**
   * Sends a message to the destination
   */
  public void send(String msgId,
                   E msg,
                   int priority,
                   long expires)
    throws MessageException;
  
  /**
   * Synchronous/blocking message receiving.
   * Listen for a message from the queue, until a message is received
   * or the timeout occurs.
   */
  public QueueEntry<E> receiveEntry(long expireTime, boolean isAutoAck)
    throws MessageException;
  
  /**
   * Registers a message callback with the queue.  Each message callback
   * will receive messages one at a time until the messages complete.
   */
  public EntryCallback<E> addMessageCallback(MessageCallback<E> messageCallback,
                                             boolean isAutoAck)
    throws MessageException;

  /**
   * Removes the callback when messages are done listening
   */
  public void removeMessageCallback(EntryCallback<E> entryCallback);
    
  /**
   * Rollback a message read
   */
  public void rollback(String msgId);
    
  /**
   * Acknowledges the receipt of a message
   */
  public void acknowledge(String msgId);

  /**
   * Browsing
   */
  // public ArrayList<String> getMessageIds();
}
