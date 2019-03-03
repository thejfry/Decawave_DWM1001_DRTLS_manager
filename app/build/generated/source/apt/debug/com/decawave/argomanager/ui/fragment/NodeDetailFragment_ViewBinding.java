// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class NodeDetailFragment_ViewBinding implements Unbinder {
  private NodeDetailFragment target;

  private View view2131624219;

  private View view2131624222;

  private View view2131624214;

  private View view2131624212;

  private View view2131624215;

  @UiThread
  public NodeDetailFragment_ViewBinding(final NodeDetailFragment target, View source) {
    this.target = target;

    View view;
    target.etPosX = Utils.findRequiredViewAsType(source, R.id.node_detail_position_x, "field 'etPosX'", EditText.class);
    target.etPosY = Utils.findRequiredViewAsType(source, R.id.node_detail_position_y, "field 'etPosY'", EditText.class);
    target.etPosZ = Utils.findRequiredViewAsType(source, R.id.node_detail_position_z, "field 'etPosZ'", EditText.class);
    target.etNodeLabel = Utils.findRequiredViewAsType(source, R.id.etNodeLabel, "field 'etNodeLabel'", EditText.class);
    target.tvNodeId = Utils.findRequiredViewAsType(source, R.id.tvNodeId, "field 'tvNodeId'", TextView.class);
    target.tvNodeBleAddress = Utils.findRequiredViewAsType(source, R.id.tvNodeBleAddress, "field 'tvNodeBleAddress'", TextView.class);
    target.tvNetworkName = Utils.findRequiredViewAsType(source, R.id.tvNetworkName, "field 'tvNetworkName'", TextView.class);
    target.updateRateLabel = Utils.findRequiredView(source, R.id.updateRateLabel, "field 'updateRateLabel'");
    view = Utils.findRequiredView(source, R.id.updateRateSelector, "field 'updateRateSelector' and method 'onUpdateRateClicked'");
    target.updateRateSelector = view;
    view2131624219 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onUpdateRateClicked();
      }
    });
    target.tvUpdateRate = Utils.findRequiredViewAsType(source, R.id.tvUpdateRate, "field 'tvUpdateRate'", TextView.class);
    target.arrowUpdateRate = Utils.findRequiredView(source, R.id.arrowUpdateRate, "field 'arrowUpdateRate'");
    target.stationaryUpdateRateLabel = Utils.findRequiredView(source, R.id.stationaryUpdateRateLabel, "field 'stationaryUpdateRateLabel'");
    view = Utils.findRequiredView(source, R.id.stationaryUpdateRateSelector, "field 'stationaryUpdateRateSelector' and method 'onStationaryUpdateRateClicked'");
    target.stationaryUpdateRateSelector = view;
    view2131624222 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onStationaryUpdateRateClicked();
      }
    });
    target.tvStationaryUpdateRate = Utils.findRequiredViewAsType(source, R.id.tvStationaryUpdateRate, "field 'tvStationaryUpdateRate'", TextView.class);
    target.arrowStationaryUpdateRate = Utils.findRequiredView(source, R.id.arrowStationaryUpdateRate, "field 'arrowStationaryUpdateRate'");
    target.tvNodeType = Utils.findRequiredViewAsType(source, R.id.tvNodeType, "field 'tvNodeType'", TextView.class);
    target.tvUwbMode = Utils.findRequiredViewAsType(source, R.id.tvUwbMode, "field 'tvUwbMode'", TextView.class);
    view = Utils.findRequiredView(source, R.id.networkNameSelector, "field 'networkViewGroup' and method 'onNetworkClicked'");
    target.networkViewGroup = view;
    view2131624214 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onNetworkClicked();
      }
    });
    view = Utils.findRequiredView(source, R.id.nodeTypeSelector, "field 'nodeTypeViewGroup' and method 'onNodeTypeClicked'");
    target.nodeTypeViewGroup = view;
    view2131624212 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onNodeTypeClicked();
      }
    });
    view = Utils.findRequiredView(source, R.id.uwbModeSelector, "field 'uwbModeViewGroup' and method 'onUwbModeClicked'");
    target.uwbModeViewGroup = view;
    view2131624215 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onUwbModeClicked();
      }
    });
    target.chboxInitiator = Utils.findRequiredViewAsType(source, R.id.chboxInitiator, "field 'chboxInitiator'", CheckBox.class);
    target.chboxFirmwareUpdate = Utils.findRequiredViewAsType(source, R.id.chboxFirmwareUpdate, "field 'chboxFirmwareUpdate'", CheckBox.class);
    target.chboxAccelerometer = Utils.findRequiredViewAsType(source, R.id.chboxAccelerometer, "field 'chboxAccelerometer'", CheckBox.class);
    target.chboxLedIndication = Utils.findRequiredViewAsType(source, R.id.chboxLedIndication, "field 'chboxLedIndication'", CheckBox.class);
    target.chboxResponsiveMode = Utils.findRequiredViewAsType(source, R.id.chboxResponsiveMode, "field 'chboxResponsiveMode'", CheckBox.class);
    target.chboxLocationEngine = Utils.findRequiredViewAsType(source, R.id.chboxLocationEngine, "field 'chboxLocationEngine'", CheckBox.class);
    target.progressFrame = Utils.findRequiredView(source, R.id.progressFrame, "field 'progressFrame'");
    target.contentFrame = Utils.findRequiredView(source, R.id.contentFrame, "field 'contentFrame'");
    target.tvPositionTitle = Utils.findRequiredViewAsType(source, R.id.tvPositionTitle, "field 'tvPositionTitle'", TextView.class);
    target.anchorSpecificViews = Utils.listOf(
        Utils.findRequiredView(source, R.id.chboxInitiator, "field 'anchorSpecificViews'"), 
        Utils.findRequiredView(source, R.id.tvPositionTitle, "field 'anchorSpecificViews'"), 
        Utils.findRequiredView(source, R.id.tvPositionContainer, "field 'anchorSpecificViews'"));
    target.tagSpecificViews = Utils.listOf(
        Utils.findRequiredView(source, R.id.updateRateContainer, "field 'tagSpecificViews'"), 
        Utils.findRequiredView(source, R.id.chboxAccelerometer, "field 'tagSpecificViews'"), 
        Utils.findRequiredView(source, R.id.chboxResponsiveMode, "field 'tagSpecificViews'"), 
        Utils.findRequiredView(source, R.id.chboxLocationEngine, "field 'tagSpecificViews'"));
  }

  @Override
  @CallSuper
  public void unbind() {
    NodeDetailFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.etPosX = null;
    target.etPosY = null;
    target.etPosZ = null;
    target.etNodeLabel = null;
    target.tvNodeId = null;
    target.tvNodeBleAddress = null;
    target.tvNetworkName = null;
    target.updateRateLabel = null;
    target.updateRateSelector = null;
    target.tvUpdateRate = null;
    target.arrowUpdateRate = null;
    target.stationaryUpdateRateLabel = null;
    target.stationaryUpdateRateSelector = null;
    target.tvStationaryUpdateRate = null;
    target.arrowStationaryUpdateRate = null;
    target.tvNodeType = null;
    target.tvUwbMode = null;
    target.networkViewGroup = null;
    target.nodeTypeViewGroup = null;
    target.uwbModeViewGroup = null;
    target.chboxInitiator = null;
    target.chboxFirmwareUpdate = null;
    target.chboxAccelerometer = null;
    target.chboxLedIndication = null;
    target.chboxResponsiveMode = null;
    target.chboxLocationEngine = null;
    target.progressFrame = null;
    target.contentFrame = null;
    target.tvPositionTitle = null;
    target.anchorSpecificViews = null;
    target.tagSpecificViews = null;

    view2131624219.setOnClickListener(null);
    view2131624219 = null;
    view2131624222.setOnClickListener(null);
    view2131624222 = null;
    view2131624214.setOnClickListener(null);
    view2131624214 = null;
    view2131624212.setOnClickListener(null);
    view2131624212 = null;
    view2131624215.setOnClickListener(null);
    view2131624215 = null;
  }
}
