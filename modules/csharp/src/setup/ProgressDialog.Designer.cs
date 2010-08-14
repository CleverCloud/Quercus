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

namespace Caucho
{
  partial class ProgressDialog
  {
    /// <summary>
    /// Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;

    /// <summary>
    /// Clean up any resources being used.
    /// </summary>
    /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
    protected override void Dispose(bool disposing)
    {
      if (disposing && (components != null)) {
        components.Dispose();
      }
      base.Dispose(disposing);
    }

    #region Windows Form Designer generated code

    /// <summary>
    /// Required method for Designer support - do not modify
    /// the contents of this method with the code editor.
    /// </summary>
    private void InitializeComponent()
    {
      this.components = new System.ComponentModel.Container();
      this._timer = new System.Windows.Forms.Timer(this.components);
      this._layout = new System.Windows.Forms.TableLayoutPanel();
      this._progressBar = new System.Windows.Forms.ProgressBar();
      this._message = new System.Windows.Forms.Label();
      this._closeButton = new System.Windows.Forms.Button();
      this._statusText = new System.Windows.Forms.RichTextBox();
      this._errorProvider = new System.Windows.Forms.ErrorProvider(this.components);
      this._layout.SuspendLayout();
      ((System.ComponentModel.ISupportInitialize)(this._errorProvider)).BeginInit();
      this.SuspendLayout();
      // 
      // _timer
      // 
      this._timer.Tick += new System.EventHandler(this.TimerTick);
      // 
      // _layout
      // 
      this._layout.AutoSize = true;
      this._layout.AutoSizeMode = System.Windows.Forms.AutoSizeMode.GrowAndShrink;
      this._layout.ColumnCount = 3;
      this._layout.ColumnStyles.Add(new System.Windows.Forms.ColumnStyle());
      this._layout.ColumnStyles.Add(new System.Windows.Forms.ColumnStyle());
      this._layout.ColumnStyles.Add(new System.Windows.Forms.ColumnStyle());
      this._layout.Controls.Add(this._progressBar, 1, 1);
      this._layout.Controls.Add(this._message, 0, 1);
      this._layout.Controls.Add(this._closeButton, 2, 1);
      this._layout.Controls.Add(this._statusText, 1, 0);
      this._layout.Location = new System.Drawing.Point(-10, 0);
      this._layout.Name = "_layout";
      this._layout.Padding = new System.Windows.Forms.Padding(10, 20, 10, 0);
      this._layout.RowCount = 2;
      this._layout.RowStyles.Add(new System.Windows.Forms.RowStyle());
      this._layout.RowStyles.Add(new System.Windows.Forms.RowStyle());
      this._layout.Size = new System.Drawing.Size(557, 134);
      this._layout.TabIndex = 0;
      // 
      // _progressBar
      // 
      this._progressBar.Dock = System.Windows.Forms.DockStyle.Fill;
      this._errorProvider.SetIconAlignment(this._progressBar, System.Windows.Forms.ErrorIconAlignment.MiddleLeft);
      this._progressBar.Location = new System.Drawing.Point(70, 112);
      this._progressBar.Margin = new System.Windows.Forms.Padding(7);
      this._progressBar.Maximum = 1000;
      this._progressBar.Name = "_progressBar";
      this._progressBar.Size = new System.Drawing.Size(372, 15);
      this._progressBar.TabIndex = 0;
      // 
      // _message
      // 
      this._message.Anchor = System.Windows.Forms.AnchorStyles.None;
      this._message.AutoSize = true;
      this._message.Location = new System.Drawing.Point(13, 113);
      this._message.Margin = new System.Windows.Forms.Padding(3);
      this._message.Name = "_message";
      this._message.Size = new System.Drawing.Size(47, 13);
      this._message.TabIndex = 2;
      this._message.Text = "progress";
      this._message.TextAlign = System.Drawing.ContentAlignment.MiddleCenter;
      // 
      // _closeButton
      // 
      this._closeButton.DialogResult = System.Windows.Forms.DialogResult.Cancel;
      this._closeButton.Dock = System.Windows.Forms.DockStyle.Fill;
      this._closeButton.Location = new System.Drawing.Point(469, 108);
      this._closeButton.Margin = new System.Windows.Forms.Padding(20, 3, 3, 3);
      this._closeButton.Name = "_closeButton";
      this._closeButton.Size = new System.Drawing.Size(75, 23);
      this._closeButton.TabIndex = 3;
      this._closeButton.Text = "&Close";
      this._closeButton.UseVisualStyleBackColor = true;
      this._closeButton.Click += new System.EventHandler(this.CloseButtonClick);
      // 
      // _statusText
      // 
      this._statusText.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom)
                  | System.Windows.Forms.AnchorStyles.Left)
                  | System.Windows.Forms.AnchorStyles.Right)));
      this._errorProvider.SetIconAlignment(this._statusText, System.Windows.Forms.ErrorIconAlignment.TopLeft);
      this._errorProvider.SetIconPadding(this._statusText, 5);
      this._statusText.Location = new System.Drawing.Point(70, 27);
      this._statusText.Margin = new System.Windows.Forms.Padding(7);
      this._statusText.Name = "_statusText";
      this._statusText.Size = new System.Drawing.Size(372, 71);
      this._statusText.TabIndex = 5;
      this._statusText.Text = "";
      this._statusText.KeyDown += new System.Windows.Forms.KeyEventHandler(this.StatusTextKeyDown);
      this._statusText.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.StatusTextKeyPress);
      // 
      // _errorProvider
      // 
      this._errorProvider.ContainerControl = this;
      // 
      // ProgressDialog
      // 
      this.AcceptButton = this._closeButton;
      this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
      this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
      this.AutoSize = true;
      this.AutoSizeMode = System.Windows.Forms.AutoSizeMode.GrowAndShrink;
      this.CancelButton = this._closeButton;
      this.ClientSize = new System.Drawing.Size(571, 303);
      this.Controls.Add(this._layout);
      this.Name = "ProgressDialog";
      this.Padding = new System.Windows.Forms.Padding(10);
      this.StartPosition = System.Windows.Forms.FormStartPosition.CenterParent;
      this.FormClosed += new System.Windows.Forms.FormClosedEventHandler(this.ProgressDialogFormClosed);
      this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.ProgressDialogClosing);
      this._layout.ResumeLayout(false);
      this._layout.PerformLayout();
      ((System.ComponentModel.ISupportInitialize)(this._errorProvider)).EndInit();
      this.ResumeLayout(false);
      this.PerformLayout();

    }

    #endregion

    private System.Windows.Forms.Timer _timer;
    private System.Windows.Forms.TableLayoutPanel _layout;
    private System.Windows.Forms.Label _message;
    private System.Windows.Forms.ProgressBar _progressBar;
    private System.Windows.Forms.Button _closeButton;
    private System.Windows.Forms.ErrorProvider _errorProvider;
    private System.Windows.Forms.RichTextBox _statusText;
  }
}