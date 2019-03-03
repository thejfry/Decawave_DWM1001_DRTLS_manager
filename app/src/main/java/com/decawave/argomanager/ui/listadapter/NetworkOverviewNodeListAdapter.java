/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.TagNode;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.argoapi.ble.BleConnectionApi;
import com.decawave.argomanager.argoapi.ble.BleConstants;
import com.decawave.argomanager.ble.signal.SignalStrength;
import com.decawave.argomanager.ble.signal.SignalStrengthInterpreter;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.LocationDataObserver;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.components.struct.NodeWarning;
import com.decawave.argomanager.components.struct.PresenceStatus;
import com.decawave.argomanager.components.struct.TrackMode;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.ApplicationMode;
import com.decawave.argomanager.ui.IhMainActivityProvider;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.fragment.DeviceDebugConsoleFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;
import com.decawave.argomanager.ui.fragment.GridFragment;
import com.decawave.argomanager.ui.fragment.NodeDetailFragment;
import com.decawave.argomanager.ui.view.NodeStateView;
import com.decawave.argomanager.ui.view.SignalStrengthView;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.NetworkNodePropertyDecorator;
import com.decawave.argomanager.util.ToastUtil;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.common.Pair;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubContract;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 *
 */
public class NetworkOverviewNodeListAdapter extends RecyclerView.Adapter<NetworkOverviewNodeListAdapter.ViewHolder> {
    private static final ComponentLog log = new ComponentLog(NetworkOverviewNodeListAdapter.class);
    private static final String BK_EXPANDED_ITEMS = "EXPANDED_ITEMS";
    private static final int MAX_TRACKED_TAGS = BleConstants.LOCATION_DATA_OBSERVER_MAX_TRACKED_TAGS_COUNT;
    private static final boolean DEBUG = Constants.DEBUG;

    private static final List<NetworkNodeProperty> BASIC_PROPERTIES_ANCHOR;
    private static final List<NetworkNodeProperty> ADVANCED_PROPERTIES_ANCHOR;
    private static final List<NetworkNodeProperty> BASIC_PROPERTIES_TAG;
    private static final List<NetworkNodeProperty> ADVANCED_PROPERTIES_TAG;

    static {
        List<NetworkNodeProperty> commonBasicProperties = Lists.newArrayList(
                NetworkNodeProperty.ID,
                NetworkNodeProperty.BLE_ADDRESS,
                NetworkNodeProperty.UWB_MODE,
                NetworkNodeProperty.FIRMWARE_UPDATE_ENABLE,
                NetworkNodeProperty.LED_INDICATION_ENABLE
        );

        List<NetworkNodeProperty> anchorBasicProperties = Lists.newArrayList(
                NetworkNodeProperty.ANCHOR_INITIATOR,
                NetworkNodeProperty.ANCHOR_POSITION,
                NetworkNodeProperty.ANCHOR_SEAT,
                NetworkNodeProperty.OPERATING_FIRMWARE,
                NetworkNodeProperty.LAST_SEEN
        );

        List<NetworkNodeProperty> tagBasicProperties = Lists.newArrayList(
                NetworkNodeProperty.TAG_LOW_POWER_MODE_ENABLE,
                NetworkNodeProperty.TAG_LOCATION_ENGINE_ENABLE,
                NetworkNodeProperty.TAG_ACCELEROMETER_ENABLE,
                NetworkNodeProperty.TAG_UPDATE_RATE,
                NetworkNodeProperty.TAG_STATIONARY_UPDATE_RATE,
                NetworkNodeProperty.OPERATING_FIRMWARE,
                NetworkNodeProperty.LAST_SEEN
                );

        List<NetworkNodeProperty> commonAdvancedProperties = Lists.newArrayList(
                NetworkNodeProperty.HW_VERSION,
                NetworkNodeProperty.FW1_VERSION,
                NetworkNodeProperty.FW2_VERSION,
                NetworkNodeProperty.FW1_CHECKSUM,
                NetworkNodeProperty.FW2_CHECKSUM
        );

        List<NetworkNodeProperty> anchorAdvancedProperties = Lists.newArrayList(
                // anchor-specific
                NetworkNodeProperty.ANCHOR_CLUSTER_MAP,
                NetworkNodeProperty.ANCHOR_CLUSTER_NEIGHBOUR_MAP,
                NetworkNodeProperty.ANCHOR_MAC_STATS
        );
        // TAG
        BASIC_PROPERTIES_TAG = new ArrayList<>(commonBasicProperties);
        BASIC_PROPERTIES_TAG.addAll(tagBasicProperties);

        ADVANCED_PROPERTIES_TAG = new ArrayList<>(BASIC_PROPERTIES_TAG);
        ADVANCED_PROPERTIES_TAG.addAll(commonAdvancedProperties);

        // ANCHOR
        BASIC_PROPERTIES_ANCHOR = new ArrayList<>(commonBasicProperties);
        BASIC_PROPERTIES_ANCHOR.addAll(anchorBasicProperties);

        ADVANCED_PROPERTIES_ANCHOR = new ArrayList<>(BASIC_PROPERTIES_ANCHOR);
        ADVANCED_PROPERTIES_ANCHOR.addAll(commonAdvancedProperties);
        ADVANCED_PROPERTIES_ANCHOR.addAll(anchorAdvancedProperties);

    }

