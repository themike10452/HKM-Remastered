package lb.themike10452.hellscorekernelmanagerl.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lb.themike10452.hellscorekernelmanagerl.MainActivity;
import lb.themike10452.hellscorekernelmanagerl.R;
import lb.themike10452.hellscorekernelmanagerl.properties.HKMPropertyInterface;
import lb.themike10452.hellscorekernelmanagerl.properties.MultiCoreIntProperty;
import lb.themike10452.hellscorekernelmanagerl.properties.MultiCoreLongProperty;
import lb.themike10452.hellscorekernelmanagerl.properties.MultiLineValueProperty;
import lb.themike10452.hellscorekernelmanagerl.properties.MultiRootPathIntProperty;
import lb.themike10452.hellscorekernelmanagerl.properties.PropertyUtils;
import lb.themike10452.hellscorekernelmanagerl.properties.StringProperty;
import lb.themike10452.hellscorekernelmanagerl.properties.intProperty;
import lb.themike10452.hellscorekernelmanagerl.properties.longProperty;
import lb.themike10452.hellscorekernelmanagerl.utils.Library;
import lb.themike10452.hellscorekernelmanagerl.utils.Tools;

/**
 * Created by Mike on 2/22/2015.
 */
public class CPUControl extends Fragment implements HKMFragment {

    private static final String ERR_STR = "hkm_n_a";
    private static final int ERR_INT = -1;

    private static CPUControl instance;

    private Activity mActivity;
    private long[] available_freqs;
    private String[] available_governors;

    private MultiRootPathIntProperty maxCoresProperty;
    private MultiRootPathIntProperty minCoresProperty;
    private intProperty boostedCoresProperty;
    private intProperty boostDurationProperty;
    private intProperty screenoffMaxStateProperty;
    private intProperty screenoffSglCoreProperty;
    private MultiCoreIntProperty c0wfiProperty;
    private MultiCoreIntProperty c1retProperty;
    private MultiCoreIntProperty c2spcProperty;
    private MultiCoreIntProperty c3pcProperty;
    private MultiCoreLongProperty maxFreqProperty;
    private MultiCoreLongProperty minFreqProperty;
    private longProperty screenoffMaxProperty;
    private MultiLineValueProperty touchBoostProperty;
    private intProperty touchBoostStateProperty;
    private StringProperty governorProperty;

    private HKMPropertyInterface[] properties;

    private View mView;

    private View.OnClickListener listClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final HKMPropertyInterface property = PropertyUtils.findProperty(properties, v);

