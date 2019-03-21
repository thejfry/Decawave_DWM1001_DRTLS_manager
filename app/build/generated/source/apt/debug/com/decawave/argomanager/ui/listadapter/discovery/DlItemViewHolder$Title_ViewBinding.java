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

public class DlItemViewHolder$Title_ViewBinding implements Unbinder {
  private DlItemViewHolder.Title target;

  @UiThread
  public DlItemViewHolder$Title_ViewBinding(DlItemViewHolder.Title target, View source) {
    this.target = target;

    target.discoveryInfo = Utils.findRequiredViewAsType(source, R.id.discoveryInfo, "field 'discoveryInfo'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    DlItemViewHolder.Title target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.discoveryInfo = null;
  }
}