    //
    private final MainActivity mainActivity;
    private final AppPreferenceAccessor appPreferenceAccessor;
    private final AndroidPermissionHelper permissionHelper;
    private final SignalStrengthInterpreter signalStrengthInterpreter;
    private final NetworkNodeManager networkNodeManager;
    private final BlePresenceApi presenceApi;
    private final NetworkNodePropertyDecorator propertyDecorator;
    private final BleConnectionApi bleConnectionApi;
    private final LocationDataObserver locationDataObserver;
    private final Supplier<ApplicationMode> applicationModeSupplier;
    private Set<Long> expandedNodeIds = new HashSet<>();
    private List<NetworkNodeEnhanced> nodes = new ArrayList<>();
    private boolean showingNewNodesDiscovered;

    // item types
    private static final int ITEM_TYPE_SUMMARY = 0;
    private static final int ITEM_TYPE_NEW_NODES_DISCOVERED = 1;
    private static final int ITEM_TYPE_NODE = 2;

    public NetworkOverviewNodeListAdapter(Collection<NetworkNodeEnhanced> networkNodes,
                                          AndroidPermissionHelper permissionHelper,
                                          NetworkNodeManager networkNodeManager,
                                          BleConnectionApi bleConnectionApi,
                                          LocationDataObserver locationDataObserver,
                                          AppPreferenceAccessor appPreferenceAccessor,
                                          boolean showNewNodesDiscovered,
                                          Supplier<ApplicationMode> applicationModeSupplier,
                                          MainActivity mainActivity,
                                          SignalStrengthInterpreter signalStrengthInterpreter,
                                          BlePresenceApi presenceApi,
                                          NetworkNodePropertyDecorator propertyDecorator) {
        this.permissionHelper = permissionHelper;
        this.bleConnectionApi = bleConnectionApi;
        this.locationDataObserver = locationDataObserver;
        this.appPreferenceAccessor = appPreferenceAccessor;
        this.mainActivity = mainActivity;
        this.networkNodeManager = networkNodeManager;
        this.presenceApi = presenceApi;
        this.showingNewNodesDiscovered = showNewNodesDiscovered;
        this.applicationModeSupplier = applicationModeSupplier;
        this.nodes = getSortedNetworkNodes(networkNodes);
        this.signalStrengthInterpreter = signalStrengthInterpreter;
        this.propertyDecorator = propertyDecorator;
    }

