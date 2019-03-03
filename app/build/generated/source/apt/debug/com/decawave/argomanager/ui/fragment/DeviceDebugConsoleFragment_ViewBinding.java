// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.Button;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.view.NodeStateView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class DeviceDebugConsoleFragment_ViewBinding implements Unbinder {
  private DeviceDebugConsoleFragment target;

  @UiThread
  public DeviceDebugConsoleFragment_ViewBinding(DeviceDebugConsoleFragment target, View source) {
    this.target = target;

    target.btnConnect = Utils.findRequiredViewAsType(source, R.id.connectButton, "field 'btnConnect'", Button.class);
    target.btnDisconnect = Utils.findRequiredViewAsType(source, R.id.disconnectButton, "field 'btnDisconnect'", Button.class);
    target.btnFetch = Utils.findRequiredViewAsType(source, R.id.fetchButton, "field 'btnFetch'", Button.class);
    target.btnObserve = Utils.findRequiredViewAsType(source, R.id.observePositionButton, "field 'btnObserve'", Button.class);
    target.observeFiller = Utils.findRequiredView(source, R.id.observePositionFiller, "field 'observeFiller'");
    target.nodeStateView = Utils.findRequiredViewAsType(source, R.id.nodeTypeView, "field 'nodeStateView'", NodeStateView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    DeviceDebugConsoleFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.btnConnect = null;
    target.btnDisconnect = null;
    target.btnFetch = null;
    target.btnObserve = null;
    target.observeFiller = null;
    target.nodeStateView = null;
  }
}
