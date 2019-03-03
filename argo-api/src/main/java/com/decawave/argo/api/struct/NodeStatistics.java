/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import java.util.Arrays;

/**
 * Raw statistics.
 *
 * long is used instead of unsigned int
 */
public class NodeStatistics {
    /* System */
    public long uptime;
    public long memfree;
    public int mcu_temp;
    public float drift_avg_rtc;

    /* Interfaces */
    public long uwb0_intr;
    public long uwb0_rst;
    public long rx_ok;
    public long rx_err;
    public long tx_err;
    public long tx_errx;

    /* MAC */
    public long alma_tx_ok;
    public long alma_tx_err;
    public long alma_rx_ok;
    public long bcn_tx_ok;
    public long bcn_tx_err;
    public long bcn_rx_ok;
    public long cl_tx_ok;
    public long cl_tx_err;
    public long cl_rx_ok;
    public long cl_coll;
    public long fwup_tx_ok;
    public long fwup_tx_err;
    public long fwup_rx_ok;
    public long svc_tx_err;
    public long svc_tx_ok;
    public long svc_rx_ok;
    public long clk_sync;

    /* BLE */
    public long ble_con_ok;
    public long ble_dis_ok;
    public long ble_err;

    /* Measurements / Data */
    public long tdoa_ok;
    public long tdoa_err;
    public long twr_ok;
    public long twr_err;

    /* Reserved - max 8 elements */
    public long reserved[];

    public NodeStatistics() {
    }


    public NodeStatistics(NodeStatistics other) {
        this.uptime = other.uptime;
        this.memfree = other.memfree;
        this.mcu_temp = other.mcu_temp;
        this.drift_avg_rtc = other.drift_avg_rtc;
        this.uwb0_intr = other.uwb0_intr;
        this.uwb0_rst = other.uwb0_rst;
        this.rx_ok = other.rx_ok;
        this.rx_err = other.rx_err;
        this.tx_err = other.tx_err;
        this.tx_errx = other.tx_errx;
        this.alma_tx_ok = other.alma_tx_ok;
        this.alma_tx_err = other.alma_tx_err;
        this.alma_rx_ok = other.alma_rx_ok;
        this.bcn_tx_ok = other.bcn_tx_ok;
        this.bcn_tx_err = other.bcn_tx_err;
        this.bcn_rx_ok = other.bcn_rx_ok;
        this.cl_tx_ok = other.cl_tx_ok;
        this.cl_tx_err = other.cl_tx_err;
        this.cl_rx_ok = other.cl_rx_ok;
        this.cl_coll = other.cl_coll;
        this.fwup_tx_ok = other.fwup_tx_ok;
        this.fwup_tx_err = other.fwup_tx_err;
        this.fwup_rx_ok = other.fwup_rx_ok;
        this.svc_tx_err = other.svc_tx_err;
        this.svc_tx_ok = other.svc_tx_ok;
        this.svc_rx_ok = other.svc_rx_ok;
        this.clk_sync = other.clk_sync;
        this.ble_con_ok = other.ble_con_ok;
        this.ble_dis_ok = other.ble_dis_ok;
        this.ble_err = other.ble_err;
        this.tdoa_ok = other.tdoa_ok;
        this.tdoa_err = other.tdoa_err;
        this.twr_ok = other.twr_ok;
        this.twr_err = other.twr_err;
        this.reserved = Arrays.copyOf(other.reserved, other.reserved.length);
    }

    @Override
    public String toString() {
        String s = "NodeStatistics{" +
                "uptime=" + uptime +
                ", memfree=" + memfree +
                ", mcu_temp=" + mcu_temp +
                ", drift_avg_rtc=" + drift_avg_rtc +
                ", uwb0_intr=" + uwb0_intr +
                ", uwb0_rst=" + uwb0_rst +
                ", rx_ok=" + rx_ok +
                ", rx_err=" + rx_err +
                ", tx_err=" + tx_err +
                ", tx_errx=" + tx_errx +
                ", alma_tx_ok=" + alma_tx_ok +
                ", alma_tx_err=" + alma_tx_err +
                ", alma_rx_ok=" + alma_rx_ok +
                ", bcn_tx_ok=" + bcn_tx_ok +
                ", bcn_tx_err=" + bcn_tx_err +
                ", bcn_rx_ok=" + bcn_rx_ok +
                ", cl_tx_ok=" + cl_tx_ok +
                ", cl_tx_err=" + cl_tx_err +
                ", cl_rx_ok=" + cl_rx_ok +
                ", cl_coll=" + cl_coll +
                ", fwup_tx_ok=" + fwup_tx_ok +
                ", fwup_tx_err=" + fwup_tx_err +
                ", fwup_rx_ok=" + fwup_rx_ok +
                ", svc_tx_err=" + svc_tx_err +
                ", svc_tx_ok=" + svc_tx_ok +
                ", svc_rx_ok=" + svc_rx_ok +
                ", clk_sync=" + clk_sync +
                ", ble_con_ok=" + ble_con_ok +
                ", ble_dis_ok=" + ble_dis_ok +
                ", ble_err=" + ble_err +
                ", tdoa_ok=" + tdoa_ok +
                ", tdoa_err=" + tdoa_err +
                ", twr_ok=" + twr_ok +
                ", twr_err=" + twr_err;
        if (reserved != null && reserved.length > 0) {
            for (int i = 0; i < reserved.length; i++) {
                s += ", res[" + i + "]=" + reserved[i];
            }
        }
        s += '}';
        return s;
    }
}