    @NonNull
    private static List<NetworkNodeEnhanced> getSortedNetworkNodes(Collection<NetworkNodeEnhanced> nodes) {
        LinkedList<NetworkNodeEnhanced> lst = new LinkedList<>(nodes);
        // sort nodes by id
        Collections.sort(lst, Constants.NETWORK_NODE_COMPARATOR);
        return lst;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mainActivity);
        switch (viewType) {
            case ITEM_TYPE_SUMMARY:
                View view = inflater.inflate(R.layout.li_network_summary, parent, false);
                // wrap with view holder
                return new NetworkSummaryItemHolder(view);
            case ITEM_TYPE_NODE:
                view = inflater.inflate(R.layout.li_network_node, parent, false);
                // wrap with view holder
                return new NetworkNodeListItemHolder(view);
            case ITEM_TYPE_NEW_NODES_DISCOVERED:
                view = inflater.inflate(R.layout.li_network_new_nodes_discovered, parent, false);
                // wrap with view holder
                return new NewNodesDiscoveredItemHolder(view);
            default:
                throw new IllegalStateException("unsupported item view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof NetworkSummaryItemHolder) {
            NetworkModel network = networkNodeManager.getActiveNetwork();
            // fixing bug #179 {begin}
            if (network == null) {
                // there is not active network, we should not get called
                // Android, however, calls us
                // the screen should be soon switched to a different view anyway
                return;
            }
            // fixing bug #179 {end}
            ((NetworkSummaryItemHolder) holder).bind(network);
        } else if (holder instanceof NetworkNodeListItemHolder) {
            int p = position - getFirstNetworkNodeIndex();
            NetworkNodeListItemHolder h = (NetworkNodeListItemHolder) holder;
            h.bind(nodes.get(p), p == 0, p == nodes.size() - 1);
        } // else: no need to bind
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_TYPE_SUMMARY;
        } else if (position == 1 && showingNewNodesDiscovered) {
            return ITEM_TYPE_NEW_NODES_DISCOVERED;
        } else {
            return ITEM_TYPE_NODE;
        }
    }

    @Override
    public int getItemCount() {
        return nodes.size() + getFirstNetworkNodeIndex();
    }

    public void removeNode(long id) {
        // find position where the node has been removed
        Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> nodeIt = findNodeById(id);
        if (DEBUG) {
            log.d("removeNode: " + "id = [" + Util.shortenNodeId(id, false) + "], node = " + (nodeIt != null ? nodeIt.first : null));
        }
        if (nodeIt != null) {
            ListIterator<NetworkNodeEnhanced> it = nodeIt.second;
            notifyItemChanged(0);
            int idx = getViewIndexForNodeIterator(it);
            int firstNetworkNodeIndex = getFirstNetworkNodeIndex();
            int lastNetworkNodeIndex = getLastNetworkNodeIndex();
            it.remove();
            notifyItemRemoved(idx);
            if (idx == firstNetworkNodeIndex) {
                notifyItemChanged(idx);
            } else if (idx == lastNetworkNodeIndex) {
                notifyItemChanged(idx - 1);
            }
        }
    }

    public void addNode(NetworkNodeEnhanced node, boolean anyTransientNodeDiscovered) {
        if (DEBUG) {
            log.d("addNode: " + "node = [" + node + "], anyTransientNodeDiscovered = [" + anyTransientNodeDiscovered + "]");
        }
        // find position where to add the node
        ListIterator<NetworkNodeEnhanced> it = nodes.listIterator();
        int i = 0;
        while (it.hasNext()) {
            NetworkNodeEnhanced n = it.next();
            if (Constants.NETWORK_NODE_COMPARATOR.compare(n, node) > 0) {
                // stop here, move back
                it.previous();
                break;
            }
            i++;
        }
        //
        int idx = getViewIndexForNodeIterator(it);
        int firstNetworkNodeIndex = getFirstNetworkNodeIndex();
        int lastNetworkNodeIndex = getLastNetworkNodeIndex();
        // insert
        it.add(node);
        // notify the adapter
        notifyItemInserted(i + firstNetworkNodeIndex);
        // summary changed as well
        notifyItemChanged(0);
        //
        if (i == 0 && it.hasNext()) {
            // the item was inserted before the first (now second) element
            // such element needs to be redrawn as well
            notifyItemChanged(firstNetworkNodeIndex + 1);
        } else if (idx == lastNetworkNodeIndex && nodes.size() > 1) {
            // the item was inserted after the last element
            // such element needs to be redrawn as well
            notifyItemChanged(firstNetworkNodeIndex + i - 1);
        }
        // check if the stateful discovery still contains transient node
        setShowNewNodesDiscovered(anyTransientNodeDiscovered);
    }

    public void updateNodeInCollection(NetworkNodeEnhanced node) {
        if (DEBUG) {
            log.d("updateNodeInCollection: " + "node = [" + node + "]");
        }
        // find the position of the affected node
        Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> nodeIt = findNodeById(node.getId());
        if (nodeIt != null) {
            nodeIt.second.set(node);
        }
        // do not forget to call corresponding update routine!
    }

    private int getViewIndexForNodeIterator(@NotNull ListIterator<NetworkNodeEnhanced> listIterator) {
        return getFirstNetworkNodeIndex() + listIterator.previousIndex();
    }

    private int getFirstNetworkNodeIndex() {
        return 1 + (showingNewNodesDiscovered ? 1 : 0);
    }

    private int getLastNetworkNodeIndex() {
        return 1 + (showingNewNodesDiscovered ? 1 : 0) + nodes.size() - 1;
    }

    /**
     * Unfortunately we are not able to return iterator which keep the next() returned value.
     * Actually we are, but this wouldn't be very nice.
     *
     * @param id identifies node
     */
    private Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> findNodeById(long id) {
        ListIterator<NetworkNodeEnhanced> it = nodes.listIterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            NetworkNodeEnhanced n = it.next();
            if (n.getId() == id) {
                // stop here
                return new Pair<>(n, it);
            }
        }
        return null;
    }

    private Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> findNodeByBleAddress(String bleAddress) {
        ListIterator<NetworkNodeEnhanced> it = nodes.listIterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            NetworkNodeEnhanced n = it.next();
            if (n.getBleAddress().equals(bleAddress)) {
                // stop here
                return new Pair<>(n, it);
            }
        }
        return null;
    }

    public void setShowNewNodesDiscovered(boolean show) {
        if (show != showingNewNodesDiscovered) {
            // process the change
            showingNewNodesDiscovered = show;
            if (show) {
                notifyItemInserted(1);
            } else {
                notifyItemRemoved(1);
            }
        }
    }

    public Integer getNodePosition(String nodeBleAddress) {
        Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> nodeIt = findNodeByBleAddress(nodeBleAddress);
        if (nodeIt != null) {
            return getViewIndexForNodeIterator(nodeIt.second);
        }
        return null;
    }

    public void makeSignalIndicatorsObsolete() {
        notifyDataSetChanged();
            // it would be better to lookup the visible views and modify them directly
    }

    public Long getNodeIdBehindViewHolder(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof NetworkNodeListItemHolder) {
            return ((NetworkNodeListItemHolder) viewHolder).nodeId;
        } // else:
        return null;
    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private long lastToastShowSysTime = 0;

    public class NetworkNodeListItemHolder extends ViewHolder {
        // references to views
        @BindView(R.id.nodeName)
        TextView nodeName;
        @BindView(R.id.bleAddress)
        TextView tvNodeBleAddress;
        @BindView(R.id.cardTop)
        View cardTop;
        @BindView(R.id.cardTopSeparator)
        View cardTopSeparator;
        @BindView(R.id.bottomSeparator)
        View nodeSeparator;
        @BindView(R.id.lastNodeBottomSeparator)
        View lastNodeSeparator;
        @BindView(R.id.nodeType)
        NodeStateView nodeStateView;
        @BindView(R.id.signalStrength)
        SignalStrengthView signalStrengthView;
        @BindView(R.id.warningIcon)
        TextView warningIcon;
        @BindView(R.id.trackModeIcon)
        ImageView trackModeIcon;
        @BindView(R.id.locateIcon)
        ImageView locateIcon;
        @BindView(R.id.editIcon)
        ImageView editIcon;
        // this table is to be filled in onBind()
        @BindView(R.id.detailsTable)
        TableLayout detailsTable;


        // identification of the network node
        public long nodeId;
        public String nodeBle;
        boolean passive;
        NodeType nodeType;
        Position position;


        NetworkNodeListItemHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this,itemView);
            itemView.findViewById(R.id.cardContent).setOnClickListener(view -> onNetworkNodeSelected(nodeId));
            trackModeIcon.setOnClickListener(view -> {
                NetworkNodeEnhanced nne = networkNodeManager.getNode(nodeId);
                TrackMode trackMode = nne.getTrackMode();
                TrackMode newTrackMode = null;
                switch (trackMode) {
                    case TRACKED_POSITION:
                        //noinspection ConstantConditions
                        short networkId = networkNodeManager.getActiveNetwork().getNetworkId();
                        if (networkNodeManager.getNumberOfDirectlyTrackedTags(networkId) >= MAX_TRACKED_TAGS) {
                            newTrackMode = TrackMode.NOT_TRACKED;
                            long now = SystemClock.uptimeMillis();
                            if (lastToastShowSysTime + 10000 < now) {
                                ToastUtil.showToast(daApp.getString(R.string.max_number_of_tracked_tags_reached, MAX_TRACKED_TAGS), Toast.LENGTH_LONG);
                                lastToastShowSysTime = now;
                            }
                        } else {
                            newTrackMode = TrackMode.TRACKED_POSITION_AND_RANGING;
                        }
                        break;
                    case TRACKED_POSITION_AND_RANGING:
                        newTrackMode = TrackMode.NOT_TRACKED;
                        break;
                    case NOT_TRACKED:
                        newTrackMode = TrackMode.TRACKED_POSITION;
                        break;
                }
                // the track mode has changed
                networkNodeManager.setNodeTrackMode(nodeId, newTrackMode);
                trackModeIcon.setImageResource(getTrackModeResource(newTrackMode));
                position = nne.asPlainNode().extractPositionDirect();
                updateLocateIcon(nodeType, newTrackMode, nne.asPlainNode().getUwbMode());
            });
            locateIcon.setOnClickListener(view -> {
                locationDataObserver.setPreferentiallyObservedNode(nodeBle);
                Bundle args = GridFragment.getArgsForPosition(position);
                mainActivity.showFragment(FragmentType.GRID, args);
            });
            editIcon.setOnClickListener(view ->
                    mainActivity.showFragment(FragmentType.NODE_DETAILS, NodeDetailFragment.getArgumentsForActiveNetworkNode(nodeId)));
        }

        private void log(String msg) {
            log.d("NetworkNodeListItemHolder@" + System.identityHashCode(this) + ": nodeBle = " + nodeBle + "; " + msg);
        }

        void bind(NetworkNodeEnhanced networkNode, boolean firstNode, boolean lastNode) {
            if (DEBUG) {
                log("bind: networkNode = [" + networkNode + "], firstNode = [" + firstNode + "], lastNode = [" + lastNode + "]");
            }
            // bind the network node first
            bind(networkNode, false);
            // and top/bottom margins
            cardTop.setVisibility(firstNode ? View.VISIBLE : View.GONE);
            cardTopSeparator.setVisibility(firstNode ? View.GONE : View.VISIBLE);
            if (lastNode) {
                lastNodeSeparator.setVisibility(View.VISIBLE);
                nodeSeparator.setVisibility(View.GONE);
            } else {
                lastNodeSeparator.setVisibility(View.GONE);
                nodeSeparator.setVisibility(View.VISIBLE);
            }
        }

        public void bind(NetworkNodeEnhanced networkNode, boolean continuous) {
            if (DEBUG) {
                log("bind: networkNode = [" + networkNode + "], continuous = [" + continuous + "]");
            }
            // fill simple variables first
            nodeId = networkNode.getId();
            NetworkNode nn = networkNode.asPlainNode();
            nodeBle = nn.getBleAddress();
            nodeType = nn.getType();
            position = nn.extractPositionDirect();
            passive = nn.getUwbMode() == UwbMode.PASSIVE;
            TrackMode trackMode = networkNode.getTrackMode();
            if (nn.isAnchor()) {
                trackModeIcon.setVisibility(View.GONE);
            } else {
                trackModeIcon.setVisibility(View.VISIBLE);
                trackModeIcon.setImageResource(getTrackModeResource(trackMode));
            }
            // warning icon
            List<NodeWarning> warnings = networkNode.getWarnings();
            warningIcon.setVisibility(warnings.isEmpty() ? View.GONE : View.VISIBLE);
            //
            itemView.setTag(nodeBle);
            // fill UI elements next
            nodeName.setText(nn.getLabel());
            tvNodeBleAddress.setText(nn.getBleAddress());
            nodeStateView.setNetworkNode(nn);
            configureNodeStateView(continuous);
            // set up signal strength indicator
            Integer nodeRssi = presenceApi.getNodeRssi(nodeBle);
            PresenceStatus nodeStatus = presenceApi.getNodeStatus(nodeBle);
            updateSignalStrengthAndEditIcon(signalStrengthInterpreter.asSignalStrength(nodeRssi), nodeStatus);
            updateLocateIcon(nn.getType(), trackMode, nn.getUwbMode());
            boolean expanded = expandedNodeIds.contains(networkNode.getId());
            if (expanded) {
                //
                detailsTable.setVisibility(View.VISIBLE);
                // first remove all previously added children
                detailsTable.removeAllViews();
                // now fill with the new values
                LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
                List<NetworkNodeProperty> properties = getPropertiesForModeAndNode(nodeType, applicationModeSupplier.get());
                for (NetworkNodeProperty property : properties) {
                    if (nodeType == NodeType.TAG && !((TagNode) nn).isAccelerometerEnable() && property == NetworkNodeProperty.TAG_STATIONARY_UPDATE_RATE) {
                        // skip this one
                        continue;
                    }
                    // for each property, generate a table row
                    @SuppressLint("InflateParams") TableRow tableRow = (TableRow) inflater.inflate(R.layout.li_network_node_table_row, null);
                    fillTableRow(networkNode, property, tableRow);
                    detailsTable.addView(tableRow);
                }
                // check if the node is in warning conditions
                if (warnings.size() > 0) {
                    @SuppressLint("InflateParams") TableRow tableRow = (TableRow) inflater.inflate(R.layout.li_network_node_table_row, null);
                    StringBuilder sb = new StringBuilder();
                    for (NodeWarning warning : warnings) {
                        switch (warning.type) {
                            case UWB_OFF:
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(daApp.getString(R.string.warning_node_uwb_off));
                                break;
                            case TAG_UWB_PASSIVE:
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(daApp.getString(R.string.warning_tag_node_uwb_passive));
                                break;
                            case OTHER_ANCHOR_SAME_POSITION:
                                if (sb.length() > 0) sb.append(", ");
                                boolean first = true;
                                for (Long nodeId : warning.relatedNodesParam) {
                                    NetworkNodeEnhanced node = networkNodeManager.getNode(nodeId);
                                    Preconditions.checkNotNull(node, "unknown related node " + Util.formatAsHexa(nodeId) + "!");
                                    String label = node.asPlainNode().getLabel();
                                        if (first) {
                                            sb.append(daApp.getString(R.string.warning_same_position, label));
                                            first = false;
                                        } else {
                                            sb.append(", ").append(label);
                                        }
                                    }


                                break;
                        }
                    }
                    //noinspection unchecked
                    setupTableRow(tableRow, daApp.getString(R.string.overview_node_warning), sb.toString(), R.style.NodeDetailsWarning);
                    detailsTable.addView(tableRow);
                }
            } else {
                detailsTable.setVisibility(View.GONE);
            }
        }

        private void configureNodeStateView(boolean animateStateChange) {
            if (DEBUG) {
                log("configureNodeStateView: " + "animateStateChange = [" + animateStateChange + "]");
            }
            NodeStateView.State nodeTypeState;
            Boolean lastSessionSuccessful = bleConnectionApi.lastSessionSuccessful(nodeBle);
            boolean advancedMode = appPreferenceAccessor.getApplicationMode() == ApplicationMode.ADVANCED;
            if (!bleConnectionApi.getConnectionState(nodeBle).disconnected) {
                nodeTypeState = NodeStateView.State.CONNECTED;
            } else if (advancedMode && lastSessionSuccessful != null && !lastSessionSuccessful) {
                nodeTypeState = NodeStateView.State.ERROR;
            } else {
                nodeTypeState = NodeStateView.State.SHOW_NODE_TYPE;
            }
            nodeStateView.setOnClickAction(advancedMode ?
                    () -> mainActivity.showFragment(FragmentType.DEVICE_DEBUG_CONSOLE, DeviceDebugConsoleFragment.getArgsForDevice(nodeBle))
                    : null);
            nodeStateView.setState(nodeTypeState, animateStateChange);
            nodeStateView.setPassive(passive);
        }

        private void fillTableRow(NetworkNodeEnhanced networkNode, NetworkNodeProperty property, TableRow tableRow) {
            NetworkNodePropertyDecorator.DecoratedProperty decoratedProperty = propertyDecorator.decorate(property);
            Object propertyValue = networkNode.getProperty(property);
            Integer extraStyleId = null;
            if (property == NetworkNodeProperty.UWB_MODE && propertyValue == UwbMode.PASSIVE && networkNode.isAnchor()) {
                extraStyleId = R.style.AnchorDetailsPassiveUwb;
            }
            //noinspection unchecked
            setupTableRow(tableRow, decoratedProperty.label,
                    propertyValue == null ? daApp.getString(R.string.value_not_known_tabular) : decoratedProperty.formatValue(propertyValue),
                    extraStyleId);
        }

        private void setupTableRow(TableRow tableRow,
                                   String key,
                                   String formatted, Integer extraStyleId) {
            ((TextView) tableRow.findViewById(R.id.tvKey)).setText(key);
            TextView tvValue = (TextView) tableRow.findViewById(R.id.tvValue);
            tvValue.setText(formatted == null ? "" : formatted);
            if (extraStyleId != null) {
                applyStyle(tvValue, extraStyleId);
            }
        }

        private void applyStyle(TextView textView, int extraStyle) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextAppearance(extraStyle);
            } else {
                //noinspection deprecation
                textView.setTextAppearance(mainActivity, extraStyle);
            }
        }

        public void updateNodeState() {
            if (DEBUG) {
                log("updateNodeState");
            }
            configureNodeStateView(true);
        }

        void updateLocateIcon(NodeType nodeType, TrackMode trackMode, UwbMode uwbMode) {
            locateIcon.setVisibility(
                    GridFragment.isNodeTracked(nodeType, position, trackMode, uwbMode) ? View.VISIBLE : View.GONE);
        }

        public void updateSignalStrengthAndEditIcon(SignalStrength strength,
                                                    PresenceStatus presenceStatus) {
            if (DEBUG) {
                log("updateSignalStrengthAndEditIcon: " + "strength = [" + strength + "], presenceStatus = [" + presenceStatus + "]");
            }
            // transform the signal strength first
            boolean showing;
            switch (presenceStatus) {
                case PRESENT:
                    showing = true;
                    signalStrengthView.setSignalStrength(strength, true);
                    break;
                case PROBABLY_PRESENT:
                    showing = true;
                    signalStrengthView.setSignalStrength(strength, false);
                    break;
                case PROBABLY_MISSING:
                case MISSING:
                    showing = false;
                    break;
                default:
                    throw new IllegalStateException("unexpected presence status: " + presenceStatus);
            }
            signalStrengthView.setVisibility(showing ? View.VISIBLE : View.INVISIBLE);
            editIcon.setEnabled(showing);
        }

    }

    private List<NetworkNodeProperty> getPropertiesForModeAndNode(NodeType nodeType, ApplicationMode applicationMode) {
        boolean advancedMode = applicationMode == ApplicationMode.ADVANCED;
        if (nodeType == NodeType.ANCHOR) {
            return advancedMode ? ADVANCED_PROPERTIES_ANCHOR : BASIC_PROPERTIES_ANCHOR;
        } else {
            return advancedMode ? ADVANCED_PROPERTIES_TAG : BASIC_PROPERTIES_TAG;
        }
    }

    private static int getTrackModeResource(TrackMode trackMode) {
        switch (trackMode) {
            case NOT_TRACKED:
                return R.drawable.ic_track_disabled;
            case TRACKED_POSITION:
                return R.drawable.ic_track_enabled;
            case TRACKED_POSITION_AND_RANGING:
                return R.drawable.ic_location_with_ranging;
            default:
                throw new IllegalArgumentException("unrecognized track mode: " + trackMode);
        }
    }

    private void onNetworkNodeSelected(long nodeId) {
        Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> pair = findNodeById(nodeId);
        if (pair != null) {
            Integer i = pair.second.previousIndex();
            int firstNetworkNodeIndex = getFirstNetworkNodeIndex();
            log.d("clicked " + i + ". node (" + (firstNetworkNodeIndex + i) + " item overall)");
            if (!expandedNodeIds.remove(nodeId)) {
                // the item is not there, we will not collapse but expand
                expandedNodeIds.add(nodeId);
            }
            notifyItemChanged(firstNetworkNodeIndex + i);
        }
    }


    class NetworkSummaryItemHolder extends ViewHolder {
        // references to views
        @BindView(R.id.networkName)
        TextView networkName;
        @BindView(R.id.infoNumberOfAnchors)
        TextView numberOfAnchors;
        @BindView(R.id.infoNumberOfTags)
        TextView numberOfTags;
        @BindView(R.id.infoNetworkId)
        TextView networkId;
        @BindView(R.id.tagPictogram)
        NodeStateView tagPictogram;
        @BindView(R.id.anchorPictogram)
        NodeStateView anchorPictogram;
        //
        NetworkModel network;


        NetworkSummaryItemHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this, itemView);
            // set icons
            anchorPictogram.setState(NodeStateView.State.ANCHOR, false);
            tagPictogram.setState(NodeStateView.State.TAG, false);
        }

        void bind(NetworkModel network) {
            this.network = network;
            // fill UI elements
            networkName.setText(network.getNetworkName());
            int anchors = networkNodeManager.getNumberOfAnchors(network.networkId);
            int tags = networkNodeManager.getNumberOfTags(network.networkId);
            this.numberOfAnchors.setText(daApp.getResources().getQuantityString(R.plurals.number_of_anchors, anchors, anchors));
            this.numberOfTags.setText(daApp.getResources().getQuantityString(R.plurals.number_of_tags, tags, tags));
            networkId.setText(daApp.getString(R.string.network_id, Util.formatNetworkId(network.getNetworkId())));
        }

    }

    private class TapHereTextCardItemHolder extends ViewHolder {

        TapHereTextCardItemHolder(View itemView, View.OnClickListener onClickListener, int textResId) {
            super(itemView);
            TextView tv = (TextView) itemView.findViewById(R.id.text);
            // set click listener
            tv.setOnClickListener(onClickListener);
            // set proper text
            String tapString = daApp.getString(R.string.tap_here);
            String str = daApp.getString(textResId, tapString);
            SpannableString ss = new SpannableString(str);
            //
            int i = str.indexOf(tapString);
            if (i >= 0) {
                int to = i + tapString.length();
                ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(daApp, R.color.mtrl_accent)),
                        i, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(ss, TextView.BufferType.SPANNABLE);
            }
        }

    }

    private class NewNodesDiscoveredItemHolder extends TapHereTextCardItemHolder {

        NewNodesDiscoveredItemHolder(View itemView) {
            //noinspection Convert2Lambda
            super(itemView, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    permissionHelper.mkSureServicesEnabledAndPermissionsGranted(mainActivity, () -> {
                        // show appropriate fragment
                        InterfaceHub.getHandlerHub(IhMainActivityProvider.class, InterfaceHubContract.Delivery.RELIABLE).provideMainActivity((m) -> m.showFragment(FragmentType.DISCOVERY));
                    });
                }
            }, R.string.tap_to_show_discovered_nodes);
        }

    }

    public Bundle saveState() {
        Bundle b = new Bundle();
        Long[] arr = expandedNodeIds.toArray(new Long[expandedNodeIds.size()]);
        b.putLongArray(BK_EXPANDED_ITEMS, ArrayUtils.toPrimitive(arr));
        return b;
    }

    public void restoreState(Bundle bundle) {
        long[] longs = bundle.getLongArray(BK_EXPANDED_ITEMS);
        if (longs != null) {
            for (long aLong : longs) {
                expandedNodeIds.add(aLong);
            }
        }
    }

    public void setExpandedNodeId(Long nodeId) {
        expandedNodeIds.clear();
        expandedNodeIds.add(nodeId);
    }

    public boolean isShowingNewNodesDiscovered() {
        return showingNewNodesDiscovered;
    }
}
