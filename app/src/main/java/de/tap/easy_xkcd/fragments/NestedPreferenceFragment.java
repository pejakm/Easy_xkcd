package de.tap.easy_xkcd.fragments;


import android.Manifest;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.tap.xkcd_reader.R;
import com.turhanoz.android.reactivedirectorychooser.event.OnDirectoryCancelEvent;
import com.turhanoz.android.reactivedirectorychooser.event.OnDirectoryChosenEvent;
import com.turhanoz.android.reactivedirectorychooser.ui.DirectoryChooserFragment;
import com.turhanoz.android.reactivedirectorychooser.ui.OnDirectoryChooserFragmentInteraction;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import de.tap.easy_xkcd.notifications.ComicListener;
import de.tap.easy_xkcd.utils.Comic;
import de.tap.easy_xkcd.utils.Favorites;
import de.tap.easy_xkcd.utils.PrefHelper;
import de.tap.easy_xkcd.Activities.MainActivity;
import de.tap.easy_xkcd.Activities.NestedSettingsActivity;
import de.tap.easy_xkcd.Activities.SettingsActivity;

public class NestedPreferenceFragment extends PreferenceFragment {
    private static final String APPEARANCE = "appearance";
    private static final String BEHAVIOR = "behavior";
    private static final String ALT_SHARING = "altSharing";
    private static final String ADVANCED = "advanced";
    private static final String NIGHT = "night";
    private static final String TAG_KEY = "NESTED_KEY";

    private static final String COLORED_NAVBAR = "pref_navbar";
    private static final String THEME = "pref_theme";
    private static final String NOTIFICATIONS_INTERVAL = "pref_notifications";
    private static final String ORIENTATION = "pref_orientation";
    private static final String FULL_OFFLINE = "pref_offline";
    private static final String WHATIF_OFFLINE = "pref_offline_whatif";
    private static final String NIGHT_THEME = "pref_night";
    private static final String AUTO_NIGHT = "pref_auto_night";
    private static final String AUTO_NIGHT_START = "pref_auto_night_start";
    private static final String AUTO_NIGHT_END = "pref_auto_night_end";
    private static final String REPAIR = "pref_repair";
    private static final String MOBILE_ENABLED = "pref_update_mobile";
    private static final String FAB_OPTIONS = "pref_random";
    private static final String OFFLINE_PATH_PREF = "pref_offline_path";

    private static final String OFFLINE_PATH = "/easy xkcd";
    private static final String OFFLINE_WHATIF_PATH = "/easy xkcd/what if/";
    private static final String OFFLINE_WHATIF_OVERVIEW_PATH = "/easy xkcd/what if/overview";



    public static boolean themeSettingChanged;

    public static NestedPreferenceFragment newInstance(String key) {
        NestedPreferenceFragment fragment = new NestedPreferenceFragment();
        // supply arguments to bundle.
        Bundle args = new Bundle();
        args.putString(TAG_KEY, key);
        fragment.setArguments(args);
        themeSettingChanged = false;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPreferenceResource();
    }

