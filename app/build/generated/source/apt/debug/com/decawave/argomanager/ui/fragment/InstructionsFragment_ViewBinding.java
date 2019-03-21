// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.webkit.WebView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class InstructionsFragment_ViewBinding implements Unbinder {
  private InstructionsFragment target;

  @UiThread
  public InstructionsFragment_ViewBinding(InstructionsFragment target, View source) {
    this.target = target;

    target.webView = Utils.findRequiredViewAsType(source, R.id.htmlInstructions, "field 'webView'", WebView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    InstructionsFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.webView = null;
  }
}
