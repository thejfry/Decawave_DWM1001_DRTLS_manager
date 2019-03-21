// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.dialog;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class UwbModePickerDialogFragment$ViewHolder_ViewBinding implements Unbinder {
  private UwbModePickerDialogFragment.ViewHolder target;

  @UiThread
  public UwbModePickerDialogFragment$ViewHolder_ViewBinding(UwbModePickerDialogFragment.ViewHolder target, View source) {
    this.target = target;

    target.rb = Utils.findRequiredViewAsType(source, R.id.radio, "field 'rb'", RadioButton.class);
    target.tvNodeType = Utils.findRequiredViewAsType(source, R.id.tvUpdateRate, "field 'tvNodeType'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    UwbModePickerDialogFragment.ViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.rb = null;
    target.tvNodeType = null;
  }
}
