package com.justbeatit.smartguide;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.justbeatit.smartguide.context.Messanger;
import com.justbeatit.smartguide.context.MessangerImpl;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static int REQUEST_ENABLE_BT = 1;

    Set<Place> places = new HashSet<>();
    Place currentPlace;
    Beacon currentBeacon;

    final Set<String> devices = new HashSet<>();
    final Set<String> newDevices = new HashSet<>();
    Messanger messanger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        messanger = new MessangerImpl(getApplicationContext(), this);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Speaking ...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                messanger.sendMessage("Testowy komunikat!");
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setupLocations();

        discoverDevices();
        viewDiscoveredBeacons();
    }

    private void setupLocations() {
        Place shakespeareTheatre = new Place();
        shakespeareTheatre.Name = "Gdański Teatr Szekspirowski";
        shakespeareTheatre.Discounts = "Bilety promocyjne można zakupić tylko w kasie biletowej Teatru. Bilety ulgowe przysługują uczniom, studentom, nauczycielom, emerytom, rencistom oraz osobom niepełnosprawnym. Bilety ulgowe bez udokumentowania prawa do ulgi nie uprawniają do wejścia na widownię. Kupujący winien udać się do kasy biletowej Teatru i uiścić dopłatę.";
        shakespeareTheatre.Timetable = "12:00 Dziennik przebudzenia 20:00 Mój ulubiony Młynarski";

        shakespeareTheatre.Beacons = new HashSet<>();
        shakespeareTheatre.Beacons.add(new Beacon("Kasa", "Tu możesz kupić bilety.", "Jesteś na parterze", "Agnieszka"));
        shakespeareTheatre.Beacons.add(new Beacon("Toalety", "Toaleta dla niepełnosprawnych znajduje się na końcu korytarza", "Jesteś na poziomie -1", "Aurelia"));
        shakespeareTheatre.Beacons.add(new Beacon("Scena", "Główna scena teatru. Tu odbywają się koncerty.", "Jesteś na parterze", "Dominik"));
        shakespeareTheatre.Beacons.add(new Beacon("Wejście", "Główne wejście do budynku.", "Jesteś na parterze.", "robert"));

        currentPlace = shakespeareTheatre;
    }

    private void discoverDevices() {
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String deviceId  = device.getName() + " " + device.getAddress();
                    if (!newDevices.contains(deviceId)) {
                        newDevices.add(deviceId);
                        setCurrentBeacon(deviceId);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    devices.clear();
                    devices.addAll(newDevices);
                    newDevices.clear();
                    mBluetoothAdapter.startDiscovery();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();

    }

    private void setCurrentBeacon(String deviceId) {
        if (currentPlace == null) return;

        for (Beacon beacon : currentPlace.Beacons) {
            if (deviceId.contains(beacon.DeviceId) && currentBeacon != beacon) {
                currentBeacon = beacon;
                messanger.sendMessage(currentBeacon.Name);
                messanger.sendMessage(currentBeacon.Info);
            }
        }
    }


    private void viewDiscoveredBeacons() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            StringBuilder sb = new StringBuilder();
                            for (String s : devices) {
                                sb.append(s);
                                sb.append("\n");
                            }

                            TextView textView = (TextView) findViewById(R.id.mainTextView);
                            textView.setText(sb.toString());

                        } catch (Exception e) {
                            String m = e.getMessage();
                        }
                    }
                });
            }
        }, 10, 1000);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
