package com.padeoe.njunet.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.padeoe.nicservice.njuwlan.object.portal.UserInfo;
import com.padeoe.njunet.App;

import com.padeoe.njunet.R;
import com.padeoe.njunet.connect.controller.ConnectManager;
import com.padeoe.njunet.connect.controller.ConnectService;
import com.padeoe.njunet.connect.controller.UpdateInfo;
import com.padeoe.njunet.connect.uihandler.ConnectResultHandle;
import com.padeoe.njunet.settings.MySettingsActivity;
import com.padeoe.njunet.util.MyAnimation;
import com.padeoe.njunet.util.MyObservable;
import com.padeoe.njunet.util.MyObserver;

public class MainActivity extends AppCompatActivity implements MyObserver<ConnectResultHandle> {
    public ConnectManager.Status status = ConnectManager.Status.ONLINE;
    public View online;
    public View offline;
    public View remoteOnline;
    public TextView netinfo;
    public TextView amount;
    public TextView time;
    public TextView location;
    public TextView ip;
    public ProgressBar progressBar;
    public AppBarLayout appBarLayout;
    BroadcastReceiver receiver;
    public TextView status_internet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        //开启后台连接服务
        if (PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("auto_connect", true)) {
            startService(new Intent(this, ConnectService.class));
        }
        //开启状态栏通知
        if (App.getShowNotification()) {
            StatusNotificationManager.showStatus();
        }
        //注册应用内后台连接的广播，接收到广播后更新界面
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectResultHandle infoHandle = null;
                switch (intent.getAction()) {
                    case ConnectManager.BACKGROUND_LOGIN_ACTION:
                        infoHandle = intent.getParcelableExtra("LOGIN_RESULT");
                        break;
                    case ConnectManager.WIFI_LOST_ACTION:
                        infoHandle = intent.getParcelableExtra("WIFI_LOST");
                        break;
                    case ConnectManager.UNKNOWNNET_ACTION:
                        infoHandle = intent.getParcelableExtra("NETWORK_INFO");
                        break;
                    case ConnectManager.AUTHFAIL_ACTION:
                        infoHandle = intent.getParcelableExtra("AUTH_ERROR");
                        break;
                    case ConnectManager.WIFI_AVAILABLE_ACTION:
                        infoHandle = intent.getParcelableExtra("NETINFO");
                        break;
                    default:
                        Log.e("unknown action", intent.getAction());
                }
                if (infoHandle != null) {
                    infoHandle.updateView(MainActivity.this);
                }
            }
        };
    }

    private void initUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress();
                ConnectManager connectManager = new ConnectManager();
                connectManager.addObserver(MainActivity.this);
                Log.d("login request", "triggered by button in MainActivity");
                connectManager.login();
            }
        });

        online = findViewById(R.id.status_iamge);
        offline = findViewById(R.id.status_image_noconnection);
        remoteOnline = findViewById(R.id.status_image_remote_online);
        netinfo = (TextView) findViewById(R.id.netinfo);
        amount = (TextView) findViewById(R.id.amount);
        time = (TextView) findViewById(R.id.time);
        location = (TextView) findViewById(R.id.location);
        ip = (TextView) findViewById(R.id.ip);
        status_internet = (TextView) findViewById(R.id.status_internet);
    }

    public void setNetInfo(UserInfo userInfo, String ssid) {
        if (ssid == null) {
            netinfo.setText(new SpannableString(getResources().getString(R.string.no_wifi)));
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(userInfo.getFullname()).append('(').append(userInfo.getUsername()).append(')').append("\n").append(ssid);
            String status_internet_str = builder.toString();
            SpannableString spannableString = new SpannableString(status_internet_str);
            spannableString.setSpan(new StyleSpan(Typeface.ITALIC), status_internet_str.indexOf(ssid), status_internet_str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new RelativeSizeSpan(0.7f), status_internet_str.indexOf(ssid), status_internet_str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            netinfo.setText(spannableString);
        }
    }

    public void setNetInfo(String title, String subtitle) {
        if (subtitle == null) {
            netinfo.setText(title);
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(title).append('\n').append(subtitle);
            String status_internet_str = builder.toString();
            SpannableString spannableString = new SpannableString(status_internet_str);
            spannableString.setSpan(new StyleSpan(Typeface.ITALIC), status_internet_str.indexOf('\n'), status_internet_str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new RelativeSizeSpan(0.7f), status_internet_str.indexOf('\n'), status_internet_str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            netinfo.setText(spannableString);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //BackgrdInfoHandleaction bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.disconnect) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                appBarLayout.setElevation(0f);
            }
            progressBar.setVisibility(View.VISIBLE);
            ConnectManager connectManager = new ConnectManager();
            connectManager.addObserver(MainActivity.this);
            connectManager.disconnectNow();
            return true;
        }
        if (id == R.id.settings) {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, MySettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 在MainActivity显示内容为string的Toast
     *
     * @param string 要显示的字符
     */
    public void showOnMainActivity(String string) {
        final String string2 = string;
        try {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(App.context, string2, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("showOnMainActivity", "Toast显示失败");
        }
    }

    public void updateViewStatus(ConnectManager.Status oldStatus, ConnectManager.Status newStatus) {
        System.out.println(oldStatus+" "+newStatus);
        status_internet.setText(ConnectManager.getInternetStatus());
        MyAnimation.fadeInTextView(status_internet);
        if (getStatusView(oldStatus) != getStatusView(newStatus)) {
            MyAnimation.flip(getStatusView(oldStatus), getStatusView(newStatus), 300);
            MyAnimation.updateBackground(appBarLayout, ConnectManager.getColor(oldStatus), ConnectManager.getColor(newStatus), 1000);
            this.status = newStatus;
        }
    }

    public View getStatusView(ConnectManager.Status status) {
        switch (status) {
            case ONLINE:
                return online;
            case OFFLINE:
                return offline;
            case WIFI_LOST:
                return offline;
            case DETECTING:
                return offline;
            case REMOTE_ONLINE:
                return remoteOnline;
        }
        return null;
    }


    public void initData() {
        showProgress();

        if (PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("login_on_activity_start", true)) {
            ConnectManager connectManager = new ConnectManager();
            connectManager.addObserver(this);
            connectManager.backgrConnect();
        } else {
            System.out.println("即将获得当前是否登陆的数据");
            UpdateInfo updateInfo = new UpdateInfo();
            updateInfo.addObserver(this);
            updateInfo.updateInfo();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();

    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectManager.BACKGROUND_LOGIN_ACTION);
        filter.addAction(ConnectManager.WIFI_LOST_ACTION);
        filter.addAction(ConnectManager.UNKNOWNNET_ACTION);
        filter.addAction(ConnectManager.WIFI_AVAILABLE_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    public void update(MyObservable myObservable, final ConnectResultHandle data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                data.updateView(MainActivity.this);
            }
        });
    }

    public void showProgress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(0f);
        }
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(5f);
        }
        progressBar.setVisibility(View.INVISIBLE);
    }
}
