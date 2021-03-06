package com.lechucksoftware.proxy.proxysettings.services;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.text.TextUtils;
import android.util.Log;

import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.constants.Intents;
import com.lechucksoftware.proxy.proxysettings.db.WiFiAPEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiAPConfig;
import be.shouldit.proxy.lib.constants.APLReflectionConstants;
import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.utils.ProxyUtils;
import timber.log.Timber;

/**
 * Created by Marco on 09/03/14.
 */
public class WifiSyncService extends EnhancedIntentService
{
    public static final String CALLER_INTENT = "CallerIntent";
    public static String TAG = WifiSyncService.class.getSimpleName();
    private boolean isHandling = false;
    private static WifiSyncService instance;

    public WifiSyncService()
    {
        super("WifiSyncService", android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
    }

    public static WifiSyncService getInstance()
    {
        return instance;
    }

    public boolean isHandlingIntent()
    {
        return isHandling;
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        instance = this;
        isHandling = true;

        App.getTraceUtils().startTrace(TAG, "syncAP", "Started handling intent", Log.INFO, true);

        List<APLNetworkId> configsToCheck = getConfigsToCheck(intent);
        App.getTraceUtils().partialTrace(TAG, "syncAP", "Got configurations to check", Log.INFO);

        syncProxyConfigurations(configsToCheck);
        App.getTraceUtils().stopTrace(TAG, "syncAP", "Sync-ed configurations", Log.INFO);

        isHandling = false;
    }

    private List<APLNetworkId> getConfigsToCheck(Intent intent)
    {
        List<APLNetworkId> networkIds = new ArrayList<APLNetworkId>();

        if (intent != null && intent.hasExtra(WifiSyncService.CALLER_INTENT))
        {
            Intent caller = (Intent) intent.getExtras().get(WifiSyncService.CALLER_INTENT);

            if (caller != null)
            {
                App.getTraceUtils().logIntent(TAG, caller, Log.INFO, true);

                if (caller.getAction().equals(APLReflectionConstants.CONFIGURED_NETWORKS_CHANGED_ACTION))
                {
                    if (caller.hasExtra(APLReflectionConstants.EXTRA_WIFI_CONFIGURATION))
                    {
                        WifiConfiguration wifiConf = (WifiConfiguration) caller.getExtras().get(APLReflectionConstants.EXTRA_WIFI_CONFIGURATION);
                        if (wifiConf != null)
                        {
                            Timber.d("Got change for WifiConfig: %s", wifiConf.toString());

                            APLNetworkId wifiId = null;

                            String SSID = ProxyUtils.cleanUpSSID(wifiConf.SSID);
                            SecurityType securityType = ProxyUtils.getSecurity(wifiConf);

                            if (!TextUtils.isEmpty(wifiConf.SSID) && securityType != null)
                            {
                                wifiId = new APLNetworkId(SSID,securityType);
                            }

                            if (wifiId == null)
                            {
                                // Instead of using the WifiConfiguration passed into the Intent,
                                // get the WifiConfiguration based on the network Id
                                Timber.d("Cannot prepare APLNetworkId from WifiConfiguration, trying getting from the configured networks");
                                WifiConfiguration conf = APL.getConfiguredNetwork(wifiConf.networkId);
                                if (conf != null)
                                {
                                    wifiId = new APLNetworkId(ProxyUtils.cleanUpSSID(conf.SSID), ProxyUtils.getSecurity(conf));
                                }
                            }

                            if (wifiId != null)
                            {
                                Timber.d("Adding network %s to list to be checked", wifiId.toString());
                                networkIds.add(wifiId);
                            }
                            else
                            {
                                Timber.e("Cannot get WifiConfiguration from Intent");
                            }
                        }
                    }

                    if (caller.hasExtra(APLReflectionConstants.EXTRA_MULTIPLE_NETWORKS_CHANGED))
                    {
                        boolean multipleChanges = (boolean) caller.getExtras().get(APLReflectionConstants.EXTRA_MULTIPLE_NETWORKS_CHANGED);
                        if (multipleChanges)
                        {
                            Timber.e("EXTRA_MULTIPLE_NETWORKS_CHANGED not handled at the moment");
                        }
                    }
                }
            }
        }

        return networkIds;
    }

    private void syncProxyConfigurations(List<APLNetworkId> configurations)
    {
        Map<APLNetworkId, WifiConfiguration> configuredNetworks = APL.getConfiguredNetworks();
        Timber.i("Configured %d Wi-Fi on the device", configuredNetworks.size());

        if (configurations.isEmpty())
        {
            Timber.i("No configurations specificed, must sync all of them!");
            configurations.addAll(configuredNetworks.keySet());
        }

        Timber.i("Analyzing %d Wi-Fi networks of %d total", configurations.size(), configuredNetworks.size());

        int upserted = 0;
        int removed = 0;

        for (APLNetworkId aplNetworkId : configurations)
        {
            try
            {
                App.getTraceUtils().partialTrace(TAG, "syncAP", "Handling network: " + aplNetworkId.toString(), Log.INFO);

                if (configuredNetworks.containsKey(aplNetworkId))
                {
                    WifiConfiguration wifiConfiguration = configuredNetworks.get(aplNetworkId);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "Get WifiConfiguration", Log.INFO);

                    WiFiAPConfig wiFiAPConfig = APL.getWiFiAPConfiguration(wifiConfiguration);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "Get WiFiAPConfig", Log.INFO);

                    WiFiAPEntity wiFiAPEntity = App.getDBManager().upsertWifiAP(wiFiAPConfig);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "Upsert WiFiAPEntity", Log.INFO);

                    App.getWifiNetworksManager().updateWifiConfig(wiFiAPConfig);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "updateWifiConfig: " + wiFiAPEntity.toString(), Log.INFO);

                    upserted++;
                }
                else
                {
                    App.getDBManager().deleteWifiAP(aplNetworkId);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "deleteWifiAP: " + aplNetworkId.toString(), Log.INFO);

                    App.getWifiNetworksManager().removeWifiConfig(aplNetworkId);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "removeWifiConfig: " + aplNetworkId.toString(), Log.INFO);

                    removed++;
                }
            }
            catch (Exception e)
            {
                Timber.e(e,"Exception during ProxySyncService");
            }
        }

        Timber.i("Analyzed %d Wi-Fi networks of %d total (%d upserted, %d removed)", configurations.size(), configuredNetworks.size(), upserted, removed);

        Timber.i("Sending broadcast intent " + Intents.PROXY_REFRESH_UI);
        Intent intent = new Intent(Intents.PROXY_REFRESH_UI);
        getApplicationContext().sendBroadcast(intent);
    }
}
