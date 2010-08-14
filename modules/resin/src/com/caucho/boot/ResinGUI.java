/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.boot;

import com.caucho.Version;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("serial")
public class ResinGUI extends JFrame implements WindowListener, ActionListener {

  private final WatchdogClient _client;
  private final ResinBoot _boot;
  private final ExecutorService _exec = Executors.newSingleThreadExecutor();

  private JRadioButton _start;
  private JRadioButton _stop;
  private JButton _quit;

  public ResinGUI(ResinBoot boot, WatchdogClient client)
    throws HeadlessException, IOException
  {
    super(Version.FULL_VERSION);

    _boot = boot;
    _client = client;

    try {
      _client.startConsole();
    } catch (IOException e) {
      throw e;
    }

    init();
    pack();

    double titleWidth = this.getFontMetrics(this.getFont())
      .getStringBounds(Version.FULL_VERSION,
                       this.getGraphics())
      .getWidth();

    final Dimension size = this.getSize();
    int width = (int) titleWidth + 96;

    if (width < size.getWidth())
      width = (int) size.getWidth();

    Dimension dim = new Dimension(width, (int) size.getHeight());

    this.setMinimumSize(dim);
    this.setSize(dim);
  }

  private void init()
  {
    String id = _client.getId();
    
    if (id == null || id.isEmpty())
      id = "default";

    this.setLayout(new BorderLayout());

    Box box = new Box(BoxLayout.Y_AXIS);

    Border border
      = BorderFactory.createCompoundBorder(new EmptyBorder(5, 5, 5, 5),
                                           new TitledBorder("Server: " + id));

    box.setBorder(border);

    ButtonGroup group = new ButtonGroup();
    _start = new JRadioButton("Start");
    _start.setActionCommand("start");
    _start.addActionListener(this);
    _start.setSelected(true);

    _stop = new JRadioButton("Stop");
    _stop.setActionCommand("stop");
    _stop.addActionListener(this);

    group.add(_start);
    group.add(_stop);

    box.add(_start);
    box.add(_stop);

    this.add(box, BorderLayout.CENTER);

    _quit = new JButton("Quit");
    _quit.setActionCommand("quit");
    _quit.addActionListener(this);
    JPanel panel = new JPanel();
    panel.add(_quit);

    this.add(panel, BorderLayout.SOUTH);

    this.addWindowListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    setUiEnabled(false);
    _exec.execute(new ActionRunnable(e.getActionCommand()));
  }

  private void setUiEnabled(boolean enabled)
  {
    _start.setEnabled(enabled);
    _stop.setEnabled(enabled);
    _quit.setEnabled(enabled);
  }

  @Override
  public void windowDeactivated(WindowEvent e)
  {

  }

  @Override
  public void windowActivated(WindowEvent e)
  {

  }

  @Override
  public void windowDeiconified(WindowEvent e)
  {

  }

  @Override
  public void windowIconified(WindowEvent e)
  {

  }

  @Override
  public void windowClosed(WindowEvent e)
  {
    synchronized (_boot) {
      _boot.notifyAll();
    }
  }

  @Override
  public void windowClosing(WindowEvent e)
  {
    synchronized (_boot) {
      _boot.notifyAll();
    }
  }

  @Override
  public void windowOpened(WindowEvent e)
  {

  }

  public class ActionRunnable implements Runnable {
    private String _action;

    public ActionRunnable(String action)
    {
      _action = action;
    }

    @Override
    public void run()
    {
      if ("start".equals(_action)) {
        try {
          _client.startConsole();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      else
        _client.stopConsole();

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run()
        {
          if ("quit".equals(_action)) {
            setVisible(false);
            dispose();
          }
          else
            setUiEnabled(true);
        }
      });
    }
  }
}