            if (property != null) {

                final List<String> choices = new ArrayList<>();

                if (property instanceof intProperty) {
                    int flags = ((intProperty) property).FLAGS;
                    if ((PropertyUtils.FLAG_CPU_CORES & flags) == PropertyUtils.FLAG_CPU_CORES) {
                        if ((PropertyUtils.FLAG_CPU_CORES_ALLOW_ZERO & flags) == PropertyUtils.FLAG_CPU_CORES_ALLOW_ZERO) {
                            choices.add("0");
                        }
                        for (int i = 1; i <= 4; i++) {
                            choices.add(Integer.toString(i));
                        }
                    }
                } else if (property instanceof StringProperty) {
                    if (property == governorProperty && available_governors != null) {
                        choices.addAll(Arrays.asList(available_governors));
                    }
                } else if (property instanceof longProperty) {
                    if (property == screenoffMaxProperty && available_freqs != null) {
                        for (long l : available_freqs) {
                            choices.add(Long.toString(l));
                        }
                    }
                }

                if (!choices.isEmpty()) {
                    final CharSequence[] listItems = choices.toArray(new CharSequence[choices.size()]);
                    new AlertDialog.Builder(mActivity)
                            .setCancelable(true)
                            .setItems(listItems, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    View disp = findViewById(property.getViewId()).findViewById(R.id.value);
                                    if (disp != null && disp instanceof TextView)
                                        ((TextView) disp).setText(listItems[which]);
                                }
                            })
                            .show();
                }
            }
        }
    };

    private View.OnClickListener pickerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            HKMPropertyInterface property = PropertyUtils.findProperty(properties, v);
            if (property != null) {
                final List<String> values = new ArrayList<>();
                for (int i = 0; i < available_freqs.length; i++) {
                    values.add(Long.toString(available_freqs[i]));
                }

                Point p = new Point();
                mActivity.getWindowManager().getDefaultDisplay().getSize(p);

                PopupWindow popupWindow = new PopupWindow(mActivity);
                popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                popupWindow.setHeight(1);
                popupWindow.setWidth(p.x);
                popupWindow.setFocusable(true);
                popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_window_background));
                popupWindow.setClippingEnabled(false);
                popupWindow.setElevation(10f);

                if (property == screenoffMaxProperty) {
                    View layout = ((LayoutInflater) mActivity
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                            .inflate(R.layout.simple_picker_layout, null, false);

                    popupWindow.setContentView(layout);
                    popupWindow.showAsDropDown(v, v.getWidth() - 300, 0);

                    final NumberPicker picker1 = (NumberPicker) layout.findViewById(R.id.numberPicker);
                    picker1.setMinValue(0);
                    picker1.setMaxValue(available_freqs.length - 1);
                    picker1.setValue(values.indexOf(((TextView) findViewById(property.getViewId()).findViewById(R.id.value)).getText().toString()));
                    picker1.setDisplayedValues(values.toArray(new String[values.size()]));
                    picker1.setWrapSelectorWheel(false);

                    popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            String newValue = values.get(picker1.getValue());
                            View disp = v.findViewById(R.id.value);
                            if (disp != null && disp instanceof TextView) {
                                ((TextView) disp).setText(newValue);
                            }
                        }
                    });

                } else if (property instanceof MultiLineValueProperty) {
                    View layout = ((LayoutInflater) mActivity
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                            .inflate(R.layout.quad_picker_layout, null, false);

                    popupWindow.setContentView(layout);
                    popupWindow.showAsDropDown(v);

                    LinearLayout parent = (LinearLayout) layout.findViewById((R.id.parent));
                    int count = parent.getChildCount();
                    final NumberPicker[] pickers = new NumberPicker[count];
                    String str = ((TextView) findViewById(property.getViewId()).findViewById(R.id.value)).getText().toString();
                    str = str.substring(1);
                    str = str.substring(0, str.length() - 1);
                    String[] currentValues = str.split(",");
                    for (int i = 0; i < count; i++) {
                        TextView title = (TextView) parent.getChildAt(i).findViewById(R.id.title);
                        title.setVisibility(View.VISIBLE);
                        title.setText("CPU" + i);
                        pickers[i] = (NumberPicker) parent.getChildAt(i).findViewById(R.id.numberPicker);
                        pickers[i].setMinValue(0);
                        pickers[i].setMaxValue(available_freqs.length - 1);
                        pickers[i].setValue(values.indexOf(currentValues[i].trim()));
                        pickers[i].setDisplayedValues(values.toArray(new String[values.size()]));
                        pickers[i].setWrapSelectorWheel(false);
                        pickers[i].setPadding(3, 0, 3, 0);
                    }

                    popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            String newValues = "";
                            for (NumberPicker picker : pickers)
                                newValues = newValues.concat(values.get(picker.getValue())).concat(" ");
                            newValues = newValues.trim();
                            touchBoostProperty.setDisplayedValue(Arrays.asList(newValues.split(" ")), v);
                        }
                    });
                }
            }
        }
    };

    public static CPUControl getInstance() {
        return instance != null ? instance : new CPUControl();
    }

    public CPUControl() {
        instance = this;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        governorProperty = new StringProperty(Library.GOV0, R.id.govBtn, ERR_STR);
        maxFreqProperty = new MultiCoreLongProperty(Library.MAX_FREQ_PATH, R.id.maxFreqHolder, ERR_INT);
        minFreqProperty = new MultiCoreLongProperty(Library.MIN_FREQ_PATH, R.id.minFreqHolder, ERR_INT);

        maxCoresProperty = new MultiRootPathIntProperty(R.id.maxCoresOnBtn, ERR_INT, Library.MAX_CPUS_ONLINE_PATH0, Library.MAX_CPUS_ONLINE_PATH1);
        minCoresProperty = new MultiRootPathIntProperty(R.id.minCoresOnBtn, ERR_INT, Library.MIN_CPUS_ONLINE_PATH0, Library.MIN_CPUS_ONLINE_PATH1);
        boostedCoresProperty = new intProperty(Library.BOOSTED_CPUS_PATH, R.id.boostedCoresBtn, ERR_INT);
        boostDurationProperty = new intProperty(Library.BOOST_LOCK_DURATION_PATH, R.id.boostDurationBtn, ERR_INT);
        screenoffMaxProperty = new longProperty(Library.SCREEN_OFF_MAX_FREQ, R.id.screenOffMaxBtn, ERR_INT);
        screenoffMaxStateProperty = new intProperty(Library.SCREEN_OFF_MAX_STATE, R.id.screenOffMaxStateSwitch, ERR_INT);
        screenoffSglCoreProperty = new intProperty(Library.SCREEN_OFF_SINGLE_CORE_PATH, R.id.screenOffSglCoreHolder, ERR_INT);
        touchBoostProperty = new MultiLineValueProperty(R.id.touchBoostBtn, Library.TOUCH_BOOST_FREQS_PATH, ERR_STR);
        touchBoostStateProperty = new intProperty(Library.TOUCH_BOOST_PATH, R.id.touchBoostSwitch, ERR_INT);

        c0wfiProperty = new MultiCoreIntProperty(Library.CPU_IDLE_C0_PATH, R.id.c0_switch, ERR_INT);
        c1retProperty = new MultiCoreIntProperty(Library.CPU_IDLE_C1_PATH, R.id.c1_switch, ERR_INT);
        c2spcProperty = new MultiCoreIntProperty(Library.CPU_IDLE_C2_PATH, R.id.c2_switch, ERR_INT);
        c3pcProperty = new MultiCoreIntProperty(Library.CPU_IDLE_C3_PATH, R.id.c3_switch, ERR_INT);

        governorProperty.FLAGS = PropertyUtils.FLAG_VIEW_COMBO;
        maxCoresProperty.FLAGS = PropertyUtils.FLAG_CPU_CORES;
        minCoresProperty.FLAGS = PropertyUtils.FLAG_CPU_CORES;
        boostedCoresProperty.FLAGS = PropertyUtils.FLAG_CPU_CORES | PropertyUtils.FLAG_CPU_CORES_ALLOW_ZERO;
        screenoffMaxProperty.FLAGS = PropertyUtils.FLAG_VIEW_COMBO;
        touchBoostProperty.FLAGS = PropertyUtils.FLAG_VIEW_COMBO;

        initProperties();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mView = inflater.inflate(R.layout.fragment_cpu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh();

        final SeekBar maxFreq = (SeekBar) findViewById(maxFreqProperty.getViewId()).findViewById(R.id.seekBar);
        final SeekBar minFreq = (SeekBar) findViewById(minFreqProperty.getViewId()).findViewById(R.id.seekBar);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == maxFreq) {
                    if (progress < minFreq.getProgress()) {
                        minFreq.setProgress(progress);
                    }
                    ((TextView) findViewById(maxFreqProperty.getViewId()).findViewById(R.id.value)).setText(Long.toString(available_freqs[progress]));
                } else {
                    if (progress > maxFreq.getProgress()) {
                        maxFreq.setProgress(progress);
                    }
                    ((TextView) findViewById(minFreqProperty.getViewId()).findViewById(R.id.value)).setText(Long.toString(available_freqs[progress]));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        maxFreq.setOnSeekBarChangeListener(listener);
        minFreq.setOnSeekBarChangeListener(listener);

        findViewById(governorProperty.getViewId()).setOnClickListener(listClickListener);
        findViewById(maxCoresProperty.getViewId()).setOnClickListener(listClickListener);
        findViewById(minCoresProperty.getViewId()).setOnClickListener(listClickListener);
        findViewById(boostedCoresProperty.getViewId()).setOnClickListener(listClickListener);

        findViewById(screenoffMaxProperty.getViewId()).setOnClickListener(pickerClickListener);
        findViewById(touchBoostProperty.getViewId()).setOnClickListener(pickerClickListener);

    }

    private void initProperties() {
        properties = new HKMPropertyInterface[]{
                governorProperty,
                maxFreqProperty,
                minFreqProperty,
                maxCoresProperty,
                minCoresProperty,
                boostedCoresProperty,
                boostDurationProperty,
                screenoffMaxStateProperty,
                screenoffMaxProperty,
                screenoffSglCoreProperty,
                touchBoostStateProperty,
                touchBoostProperty,
                c0wfiProperty,
                c1retProperty,
                c2spcProperty,
                c3pcProperty
        };
    }

    public void refresh() {
        new AsyncTask<Void, Void, Void>() {
            Object[] values;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mActivity.sendBroadcast(new Intent(MainActivity.ACTION_SHOW_TOUCH_BARRIER));
                values = new Object[properties.length];
            }

            @Override
            protected Void doInBackground(Void... params) {
                for (int i = 0; i < properties.length; i++) {
                    HKMPropertyInterface property = properties[i];
                    if (property instanceof intProperty) {
                        values[i] = ((intProperty) property).getValue();
                    } else if (property instanceof StringProperty) {
                        values[i] = ((StringProperty) property).getValue();
                    } else if (property instanceof longProperty) {
                        values[i] = ((longProperty) property).getValue();
                    } else if (property instanceof MultiLineValueProperty) {
                        values[i] = ((MultiLineValueProperty) property).getValue();
                    }
                }

                String tmp = Tools.getInstance().readLineFromFile(Library.AVAIL_FREQ_PATH);
                if (tmp != null) {
                    String[] freqs = tmp.split(" ");
                    available_freqs = new long[freqs.length];
                    try {
                        for (int i = 0; i < freqs.length; i++) {
                            available_freqs[i] = Long.parseLong(freqs[i]);
                        }
                    } catch (NumberFormatException e) {
                        available_freqs = null;
                    }
                }

                tmp = Tools.getInstance().readLineFromFile(Library.AVAIL_GOV_PATH);
                if (tmp != null) {
                    try {
                        available_governors = tmp.split(" ");
                    } catch (Exception e) {
                        available_governors = null;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                for (int i = 0; i < properties.length; i++) {
                    HKMPropertyInterface property = properties[i];
                    View parent = findViewById(property.getViewId());
                    try {
                        if ((PropertyUtils.FLAG_VIEW_COMBO & property.getFlags()) == PropertyUtils.FLAG_VIEW_COMBO)
                            parent = (View) parent.getParent();
                    } catch (Exception ignored) {
                    }

                    property.setDisplayedValue(values[i], parent);

                    if (property instanceof longProperty) {
                        long value = (long) values[i];
                        property.setDisplayedValue(values[i], parent);
                        View disp = parent.findViewById(R.id.seekBar);
                        if (disp != null && disp instanceof SeekBar) {
                            SeekBar seekBar = (SeekBar) disp;
                            if (available_freqs != null) {
                                if (property == maxFreqProperty || property == minFreqProperty) {
                                    seekBar.setMax(available_freqs.length - 1);
                                    seekBar.setProgress(indexOf(value, available_freqs));
                                }
                            }
                        }
                    }
                }

                mActivity.sendBroadcast(new Intent(MainActivity.ACTION_HIDE_TOUCH_BARRIER));
            }
        }.execute();

    }

    public void saveAll() {
        for (HKMPropertyInterface property : properties) {
            boolean isCombo = (PropertyUtils.FLAG_VIEW_COMBO & property.getFlags()) == PropertyUtils.FLAG_VIEW_COMBO;
            View holder = findViewById(property.getViewId());
            if (holder.getVisibility() == View.VISIBLE && (!isCombo || ((View) holder.getParent()).getVisibility() == View.VISIBLE)) {
                String value = property.readDisplayedValue(holder);
                if (value != null) {
                    if (property == touchBoostProperty) {
                        String str = value;
                        str = str.substring(1);
                        str = str.substring(0, str.length() - 1);
                        str = str.trim().replace(" ", "");
                        List<String> values = Arrays.asList(str.split(","));
                        List<String> prefixes = Arrays.asList("0", "1", "2", "3");
                        touchBoostProperty.setValue(values, prefixes, null);
                    } else {
                        property.setValue(value);
                    }
                }
            }
        }
    }

    private View findViewById(int id) {
        return mView.findViewById(id);
    }

    private int indexOf(long obj, long[] objs) {
        for (int i = 0; i < objs.length; i++) {
            if (objs[i] == obj)
                return i;
        }
        return -1;
    }
}
