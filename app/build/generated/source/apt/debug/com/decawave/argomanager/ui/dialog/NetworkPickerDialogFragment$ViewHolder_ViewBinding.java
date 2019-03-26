// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.dialog;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class NetworkPickerDialogFragment$ViewHolder_ViewBinding implements Unbinder {
  private NetworkPickerDialogFragment.ViewHolder target;

  @UiThread
  public NetworkPickerDialogFragment$ViewHolder_ViewBinding(NetworkPickerDialogFragment.ViewHolder target, View source) {
    this.target = target;

    target.rb = Utils.findRequiredViewAsType(source, R.id.radio, "field 'rb'", RadioButton.class);
    target.tvNetworkName = Utils.findOptionalViewAsType(source, R.id.tvNetworkName, "field 'tvNetworkName'", TextView.class);
    target.etNewNetworkName = Utils.findOptionalViewAsType(source, R.id.etNewNetworkName, "field 'etNewNetworkName'", EditText.class);
    target.focusableView = source.findViewById(R.id.focusableView);
  }

  @Override
  @CallSuper
  public void unbind() {
    NetworkPickerDialogFragment.ViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.rb = null;
    target.tvNetworkName = null;
    target.etNewNetworkName = null;
    target.focusableView = null;
  }
}
