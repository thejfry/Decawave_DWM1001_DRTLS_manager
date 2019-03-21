// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.view.GridView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class ApPreviewFragment_ViewBinding implements Unbinder {
  private ApPreviewFragment target;

  @UiThread
  public ApPreviewFragment_ViewBinding(ApPreviewFragment target, View source) {
    this.target = target;

    target.grid = Utils.findRequiredViewAsType(source, R.id.gridView, "field 'grid'", GridView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    ApPreviewFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.grid = null;
  }
}
