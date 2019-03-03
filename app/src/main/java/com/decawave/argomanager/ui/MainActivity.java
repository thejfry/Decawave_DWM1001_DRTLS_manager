/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.function.Function;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.components.DiscoveryManager;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.components.ih.IhNetworkChangeListener;
import com.decawave.argomanager.components.ih.IhNetworkChangeListenerAdapter;
import com.decawave.argomanager.prefs.AppPreference;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.ApplicationMode;
import com.decawave.argomanager.prefs.IhAppPreferenceListener;
import com.decawave.argomanager.ui.actionbar.AbSpinnerAdapter;
import com.decawave.argomanager.ui.actionbar.AbSpinnerPopup;
import com.decawave.argomanager.ui.actionbar.DevelopmentToolsSpinnerItem;
import com.decawave.argomanager.ui.actionbar.MainSpinnerItem;
import com.decawave.argomanager.ui.actionbar.SpinnerItem;
import com.decawave.argomanager.ui.fragment.AbstractArgoFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;
import com.decawave.argomanager.ui.layout.IconizableSnackbar;
import com.decawave.argomanager.ui.uiutil.ISnackbar;
import com.decawave.argomanager.util.AndroidPermissionHelper;
import com.decawave.argomanager.util.IhOnActivityResultListener;
import com.decawave.argomanager.util.ToastUtil;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubContract;
import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action1;

