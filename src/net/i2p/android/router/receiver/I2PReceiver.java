package net.i2p.android.router.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import net.i2p.android.router.binder.RouterBinder;
import net.i2p.android.router.service.RouterService;

public class I2PReceiver extends BroadcastReceiver {
    private final Context _context;
    private boolean _isBound;
    private RouterService _routerService;
    private ServiceConnection _connection;

    /**
     *  Registers itself
     */
    public I2PReceiver(Context context) {
        super();
        _context = context;
        getInfo();
        IntentFilter intents = new IntentFilter();
        intents.addAction(Intent.ACTION_TIME_CHANGED);
        intents.addAction(Intent.ACTION_TIME_TICK);  // once per minute, for testing
        intents.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, intents);
        boolean success = bindRouter();
        if (!success)
            System.err.println(this + " Bind router failed");
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        System.err.println("Got broadcast: " + action);

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
            boolean noConn = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo other = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            System.err.println("No conn? " + noConn + " failover? " + failover + 
                               " info: " + info + " other: " + other);
            printInfo(info);
            printInfo(other);
            getInfo();
        }
    }

    public boolean isConnected() {
        NetworkInfo current = getInfo();
        return current != null && current.isConnected();
    }

    private NetworkInfo getInfo() {
        ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = cm.getActiveNetworkInfo();
        System.err.println("Current network info:");
        printInfo(current);
        return current;
    }

    private static void printInfo(NetworkInfo ni) {
        if (ni == null) {
            System.err.println("Network info is null");
            return;
        }
        System.err.println(
             "state: " + ni.getState() +
             " detail: " + ni.getDetailedState() +
             " extrainfo: " + ni.getExtraInfo() +
             " reason: " + ni.getReason() +
             " typename: " + ni.getTypeName() +
             " available: " + ni.isAvailable() +
             " connected: " + ni.isConnected() +
             " conorcon: " + ni.isConnectedOrConnecting() +
             " failover: " + ni.isFailover());

    }

    private boolean bindRouter() {
        Intent intent = new Intent();
        intent.setClassName(_context, "net.i2p.android.router.service.RouterService");
        System.err.println(this + " calling bindService");
        _connection = new RouterConnection();
        boolean success = _context.bindService(intent, _connection, Context.BIND_AUTO_CREATE);
        System.err.println(this + " got from bindService: " + success);
        return success;
    }

    public void unbindRouter() {
        if (_connection != null)
            _context.unbindService(_connection);
    }

    private class RouterConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName name, IBinder service) {
            RouterBinder binder = (RouterBinder) service;
            _routerService = binder.getService();
            _isBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            _isBound = false;
        }
    }
}
