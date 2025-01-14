/*
 *     TunProxy is a proxy forwarding tool using Android's VPNService.
 *     Copyright (C) 2021 raise.isayan@gmail.com / Karsten Ohme
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package tun.proxy;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.*;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import tun.proxy.di.DaggerWrapper;
import tun.proxy.service.SharedPrefService;

/**
 * Settings activity of the app.
 * @author <a href="mailto:raise.isayan@gmail.com">raise.isayan@gmail.com</a>
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TITLE_TAG = "Settings";

    public enum AppSortBy {APPNAME, PKGNAME};
    public enum AppOrderBy {ASC, DESC};

    @Inject
    SharedPrefService sharedPrefService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerWrapper.getComponent(this).inject(this);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_settings, new SettingsFragment(), "preference_root")
                .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.title_activity_settings);
            }
            }
        });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
        public static final String VPN_CONNECTION_MODE = "vpn_connection_mode";
        public static final String VPN_DISALLOWED_APPLICATION_LIST = "vpn_disallowed_application_list";
        public static final String VPN_ALLOWED_APPLICATION_LIST = "vpn_allowed_application_list";
        public static final String VPN_CLEAR_ALL_SELECTION = "vpn_clear_all_selection";

        @Inject
        SharedPrefService sharedPrefService;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            DaggerWrapper.getComponent(getContext()).inject(this);
            addPreferencesFromResource(R.xml.preferences);
            setHasOptionsMenu(true);

            /* Allowed / Disallowed Application */
            final ListPreference prefPackage = findPreference(VPN_CONNECTION_MODE);
            final PreferenceScreen prefDisallow = findPreference(VPN_DISALLOWED_APPLICATION_LIST);
            final PreferenceScreen prefAllow = findPreference(VPN_ALLOWED_APPLICATION_LIST);
            final PreferenceScreen clearAllSelection = findPreference(VPN_CLEAR_ALL_SELECTION);
            clearAllSelection.setOnPreferenceClickListener(this);

            prefPackage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                if (preference instanceof ListPreference) {
                    final ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue((String) value);
                    prefDisallow.setEnabled(index == SharedPrefService.VPNMode.DISALLOW.ordinal());
                    prefAllow.setEnabled(index ==  SharedPrefService.VPNMode.ALLOW.ordinal());

                    // Set the summary to reflect the new value.
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

                    SharedPrefService.VPNMode mode =  SharedPrefService.VPNMode.values()[index];
                    sharedPrefService.storeVPNMode(mode);
                }
                return true;
                }
            });
            prefPackage.setSummary(getResources().getStringArray(R.array.pref_vpn_connection_name)[
                    sharedPrefService.loadVPNMode().ordinal()]);
            prefDisallow.setEnabled(SharedPrefService.VPNMode.DISALLOW.name().equals(prefPackage.getValue()));
            prefAllow.setEnabled(SharedPrefService.VPNMode.ALLOW.name().equals(prefPackage.getValue()));
            updateMenuItem();
        }

        private void updateMenuItem() {
            final PreferenceScreen prefDisallow = findPreference(VPN_DISALLOWED_APPLICATION_LIST);
            final PreferenceScreen prefAllow = findPreference(VPN_ALLOWED_APPLICATION_LIST);

            int countDisallow = sharedPrefService.loadVPNApplication(SharedPrefService.VPNMode.DISALLOW).size();
            int countAllow = sharedPrefService.loadVPNApplication(SharedPrefService.VPNMode.ALLOW).size();
            prefDisallow.setTitle(getString(R.string.pref_header_disallowed_application_list, countDisallow));
            prefAllow.setTitle(getString(R.string.pref_header_disallowed_application_list, countAllow));
        }

        /*
         * https://developer.android.com/guide/topics/ui/settings/organize-your-settings
         */

        // リスナー部分
        @Override
        public boolean onPreferenceClick(Preference preference) {
            // keyを見てクリックされたPreferenceを特定
            switch (preference.getKey()) {
                case VPN_DISALLOWED_APPLICATION_LIST:
                case VPN_ALLOWED_APPLICATION_LIST:
                    break;
                case VPN_CLEAR_ALL_SELECTION:
                    new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.title_activity_settings))
                        .setMessage(getString(R.string.pref_dialog_clear_all_application_msg))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Set<String> set = new HashSet<>();
                                sharedPrefService.storeVPNApplication(SharedPrefService.VPNMode.ALLOW, set);
                                sharedPrefService.storeVPNApplication(SharedPrefService.VPNMode.DISALLOW,
                                        set);
                                updateMenuItem();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                    break;
            }
            return false;
        }

    }

    public static class DisallowedPackageListFragment extends PackageListFragment {
        public DisallowedPackageListFragment() {
            super(SharedPrefService.VPNMode.DISALLOW);
        }
    }

    public static class AllowedPackageListFragment extends PackageListFragment  {
        public AllowedPackageListFragment() {
            super(SharedPrefService.VPNMode.ALLOW);
        }
    }

    public static class PackageListFragment extends PreferenceFragmentCompat
            implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
        private final Map<String, Boolean> mAllPackageInfoMap = new HashMap<>();
        private final static String PREF_VPN_APPLICATION_ORDERBY = "pref_vpn_application_app_orderby";
        private final static String PREF_VPN_APPLICATION_FILTERBY = "pref_vpn_application_app_filterby";
        private final static String PREF_VPN_APPLICATION_SORTBY = "pref_vpn_application_app_sortby";

        private AsyncTaskProgress task;

        private SharedPrefService.VPNMode mode;
        private AppSortBy appSortBy = AppSortBy.APPNAME;
        private AppOrderBy appOrderBy = AppOrderBy.ASC;
        private AppSortBy appFilterBy = AppSortBy.APPNAME;
        private PreferenceScreen mFilterPreferenceScreen;

        @Inject
        SharedPrefService sharedPrefService;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            DaggerWrapper.getComponent(getContext()).inject(this);
        }

        public PackageListFragment(SharedPrefService.VPNMode mode) {
            super();
            this.mode = mode;
            this.task = new AsyncTaskProgress(this);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setHasOptionsMenu(true);
            mFilterPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(mFilterPreferenceScreen);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.menu_search, menu);

            final MenuItem menuSearch = menu.findItem(R.id.menu_search_item);
            this.searchView = (SearchView) menuSearch.getActionView();
            this.searchView.setOnQueryTextListener(this);
            this.searchView.setOnCloseListener(this);
            this.searchView.setSubmitButtonEnabled(false);

            switch (this.appOrderBy) {
                case ASC: {
                    final MenuItem menuItem = menu.findItem(R.id.menu_sort_order_asc);
                    menuItem.setChecked(true);
                    break;
                }
                case DESC: {
                    final MenuItem menuItem = menu.findItem(R.id.menu_sort_order_desc);
                    menuItem.setChecked(true);
                    break;
                }
            }

            switch (this.appFilterBy) {
                case APPNAME: {
                    final MenuItem menuItem = menu.findItem(R.id.menu_filter_app_name);
                    menuItem.setChecked(true);
                    break;
                }
                case PKGNAME: {
                    final MenuItem menuItem = menu.findItem(R.id.menu_filter_pkg_name);
                    menuItem.setChecked(true);
                    break;
                }
            }

            switch (this.appSortBy) {
                case APPNAME: {
                    final MenuItem menuItem = menu.findItem(R.id.menu_sort_app_name);
                    menuItem.setChecked(true);
                    break;
                }
                case PKGNAME: {
                    final MenuItem menuItem = menu.findItem(R.id.menu_sort_pkg_name);
                    menuItem.setChecked(true);
                    break;
                }
            }
        }

        private String searchFilter = "";
        private SearchView searchView;

        protected void filter(String filter) {
            this.filter(filter, this.appFilterBy, this.appOrderBy, this.appSortBy);
        }

        protected void filter(String filter, final AppSortBy filterBy, final AppOrderBy orderBy, final AppSortBy sortBy) {
            if (filter == null) {
                filter = searchFilter;
            } else {
                searchFilter = filter;
            }
            this.appFilterBy = filterBy;
            this.appOrderBy = orderBy;
            this.appSortBy = sortBy;

            Set<String> selected = this.getAllSelectedPackageSet();
            storeSelectedPackageSet(selected);

            this.removeAllPreferenceScreen();

            if (task != null && task.getStatus() == AsyncTask.Status.PENDING) {
                task.execute();
            }
            else {
                task = new AsyncTaskProgress(this);
                task.execute();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (this.task != null) {
                this.task.cancel(true);
                this.task = null;
            }

            Set<String> selected = this.getAllSelectedPackageSet();
            storeSelectedPackageSet(selected);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(PREF_VPN_APPLICATION_ORDERBY, this.appOrderBy.name());
            edit.putString(PREF_VPN_APPLICATION_FILTERBY, this.appFilterBy.name());
            edit.putString(PREF_VPN_APPLICATION_SORTBY, this.appSortBy.name());
            edit.apply();
        }

        @Override
        public void onResume() {
            super.onResume();
            Set<String> loadMap = sharedPrefService.loadVPNApplication(this.mode);
            for (String pkgName : loadMap) {
                this.mAllPackageInfoMap.put(pkgName, loadMap.contains(pkgName));
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String appOrderBy = prefs.getString(PREF_VPN_APPLICATION_ORDERBY, AppOrderBy.ASC.name());
            String appFilterBy = prefs.getString(PREF_VPN_APPLICATION_FILTERBY, AppSortBy.APPNAME.name());
            String appSortBy = prefs.getString(PREF_VPN_APPLICATION_SORTBY, AppSortBy.APPNAME.name());
            this.appOrderBy = Enum.valueOf(AppOrderBy.class, appOrderBy);
            this.appFilterBy = Enum.valueOf(AppSortBy.class, appFilterBy);
            this.appSortBy = Enum.valueOf(AppSortBy.class, appSortBy);
            filter(null);
        }

        private void removeAllPreferenceScreen() {
            this.mFilterPreferenceScreen.removeAll();
        }

        private Preference buildPackagePreferences(final PackageManager pm, final PackageInfo pi) {
            final CheckBoxPreference prefCheck = new CheckBoxPreference(getContext());
            prefCheck.setIcon(pi.applicationInfo.loadIcon(pm));
            prefCheck.setTitle(pi.applicationInfo.loadLabel(pm).toString());
            prefCheck.setSummary(pi.packageName);
            boolean checked = this.mAllPackageInfoMap.containsKey(pi.packageName) ? this.mAllPackageInfoMap.get(pi.packageName) : false;
            prefCheck.setChecked(checked);
            Preference.OnPreferenceClickListener click = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                mAllPackageInfoMap.put(prefCheck.getSummary().toString(), prefCheck.isChecked());
                return false;
                }
            };
            prefCheck.setOnPreferenceClickListener(click);
            return prefCheck;
        }

        private Set<String> getFilterSelectedPackageSet() {
            final Set<String> selected = new HashSet<>();
            for (int i = 0; i < this.mFilterPreferenceScreen.getPreferenceCount(); i++) {
                Preference pref = this.mFilterPreferenceScreen.getPreference(i);
                if ((pref instanceof CheckBoxPreference)) {
                    CheckBoxPreference prefCheck = (CheckBoxPreference) pref;
                    if (prefCheck.isChecked()) {
                        selected.add(prefCheck.getSummary().toString());
                    }
                }
            }
            return selected;
        }

        private void setSelectedPackageSet(Set<String> selected) {
            for (int i = 0; i < this.mFilterPreferenceScreen.getPreferenceCount(); i++) {
                Preference pref = this.mFilterPreferenceScreen.getPreference(i);
                if ((pref instanceof CheckBoxPreference)) {
                    CheckBoxPreference prefCheck = (CheckBoxPreference) pref;
                    if (selected.contains((prefCheck.getSummary()))) {
                        prefCheck.setChecked(true);
                    }
                }
            }
        }

        private void clearAllSelectedPackageSet() {
            final Set<String> selected = this.getFilterSelectedPackageSet();
            for (Map.Entry<String, Boolean> value : this.mAllPackageInfoMap
                    .entrySet()) {
                if (value.getValue()) {
                    value.setValue(false);
                }
            }
        }

        private Set<String> getAllSelectedPackageSet() {
            final Set<String> selected = this.getFilterSelectedPackageSet();
            for (Map.Entry<String, Boolean> value : this.mAllPackageInfoMap.entrySet()) {
                if (value.getValue()) {
                    selected.add(value.getKey());
                }
            }
            return selected;
        }

        private void storeSelectedPackageSet(final Set<String> set) {
            sharedPrefService.storeVPNMode(this.mode);
            sharedPrefService.storeVPNApplication(this.mode, set);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            switch (id) {
                case android.R.id.home:
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                    return true;
                case R.id.menu_sort_order_asc:
                    item.setChecked(!item.isChecked());
                    filter(null, appFilterBy, AppOrderBy.ASC, appSortBy);
                    break;
                case R.id.menu_sort_order_desc:
                    item.setChecked(!item.isChecked());
                    filter(null, appFilterBy, AppOrderBy.DESC, appSortBy);
                    break;
                case R.id.menu_filter_app_name:
                    item.setChecked(!item.isChecked());
                    this.appFilterBy = AppSortBy.APPNAME;
                    //filter(null, AppSortBy.APPNAME, appOrderBy, appSortBy);
                    break;
                case R.id.menu_filter_pkg_name:
                    item.setChecked(!item.isChecked());
                    this.appFilterBy = AppSortBy.PKGNAME;
                    //filter(null, AppSortBy.PKGNAME, appOrderBy, appSortBy);
                    break;
                case R.id.menu_sort_app_name:
                    item.setChecked(!item.isChecked());
                    filter(null, appFilterBy, appOrderBy, AppSortBy.APPNAME);
                    break;
                case R.id.menu_sort_pkg_name:
                    item.setChecked(!item.isChecked());
                    filter(null, appFilterBy, appOrderBy, AppSortBy.PKGNAME);
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            this.searchView.clearFocus();
            if (!query.trim().isEmpty()) {
                filter(query);
                return true;
            } else {
                filter("");
                return true;
            }
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }

        @Override
        public boolean onClose() {
            Set<String> selected = this.getAllSelectedPackageSet();
            storeSelectedPackageSet(selected);
            filter("");
            return false;
        }
    }

    /*
    * AsyncTask
    * https://developer.android.com/reference/android/os/AsyncTask
    * Deprecated in API level R
    * */
    public static class AsyncTaskProgress extends AsyncTask<String, String, List<PackageInfo>> {

        final PackageListFragment packageFragment;

        public AsyncTaskProgress(PackageListFragment packageFragment) {
            this.packageFragment = packageFragment;
        }

        @Override
        protected void onPreExecute() {
            packageFragment.mFilterPreferenceScreen.addPreference(new ProgressPreference(packageFragment.getContext()));
        }

        @Override
        protected List<PackageInfo> doInBackground(String... params) {
            return filterPackages(packageFragment.searchFilter, packageFragment.appFilterBy, packageFragment.appOrderBy, packageFragment.appSortBy);
        }

        private List<PackageInfo> filterPackages(String filter, final AppSortBy filterBy, final AppOrderBy orderBy, final AppSortBy sortBy) {
            final PackageManager pm = packageFragment.getContext().getPackageManager();
            final List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            Collections.sort(installedPackages, new Comparator<PackageInfo>() {
                @Override
                public int compare(PackageInfo o1, PackageInfo o2) {
                    String t1 = "";
                    String t2 = "";
                    switch (sortBy) {
                        case APPNAME:
                            t1 = o1.applicationInfo.loadLabel(pm).toString();
                            t2 = o2.applicationInfo.loadLabel(pm).toString();
                            break;
                        case PKGNAME:
                            t1 = o1.packageName;
                            t2 = o2.packageName;
                            break;
                    }
                    if (AppOrderBy.ASC.equals(orderBy))
                        return t1.compareTo(t2);
                    else
                        return t2.compareTo(t1);
                }
            });
            final Map<String, Boolean> installedPackageMap = new HashMap<>();
            for (final PackageInfo pi : installedPackages) {
                if (isCancelled()) continue;
                // exclude self package
                if (pi.packageName.equals(packageFragment.getContext().getPackageName())) {
                    continue;
                }
                boolean checked = packageFragment.mAllPackageInfoMap.containsKey(pi.packageName) ? packageFragment.mAllPackageInfoMap.get(pi.packageName) : false;
                installedPackageMap.put(pi.packageName, checked);
            }
            packageFragment.mAllPackageInfoMap.clear();
            packageFragment.mAllPackageInfoMap.putAll(installedPackageMap);
            return installedPackages;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> installedPackages) {
            final PackageManager pm = packageFragment.getContext().getPackageManager();
            packageFragment.mFilterPreferenceScreen.removeAll();
            for (final PackageInfo pi : installedPackages) {
                // exclude self package
                if (pi.packageName.equals(packageFragment.getContext().getPackageName())) {
                    continue;
                }
                String t1 = "";
                String t2 = packageFragment.searchFilter.trim();
                switch (packageFragment.appFilterBy) {
                    case APPNAME:
                        t1 = pi.applicationInfo.loadLabel(pm).toString();
                        break;
                    case PKGNAME:
                        t1 = pi.packageName;
                        break;
                }
                if (t2.isEmpty() || t1.toLowerCase().contains(t2.toLowerCase())) {
                    final Preference preference = packageFragment.buildPackagePreferences(pm, pi);
                    packageFragment.mFilterPreferenceScreen.addPreference(preference);
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            packageFragment.mAllPackageInfoMap.clear();
            packageFragment.mFilterPreferenceScreen.removeAll();
        }

    }

    protected static class ProgressPreference extends Preference {
        public ProgressPreference(Context context){
            super(context);
            setLayoutResource(R.layout.preference_progress);
        }
    }
}

