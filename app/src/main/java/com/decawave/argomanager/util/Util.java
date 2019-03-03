/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.util;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.annimon.stream.Collector;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.struct.AnchorNode;
import com.decawave.argo.api.struct.LocationDataMode;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.NodeType;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argo.api.struct.Position;
import com.decawave.argo.api.struct.UwbMode;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.BuildConfig;
import com.decawave.argomanager.R;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.error.ErrorCodeInterpreter;
import com.decawave.argomanager.prefs.LengthUnit;
import com.decawave.argomanager.ui.IhMainActivityProvider;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.fragment.AbstractArgoFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubContract;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 * Various utility methods.
 */
@SuppressWarnings("WeakerAccess")
public class Util {
    public static final float MM_IN_METER = 1000;
    public static final float MM_IN_YARD = 914.4f;
    public static final float CM_IN_INCH = 2.54f;

    private static final int[] TAG_BASE_COLORS = { 0xFF2196F3, 0xFFFFB74D, 0xFFFF9100, 0xFF40C4FF, 0xFFff8dfb };

    public static Comparator<NetworkModel> NETWORK_NAME_COMPARATOR = (n1, n2) -> n1.getNetworkName().compareTo(n2.getNetworkName());

    private static Cache<Long, String> nodeIdAsStringCache = CacheBuilder.newBuilder()
                .weakKeys()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build();

    public static String formatNetworkId(short networkId) {
        return formatAsHexa(networkId, true);
    }

