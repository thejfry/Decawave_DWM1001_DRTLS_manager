// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class DeviceErrorFragment_ViewBinding implements Unbinder {
  private DeviceErrorFragment target;

  @UiThread
  public DeviceErrorFragment_ViewBinding(DeviceErrorFragment target, View source) {
    this.target = target;

    target.elementList = Utils.findRequiredViewAsType(source, R.id.elementList, "field 'elementList'", RecyclerView.class);
    target.noErrors = Utils.findRequiredViewAsType(source, R.id.noErrors, "field 'noErrors'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    DeviceErrorFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.elementList = null;
    target.noErrors = null;
  }
}
