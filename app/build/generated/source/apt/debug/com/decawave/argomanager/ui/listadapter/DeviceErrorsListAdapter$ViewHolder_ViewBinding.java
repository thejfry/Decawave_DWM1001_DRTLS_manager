// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class DeviceErrorsListAdapter$ViewHolder_ViewBinding implements Unbinder {
  private DeviceErrorsListAdapter.ViewHolder target;

  @UiThread
  public DeviceErrorsListAdapter$ViewHolder_ViewBinding(DeviceErrorsListAdapter.ViewHolder target, View source) {
    this.target = target;

    target.nodeSeparator = Utils.findRequiredView(source, R.id.bottomSeparator, "field 'nodeSeparator'");
    target.cardTop = Utils.findRequiredView(source, R.id.cardTop, "field 'cardTop'");
    target.lastNodeSeparator = Utils.findRequiredView(source, R.id.lastNodeBottomSeparator, "field 'lastNodeSeparator'");
  }

  @Override
  @CallSuper
  public void unbind() {
    DeviceErrorsListAdapter.ViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.nodeSeparator = null;
    target.cardTop = null;
    target.lastNodeSeparator = null;
  }
}