    @SuppressWarnings("WeakerAccess")
    public static String formatAsColonHexa(long number) {
        // print representation
        try {
            return nodeIdAsStringCache.get(number, () -> String.format("%016X", number).replaceAll("(?<=..)(..)", ":$1"));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public static <T extends Number> String formatAsHexa(T number) {
        return formatAsHexa(number, true);
    }

    @SuppressWarnings("MalformedFormatString")
    public static <T extends Number> String formatAsHexa(T number, boolean prepend0x) {
        return (prepend0x ? "0x" : "") + String.format("%04X", number);
    }

    @SuppressWarnings("MalformedFormatString")
    public static String formatIntAsHexa(int number) {
        return "0x" + String.format("%08X", number);
    }

    public static boolean isRealInitiator(NetworkNode networkNode) {
        return networkNode instanceof AnchorNode
                && ((AnchorNode) networkNode).isInitiator()
                && isRealInitiator(((AnchorNode) networkNode).getMacStats());
    }
    public static boolean isRealInitiator(int macStats) {
        return (macStats & (1 << 3)) != 0;
    }

    public static String shortenNodeId(long number, boolean prepend0x) {
        return com.decawave.argo.api.Util.shortenNodeId(number, prepend0x);
    }

    public static String operatingFirmwareString(OperatingFirmware operatingFirmware) {
        switch (operatingFirmware) {
            case FW1:
                return daApp.getString(R.string.fw1);
            case FW2:
                return daApp.getString(R.string.fw2);
            default:
                throw new IllegalStateException("unsupported operating firmware: " + operatingFirmware);
        }
    }
    public static String nodeTypeString(NodeType nodeType) {
        switch (nodeType) {
            case ANCHOR:
                return daApp.getString(R.string.node_type_anchor);
            case TAG:
                return daApp.getString(R.string.node_type_tag);
            default:
                throw new IllegalStateException("unsupported node type: " + nodeType);
        }
    }

    public static ByteBuffer newByteBuffer(byte[] bytes) {
        ByteBuffer b = ByteBuffer.wrap(bytes);
        // as per BLE spec
        b.order(ByteOrder.LITTLE_ENDIAN);
        return b;
    }

    public static void formatLogEntry(StringBuilder sb, long timeInMillis, String message) {
        String time = formatMsgTime(timeInMillis);
        sb.append(time).append(": ").append(message).append('\n');
    }

    public static void formatLogEntry(StringBuilder sb, long logTime, String logMessage, Integer errorCode, Throwable exception) {
        String time = formatMsgTime(logTime);
        sb.append(time).append(": ").append(logMessage);
        if (errorCode != null) {
            sb.append(", errorCode ").append(errorCode).append(": ").append(ErrorCodeInterpreter.getName(errorCode));
        }
        if (exception != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(baos);
            exception.printStackTrace(stream);
            try {
                sb.append(baos.toString("utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 not supported?!", e);
            }
        } // final newline
        sb.append('\n');
    }

    public static boolean anyStaticProperty(Set<NetworkNodeProperty> properties) {
        for (NetworkNodeProperty p : properties) {
            if (!p.dynamic) {
                return true;
            }
        }
        return false;
    }

    public static void shareUriContent(Uri providerUri, String type) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, providerUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType(type);
        daApp.startActivity(intent);
    }

    public static String formatMsgTime(long time) {
        long delta = time - ArgoApp.startTime;    // we interested into tenths of ms
        String fmt = String.format(Locale.US, "%04d", delta);
        // XXXX -> X.XXX
        int pointIdx = fmt.length() - 3;
        return fmt.substring(0, pointIdx) + "." + fmt.substring(pointIdx, fmt.length());
    }

    public static String formatPosition(Position position, LengthUnit lengthUnit) {
        if (position == null) {
            return "null";
        }
        String x = Util.formatLength(position.x, lengthUnit);
        String y = Util.formatLength(position.y, lengthUnit);
        String z = Util.formatLength(position.z, lengthUnit);
        String q = String.valueOf(position.qualityFactor);
        return daApp.getString(R.string.position_template, x, y, z, q);
    }
    
    public static String formatLength(float value, LengthUnit lengthUnit) {
        switch (lengthUnit) {
            case METRIC:
                // meters
                return String.format(Locale.ENGLISH, "%.2f", value / MM_IN_METER);
            case IMPERIAL:
                // yards
                return String.format(Locale.ENGLISH, "%.2f", value / MM_IN_YARD);
            default:
                throw new IllegalArgumentException("unexpected length unit: " + lengthUnit);
        }
    }

    public static int parseLength(@NotNull String strLength, LengthUnit lengthUnit) throws NumberFormatException {
        float factor;
        switch (lengthUnit) {
            case METRIC:
                factor = Util.MM_IN_METER;
                break;
            case IMPERIAL:
                factor = Util.MM_IN_YARD;
                break;
            default:
                throw new IllegalArgumentException("unexpected length unit: " + lengthUnit);
        }
        return (int) (Float.valueOf(strLength) * factor + 0.5);
    }

    public static int computeColorForAddress(String bleAddress) {
        // parse the address
        String bytes[] = bleAddress.split(":");
        int r = byteHashFromInt(Integer.parseInt(bytes[0] + bytes[1], 16));
        int g = byteHashFromInt(Integer.parseInt(bytes[2] + bytes[3], 16));
        int b = byteHashFromInt(Integer.parseInt(bytes[4] + bytes[5], 16));
        int rnd = byteHashFromInt(r, g, b);
        int r2 = 0, g2 = 0, b2 = 0;
        int baseColor = TAG_BASE_COLORS[rnd % TAG_BASE_COLORS.length];
        r2 += r - Color.red(baseColor);
        g2 += g - Color.green(baseColor);
        b2 += b - Color.blue(baseColor);
        int factorDiv;
        if (rnd < 172) {
            factorDiv = 1;
        } else if (rnd < 222) {
            factorDiv = 2;
        } else if (rnd < 248) {
            factorDiv = 3;
        } else {
            factorDiv = 4;
        }
        float factor = 0.60f;
        r = Math.max(Math.min(r - (int) (r2 * factor / factorDiv), 255), 0);
        g = Math.max(Math.min(g - (int) (g2 * factor / factorDiv), 255), 0);
        b = Math.max(Math.min(b - (int) (b2 * factor / factorDiv), 255), 0);
        return Color.rgb(r, g, b);
    }

    private static int byteHashFromInt(int i) {
        return Math.abs(i) % 256;
    }

    private static int byteHashFromInt(int... is) {
        int acc = 0;
        for (int i : is) {
            acc += i * 7;
        }
        return byteHashFromInt(acc);
    }

    static String formatLocationDataMode(LocationDataMode locationDataMode) {
        switch (locationDataMode) {
            case POSITION:
                return daApp.getString(R.string.location_data_mode_position);
            case DISTANCES:
                return daApp.getString(R.string.location_data_mode_distances);
            case POSITION_AND_DISTANCES:
                return daApp.getString(R.string.location_data_mode_position_and_distances);
            default:
                throw new IllegalStateException("unexpected location data mode: " + locationDataMode);
        }

    }

    public static String formatUwbMode(UwbMode uwbMode) {
        switch (uwbMode) {
            case OFF:
                return daApp.getString(R.string.uwb_mode_off);
            case PASSIVE:
                return daApp.getString(R.string.uwb_mode_passive);
            case ACTIVE:
                return daApp.getString(R.string.uwb_mode_active);
            default:
                throw new IllegalStateException("unexpected uwb mode: " + uwbMode);
        }

    }

    public static <T> Collector<T, ?, ArrayList<T>> toArrayList(int size) {
        return new Collector<T, ArrayList<T>, ArrayList<T>>() {

            @Override
            public Supplier<ArrayList<T>> supplier() {
                return () -> new ArrayList<>(size);
            }

            @Override
            public BiConsumer<ArrayList<T>, T> accumulator() {
                return ArrayList::add;
            }

            @Override
            public Function<ArrayList<T>, ArrayList<T>> finisher() {
                return null;
            }
        };
    }

    public static int nodeDistance(Position p1, Position p2) {
        return distance(p2.x - p1.x, p2.y - p1.y);
    }

    public static int distance(int x, int y) {
        return (int) (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) + 0.5);
    }

    public static void configureNoNetworkScreen(View rootView, AndroidPermissionHelper permissionHelper, MainActivity mainActivity) {
        //
        ((TextView) rootView.findViewById(R.id.tvVersion)).setText(AbstractArgoFragment.daApp.getString(R.string.app_version, BuildConfig.VERSION_NAME));
        rootView.findViewById(R.id.btnDiscover).setOnClickListener(
                v -> {
                    // make sure that we got all the permissions
                    permissionHelper.mkSureServicesEnabledAndPermissionsGranted(mainActivity, () -> {
                        // show appropriate fragment
                        InterfaceHub.getHandlerHub(IhMainActivityProvider.class, InterfaceHubContract.Delivery.RELIABLE).provideMainActivity((m) -> m.showFragment(FragmentType.DISCOVERY));
                    });
                });
        rootView.findViewById(R.id.btnInstructions).setOnClickListener(
                v -> {
                    // show instructions screen
                    mainActivity.showFragment(FragmentType.INSTRUCTIONS);
                }
        );
    }
}
