// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class LogMessageHolder_ViewBinding implements Unbinder {
  private LogMessageHolder target;

  @UiThread
  public LogMessageHolder_ViewBinding(LogMessageHolder target, View source) {
    this.target = target;

    target.msgTime = Utils.findRequiredViewAsType(source, R.id.logEntryTime, "field 'msgTime'", TextView.class);
    target.msgText = Utils.findRequiredViewAsType(source, R.id.logEntryText, "field 'msgText'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    LogMessageHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.msgTime = null;
    target.msgText = null;
  }
}
