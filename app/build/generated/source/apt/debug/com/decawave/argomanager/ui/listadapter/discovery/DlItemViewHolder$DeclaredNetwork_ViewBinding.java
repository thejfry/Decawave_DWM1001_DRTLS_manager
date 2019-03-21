// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter.discovery;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class DlItemViewHolder$DeclaredNetwork_ViewBinding implements Unbinder {
  private DlItemViewHolder.DeclaredNetwork target;

  @UiThread
  public DlItemViewHolder$DeclaredNetwork_ViewBinding(DlItemViewHolder.DeclaredNetwork target, View source) {
    this.target = target;

    target.networkName = Utils.findRequiredViewAsType(source, R.id.networkId, "field 'networkName'", TextView.class);
    target.anchorNodes = Utils.findRequiredViewAsType(source, R.id.networkAnchors, "field 'anchorNodes'", TextView.class);
    target.tagNodes = Utils.findRequiredViewAsType(source, R.id.networkTags, "field 'tagNodes'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    DlItemViewHolder.DeclaredNetwork target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.networkName = null;
    target.anchorNodes = null;
    target.tagNodes = null;
  }
}
