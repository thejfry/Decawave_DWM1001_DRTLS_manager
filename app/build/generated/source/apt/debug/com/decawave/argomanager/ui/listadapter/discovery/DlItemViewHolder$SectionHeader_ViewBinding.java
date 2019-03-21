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

public class DlItemViewHolder$SectionHeader_ViewBinding implements Unbinder {
  private DlItemViewHolder.SectionHeader target;

  @UiThread
  public DlItemViewHolder$SectionHeader_ViewBinding(DlItemViewHolder.SectionHeader target, View source) {
    this.target = target;

    target.tvTitle = Utils.findRequiredViewAsType(source, R.id.tvTitle, "field 'tvTitle'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    DlItemViewHolder.SectionHeader target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.tvTitle = null;
  }
}
