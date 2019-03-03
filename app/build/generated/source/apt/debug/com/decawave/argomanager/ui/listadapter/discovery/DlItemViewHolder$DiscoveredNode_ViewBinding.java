// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter.discovery;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.view.SimpleProgressView;
import eu.davidea.flipview.FlipView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class DlItemViewHolder$DiscoveredNode_ViewBinding implements Unbinder {
  private DlItemViewHolder.DiscoveredNode target;

  @UiThread
  public DlItemViewHolder$DiscoveredNode_ViewBinding(DlItemViewHolder.DiscoveredNode target, View source) {
    this.target = target;

    target.progress = Utils.findRequiredViewAsType(source, R.id.progressView, "field 'progress'", SimpleProgressView.class);
    target.nodeName = Utils.findRequiredViewAsType(source, R.id.nodeName, "field 'nodeName'", TextView.class);
    target.tvNodeId = Utils.findRequiredViewAsType(source, R.id.nodeId, "field 'tvNodeId'", TextView.class);
    target.bleAddress = Utils.findRequiredViewAsType(source, R.id.nodeBleAddress, "field 'bleAddress'", TextView.class);
    target.flipView = Utils.findRequiredViewAsType(source, R.id.nodeTypeView, "field 'flipView'", FlipView.class);
    target.failIndicator = Utils.findRequiredViewAsType(source, R.id.failIndicator, "field 'failIndicator'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    DlItemViewHolder.DiscoveredNode target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.progress = null;
    target.nodeName = null;
    target.tvNodeId = null;
    target.bleAddress = null;
    target.flipView = null;
    target.failIndicator = null;
  }
}
