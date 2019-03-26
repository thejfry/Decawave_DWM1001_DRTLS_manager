// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.view.NodeStateView;
import com.decawave.argomanager.ui.view.SimpleProgressView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class FirmwareUpdateNodeListAdapter$FwNodeListItemHolder_ViewBinding implements Unbinder {
  private FirmwareUpdateNodeListAdapter.FwNodeListItemHolder target;

  @UiThread
  public FirmwareUpdateNodeListAdapter$FwNodeListItemHolder_ViewBinding(FirmwareUpdateNodeListAdapter.FwNodeListItemHolder target, View source) {
    this.target = target;

    target.nodeCheckbox = Utils.findRequiredViewAsType(source, R.id.nodeCheckbox, "field 'nodeCheckbox'", CheckBox.class);
    target.nodeTypeView = Utils.findRequiredViewAsType(source, R.id.nodeType, "field 'nodeTypeView'", NodeStateView.class);
    target.nodeName = Utils.findRequiredViewAsType(source, R.id.nodeName, "field 'nodeName'", TextView.class);
    target.tvNodeBleAddress = Utils.findRequiredViewAsType(source, R.id.bleAddress, "field 'tvNodeBleAddress'", TextView.class);
    target.tvFirmware1VersionChecksum = Utils.findRequiredViewAsType(source, R.id.tvFirmware1VersionAndChecksum, "field 'tvFirmware1VersionChecksum'", TextView.class);
    target.tvFirmware2VersionChecksum = Utils.findRequiredViewAsType(source, R.id.tvFirmware2VersionAndChecksum, "field 'tvFirmware2VersionChecksum'", TextView.class);
    target.uploadProgressContainer = Utils.findRequiredView(source, R.id.uploadProgress, "field 'uploadProgressContainer'");
    target.tvUploadFwType = Utils.findRequiredViewAsType(source, R.id.uploadFwType, "field 'tvUploadFwType'", TextView.class);
    target.tvUploadPercentage = Utils.findRequiredViewAsType(source, R.id.uploadPercentage, "field 'tvUploadPercentage'", TextView.class);
    target.cardContent = Utils.findRequiredView(source, R.id.cardContent, "field 'cardContent'");
    target.cardTop = Utils.findRequiredView(source, R.id.cardTop, "field 'cardTop'");
    target.progressViewSeparator = Utils.findRequiredViewAsType(source, R.id.progressView, "field 'progressViewSeparator'", SimpleProgressView.class);
    target.lastNodeSeparator = Utils.findRequiredView(source, R.id.lastNodeBottomSeparator, "field 'lastNodeSeparator'");
  }

  @Override
  @CallSuper
  public void unbind() {
    FirmwareUpdateNodeListAdapter.FwNodeListItemHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.nodeCheckbox = null;
    target.nodeTypeView = null;
    target.nodeName = null;
    target.tvNodeBleAddress = null;
    target.tvFirmware1VersionChecksum = null;
    target.tvFirmware2VersionChecksum = null;
    target.uploadProgressContainer = null;
    target.tvUploadFwType = null;
    target.tvUploadPercentage = null;
    target.cardContent = null;
    target.cardTop = null;
    target.progressViewSeparator = null;
    target.lastNodeSeparator = null;
  }
}