    private void checkPreferenceResource() {
        String key = getArguments().getString(TAG_KEY);
        assert key != null;
        // Load the preferences from an XML resource
        switch (key) {
            case APPEARANCE:
                addPreferencesFromResource(R.xml.pref_appearance);
                findPreference(COLORED_NAVBAR).setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                findPreference(COLORED_NAVBAR).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        themeSettingChanged = true;
                        return true;
                    }
                });
                findPreference(THEME).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        themeSettingChanged = true;
                        return true;
                    }
                });
                findPreference(FAB_OPTIONS).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        themeSettingChanged = true;
                        return true;
                    }
                });
                break;

            case BEHAVIOR:
                addPreferencesFromResource(R.xml.pref_behavior);
                findPreference(NOTIFICATIONS_INTERVAL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        int newValue = Integer.parseInt(o.toString());
                        if (newValue != 0) {
                            WakefulIntentService.scheduleAlarms(new ComicListener(), MainActivity.getInstance(), true);
                        } else {
                            WakefulIntentService.cancelAlarms(MainActivity.getInstance());
                        }
                        return true;
                    }
                });
                findPreference(ORIENTATION).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        switch (Integer.parseInt(PrefHelper.getOrientation())) {
                            case 1:
                                MainActivity.getInstance().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                                break;
                            case 2:
                                MainActivity.getInstance().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                break;
                            case 3:
                                MainActivity.getInstance().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                break;
                        }
                        return true;
                    }
                });
                findPreference(FULL_OFFLINE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean checked = Boolean.valueOf(newValue.toString());
                        if (checked) {
                            if (PrefHelper.isOnline(getActivity())) {
                                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                    new downloadComicsTask().execute();
                                    return true;
                                } else {
                                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                                    return false;
                                }
                            } else {
                                Toast.makeText(getActivity(), R.string.no_connection, Toast.LENGTH_SHORT).show();
                                return false;
                            }

                        } else {
                            android.support.v7.app.AlertDialog.Builder mDialog = new android.support.v7.app.AlertDialog.Builder(getActivity(), PrefHelper.getDialogTheme());
                            mDialog.setMessage(R.string.delete_offline_dialog)
                                    .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            getActivity().finish();
                                            PrefHelper.setFullOffline(true);
                                        }
                                    })
                                    .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                                new deleteComicsTask().execute();
                                            } else {
                                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                                            }
                                        }
                                    })
                                    .setCancelable(false);
                            mDialog.show();
                            return true;
                        }
                    }
                });
                findPreference(WHATIF_OFFLINE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean checked = Boolean.valueOf(newValue.toString());
                        if (checked) {
                            if (PrefHelper.isOnline(getActivity())) {
                                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                    new downloadArticlesTask().execute();
                                    return true;
                                } else {
                                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
                                    return false;
                                }
                            } else {
                                Toast.makeText(getActivity(), R.string.no_connection, Toast.LENGTH_SHORT).show();
                                return false;
                            }

                        } else {
                            android.support.v7.app.AlertDialog.Builder mDialog = new android.support.v7.app.AlertDialog.Builder(getActivity(), PrefHelper.getDialogTheme());
                            mDialog.setMessage(R.string.delete_offline_whatif_dialog)
                                    .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            getActivity().finish();
                                            PrefHelper.setFullOfflineWhatIf(true);
                                        }
                                    })
                                    .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                                new deleteArticlesTask().execute();
                                            } else {
                                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                                            }
                                        }
                                    })
                                    .setCancelable(false);
                            mDialog.show();
                            return true;
                        }
                    }
                });
                break;

            case NIGHT:
                addPreferencesFromResource(R.xml.pref_night);
                final Preference start = findPreference(AUTO_NIGHT_START);
                final Preference end = findPreference(AUTO_NIGHT_END);
                final int[] startTime = PrefHelper.getAutoNightStart();
                final int[] endTime = PrefHelper.getAutoNightEnd();
                start.setSummary(PrefHelper.getStartSummary());
                end.setSummary(PrefHelper.getEndSummary());



                findPreference(NIGHT_THEME).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        PrefHelper.setNightMode(Boolean.valueOf(newValue.toString()));
                        themeSettingChanged = true;
                        return true;
                    }
                });
                findPreference(AUTO_NIGHT).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        themeSettingChanged = true;
                        return true;
                    }
                });
                start.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        TimePickerDialog tpd = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                                PrefHelper.setAutoNightStart(new int[]{hourOfDay, minute});
                                themeSettingChanged = true;
                                start.setSummary(PrefHelper.getStartSummary());
                            }
                        }, startTime[0], startTime[1], android.text.format.DateFormat.is24HourFormat(getActivity()));
                        tpd.show();
                        return true;
                    }
                });
                end.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        TimePickerDialog tpd = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                                PrefHelper.setAutoNightEnd(new int[]{hourOfDay, minute});
                                themeSettingChanged = true;
                                end.setSummary(PrefHelper.getEndSummary());
                            }
                        }, endTime[0], endTime[1], android.text.format.DateFormat.is24HourFormat(getActivity()));
                        tpd.show();
                        return true;
                    }
                });
                break;

            case ALT_SHARING:
                addPreferencesFromResource(R.xml.pref_alt_sharing);
                break;

            case ADVANCED:
                addPreferencesFromResource(R.xml.pref_advanced);
                findPreference(REPAIR).setEnabled(MainActivity.fullOffline);
                findPreference(MOBILE_ENABLED).setEnabled(MainActivity.fullOffline | MainActivity.fullOfflineWhatIf);
                findPreference(OFFLINE_PATH_PREF).setEnabled(MainActivity.fullOffline | MainActivity.fullOfflineWhatIf);

                findPreference(REPAIR).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (PrefHelper.isOnline(getActivity())) {
                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                new repairComicsTask().execute();
                            } else {
                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5);
                            }
                        } else {
                            Toast.makeText(getActivity(), R.string.no_connection, Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
                findPreference(OFFLINE_PATH_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference){
                        DialogFragment directoryChooserFragment = DirectoryChooserFragment.newInstance(Environment.getExternalStorageDirectory());

                        FragmentTransaction transaction = ((NestedSettingsActivity) getActivity()).getManger().beginTransaction();
                        directoryChooserFragment.show(transaction, "RDC");
                        return true;
                    }
                });
                break;
        }
    }

    public class repairComicsTask extends AsyncTask<Void, Integer, Void> {
        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(getResources().getString(R.string.loading_offline));
            progress.setMessage(getResources().getString(R.string.loading_offline_message));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            int newest;
            try {
                newest = new Comic(0).getComicNumber();
                PrefHelper.setNewestComic(newest);
                PrefHelper.setHighestOffline(newest);
            } catch (Exception e) {
                newest = PrefHelper.getNewest();
            }
            Bitmap mBitmap = null;
            for (int i = 1; i <= newest; i++) {
                Log.d("i", String.valueOf(i));
                try {
                    FileInputStream fis = getActivity().openFileInput(String.valueOf(i));
                    //mBitmap = BitmapFactory.decodeStream(fis);
                    fis.close();
                } catch (Exception e) {
                    Log.e("error", "not found in internal");
                    try {
                        File sdCard = PrefHelper.getOfflinePath();
                        File dir = new File(sdCard.getAbsolutePath() + OFFLINE_PATH);
                        File file = new File(dir, String.valueOf(i) + ".png");
                        FileInputStream fis = new FileInputStream(file);
                        mBitmap = BitmapFactory.decodeStream(fis);
                        fis.close();
                    } catch (Exception e2) {
                        Log.e("error", "not found in external");
                        redownloadComic(i);
                    }
                }
                int p = (int) (i / ((float) newest) * 100);
                publishProgress(p);
            }

            return null;
        }

        private void redownloadComic(int i) {
            try {
                Comic comic = new Comic(i, getActivity());
                String url = comic.getComicData()[2];
                Bitmap mBitmap = Glide.with(getActivity())
                        .load(url)
                        .asBitmap()
                        .into(-1, -1)
                        .get();
                try {
                    File sdCard = PrefHelper.getOfflinePath();
                    File dir = new File(sdCard.getAbsolutePath() + OFFLINE_PATH);
                    dir.mkdirs();
                    File file = new File(dir, String.valueOf(i) + ".png");
                    FileOutputStream fos = new FileOutputStream(file);
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    Log.e("Error", "Saving to external storage failed");
                    try {
                        FileOutputStream fos = getActivity().openFileOutput(String.valueOf(i), Context.MODE_PRIVATE);
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
                PrefHelper.addTitle(comic.getComicData()[0], i);
                PrefHelper.addAlt(comic.getComicData()[1], i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected void onProgressUpdate(Integer... pro) {
            progress.setProgress(pro[0]);
        }

        @Override
        protected void onPostExecute(Void dummy) {
            progress.dismiss();
            MainActivity.getInstance().finish();
            SettingsActivity.getInstance().finish();
            getActivity().finish();
            startActivity(MainActivity.getInstance().getIntent());
        }
    }

    public class downloadComicsTask extends AsyncTask<Void, Integer, Void> {
        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(getResources().getString(R.string.loading_offline));
            progress.setMessage(getResources().getString(R.string.loading_offline_message));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 1; i <= ComicBrowserFragment.sNewestComicNumber; i++) {
                Log.d("i", String.valueOf(i));
                try {
                    Comic comic = new Comic(i, getActivity());
                    String url = comic.getComicData()[2];
                    Bitmap mBitmap = Glide.with(getActivity())
                            .load(url)
                            .asBitmap()
                            .into(-1, -1)
                            .get();
                    try {
                        File sdCard = PrefHelper.getOfflinePath();
                        File dir = new File(sdCard.getAbsolutePath() + OFFLINE_PATH);
                        dir.mkdirs();
                        File file = new File(dir, String.valueOf(i) + ".png");
                        FileOutputStream fos = new FileOutputStream(file);
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        Log.e("Error", "Saving to external storage failed");
                        try {
                            FileOutputStream fos = getActivity().openFileOutput(String.valueOf(i), Context.MODE_PRIVATE);
                            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            fos.close();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }

                    PrefHelper.addTitle(comic.getComicData()[0], i);
                    PrefHelper.addAlt(comic.getComicData()[1], i);
                    int p = (int) (i / ((float) ComicBrowserFragment.sNewestComicNumber) * 100);
                    publishProgress(p);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            PrefHelper.setHighestOffline(ComicBrowserFragment.sNewestComicNumber);
            return null;
        }

        protected void onProgressUpdate(Integer... pro) {
            progress.setProgress(pro[0]);
            switch (pro[0]) {
                case 2:
                    progress.setMessage(getResources().getString(R.string.loading_offline_2));
                    break;
                case 20:
                    progress.setMessage(getResources().getString(R.string.loading_offline_20));
                    break;
                case 50:
                    progress.setMessage(getResources().getString(R.string.loading_offline_50));
                    break;
                case 80:
                    progress.setMessage(getResources().getString(R.string.loading_offline_80));
                    break;
                case 95:
                    progress.setMessage(getResources().getString(R.string.loading_offline_95));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage(getResources().getString(R.string.loading_offline_96));
                        }
                    }, 1000);
                    break;
                case 97:
                    progress.setMessage(getResources().getString(R.string.loading_offline_97));
                    break;
            }
        }

        @Override
        protected void onPostExecute(Void dummy) {
            progress.dismiss();
            MainActivity.getInstance().finish();
            SettingsActivity.getInstance().finish();
            getActivity().finish();
            startActivity(MainActivity.getInstance().getIntent());
        }
    }

    public class deleteComicsTask extends AsyncTask<Void, Integer, Void> {
        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(getResources().getString(R.string.delete_offline));
            progress.setMessage(getResources().getString(R.string.loading_offline_message));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            int newest = PrefHelper.getNewest();
            for (int i = 1; i <= newest; i++) {
                if (!Favorites.checkFavorite(MainActivity.getInstance(), i)) {
                    //delete from internal storage
                    getActivity().deleteFile(String.valueOf(i));
                    //delete from external storage
                    File sdCard = PrefHelper.getOfflinePath();
                    File dir = new File(sdCard.getAbsolutePath() + OFFLINE_PATH);
                    File file = new File(dir, String.valueOf(i) + ".png");
                    file.delete();

                    int p = (int) (i / ((float) newest) * 100);
                    publishProgress(p);
                }
            }
            PrefHelper.deleteTitleAndAlt(newest, getActivity());

            PrefHelper.setHighestOffline(0);

            return null;
        }

        protected void onProgressUpdate(Integer... pro) {
            progress.setProgress(pro[0]);
        }

        @Override
        protected void onPostExecute(Void dummy) {
            progress.dismiss();
            MainActivity.getInstance().finish();
            SettingsActivity.getInstance().finish();
            getActivity().finish();
            startActivity(MainActivity.getInstance().getIntent());
        }
    }

    public class downloadArticlesTask extends AsyncTask<Void, Integer, Void> {
        private ProgressDialog progress;
        private Document doc;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(getResources().getString(R.string.loading_articles));
            progress.setMessage(getResources().getString(R.string.loading_offline_message));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Bitmap mBitmap;
            File sdCard = PrefHelper.getOfflinePath();
            File dir;
            //download overview
            try {
                doc = Jsoup.connect("https://what-if.xkcd.com/archive/")
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.19 Safari/537.36")
                        .get();
                StringBuilder sb = new StringBuilder();
                Elements titles = doc.select("h1");
                PrefHelper.setNewestWhatif(titles.size());

                sb.append(titles.first().text());
                titles.remove(0);
                for (Element title : titles) {
                    sb.append("&&");
                    sb.append(title.text());
                }
                PrefHelper.setWhatIfTitles(sb.toString());

                Elements img = doc.select("img.archive-image");
                int count = 1;
                for (Element image : img) {
                    String url = image.absUrl("src");
                    try {
                        mBitmap = Glide.with(getActivity())
                                .load(url)
                                .asBitmap()
                                .into(-1, -1)
                                .get();
                        dir = new File(sdCard.getAbsolutePath() + OFFLINE_WHATIF_OVERVIEW_PATH);
                        dir.mkdirs();
                        File file = new File(dir, String.valueOf(count) + ".png");
                        FileOutputStream fos = new FileOutputStream(file);
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d("count", String.valueOf(count));
                    int p = (int) (count / ((float) img.size()) * 100);
                    publishProgress(p);
                    count++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            //download html
            for (int i = 1; i <= PrefHelper.getNewestWhatIf(); i++) {
                int size = PrefHelper.getNewestWhatIf();
                try {
                    doc = Jsoup.connect("https://what-if.xkcd.com/" + String.valueOf(i)).get();
                    dir = new File(sdCard.getAbsolutePath() + OFFLINE_WHATIF_PATH + String.valueOf(i));
                    dir.mkdirs();
                    File file = new File(dir, String.valueOf(i) + ".html");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(doc.outerHtml());
                    writer.close();
                    //download images
                    int count = 1;
                    for (Element e : doc.select(".illustration")) {
                        try {
                            String url = "http://what-if.xkcd.com" + e.attr("src");
                            mBitmap = Glide.with(getActivity())
                                    .load(url)
                                    .asBitmap()
                                    .into(-1, -1)
                                    .get();
                            dir = new File(sdCard.getAbsolutePath() + OFFLINE_WHATIF_PATH + String.valueOf(i));
                            dir.mkdirs();
                            file = new File(dir, String.valueOf(count) + ".png");
                            FileOutputStream fos = new FileOutputStream(file);
                            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            fos.flush();
                            fos.close();
                            count++;
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                    int p = (int) (i / ((float) size) * 100);
                    publishProgress(p);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        protected void onProgressUpdate(Integer... pro) {
            progress.setProgress(pro[0]);
        }

        @Override
        protected void onPostExecute(Void dummy) {
            progress.dismiss();
            MainActivity.getInstance().finish();
            SettingsActivity.getInstance().finish();
            getActivity().finish();
            startActivity(MainActivity.getInstance().getIntent());
        }
    }

    public class deleteArticlesTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(getResources().getString(R.string.delete_offline_articles));
            progress.setMessage(getResources().getString(R.string.loading_offline_message));
            progress.setIndeterminate(true);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            File sdCard = PrefHelper.getOfflinePath();
            File dir = new File(sdCard.getAbsolutePath() + OFFLINE_WHATIF_PATH);
            deleteFolder(dir);
            return null;
        }

        @Override
        protected void onPostExecute(Void dummy) {
            progress.dismiss();
            MainActivity.getInstance().finish();
            SettingsActivity.getInstance().finish();
            getActivity().finish();
            startActivity(MainActivity.getInstance().getIntent());
        }
    }

    private void deleteFolder(File file) {
        if (file.isDirectory())
            for (File child : file.listFiles())
                deleteFolder(child);
        file.delete();
    }

}