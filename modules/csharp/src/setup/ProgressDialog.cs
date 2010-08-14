/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace Caucho
{
  public partial class ProgressDialog : Form
  {
    public ProgressDialog()
    {
      InitializeComponent();
      _errorProvider.Icon = SystemIcons.Error;
    }

    public delegate void SetSuccessCallBack(String success, bool resetStatus);

    public void SetSuccess(String success, bool resetStatus)
    {
      BeginInvoke(new SetSuccessCallBack(_SetSuccess), new object[] { success, resetStatus });
    }

    private void _SetSuccess(String success, bool resetStatus)
    {
      _timer.Stop();
      _progressBar.Value = _progressBar.Maximum;
      if (resetStatus)
        _statusText.Clear();

      _statusText.AppendText(success);
      _statusText.SelectionStart = _statusText.TextLength;
      _statusText.ScrollToCaret();

      _closeButton.Enabled = true;
    }

    public delegate void UpdateStatusCallBack(String status);

    public void UpdateStatus(String status)
    {
      BeginInvoke(new UpdateStatusCallBack(_UpdateStatus), new object[] { status });
    }

    private void _UpdateStatus(String status)
    {
      _statusText.AppendText(status);
      _statusText.AppendText("\n");
      _statusText.SelectionStart = _statusText.TextLength;
      _statusText.ScrollToCaret();
    }

    public delegate void SetErrorCallBack(String error);

    public void SetError(String error)
    {
      BeginInvoke(new SetErrorCallBack(_SetError), new object[] { error });
    }

    private void _SetError(String error)
    {
      _timer.Stop();
      _statusText.AppendText(error);
      _errorProvider.SetError(_statusText, error);
      _statusText.SelectionStart = _statusText.TextLength;
      _statusText.ScrollToCaret();

      _closeButton.Enabled = true;
    }

    public void Reset()
    {
      _statusText.Clear();
      _closeButton.Enabled = false;
      _progressBar.Value = 0;
      _timer.Start();
    }

    public String Message { get { return null; } set { _message.Text = value; } }

    private void TimerTick(object sender, EventArgs e)
    {
      int i = _progressBar.Step * (_progressBar.Maximum - _progressBar.Value) / _progressBar.Maximum;
      _progressBar.Increment(i);
    }

    private void CloseButtonClick(object sender, EventArgs e)
    {
      Hide();
    }

    private void ProgressDialogClosing(object sender, FormClosingEventArgs e)
    {
      e.Cancel = !_closeButton.Enabled;
    }

    private void StatusTextKeyDown(object sender, KeyEventArgs e)
    {
      if (e.Modifiers.Equals(Keys.Control) && e.KeyCode.Equals(Keys.C)) {
      } else {
        e.Handled = true;
      }
    }

    private void StatusTextKeyPress(object sender, KeyPressEventArgs e)
    {
      e.Handled = true;
    }

    private void ProgressDialogFormClosed(object sender, FormClosedEventArgs e)
    {
      Reset();
    }
  }
}
