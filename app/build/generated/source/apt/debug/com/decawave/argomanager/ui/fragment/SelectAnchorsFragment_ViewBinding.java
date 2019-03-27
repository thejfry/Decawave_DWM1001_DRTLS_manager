// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.Button;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class SelectAnchorsFragment_ViewBinding implements Unbinder {
  private SelectAnchorsFragment target;

  @UiThread
  public SelectAnchorsFragment_ViewBinding(SelectAnchorsFragment target, View source) {
    this.target = target;

    target.btnSubmitAnchor = Utils.findRequiredViewAsType(source, R.id.btn_submit_anchor, "field 'btnSubmitAnchor'", Button.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    SelectAnchorsFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.btnSubmitAnchor = null;
  }
}