import static com.decawave.argomanager.ArgoApp.daApp;
import static com.decawave.argomanager.ArgoApp.uiHandler;
import static com.decawave.argomanager.ioc.IocContext.daCtx;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, IhAppPreferenceListener,
        AbstractArgoFragment.OnFragmentSwitchedListener {

    private static final ComponentLog log = new ComponentLog(MainActivity.class);

    private Map<MenuItem, Short> menuItemToNetworkId = new LinkedHashMap<>();

    private FragmentType mLastDisplayedFragment;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    DiscoveryManager discoveryManager;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    @Inject
    AndroidPermissionHelper permissionHelper;

    /**
     * drop-down navigation of our ActionBar
     */
    private AbSpinnerAdapter<MainSpinnerItem> abMainSpinnerAdapter;
    private AbSpinnerAdapter<DevelopmentToolsSpinnerItem> abDevelopmentToolsSpinnerAdapter;
    private AbSpinnerPopup abSpinnerPopup;
    private TextView abSpinnerAnchorText;
    private ActionBarDrawerToggle navDrawerToggle;
    private ViewGroup mainContainer;
    private NavigationView navigationView;
    private IhNetworkChangeListener networkChangeListener;
    private IhMainActivityProvider mainActivityProvider;

    private boolean isResumed;
    private DrawerLayout drawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // onCreate UI means that we might need display metrics
        DisplayMetrics.initDisplayMetrics();
        // initialize injected fields
        daCtx.inject(this);
        // create instance of network UI manager
        setContentView(R.layout.activity_main);

        mainContainer = (ViewGroup) findViewById(R.id.main_container);

        // action bar stuff
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // enable ActionBar app icon to behave as action to toggle nav drawer
        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // AB main spinner
        abMainSpinnerAdapter = new AbSpinnerAdapter<>(MainSpinnerItem.values(), this,
                () -> appPreferenceAccessor.getLastSelectedMainSpinnerItemPos()
        );
        abDevelopmentToolsSpinnerAdapter = new AbSpinnerAdapter<>(DevelopmentToolsSpinnerItem.values(), this,
                () -> appPreferenceAccessor.getLastSelectedDevelopmentToolsSpinnerItemPos()
        );

        // drawer
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        navDrawerToggle = new ActionBarDrawerToggle(
                // we MUST pass null - otherwise backup button in AB does not work
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(navDrawerToggle);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // set up the items shown in the navigation view
        tweakMenuItemsFillNetworks(navigationView, true);
        networkChangeListener = new IhNetworkChangeListenerAdapter() {

            @Override
            public void onNetworkRenamed(short networkId, String networkName) {
                // we need to redraw and reorder items in menu
                tweakMenuItemsFillNetworks(navigationView, true);
            }

            @Override
            public void onNetworkAdded(short networkId) {
                // rebuild mapping
                tweakMenuItemsFillNetworks(navigationView, false);
            }

            @Override
            public void onNetworkUpdated(short networkId) {
                // we might need to redraw items in the menu
                tweakMenuItemsFillNetworks(navigationView, false);
            }

            @Override
            public void onNetworkRemoved(final short networkId, String networkName, boolean explicitUserAction) {
                // rebuild mapping
                tweakMenuItemsFillNetworks(navigationView, false);
                if (explicitUserAction) {
                    // show snackbar
                    makeSnackbar(getString(R.string.network_removed, networkName), 5000)
                            .setAction(R.string.undo, v -> {
                                networkNodeManager.undoNetworkRemove(networkId);
                                // if we are still in the overview fragment
                                if (mLastDisplayedFragment == FragmentType.OVERVIEW) {
                                    // make it active again
                                    appPreferenceAccessor.setActiveNetworkId(networkId);
                                }
                            })
                            .show();
                }
            }

        };
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    //noinspection ConstantConditions
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        navDrawerToggle.syncState();
    }

    /**
     *
     */
    @Override
    protected void onResumeFragments() {
        if (Constants.DEBUG) {
            log.d("onResumeFragments");
        }
        super.onResumeFragments();

        showMainFragment();
    }

    private void showMainFragment() {
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment currentFragment = fm.findFragmentById(R.id.content_main);
        if (currentFragment == null) {
            final int pos = appPreferenceAccessor.getLastSelectedMainSpinnerItemPos();
            final FragmentType fragmentType = abMainSpinnerAdapter.getItem(pos).getFragmentType();
            showFragment(fragmentType, null);
        } // else: no need to show anything
    }

    public void setAbSpinnerAnchorText(AbSpinnerAdapter spinnerAdapter) {
        if (abSpinnerAnchorText != null) {
            abSpinnerAnchorText.setText(spinnerAdapter.getSelectedAnchorText());
        }
    }


    private void showAbSpinnerPopup(final View anchorView, AbSpinnerAdapter spinnerAdapter, Action1<SpinnerItem> onSpinnerItemSelectedAction) {
        dismissAbSpinnerPopup();

        abSpinnerPopup = new AbSpinnerPopup(this, spinnerAdapter, onSpinnerItemSelectedAction::call);
        abSpinnerPopup.show(anchorView);
    }

    private void dismissAbSpinnerPopup() {
        try {
            if (abSpinnerPopup != null && abSpinnerPopup.isShowing()) {
                abSpinnerPopup.dismiss();
            }
        } catch (Exception e) {
            log.w(e);
        }
        abSpinnerPopup = null;
    }

    private void tweakMenuItemsFillNetworks(NavigationView navigationView, boolean fullyRegenerate) {
        Menu menu = navigationView.getMenu();
        Short activeNetworkId = appPreferenceAccessor.getActiveNetworkId();
        //
        // add new items/networks at the very beginning
        Map<Short, NetworkModel> networks = networkNodeManager.getNetworks();
        // remove all missing networks first
        Iterator<Map.Entry<MenuItem, Short>> it = menuItemToNetworkId.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MenuItem, Short> keyValue = it.next();
            if (!networks.containsKey(keyValue.getValue())) {
                // remove this menu item
                menu.removeItem(keyValue.getKey().getItemId());
                it.remove();
            }
        }
        MenuItem doDiscoveryItem = menu.findItem(R.id.nav_do_discovery);
        int order = doDiscoveryItem.getOrder() - 1000;
        Preconditions.checkState(order > 0);
        //
        List<NetworkModel> values = new ArrayList<>(networks.values());
        // sort the networks by name
        Collections.sort(values, Util.NETWORK_NAME_COMPARATOR);
        // add the new networks now
        if (fullyRegenerate) {
            for (MenuItem menuItem : menuItemToNetworkId.keySet()) {
                menu.removeItem(menuItem.getItemId());
            }
            // clear the mapping now
            menuItemToNetworkId.clear();
            // now add the new network one-by-one
            for (NetworkModel network : values) {
                final MenuItem menuItem = menu.add(R.id.networksSection, View.generateViewId(),
                        order, network.getNetworkName());
                configureMenuItem(activeNetworkId, network, menuItem);
                // associate network id with the action view
                menuItemToNetworkId.put(menuItem, network.getNetworkId());
                //
                order++;
            }
        } else {
            for (NetworkModel network : values) {
                if (!menuItemToNetworkId.containsValue(network.getNetworkId())) {
                    // we don't have this network yet
                    final MenuItem menuItem = menu.add(R.id.networksSection, View.generateViewId(),
                            order, network.getNetworkName());
                    configureMenuItem(activeNetworkId, network, menuItem);
                    // associate network id with the action view
                    menuItemToNetworkId.put(menuItem, network.getNetworkId());
                } else {
                    // just make sure that there is proper 'checked' status
                    if (Objects.equal(network.getNetworkId(), activeNetworkId)) {
                        // find the proper menu item
                        for (Map.Entry<MenuItem, Short> menuItemUUIDEntry : menuItemToNetworkId.entrySet()) {
                            if (menuItemUUIDEntry.getValue().equals(activeNetworkId)) {
                                menuItemUUIDEntry.getKey().setChecked(true);
                                break;
                            }
                        }
                    }
                }
                order++;
            }
        }
        if (appPreferenceAccessor.getApplicationMode() == ApplicationMode.SIMPLE && menu.findItem(R.id.nav_development_tools) != null) {
            // remove the item
            menu.removeItem(R.id.nav_development_tools);
        } else if (appPreferenceAccessor.getApplicationMode() == ApplicationMode.ADVANCED && menu.findItem(R.id.nav_development_tools) == null) {
            // add the item
            menu.add(R.id.networksSection, R.id.nav_development_tools, 9200, daApp.getString(R.string.navigation_development_tools)).setIcon(R.drawable.ic_development_tools);
        }
    }

    private void configureMenuItem(Short activeNetworkId, NetworkModel network, MenuItem menuItem) {
        menuItem.setIcon(R.drawable.ic_network);
        menuItem.setCheckable(true);
        if (Objects.equal(network.getNetworkId(), activeNetworkId)) {
            menuItem.setChecked(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // check presence of BLE adapter
        Boolean bluetoothEnabled = null;
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bluetoothEnabled = false;
            // make sure it is turned on
            final BluetoothManager bluetoothManager = (BluetoothManager) ArgoApp.daApp.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    bluetoothEnabled = bluetoothAdapter.isEnabled();
                }
            }
        }
        if (bluetoothEnabled == null) {
            // BT is not present
            uiHandler.post(() -> {
                ToastUtil.showToast(R.string.ble_not_supported, Toast.LENGTH_LONG);
                // exit the UI
                finish();
            });
        }
        //
        InterfaceHub.registerHandler(this);
        InterfaceHub.registerHandler(networkChangeListener);
        // highlight the network which is selected - being worked with
        highlightActiveNetwork(appPreferenceAccessor.getActiveNetworkId());
        // register ourselves as main activity provider - but only after we are resumed
        uiHandler.post(() -> {
            if (isResumed) {
                mainActivityProvider = (a) -> a.call(this);
                InterfaceHub.registerHandler(mainActivityProvider);
            }
        });
        isResumed = true;
    }

    @Override
    protected void onPause() {
        isResumed = false;
        if (mainActivityProvider != null) {
            InterfaceHub.unregisterHandler(mainActivityProvider);
        }
        //
        InterfaceHub.unregisterHandler(this);
        InterfaceHub.unregisterHandler(networkChangeListener);
        //
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        // check if the item represents a network
        Short networkId = menuItemToNetworkId.get(item);
        if (networkId != null) {
            // switch active network
            appPreferenceAccessor.setActiveNetworkId(networkId);
            // start/prolong time limited discovery
            if (permissionHelper.allSetUp()) {
                discoveryManager.startTimeLimitedDiscovery(true);
            }
        } else {
            switch (id) {
                case R.id.nav_do_discovery:
                    // Handle the discovery action
                    permissionHelper.mkSureServicesEnabledAndPermissionsGranted(this, () -> {
                        // show appropriate fragment
                        InterfaceHub.getHandlerHub(IhMainActivityProvider.class, InterfaceHubContract.Delivery.RELIABLE).provideMainActivity((m) -> m.showFragment(FragmentType.DISCOVERY));
                    });
                    break;
                case R.id.nav_development_tools:
                    // determine the appropriate fragment to show
                    DevelopmentToolsSpinnerItem devSpinnerItem = DevelopmentToolsSpinnerItem.values()[appPreferenceAccessor.getLastSelectedDevelopmentToolsSpinnerItemPos()];
                    showFragment(devSpinnerItem.getFragmentType());
                    break;
                case R.id.nav_position_log:
                    showFragment(FragmentType.POSITION_LOG);
                    break;
                case R.id.nav_settings:
                    showFragment(FragmentType.SETTINGS);
                    break;
                case R.id.nav_instructions:
                    showFragment(FragmentType.INSTRUCTIONS);
                    break;
                default:
                    throw new IllegalStateException("unexpected item clicked: " + id);
            }
        }
        // close the drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void highlightActiveNetwork(@Nullable Short activeNetworkId) {
        // this call might disable all networks! (if discovery is running)
        for (Map.Entry<MenuItem, Short> entry : menuItemToNetworkId.entrySet()) {
            //
            boolean active = entry.getValue().equals(activeNetworkId);
            entry.getKey().setChecked(active);
        }
    }

    private void onSpinnerItemSelected(SpinnerItem item) {
        // on action bar spinner item selected
        if (mLastDisplayedFragment != item.getFragmentType()) {
            // main fragment changed
            showFragment(item.getFragmentType(), null);
        }
    }

    public void showFragment(FragmentType fragmentType) {
        showFragment(fragmentType, null);
    }

    public void showFragment(FragmentType fragmentType, Bundle fragArgs) {
        showFragment(fragmentType, getSupportFragmentManager(), fragArgs);
    }

    public static void showFragment(FragmentType fragmentType, FragmentManager fm, Bundle fragArgs) {
        String fragmentTag = fragmentType.name();
        Fragment fragTarget = fm.findFragmentByTag(fragmentTag);
        //
        if (fragTarget != null || fragmentType.mainScreen) {
            // pop the stack until our target is on top, or we are at root level.
            popBackstackUntil(fm, fragmentTag);
        } else if (fragmentType == FragmentType.DEBUG_LOG || fragmentType == FragmentType.DEVICE_ERRORS) {
            // try to find the other one
            String otherFragmentTag;
            if (fragmentType == FragmentType.DEBUG_LOG) {
                otherFragmentTag = FragmentType.DEVICE_ERRORS.name();
            } else {
                otherFragmentTag = FragmentType.DEBUG_LOG.name();
            }
            Fragment otherFragment = fm.findFragmentByTag(otherFragmentTag);
            if (otherFragment != null) {
                // pop the backstack up to and including the other fragment
                boolean b = popBackstackUntil(fm, otherFragmentTag);
                if (Constants.DEBUG) {
                    Preconditions.checkState(b, "FIXME");
                }
                // one more pop to remove the found other-fragment
                fm.popBackStack();
            }
        }

        // start transaction to modify the ActionBar, Footer, and if necessary,
        // the main container as well...
        final FragmentTransaction ft = fm.beginTransaction();

        if (fragTarget == null) {
            fragTarget = createNewFragmentInstance(fragmentType, fragArgs);
            ft.replace(R.id.content_main, fragTarget, fragmentTag);
        }

        if (!fragmentType.mainScreen) {
            ft.addToBackStack(fragmentTag);
        }

        ft.commit();
    }

    private static boolean popBackstackUntil(FragmentManager fm, String fragmentTag) {
        int cntStackEntries;
        while ((cntStackEntries = fm.getBackStackEntryCount()) > 0) {
            String topStackEntryName = fm.getBackStackEntryAt(cntStackEntries - 1).getName();
            if (fragmentTag.equals(topStackEntryName)) {
                // stack item with a fragment of the same type as our target.
                return true;
            } else {
                fm.popBackStackImmediate();
            }
        }
        return false;
    }

    public static Fragment createNewFragmentInstance(FragmentType fragmentType, Bundle fragArgs) {
        if (Constants.DEBUG) {
            log.d("createFragment " + fragmentType);
        }
        AbstractArgoFragment frag = fragmentType.newInstance();
        if (fragArgs != null) {
            frag.setArguments(fragArgs);
        }
        return frag;
    }


    @Override
    public void onFragmentSwitched(AbstractArgoFragment fragmentInstance) {
        FragmentType fragmentType = fragmentInstance.fragmentType;
        if (Constants.DEBUG) log.d("onFragmentSwitched: " + fragmentType);
        ActionBar ab = getSupportActionBar();
        if (fragmentType.mainScreen) {
            // tweak the actionbar - to show proper content
            tweakAbSpinnerForMainScreen(fragmentType);
        } else if (fragmentType == FragmentType.DEVICE_ERRORS || fragmentType == FragmentType.DEBUG_LOG) {
            // this is development tools
            tweakAbSpinnerForDevelopmentToolsScreen(ab, fragmentType);
        } else {
            // regular secondary screen
            tweakAbSpinnerForSecondaryScreen(ab, fragmentInstance);
        }

        // set drawer lock mode
        updateDrawerLockedState(fragmentType);
        // check if the fragment type has onFragmentLeft
        if (mLastDisplayedFragment != null && mLastDisplayedFragment.onFragmentLeft != null) {
            mLastDisplayedFragment.onFragmentLeft.call(fragmentType);
        }
        // check if the fragment type has onFragmentEntered
        if (fragmentType.onFragmentEntered != null) {
            fragmentType.onFragmentEntered.call(mLastDisplayedFragment);
        }
        // finally save what was the last displayed fragment
        mLastDisplayedFragment = fragmentType;
    }

    private void updateDrawerLockedState(FragmentType currentFragmentType) {
        drawer.setDrawerLockMode(currentFragmentType.mainScreen ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void tweakAbSpinnerForSecondaryScreen(ActionBar ab, AbstractArgoFragment fragment) {
        ab.setDisplayShowCustomEnabled(false);
        ab.setDisplayShowTitleEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);
        if (fragment.fragmentType.fullScreenDialog) {
            navDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_clear_white);
        } else {
            navDrawerToggle.setHomeAsUpIndicator(null);
        }
        navDrawerToggle.setDrawerIndicatorEnabled(false);
        if (fragment.fragmentType.hasScreenTitle) {
            ab.setTitle(fragment.getScreenTitle());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (navDrawerToggle.onOptionsItemSelected(item)) {
            return true; // consumed.
        }

        // Handle action buttons
        switch (item.getItemId()) {
            case android.R.id.home:
                // called when the up caret in the actionbar is pressed
                getSupportFragmentManager().popBackStackImmediate();
                return true;
            default:
                // (!) pass to super, to let fragment option menu callbacks be called (if any)..
                return super.onOptionsItemSelected(item);
        }
    }

    public void hideAbSpinnerNoNetwork() {
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayShowCustomEnabled(false);
        ab.setDisplayShowTitleEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);
        navDrawerToggle.setHomeAsUpIndicator(null);
        navDrawerToggle.setDrawerIndicatorEnabled(true);
        ab.setTitle(R.string.no_network);
    }

    private void tweakAbSpinnerForMainScreen(FragmentType fragmentType) {
        if (appPreferenceAccessor.getActiveNetworkId() == null) {
            // there is no active network
            hideAbSpinnerNoNetwork();
            return;
        } // else:
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayShowCustomEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayHomeAsUpEnabled(true);
        // tweak the spinner
        //noinspection ConstantConditions
        final LayoutInflater inflater = (LayoutInflater) ab.getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams")
        final View abSpinnerAnchor = inflater.inflate(R.layout.ab_spinner_anchor, null);
        abSpinnerAnchor.setOnClickListener(v ->
                showAbSpinnerPopup(abSpinnerAnchor, abMainSpinnerAdapter, this::onSpinnerItemSelected));
        ab.setCustomView(abSpinnerAnchor);

        final int newPos = abMainSpinnerAdapter.findItemPositionForFragmentType(fragmentType);
        abMainSpinnerAdapter.setSelectedItemPosition(newPos);
        abSpinnerAnchorText = (TextView) abSpinnerAnchor.findViewById(R.id.abSpinnerAnchorText);
        abSpinnerAnchorText.getBackground().mutate().setAutoMirrored(true);
        setAbSpinnerAnchorText(abMainSpinnerAdapter);
        //
        navDrawerToggle.setHomeAsUpIndicator(null);
        navDrawerToggle.setDrawerIndicatorEnabled(true);
        // save last selected ab spinner item
        appPreferenceAccessor.setLastSelectedMainSpinnerItemPos(newPos);
    }

    private void tweakAbSpinnerForDevelopmentToolsScreen(ActionBar ab, FragmentType fragmentType) {
        ab.setDisplayShowCustomEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayHomeAsUpEnabled(true);
        // tweak the spinner
        //noinspection ConstantConditions
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams")
        final View abSpinnerAnchor = inflater.inflate(R.layout.ab_spinner_anchor, null);
        abSpinnerAnchor.setOnClickListener(v ->
                showAbSpinnerPopup(abSpinnerAnchor, abDevelopmentToolsSpinnerAdapter, this::onSpinnerItemSelected));
        ab.setCustomView(abSpinnerAnchor);

        final int newPos = abDevelopmentToolsSpinnerAdapter.findItemPositionForFragmentType(fragmentType);
        abDevelopmentToolsSpinnerAdapter.setSelectedItemPosition(newPos);
        abSpinnerAnchorText = (TextView) abSpinnerAnchor.findViewById(R.id.abSpinnerAnchorText);
        abSpinnerAnchorText.getBackground().mutate().setAutoMirrored(true);
        setAbSpinnerAnchorText(abDevelopmentToolsSpinnerAdapter);
        //
        navDrawerToggle.setHomeAsUpIndicator(null);
        navDrawerToggle.setDrawerIndicatorEnabled(false);
        // save last selected ab spinner item
        appPreferenceAccessor.setLastSelectedDevelopmentToolsSpinnerItemPos(newPos);
    }


    @Override
    public void onPreferenceChanged(AppPreference.Element element, Object oldValue, Object newValue) {
        if (element == AppPreference.Element.ACTIVE_NETWORK_ID) {
            // we are interested
            Short activeNetworkId = appPreferenceAccessor.getActiveNetworkId();
            // rebuild mapping
            tweakMenuItemsFillNetworks(navigationView, false);
            // go through menu items and highlight/de-highlight the proper one
            if (!menuItemToNetworkId.isEmpty()) {
                highlightActiveNetwork(activeNetworkId);
            }
        } else if (element == AppPreference.Element.APPLICATION_MODE) {
            // just tweak the appearance of development tools
            tweakMenuItemsFillNetworks(navigationView, false);
        }
    }

    public ISnackbar makeSnackbarWithHelpIcon(CharSequence text, @SuppressWarnings("SameParameterValue") int duration) {
        return _makeSnackbar(view -> {
            IconizableSnackbar snackbar = IconizableSnackbar.make((ViewGroup) view, duration);
            snackbar.setText(text);
            snackbar.setDuration(duration);
            return snackbar;
        });
    }

    @SuppressWarnings("SameParameterValue")
    public ISnackbar makeSnackbar(CharSequence text, int duration) {
        return _makeSnackbar(view -> {
            Snackbar androidSnackbar = Snackbar.make(view, text, duration);
            return new ISnackbar() {
                @Override
                public void show() {
                    androidSnackbar.show();
                }

                @Override
                public ISnackbar setAction(int resId, View.OnClickListener listener) {
                    androidSnackbar.setAction(resId, listener);
                    return this;
                }
            };
        });
    }

    private ISnackbar _makeSnackbar(Function<View, ISnackbar> snackbarSupplier) {
        final ISnackbar sb;
        if (mainContainer instanceof CoordinatorLayout) {
            sb = snackbarSupplier.apply(mainContainer);
        } else if (mainContainer.getChildCount() > 0 && mainContainer.getChildAt(0) instanceof CoordinatorLayout) {
            final View view = mainContainer.getChildAt(0);
            sb = snackbarSupplier.apply(view);
        } else {
            // HACK: force snackbar into our view by pretending it is android.R.id.content (Snackbar searches for parent wit this id)
            // HACKCHECK: on each design support library update
            mainContainer.setId(android.R.id.content);
            sb = snackbarSupplier.apply(mainContainer);
            // revert the HACK
            mainContainer.setId(R.id.main_container);
        }
        return sb;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // broadcast
        InterfaceHub.getHandlerHub(IhOnActivityResultListener.class).onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        InterfaceHub.getHandlerHub(IhOnActivityResultListener.class).onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

}
