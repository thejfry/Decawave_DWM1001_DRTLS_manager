// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.view.SimpleProgressView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class AutoPositioningNodeListAdapter$AutoPosNodeListItemHolder_ViewBinding implements Unbinder {
  private AutoPositioningNodeListAdapter.AutoPosNodeListItemHolder target;

  @UiThread
  public AutoPositioningNodeListAdapter$AutoPosNodeListItemHolder_ViewBinding(AutoPositioningNodeListAdapter.AutoPosNodeListItemHolder target, View source) {
    this.target = target;

    target.btnDragHandle = Utils.findRequiredView(source, R.id.btnDragHandle, "field 'btnDragHandle'");
    target.nodeName = Utils.findRequiredViewAsType(source, R.id.nodeName, "field 'nodeName'", TextView.class);
    target.tvNodeBleAddress = Utils.findRequiredViewAsType(source, R.id.bleAddress, "field 'tvNodeBleAddress'", TextView.class);
    target.tvNodeState = Utils.findRequiredViewAsType(source, R.id.tvNodeState, "field 'tvNodeState'", TextView.class);
    target.cardContent = Utils.findRequiredView(source, R.id.cardContent, "field 'cardContent'");
    target.progressViewSeparator = Utils.findRequiredViewAsType(source, R.id.progressView, "field 'progressViewSeparator'", SimpleProgressView.class);
    target.tvPosition = Utils.findRequiredViewAsType(source, R.id.tvPosition, "field 'tvPosition'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    AutoPositioningNodeListAdapter.AutoPosNodeListItemHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.btnDragHandle = null;
    target.nodeName = null;
    target.tvNodeBleAddress = null;
    target.tvNodeState = null;
    target.cardContent = null;
    target.progressViewSeparator = null;
    target.tvPosition = null;
  }
}
