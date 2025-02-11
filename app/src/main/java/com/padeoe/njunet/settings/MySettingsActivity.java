package com.padeoe.njunet.settings;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.padeoe.nicservice.njuwlan.service.LoginService;
import com.padeoe.nicservice.njuwlan.service.OfflineQueryService;
import com.padeoe.njunet.App;
import com.padeoe.njunet.R;
import com.padeoe.njunet.connect.PermissionExplainFragment;
import com.padeoe.njunet.connect.StatusNotificationManager;
import com.padeoe.njunet.connect.controller.ConnectService;
import com.padeoe.njunet.util.PrefFileManager;

/**
 * Created by padeoe on 2016/5/26.
 */
public class MySettingsActivity extends AppCompatPreferenceActivity implements PermissionExplainFragment.PermissionHandler, Preference.OnPreferenceClickListener {
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case "timeout_portal":
                    LoginService.getInstance().setTimeout(Integer.parseInt(newValue.toString()));
                    break;
                case "times_relogin":
                    App.setMaxTry(Integer.parseInt(newValue.toString()));
                    break;
                case "notification_frequency":
                    if (newValue.toString().equals("1"))
                        StatusNotificationManager.removeNotification();
                    else
                        StatusNotificationManager.showStatus(Integer.parseInt(newValue.toString()));
                    break;
                case "account_edit":
                    preference.setSummary(PrefFileManager.getAccountPref().getString("username", null));
                    break;
            }
            String stringValue = newValue.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
        setupActionBar();

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("about")) {
            FragmentManager fm = getFragmentManager();
            new AboutDialogFragment().show(fm, "About_Dialog");
        }
        return true;
    }


    public static class MyPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_network);
            bindPreferenceSummaryToValue(findPreference("timeout_portal"));
            bindPreferenceSummaryToValue(findPreference("notification_frequency"));
            bindPreferenceSummaryToValue(findPreference("times_relogin"));
            bindPreferenceSummaryToValue(findPreference("account_edit"));
            findPreference("about").setOnPreferenceClickListener((MySettingsActivity) getActivity());
            findPreference("auto_connect").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        getActivity().startService(new Intent(getActivity(), ConnectService.class));
                    } else {
                        Intent stopServiceIntent = new Intent(getActivity(), ConnectService.class);
                        stopServiceIntent.setAction(ConnectService.STOP_SERVICE_ACTION);
                        getActivity().startService(stopServiceIntent);
                    }
                    return true;
                }
            });
            findPreference("scan_wifi").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.equals(true)) {
                        ((MySettingsActivity) getActivity()).mycheckPermission();
                    }
                    return true;
                }
            });
            findPreference("portal_server").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String newip = newValue.toString().trim();
                    LoginService.getInstance().setSettingsPortalIP(newip.equals("") ? null : newip);
                    return true;
                }
            });
            findPreference("bras_server").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String newip = newValue.toString().trim();
                    OfflineQueryService.setSettingsBrasIP(newip.equals("") ? null : newip);
                    return true;
                }
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            //补修改在第二屏中被修改的preferences，这在onCreate中没有捕获
            bindPreferenceSummaryToValue(findPreference("account_edit"));
        }
    }

    /**
     * Set up the {@link ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void mycheckPermission() {
        if (ContextCompat.checkSelfPermission(App.getAppContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionExplainFragment permissionExplainFragment = new PermissionExplainFragment();
            FragmentManager fragmentManager = this.getFragmentManager();
            permissionExplainFragment.show(fragmentManager, "请求定位权限");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            Toast.makeText(App.context, "未获得权限", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionExplained() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.finish();
        }
        return true;
    }

    /**
     * ListView事件响应：浏览器中打开github项目
     *
     * @param view
     */
    public void linkGithub(View view) {
        String githubURL = "https://github.com/padeoe/AutoConnect";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(githubURL));
        startActivity(intent);
    }
}
