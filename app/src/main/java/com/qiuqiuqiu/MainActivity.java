package com.qiuqiuqiu;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import android.content.ActivityNotFoundException;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 12;
    private static final int CREATE_BACKUP = 31;
    private static final int IMPORT_BACKUP = 32;
    private static final int RECORD_AUDIO_PERMISSION = 41;
    private static final int SPEECH_INPUT = 42;
    private static final int SPEECH_PERMISSION = 43;
    private static final int CUSTOM_CRYSTAL = -1;
    private static final int CUSTOM_BELL = -2;
    private static final int CUSTOM_BIRD = -3;
    private static final int CUSTOM_CAT = -4;
    private static final int CUSTOM_DOG = -5;
    private static final int CUSTOM_CHIME = -6;
    private static final int CUSTOM_COIN = -7;
    private static final int CUSTOM_POP = -8;
    private static final int CUSTOM_BUBBLE = -9;
    private static final int CUSTOM_FROG = -10;
    private static final int CUSTOM_SHEEP = -11;
    private static final int CUSTOM_RECORD = -12;
    private static final String[] SOUND_NAMES = {"清脆提示", "确认提示", "柔和提示", "低沉提示", "数字滴声", "短促滴声", "水晶清脆", "铃铛清脆", "星星叮咚", "金币轻响", "软糖弹跳", "泡泡轻点", "小鸟啾啾", "小猫喵喵", "小狗汪汪", "青蛙呱呱", "小羊咩咩", "我的录音"};
    private static final int[] SOUND_TONES = {
            ToneGenerator.TONE_PROP_BEEP,
            ToneGenerator.TONE_PROP_ACK,
            ToneGenerator.TONE_PROP_PROMPT,
            ToneGenerator.TONE_PROP_NACK,
            ToneGenerator.TONE_DTMF_1,
            ToneGenerator.TONE_DTMF_5,
            CUSTOM_CRYSTAL,
            CUSTOM_BELL,
            CUSTOM_CHIME,
            CUSTOM_COIN,
            CUSTOM_POP,
            CUSTOM_BUBBLE,
            CUSTOM_BIRD,
            CUSTOM_CAT,
            CUSTOM_DOG,
            CUSTOM_FROG,
            CUSTOM_SHEEP,
            CUSTOM_RECORD
    };

    private final int yellow = Color.rgb(255, 216, 64);
    private final int green = Color.rgb(47, 125, 104);
    private final int red = Color.rgb(214, 82, 72);
    private final int ink = Color.rgb(38, 45, 52);
    private final int muted = Color.rgb(103, 112, 122);
    private final int bg = Color.rgb(250, 250, 248);
    private final int soft = Color.rgb(244, 244, 242);
    private final int line = Color.rgb(232, 232, 228);

    private DbHelper db;
    private FrameLayout content;
    private SharedPreferences prefs;
    private ToneGenerator tone;
    private MediaRecorder recorder;
    private MediaPlayer customPlayer;
    private SpeechRecognizer aiSpeechRecognizer;
    private boolean aiSpeechListening;
    private boolean aiVoiceRecording;
    private File lastAiVoiceFile;
    private File currentRecordingFile;
    private String petAction = "idle";
    private long petActionUntil;

    private String currentType = "expense";
    private long selectedCategory;
    private String selectedCategoryName = "";
    private String selectedCategoryIcon = "";
    private long selectedAccount;
    private long selectedTargetAccount;
    private long selectedTime;
    private Calendar visibleMonth;
    private Calendar visibleDay;
    private boolean listDayMode;
    private String statsType = "expense";
    private String statsPeriod = "month";
    private String listKeyword;
    private String listType;
    private String currentPage = "home";

    private final ArrayList<String> pendingImages = new ArrayList<>();
    private EditText amountInput;
    private EditText noteInput;
    private TextView amountDisplay;
    private TextView timeText;
    private TextView imageHint;
    private long editingTransactionId;
    private boolean editingReimburse;
    private final ArrayList<String> aiMessages = new ArrayList<>();
    private AiDraft aiDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
        visibleMonth = Calendar.getInstance();
        visibleDay = Calendar.getInstance();
        selectedTime = System.currentTimeMillis();
        selectedAccount = firstId(db.accounts());
        showShell();
        registerSystemBackHandler();
        showHome();
    }

    @Override
    protected void onDestroy() {
        if (tone != null) {
            tone.release();
            tone = null;
        }
        stopRecording(false);
        releaseCustomPlayer();
        stopAiVoiceRecording(false);
        if (aiSpeechRecognizer != null) {
            aiSpeechRecognizer.destroy();
            aiSpeechRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void registerSystemBackHandler() {
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            handleBackNavigation();
                        }
                    }
            );
        }
    }

    private void handleBackNavigation() {
        if (!"home".equals(currentPage)) {
            showHome();
            return;
        }
        moveTaskToBack(true);
    }

    private void showShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(6), dp(8), dp(8));
        nav.setBackgroundColor(Color.WHITE);
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(70)));
        addNav(nav, "明细", this::showList);
        addNav(nav, "图表", this::showStats);
        Button add = new Button(this);
        add.setText("+");
        add.setTextSize(30);
        add.setTextColor(ink);
        add.setBackgroundColor(yellow);
        add.setAllCaps(false);
        touchable(add);
        add.setOnClickListener(v -> openNewTransaction());
        nav.addView(add, new LinearLayout.LayoutParams(0, -1, 1.25f));
        addNav(nav, "宠物", this::showPet);
        addNav(nav, "我的", this::showMine);
        setContentView(root);
    }

    private void addNav(LinearLayout nav, String text, Runnable action) {
        TextView tv = label(text, 14, ink, false);
        tv.setGravity(Gravity.CENTER);
        touchable(tv);
        tv.setOnClickListener(v -> action.run());
        nav.addView(tv, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void showHome() {
        currentPage = "home";
        LinearLayout page = page("");
        long[] month = monthRange(visibleMonth);
        long income = db.sum("income", month[0], month[1]);
        long expense = db.sum("expense", month[0], month[1]);
        long budget = db.budget(monthKey(visibleMonth));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(12), dp(18), dp(18));
        hero.setBackgroundColor(yellow);
        TextView title = label("小秋记账", 30, ink, true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title, new LinearLayout.LayoutParams(-1, dp(54)));
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        TextView monthText = label(monthKey(visibleMonth).replace("-", "年") + "月", 20, ink, true);
        monthText.setGravity(Gravity.CENTER_VERTICAL);
        touchable(monthText);
        monthText.setOnClickListener(v -> pickMonth(this::showHome));
        summary.addView(monthText, new LinearLayout.LayoutParams(0, dp(82), 1));
        summary.addView(headerMetric("收入", income), new LinearLayout.LayoutParams(0, dp(82), 1));
        summary.addView(headerMetric("支出", expense), new LinearLayout.LayoutParams(0, dp(82), 1));
        hero.addView(summary);
        if (budget > 0) hero.addView(label("预算 " + money(budget) + "  剩余 " + money(budget - expense), 14, ink, false));
        page.addView(hero);

        LinearLayout tools = card();
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.addView(shortcut("账单", "明", this::showList), new LinearLayout.LayoutParams(0, dp(86), 1));
        tools.addView(shortcut("AI记账", "AI", this::showAiAssistant), new LinearLayout.LayoutParams(0, dp(86), 1));
        tools.addView(shortcut("资产", "资", this::showMine), new LinearLayout.LayoutParams(0, dp(86), 1));
        tools.addView(shortcut("图表", "图", this::showStats), new LinearLayout.LayoutParams(0, dp(86), 1));
        page.addView(tools);

        page.addView(sectionTitle("本月账单"));
        renderFeed(page, month[0], month[1], null, null);
        setPage(page);
    }

    private void openNewTransaction() {
        pendingImages.clear();
        currentType = "expense";
        editingTransactionId = 0;
        editingReimburse = false;
        selectedTime = System.currentTimeMillis();
        selectedTargetAccount = 0;
        selectedCategory = 0;
        selectedCategoryName = "";
        selectedCategoryIcon = "";
        amountInput = null;
        noteInput = null;
        showCategoryPicker();
    }

    private void showCategoryPicker() {
        currentPage = "entry";
        LinearLayout page = page("");
        page.setBackgroundColor(Color.WHITE);
        LinearLayout tabs = topTabs("取消", this::showHome);
        addPickerTypeTab(tabs, "支出", "expense");
        addPickerTypeTab(tabs, "收入", "income");
        addPickerTypeTab(tabs, "转账", "transfer");
        page.addView(tabs);

        if ("transfer".equals(currentType)) {
            selectedCategoryName = "转账";
            selectedCategoryIcon = "转";
            showEntryForm();
            return;
        }

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        for (DbHelper.Item item : db.categories(currentType)) addCategoryTile(grid, item);
        addCategoryAddTile(grid);
        page.addView(grid);
        setPage(page);
    }

    private void showEntryForm() {
        currentPage = "entry";
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(0, statusBarHeight(), 0, 0);
        root.addView(entryHeader());

        amountInput = new EditText(this);
        amountInput.setVisibility(View.GONE);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, new LinearLayout.LayoutParams(1, 1));

        ScrollView bodyScroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(12), dp(16), dp(10));
        bodyScroll.addView(body);

        LinearLayout category = card();
        category.setGravity(Gravity.CENTER);
        TextView selected = label(selectedCategoryIcon + "  " + selectedCategoryName, 22, ink, true);
        selected.setGravity(Gravity.CENTER);
        category.addView(selected, new LinearLayout.LayoutParams(-1, dp(58)));
        body.addView(category);

        body.addView(sectionTitle("账户"));
        body.addView(accountScroller(false));
        if ("transfer".equals(currentType)) {
            if (selectedTargetAccount == 0) selectedTargetAccount = firstDifferentAccount(selectedAccount);
            body.addView(sectionTitle("转入账户"));
            body.addView(accountScroller(true));
        }
        imageHint = label(pendingImages.isEmpty() ? "未添加图片" : "已添加 " + pendingImages.size() + " 张图片", 12, muted, false);
        imageHint.setPadding(dp(2), dp(10), 0, 0);
        body.addView(imageHint);
        root.addView(bodyScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(Color.WHITE);
        bottom.setPadding(dp(16), dp(8), dp(16), 0);

        amountDisplay = label("0.00", 38, ink, false);
        amountDisplay.setGravity(Gravity.RIGHT);
        amountDisplay.setPadding(0, 0, dp(4), 0);
        bottom.addView(amountDisplay, new LinearLayout.LayoutParams(-1, dp(58)));

        LinearLayout noteBar = new LinearLayout(this);
        noteBar.setOrientation(LinearLayout.HORIZONTAL);
        noteBar.setGravity(Gravity.CENTER_VERTICAL);
        noteBar.setBackground(round(soft, dp(4)));
        noteInput = new EditText(this);
        noteInput.setHint("备注：点击填写备注");
        noteInput.setSingleLine(true);
        noteInput.setBackgroundColor(Color.TRANSPARENT);
        noteInput.setPadding(dp(12), 0, dp(12), 0);
        noteBar.addView(noteInput, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button image = outlineButton("图片");
        image.setOnClickListener(v -> pickImage());
        noteBar.addView(image, new LinearLayout.LayoutParams(dp(76), dp(42)));
        Button reimb = outlineButton("待报销");
        if (editingReimburse) {
            reimb.setSelected(true);
            reimb.setText("已标记");
        }
        reimb.setOnClickListener(v -> {
            reimb.setSelected(!reimb.isSelected());
            reimb.setText(reimb.isSelected() ? "已标记" : "待报销");
        });
        noteBar.addView(reimb, new LinearLayout.LayoutParams(dp(82), dp(42)));
        bottom.addView(noteBar, new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setPadding(0, dp(6), 0, dp(6));
        timeText = quickChip(shortDateLabel(selectedTime));
        timeText.setOnClickListener(v -> pickDateTime());
        quick.addView(timeText, new LinearLayout.LayoutParams(0, dp(44), 1));
        bottom.addView(quick);
        bottom.addView(keypad(reimb));
        root.addView(bottom, new LinearLayout.LayoutParams(-1, -2));
        setContent(root);
    }

    private LinearLayout topTabs(String cancelText, Runnable cancelAction) {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackgroundColor(yellow);
        Button cancel = outlineButton(cancelText);
        cancel.setBackgroundColor(yellow);
        cancel.setOnClickListener(v -> cancelAction.run());
        tabs.addView(cancel, new LinearLayout.LayoutParams(0, dp(56), 1));
        return tabs;
    }

    private void addPickerTypeTab(LinearLayout tabs, String text, String type) {
        Button b = currentType.equals(type) ? primaryButton(text) : outlineButton(text);
        b.setBackgroundColor(currentType.equals(type) ? yellow : Color.WHITE);
        b.setOnClickListener(v -> {
            currentType = type;
            selectedCategory = 0;
            selectedCategoryName = "";
            selectedCategoryIcon = "";
            showCategoryPicker();
        });
        tabs.addView(b, tabs.getChildCount() - 1, new LinearLayout.LayoutParams(0, dp(56), 1));
    }

    private View entryHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(yellow);
        Button back = outlineButton("<");
        back.setBackgroundColor(yellow);
        back.setOnClickListener(v -> {
            if ("transfer".equals(currentType)) {
                currentType = "expense";
                selectedTargetAccount = 0;
            }
            showCategoryPicker();
        });
        TextView title = label(selectedCategoryIcon + "  " + selectedCategoryName + " · " + typeText(currentType), 18, ink, true);
        title.setGravity(Gravity.CENTER);
        Button cancel = outlineButton("取消");
        cancel.setBackgroundColor(yellow);
        cancel.setOnClickListener(v -> showHome());
        header.addView(back, new LinearLayout.LayoutParams(dp(56), dp(58)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(58), 1));
        header.addView(cancel, new LinearLayout.LayoutParams(dp(80), dp(58)));
        return header;
    }

    private void addCategoryTile(GridLayout grid, DbHelper.Item item) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        View icon = iconBadge(item.name, currentType, dp(66), false);
        TextView name = label(item.name, 14, ink, false);
        name.setGravity(Gravity.CENTER);
        box.addView(icon, new LinearLayout.LayoutParams(dp(66), dp(66)));
        box.addView(name, new LinearLayout.LayoutParams(-1, dp(32)));
        touchable(box);
        box.setOnClickListener(v -> {
            selectedCategory = item.id;
            selectedCategoryName = item.name;
            selectedCategoryIcon = item.icon;
            showEntryForm();
        });
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels / 4 - dp(12);
        lp.height = dp(118);
        lp.setMargins(dp(2), dp(10), dp(2), dp(10));
        grid.addView(box, lp);
    }

    private void addCategoryAddTile(GridLayout grid) {
        DbHelper.Item fake = new DbHelper.Item(0, "设置", "+", "#999999", 0);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        View icon = iconBadge(fake.name, currentType, dp(66), true);
        TextView name = label(fake.name, 14, ink, false);
        name.setGravity(Gravity.CENTER);
        box.addView(icon, new LinearLayout.LayoutParams(dp(66), dp(66)));
        box.addView(name, new LinearLayout.LayoutParams(-1, dp(32)));
        touchable(box);
        box.setOnClickListener(v -> askCategory());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels / 4 - dp(12);
        lp.height = dp(118);
        lp.setMargins(dp(2), dp(10), dp(2), dp(10));
        grid.addView(box, lp);
    }

    private HorizontalScrollView accountScroller(boolean target) {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, 0, 0, dp(4));
        hsv.addView(row);
        for (DbHelper.Item account : db.accounts()) addAccountChoice(row, account, target);
        Button add = outlineButton("+ 账户");
        add.setOnClickListener(v -> askAccount());
        row.addView(add, new LinearLayout.LayoutParams(dp(92), dp(44)));
        return hsv;
    }

    private void addAccountChoice(LinearLayout row, DbHelper.Item item, boolean target) {
        boolean selected = target ? selectedTargetAccount == item.id : selectedAccount == item.id;
        TextView tv = label(item.name, 14, selected ? Color.WHITE : ink, false);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(selected ? round(green, dp(4)) : round(soft, dp(4)));
        tv.setPadding(dp(16), 0, dp(16), 0);
        touchable(tv);
        tv.setOnClickListener(v -> {
            if (target) selectedTargetAccount = item.id;
            else selectedAccount = item.id;
            refreshEntryInputs();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(42));
        lp.setMargins(0, 0, dp(6), 0);
        row.addView(tv, lp);
    }

    private GridLayout keypad(Button reimb) {
        GridLayout pad = new GridLayout(this);
        pad.setColumnCount(4);
        String[] keys = {"7", "8", "9", "今天", "4", "5", "6", "+", "1", "2", "3", "-", ".", "0", "⌫", "完成"};
        for (String key : keys) {
            TextView cell = label(key, "完成".equals(key) ? 22 : 24, ink, false);
            cell.setGravity(Gravity.CENTER);
            cell.setBackgroundColor("完成".equals(key) ? yellow : Color.WHITE);
            touchable(cell);
            cell.setOnClickListener(v -> keypadTap(key, reimb));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = (getResources().getDisplayMetrics().widthPixels - dp(32)) / 4;
            lp.height = dp(64);
            pad.addView(cell, lp);
        }
        return pad;
    }

    private void keypadTap(String key, Button reimb) {
        if ("完成".equals(key)) {
            saveTransaction(reimb.isSelected(), false);
            return;
        }
        if ("今天".equals(key)) {
            selectedTime = System.currentTimeMillis();
            if (timeText != null) timeText.setText("今天");
            return;
        }
        String raw = amountInput == null ? "" : amountInput.getText().toString();
        if ("⌫".equals(key)) raw = raw.length() > 0 ? raw.substring(0, raw.length() - 1) : "";
        else if ("+".equals(key) || "-".equals(key)) return;
        else if (".".equals(key)) {
            if (!raw.contains(".")) raw = raw.length() == 0 ? "0." : raw + ".";
        } else {
            int dot = raw.indexOf('.');
            if (dot < 0 || raw.length() - dot <= 2) raw += key;
        }
        amountInput.setText(raw);
        amountDisplay.setText(raw.length() == 0 ? "0.00" : raw);
    }

    private void refreshEntryInputs() {
        String amount = amountInput == null ? "" : amountInput.getText().toString();
        String note = noteInput == null ? "" : noteInput.getText().toString();
        showEntryForm();
        amountInput.setText(amount);
        amountDisplay.setText(amount.length() == 0 ? "0.00" : amount);
        noteInput.setText(note);
        imageHint.setText(pendingImages.isEmpty() ? "未添加图片" : "已添加 " + pendingImages.size() + " 张图片");
    }

    private void pickDateTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedTime);
        new DatePickerDialog(this, (view, year, month, day) -> {
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                c.set(Calendar.HOUR_OF_DAY, hour);
                c.set(Calendar.MINUTE, minute);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                selectedTime = c.getTimeInMillis();
                if (timeText != null) timeText.setText(shortDateLabel(selectedTime));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveTransaction(boolean reimb, boolean again) {
        long cents = parseAmount(amountInput == null ? "" : amountInput.getText().toString());
        if (cents <= 0) {
            toast("请输入金额");
            return;
        }
        if (selectedAccount == 0) {
            toast("请选择账户");
            return;
        }
        if ("transfer".equals(currentType)) {
            if (selectedTargetAccount == 0 || selectedTargetAccount == selectedAccount) {
                toast("请选择不同的转入账户");
                return;
            }
        } else if (selectedCategory == 0) {
            toast("请选择分类");
            return;
        }
        long id;
        if (editingTransactionId > 0) {
            id = editingTransactionId;
            db.updateTransaction(id, currentType, cents, selectedCategory, selectedAccount, selectedTargetAccount,
                    selectedTime, noteInput.getText().toString().trim(), reimb ? "pending" : "none");
        } else {
            id = db.addTransaction(currentType, cents, selectedCategory, selectedAccount, selectedTargetAccount,
                    selectedTime, noteInput.getText().toString().trim(), reimb ? "pending" : "none");
        }
        for (String path : pendingImages) db.addAttachment(id, path);
        if (editingTransactionId == 0) petReward(pendingImages.size() > 0);
        toast(editingTransactionId > 0 ? "已更新" : (petAdopted() ? "已保存，宠物获得成长奖励" : "已保存"));
        editingTransactionId = 0;
        editingReimburse = false;
        showList();
    }

    private void showList() {
        currentPage = "list";
        if (listKeyword == null) listKeyword = "";
        renderListPage();
    }

    private void setListKeyword(String keyword) {
        listKeyword = keyword == null ? "" : keyword.trim();
        renderListPage();
    }

    private void setListType(String type) {
        listType = type;
        renderListPage();
    }

    private void renderListPage() {
        LinearLayout page = page("明细");
        page.addView(listDateSwitcher(this::renderListPage));
        page.addView(listSummaryCard());

        EditText search = new EditText(this);
        search.setHint("搜索备注、分类、账户");
        search.setText(listKeyword == null ? "" : listKeyword);
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String next = s.toString();
                if (next.equals(listKeyword == null ? "" : listKeyword)) return;
                listKeyword = next;
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            setListKeyword(search.getText().toString());
            return true;
        });
        page.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(filters);
        addFilter(filters, "全部", null);
        addFilter(filters, "支出", "expense");
        addFilter(filters, "收入", "income");
        addFilter(filters, "转账", "transfer");

        renderList(page);
        setPage(page);
    }

    private void addFilter(LinearLayout filters, String label, String type) {
        Button b = "全部".equals(label) ? filterButton(label, listType == null) : filterButton(label, type != null && type.equals(listType));
        b.setOnClickListener(v -> setListType(type));
        filters.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
    }

    private void renderList(LinearLayout page) {
        long[] range = listDayMode ? dayRange(visibleDay.getTimeInMillis()) : monthRange(visibleMonth);
        renderFeed(page, range[0], range[1], listKeyword, listType, true);
    }

    private void renderFeed(LinearLayout page, long start, long end, String keyword, String type) {
        renderFeed(page, start, end, keyword, type, false);
    }

    private void renderFeed(LinearLayout page, long start, long end, String keyword, String type, boolean listEmpty) {
        List<DbHelper.Tx> txs = db.transactions(keyword, type, start, end);
        if (txs.isEmpty()) {
            page.addView(empty(listEmpty ? listEmptyText() : "没有账单"));
            return;
        }
        String lastDay = "";
        for (DbHelper.Tx tx : txs) {
            String day = formatDay(tx.time);
            if (!day.equals(lastDay)) {
                long[] r = dayRange(tx.time);
                page.addView(dayHeader(day, db.sum("income", r[0], r[1]), db.sum("expense", r[0], r[1])));
                lastDay = day;
            }
            page.addView(txRow(tx, true));
        }
    }

    private String listEmptyText() {
        String period = listDayMode ? formatDateOnly(visibleDay.getTimeInMillis()) : monthKey(visibleMonth);
        if (listKeyword != null && listKeyword.trim().length() > 0) return period + "暂无符合条件的记录";
        if (listType == null) return period + "暂无账单";
        return period + "暂无" + typeText(listType);
    }

    private LinearLayout listSummaryCard() {
        long[] range = listDayRange();
        long income = db.sum("income", range[0], range[1]);
        long expense = db.sum("expense", range[0], range[1]);
        LinearLayout card = card();
        String scope = listDayMode ? formatDateOnly(visibleDay.getTimeInMillis()) : monthKey(visibleMonth);
        String typeText = listType == null ? "全部" : typeText(listType);
        card.addView(label("当前：" + scope + "  " + typeText, 16, ink, true));
        card.addView(label("收入 " + money(income) + "    支出 " + money(expense) + "    结余 " + money(income - expense), 14, muted, false));
        return card;
    }

    private long[] listDayRange() {
        return listDayMode ? dayRange(visibleDay.getTimeInMillis()) : monthRange(visibleMonth);
    }

    private TextView dayHeader(String day, long income, long expense) {
        TextView tv = label(day + "        收入：" + money(income) + "    支出：" + money(expense), 14, muted, false);
        tv.setPadding(dp(2), dp(20), dp(2), dp(8));
        return tv;
    }

    private View txRow(DbHelper.Tx tx, boolean deletable) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(2), dp(10), dp(2), dp(10));
        row.setBackgroundColor(Color.WHITE);
        View icon = iconBadge(tx.category, tx.type, dp(52), false);
        row.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        String note = tx.note == null || tx.note.length() == 0 ? tx.category : tx.note;
        mid.addView(label(note, 16, ink, false));
        mid.addView(label(tx.account + " · " + formatTime(tx.time), 12, muted, false));
        row.addView(mid, new LinearLayout.LayoutParams(0, dp(52), 1));
        int color = "income".equals(tx.type) ? green : ("expense".equals(tx.type) ? red : muted);
        String sign = "income".equals(tx.type) ? "" : ("expense".equals(tx.type) ? "-" : "");
        TextView amount = label(sign + money(tx.amount), 18, color, false);
        amount.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(amount, new LinearLayout.LayoutParams(dp(116), dp(52)));
        if (deletable) {
            touchable(row);
            row.setOnClickListener(v -> txDetail(tx));
            row.setOnLongClickListener(v -> {
                confirmDelete(tx.id);
                return true;
            });
        }
        return row;
    }

    private void txDetail(DbHelper.Tx tx) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(4), dp(4), dp(4), 0);
        box.addView(label("分类：" + tx.category, 15, ink, false));
        box.addView(label("账户：" + tx.account, 15, ink, false));
        box.addView(label("时间：" + formatDateTime(tx.time), 15, ink, false));
        box.addView(label("备注：" + (tx.note.length() == 0 ? "无" : tx.note), 15, ink, false));
        box.addView(label("报销：" + ("pending".equals(tx.reimburse) ? "待报销" : "无"), 15, ink, false));
        List<String> images = db.attachmentPaths(tx.id);
        if (images.isEmpty()) {
            box.addView(label("图片：无", 15, muted, false));
        } else {
            box.addView(label("图片：" + images.size() + " 张，点击可查看大图", 15, ink, false));
            LinearLayout thumbs = new LinearLayout(this);
            thumbs.setOrientation(LinearLayout.HORIZONTAL);
            thumbs.setPadding(0, dp(8), 0, 0);
            for (String path : images) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageURI(Uri.fromFile(new File(path)));
                iv.setBackground(round(soft, dp(6)));
                touchable(iv);
                iv.setOnClickListener(v -> showImage(path));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), dp(72));
                lp.setMargins(0, 0, dp(8), 0);
                thumbs.addView(iv, lp);
            }
            box.addView(thumbs);
        }
        new AlertDialog.Builder(this)
                .setTitle(DbHelper.typeName(tx.type) + " " + money(tx.amount))
                .setView(box)
                .setNegativeButton("关闭", null)
                .setNeutralButton("编辑", (d, w) -> editTransaction(tx.id))
                .setPositiveButton("删除", (d, w) -> confirmDelete(tx.id))
                .show();
    }

    private void editTransaction(long id) {
        DbHelper.TxRecord record = db.transactionRecord(id);
        if (record == null) {
            toast("账单不存在");
            return;
        }
        pendingImages.clear();
        editingTransactionId = id;
        editingReimburse = "pending".equals(record.reimburse);
        currentType = record.type;
        selectedCategory = record.categoryId;
        selectedAccount = record.accountId;
        selectedTargetAccount = record.targetAccountId;
        selectedTime = record.time;
        DbHelper.Item category = categoryById(record.type, record.categoryId);
        if ("transfer".equals(record.type)) {
            selectedCategoryName = "转账";
            selectedCategoryIcon = "转";
        } else if (category != null) {
            selectedCategoryName = category.name;
            selectedCategoryIcon = category.icon;
        }
        showEntryForm();
        amountInput.setText(DbHelper.formatMoney(record.amount));
        amountDisplay.setText(DbHelper.formatMoney(record.amount));
        noteInput.setText(record.note);
        timeText.setText(shortDateLabel(record.time));
        int oldImages = db.attachmentPaths(id).size();
        imageHint.setText(oldImages == 0 ? "可继续添加图片" : "已有 " + oldImages + " 张图片，可继续添加");
    }

    private void openRecordSound() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION);
            return;
        }
        showRecordSoundDialog();
    }

    private void showRecordSoundDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(4), dp(4), dp(4), 0);
        TextView status = label("点击开始录音，建议录 0.3-1 秒的短音效。", 14, muted, false);
        box.addView(status);
        Button record = primaryButton("开始录音");
        Button preview = outlineButton("试听当前录音");
        Button use = outlineButton("设为按键音效");
        box.addView(record, new LinearLayout.LayoutParams(-1, dp(48)));
        box.addView(preview, new LinearLayout.LayoutParams(-1, dp(48)));
        box.addView(use, new LinearLayout.LayoutParams(-1, dp(48)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("录制按键音效")
                .setView(box)
                .setNegativeButton("关闭", (d, w) -> stopRecording(false))
                .create();
        record.setOnClickListener(v -> {
            if (recorder == null) {
                startRecording(status, record);
            } else {
                stopRecording(true);
                status.setText("录音已保存，可以试听或设为按键音效。");
                record.setText("重新录音");
            }
        });
        preview.setOnClickListener(v -> playRecordedSound());
        use.setOnClickListener(v -> {
            File file = recordedSoundFile();
            if (!file.exists()) {
                toast("请先录制音效");
                return;
            }
            prefs.edit()
                    .putInt("sound_tone", SOUND_TONES.length - 1)
                    .putBoolean("sound_enabled", true)
                    .apply();
            playRecordedSound();
            toast("已设为按键音效");
            dialog.dismiss();
            showSettings();
        });
        dialog.show();
    }

    private void showImage(String path) {
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageURI(Uri.fromFile(new File(path)));
        new AlertDialog.Builder(this)
                .setView(image)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setTitle("删除账单")
                .setMessage("删除后会同步恢复账户余额。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    db.deleteTransaction(id);
                    toast("已删除");
                    showList();
                }).show();
    }

    private void showStats() {
        currentPage = "stats";
        LinearLayout page = page("");
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(18), dp(12), dp(18), dp(18));
        top.setBackgroundColor(yellow);
        TextView title = label(("income".equals(statsType) ? "收入统计" : "支出统计"), 24, ink, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));
        top.addView(statsDateSwitcher(this::showStats));
        page.addView(top);
        long[] range = statsRange();
        long income = db.sum("income", range[0], range[1]);
        long expense = db.sum("expense", range[0], range[1]);
        long selectedTotal = db.sum(statsType, range[0], range[1]);
        String rankTitle = "income".equals(statsType) ? "收入排行" : "支出排行";
        List<DbHelper.Stat> stats = db.categoryStats(statsType, range[0], range[1]);
        page.addView(statsSummaryCard(range, income, expense, selectedTotal));
        page.addView(statsDonutCard(stats, selectedTotal));
        page.addView(statsTrendCard());
        page.addView(sectionTitle(rankTitle));
        if (stats.isEmpty()) {
            page.addView(empty(statsRangeLabel() + "暂无" + ("income".equals(statsType) ? "收入" : "支出")));
            Button add = primaryButton("去记一笔");
            add.setOnClickListener(v -> openNewTransaction());
            page.addView(add, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        for (DbHelper.Stat stat : stats) page.addView(statRow(stat, selectedTotal));
        setPage(page);
    }

    private LinearLayout statsSummaryCard(long[] range, long income, long expense, long selectedTotal) {
        long count = db.count(statsType, range[0], range[1]);
        long max = db.maxAmount(statsType, range[0], range[1]);
        long avg = count == 0 ? 0 : selectedTotal / count;
        LinearLayout summary = card();
        TextView total = label(("income".equals(statsType) ? "总收入" : "总支出") + "\n" + money(selectedTotal), 28, "income".equals(statsType) ? green : red, true);
        total.setGravity(Gravity.CENTER);
        summary.addView(total, new LinearLayout.LayoutParams(-1, dp(82)));
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(metric("收入", income, green), new LinearLayout.LayoutParams(0, dp(58), 1));
        row1.addView(metric("支出", expense, red), new LinearLayout.LayoutParams(0, dp(58), 1));
        row1.addView(metric("结余", income - expense, ink), new LinearLayout.LayoutParams(0, dp(58), 1));
        summary.addView(row1);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(metric("笔数", count, muted, false), new LinearLayout.LayoutParams(0, dp(54), 1));
        row2.addView(metric("平均", avg, muted), new LinearLayout.LayoutParams(0, dp(54), 1));
        row2.addView(metric("最大", max, muted), new LinearLayout.LayoutParams(0, dp(54), 1));
        summary.addView(row2);
        return summary;
    }

    private LinearLayout statsTrendCard() {
        LinearLayout card = card();
        String title = "year".equals(statsPeriod) ? "本年趋势" : ("all".equals(statsPeriod) ? "年度趋势" : "近6个月趋势");
        card.addView(label(title, 17, ink, true));
        card.addView(new BarChartView(this, trendLabels(), trendValues()), new LinearLayout.LayoutParams(-1, dp(150)));
        return card;
    }

    private LinearLayout statsDonutCard(List<DbHelper.Stat> stats, long total) {
        LinearLayout card = card();
        card.addView(label("分类占比", 17, ink, true));
        card.addView(new DonutChartView(this, stats, total), new LinearLayout.LayoutParams(-1, dp(210)));
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.VERTICAL);
        int limit = Math.min(6, stats.size());
        for (int i = 0; i < limit; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(legendItem(stats.get(i), total, chartColor(i)), new LinearLayout.LayoutParams(0, dp(30), 1));
            if (i + 1 < limit) row.addView(legendItem(stats.get(i + 1), total, chartColor(i + 1)), new LinearLayout.LayoutParams(0, dp(30), 1));
            else row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(30), 1));
            legend.addView(row);
        }
        if (stats.isEmpty()) {
            TextView empty = label("本期暂无" + ("income".equals(statsType) ? "收入" : "支出") + "数据", 14, muted, false);
            empty.setGravity(Gravity.CENTER);
            legend.addView(empty, new LinearLayout.LayoutParams(-1, dp(36)));
        }
        card.addView(legend);
        return card;
    }

    private TextView legendItem(DbHelper.Stat stat, long total, int color) {
        int pct = total <= 0 ? 0 : (int) (stat.amount * 100 / total);
        TextView tv = label("● " + stat.name + " " + pct + "%", 13, color, false);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        return tv;
    }

    private void showMine() {
        currentPage = "mine";
        LinearLayout page = page("");
        page.addView(mineHero());
        page.addView(sectionTitle("账户"));
        LinearLayout accounts = card();
        for (DbHelper.Item a : db.accounts()) accounts.addView(accountRow(a));
        page.addView(accounts);
        Button addAccount = outlineButton("+ 新增账户");
        addAccount.setOnClickListener(v -> askAccount());
        page.addView(addAccount, new LinearLayout.LayoutParams(-1, dp(48)));

        page.addView(sectionTitle("常用工具"));
        LinearLayout tools = card();
        tools.addView(toolRow("预算", "设置本月预算", "预", yellow, this::askBudget));
        tools.addView(toolRow("导出 CSV", "快速分享账单明细", "表", Color.rgb(230, 241, 255), this::shareCsv));
        tools.addView(toolRow("设置", "音效、震动、完整备份", "设", Color.rgb(229, 246, 239), this::showSettings));
        page.addView(tools);

        TextView info = label("小秋记账 v1.0.0\n包名 com.qiuqiuqiu\n数据保存在本机 SQLite，图片和录音音效保存在 APP 本地目录。", 13, muted, false);
        info.setPadding(dp(6), dp(12), dp(6), 0);
        page.addView(info);
        setPage(page);
    }

    private void showPet() {
        currentPage = "pet";
        LinearLayout page = page("宠物");
        if (!petAdopted()) {
            page.addView(petIntroCard());
            page.addView(sectionTitle("选择你的记账伙伴"));
            page.addView(petAdoptRow(
                    petAdoptCard("cat", "小秋猫", Color.rgb(255, 234, 217)),
                    petAdoptCard("dog", "小秋狗", Color.rgb(229, 246, 239)),
                    petAdoptCard("rabbit", "小秋兔", Color.rgb(240, 233, 255))
            ));
            page.addView(petAdoptRow(
                    petAdoptCard("fox", "小秋狐", Color.rgb(255, 236, 218)),
                    petAdoptCard("panda", "小秋熊猫", Color.rgb(235, 239, 240)),
                    petAdoptCard("hamster", "小秋仓鼠", Color.rgb(255, 242, 207))
            ));
            setPage(page);
            return;
        }

        page.addView(petHeroCard());
        page.addView(sectionTitle("互动"));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(petActionButton("喂食", "2金币", () -> feedPet()), new LinearLayout.LayoutParams(0, dp(74), 1));
        actions.addView(petActionButton("玩耍", "+经验", () -> playWithPet()), new LinearLayout.LayoutParams(0, dp(74), 1));
        actions.addView(petActionButton("抚摸", "+亲密", () -> petPet()), new LinearLayout.LayoutParams(0, dp(74), 1));
        page.addView(actions);

        page.addView(sectionTitle("今日小任务"));
        LinearLayout tasks = card();
        tasks.addView(taskRow("记一笔账", "+2经验"));
        tasks.addView(taskRow("添加图片票据", "+1金币"));
        tasks.addView(taskRow("每天坚持记录", "宠物会更开心"));
        page.addView(tasks);

        page.addView(sectionTitle("宠物商店"));
        LinearLayout shop = card();
        shop.addView(shopRow("小鱼干", "2金币，喂食宠物", 2, "food"));
        shop.addView(shopRow("小铃铛", "5金币，装备到脖子上", 5, "bell"));
        shop.addView(shopRow("玩具球", "6金币，放在小窝旁", 6, "ball"));
        shop.addView(shopRow("温暖小窝", "8金币，升级宠物房间", 8, "home"));
        page.addView(shop);
        setPage(page);
    }

    private void showAiAssistant() {
        currentPage = "ai";
        if (aiVoiceRecording) stopAiVoiceRecording(false);
        if (aiSpeechListening) stopAiSpeechInput();
        if (aiMessages.isEmpty()) {
            aiMessages.add("AI：你可以直接说：今天午饭28微信支付，或 昨天工资到账5000。");
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(0, statusBarHeight(), 0, 0);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), 0, dp(12), 0);
        header.setBackgroundColor(yellow);
        Button back = outlineButton("<");
        back.setBackgroundColor(yellow);
        back.setOnClickListener(v -> showHome());
        Button home = outlineButton("首页");
        home.setBackgroundColor(yellow);
        home.setOnClickListener(v -> showHome());
        TextView title = label("AI记账助手", 20, ink, true);
        title.setGravity(Gravity.CENTER);
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(56)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        header.addView(home, new LinearLayout.LayoutParams(dp(64), dp(56)));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(messages);
        LinearLayout tips = card();
        tips.addView(label("本地 AI 规则解析，不联网。识别后需要你确认才会保存。", 14, muted, false));
        messages.addView(tips);
        for (String msg : aiMessages) {
            if (!msg.startsWith("VOICE:")) messages.addView(aiBubble(msg));
        }
        if (aiDraft != null) messages.addView(aiDraftCard());
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout inputBar = new LinearLayout(this);
        inputBar.setOrientation(LinearLayout.HORIZONTAL);
        inputBar.setGravity(Gravity.CENTER_VERTICAL);
        inputBar.setPadding(dp(8), dp(8), dp(8), dp(12));
        inputBar.setBackgroundColor(Color.WHITE);
        EditText input = new EditText(this);
        input.setHint("输入一句记账内容");
        input.setSingleLine(true);
        input.setBackground(round(soft, dp(22)));
        input.setPadding(dp(14), 0, dp(14), 0);
        inputBar.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button send = primaryButton("发送");
        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.length() == 0) return;
            aiHandleUserText(text);
        });
        inputBar.addView(send, new LinearLayout.LayoutParams(dp(82), dp(48)));
        root.addView(inputBar);
        setContent(root);
    }

    private View aiBubble(String text) {
        TextView tv = label(text, 15, text.startsWith("我：") ? ink : muted, false);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(round(text.startsWith("我：") ? Color.rgb(255, 248, 222) : Color.WHITE, dp(8)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(4));
        tv.setLayoutParams(lp);
        return tv;
    }

    private View aiVoiceBubble(String path) {
        LinearLayout box = card();
        box.setBackground(round(Color.rgb(255, 248, 222), dp(8)));
        box.addView(label("我：语音消息", 15, ink, true));
        box.addView(label(new File(path).exists() ? "已保存到 APP 本地，点击播放。" : "语音文件不存在", 13, muted, false));
        Button play = outlineButton("播放语音");
        play.setOnClickListener(v -> playAudioFile(path));
        box.addView(play, new LinearLayout.LayoutParams(-1, dp(44)));
        return box;
    }

    private View aiSpeechSetupCard() {
        LinearLayout card = card();
        card.setBackground(round(Color.rgb(255, 249, 226), dp(8)));
        card.addView(label("语音转文字未开启", 17, ink, true));
        card.addView(label("当前手机没有可调用的系统语音识别服务。现在点击底部“语音”会先录音保存；开启语音输入服务后可自动转文字。", 14, muted, false));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button settings = primaryButton("去开启");
        settings.setOnClickListener(v -> openVoiceInputSettings());
        Button text = outlineButton("用文字");
        text.setOnClickListener(v -> toast("可以直接在底部输入一句记账内容"));
        row.addView(settings, new LinearLayout.LayoutParams(0, dp(46), 1));
        row.addView(text, new LinearLayout.LayoutParams(0, dp(46), 1));
        card.addView(row);
        return card;
    }

    private View aiDraftCard() {
        LinearLayout card = card();
        card.addView(label("识别结果", 17, ink, true));
        card.addView(label(aiDraftSummary(aiDraft), 15, ink, false));
        if (aiDraft.ready) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button save = primaryButton("保存账单");
            save.setOnClickListener(v -> saveAiDraft());
            Button cancel = outlineButton("取消");
            cancel.setOnClickListener(v -> {
                aiDraft = null;
                aiMessages.add("AI：已取消这次识别。");
                showAiAssistant();
            });
            row.addView(save, new LinearLayout.LayoutParams(0, dp(48), 1));
            row.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1));
            card.addView(row);
        } else {
            card.addView(label("还需要补充：" + aiDraft.missing, 14, red, false));
        }
        return card;
    }

    private void aiHandleUserText(String text) {
        aiMessages.add("我：" + text);
        aiDraft = parseAiDraft(text);
        aiMessages.add(aiDraft.ready ? "AI：我已识别出一笔账单，请确认后保存。" : "AI：我还没识别完整，可以换一种说法补充金额、账户或转入账户。");
        showAiAssistant();
    }

    private void startAiSpeechInput() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, SPEECH_PERMISSION);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toggleAiVoiceRecording();
            return;
        }
        if (aiSpeechListening) {
            stopAiSpeechInput();
            return;
        }
        if (aiVoiceRecording) {
            stopAiVoiceRecording(true);
            return;
        }
        if (aiSpeechRecognizer == null) {
            aiSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            aiSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    toast("正在听，请说出记账内容");
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {
                    aiSpeechListening = false;
                    toast("正在转文字");
                }
                @Override public void onError(int error) {
                    aiSpeechListening = false;
                    toast(aiSpeechErrorText(error));
                }
                @Override public void onResults(Bundle results) {
                    aiSpeechListening = false;
                    ArrayList<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (texts != null && !texts.isEmpty()) {
                        aiHandleUserText(texts.get(0));
                    } else {
                        toast("没有识别到语音内容");
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toString());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        aiSpeechListening = true;
        try {
            aiSpeechRecognizer.startListening(intent);
        } catch (Exception e) {
            aiSpeechListening = false;
            toast("语音转文字启动失败，可先用文字输入");
        }
    }

    private void stopAiSpeechInput() {
        if (aiSpeechRecognizer != null) {
            aiSpeechRecognizer.stopListening();
        }
        aiSpeechListening = false;
        toast("已停止语音输入");
    }

    private void toggleAiVoiceRecording() {
        if (aiVoiceRecording) {
            stopAiVoiceRecording(true);
        } else {
            startAiVoiceRecording();
        }
    }

    private void startAiVoiceRecording() {
        try {
            stopRecording(false);
            releaseCustomPlayer();
            File dir = new File(getFilesDir(), "ai_voice");
            if (!dir.exists()) dir.mkdirs();
            lastAiVoiceFile = new File(dir, "ai_voice_" + System.currentTimeMillis() + ".m4a");
            currentRecordingFile = lastAiVoiceFile;
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(lastAiVoiceFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            aiVoiceRecording = true;
            toast("已开始录音，再点一次停止");
            showAiAssistant();
        } catch (Exception e) {
            aiVoiceRecording = false;
            stopRecording(false);
            toast("录音启动失败，请检查麦克风权限");
        }
    }

    private void stopAiVoiceRecording(boolean keepFile) {
        if (!aiVoiceRecording && recorder == null) return;
        File file = lastAiVoiceFile;
        try {
            if (recorder != null) recorder.stop();
        } catch (Exception ignored) {
        }
        try {
            if (recorder != null) recorder.release();
        } catch (Exception ignored) {
        }
        recorder = null;
        aiVoiceRecording = false;
        currentRecordingFile = null;
        if (keepFile && file != null && file.exists() && file.length() > 0) {
            aiMessages.add("VOICE:" + file.getAbsolutePath());
            aiMessages.add("AI：当前设备没有语音转文字服务，我已先保存语音。你可以继续用文字补充金额、分类和账户。");
            toast("语音已保存");
            showAiAssistant();
        } else if (file != null && file.exists()) {
            file.delete();
        }
    }

    private void playAudioFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            toast("语音文件不存在");
            return;
        }
        try {
            releaseCustomPlayer();
            customPlayer = new MediaPlayer();
            customPlayer.setDataSource(file.getAbsolutePath());
            customPlayer.setOnCompletionListener(mp -> releaseCustomPlayer());
            customPlayer.prepare();
            customPlayer.start();
        } catch (Exception e) {
            releaseCustomPlayer();
            toast("语音播放失败");
        }
    }

    private String aiSpeechErrorText(int error) {
        if (error == SpeechRecognizer.ERROR_AUDIO) return "麦克风录音失败，请检查权限";
        if (error == SpeechRecognizer.ERROR_CLIENT) return "语音服务暂不可用，请稍后重试";
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) return "需要麦克风权限才能语音记账";
        if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) return "语音服务网络异常，可先用文字输入";
        if (error == SpeechRecognizer.ERROR_NO_MATCH) return "没有听清楚，请再说一次";
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) return "语音识别正忙，请稍后再试";
        if (error == SpeechRecognizer.ERROR_SERVER) return "语音识别服务异常，可先用文字输入";
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) return "没有听到声音，请再试一次";
        return "语音转文字失败，可先用文字输入";
    }

    private void showSpeechServiceMissingDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要开启语音转文字")
                .setMessage("当前手机没有可调用的系统语音识别服务。请在系统里开启语音输入，或安装手机厂商/Google 的语音识别服务。开启后回到这里再点“语音”。")
                .setNegativeButton("先用文字", null)
                .setPositiveButton("去开启", (d, w) -> openVoiceInputSettings())
                .show();
    }

    private void openVoiceInputSettings() {
        if (startSettingsIntent(Settings.ACTION_VOICE_INPUT_SETTINGS)) return;
        if (startSettingsIntent(Settings.ACTION_INPUT_METHOD_SETTINGS)) return;
        startSettingsIntent(Settings.ACTION_SETTINGS);
    }

    private boolean startSettingsIntent(String action) {
        try {
            Intent intent = new Intent(action);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String aiDraftSummary(AiDraft d) {
        return "类型：" + typeText(d.type) +
                "\n金额：" + money(d.amount) +
                "\n分类：" + ("transfer".equals(d.type) ? "转账" : d.categoryName) +
                "\n账户：" + accountName(d.accountId) +
                ("transfer".equals(d.type) ? "\n转入：" + accountName(d.targetAccountId) : "") +
                "\n时间：" + formatDateTime(d.time) +
                "\n备注：" + d.note;
    }

    private void saveAiDraft() {
        if (aiDraft == null || !aiDraft.ready) return;
        db.addTransaction(aiDraft.type, aiDraft.amount, aiDraft.categoryId, aiDraft.accountId, aiDraft.targetAccountId,
                aiDraft.time, aiDraft.note, "none");
        petReward(false);
        aiMessages.add("AI：已保存到账本。");
        aiDraft = null;
        showAiAssistant();
    }

    private AiDraft parseAiDraft(String text) {
        AiDraft d = new AiDraft();
        d.raw = text;
        d.note = text;
        d.time = parseAiTime(text);
        d.amount = parseAiAmount(text);
        d.type = parseAiType(text);
        d.accountId = matchAccount(text, false);
        if (d.accountId == 0) d.accountId = selectedAccount == 0 ? firstId(db.accounts()) : selectedAccount;
        if ("transfer".equals(d.type)) {
            d.targetAccountId = matchAccount(text, true);
            if (d.targetAccountId == 0 || d.targetAccountId == d.accountId) d.targetAccountId = firstDifferentAccount(d.accountId);
            d.categoryName = "转账";
        } else {
            DbHelper.Item category = matchCategory(text, d.type);
            if (category != null) {
                d.categoryId = category.id;
                d.categoryName = category.name;
            }
        }
        ArrayList<String> miss = new ArrayList<>();
        if (d.amount <= 0) miss.add("金额");
        if (d.accountId == 0) miss.add("账户");
        if ("transfer".equals(d.type)) {
            if (d.targetAccountId == 0 || d.targetAccountId == d.accountId) miss.add("不同的转入账户");
        } else if (d.categoryId == 0) {
            miss.add("分类");
        }
        d.ready = miss.isEmpty();
        d.missing = miss.isEmpty() ? "" : join(miss, "、");
        return d;
    }

    private long parseAiAmount(String text) {
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)").matcher(text);
        if (m.find()) return parseAmount(m.group(1));
        return 0;
    }

    private String parseAiType(String text) {
        if (containsAny(text, "转账", "转到", "转入", "转给")) return "transfer";
        if (containsAny(text, "工资", "到账", "收入", "奖金", "红包", "报销", "退款", "兼职", "理财", "收益", "收到")) return "income";
        return "expense";
    }

    private long parseAiTime(String text) {
        Calendar c = Calendar.getInstance();
        if (text.contains("前天")) c.add(Calendar.DAY_OF_MONTH, -2);
        else if (text.contains("昨天") || text.contains("昨日")) c.add(Calendar.DAY_OF_MONTH, -1);
        if (text.contains("早上") || text.contains("上午") || text.contains("早餐")) {
            c.set(Calendar.HOUR_OF_DAY, 8);
            c.set(Calendar.MINUTE, 0);
        } else if (text.contains("中午") || text.contains("午饭")) {
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 0);
        } else if (text.contains("晚上") || text.contains("晚饭") || text.contains("夜宵")) {
            c.set(Calendar.HOUR_OF_DAY, 19);
            c.set(Calendar.MINUTE, 0);
        }
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private DbHelper.Item matchCategory(String text, String type) {
        String target = categoryNameByKeyword(text, type);
        List<DbHelper.Item> list = db.categories(type);
        if (target != null) {
            for (DbHelper.Item item : list) if (item.name.equals(target)) return item;
        }
        for (DbHelper.Item item : list) if (text.contains(item.name)) return item;
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    private String categoryNameByKeyword(String text, String type) {
        if ("income".equals(type)) {
            if (containsAny(text, "工资", "薪水")) return "工资";
            if (containsAny(text, "兼职", "外快")) return "兼职";
            if (containsAny(text, "理财", "收益", "利息")) return "理财";
            if (containsAny(text, "奖金", "绩效")) return "奖金";
            if (containsAny(text, "红包")) return "红包";
            if (containsAny(text, "报销")) return "报销";
            if (containsAny(text, "退款", "退回")) return "退款";
            if (containsAny(text, "礼金")) return "礼金";
            return "其他";
        }
        if (containsAny(text, "吃", "餐", "饭", "早餐", "午饭", "晚饭", "咖啡", "奶茶", "外卖")) return "餐饮";
        if (containsAny(text, "买菜", "蔬菜")) return "蔬菜";
        if (containsAny(text, "水果")) return "水果";
        if (containsAny(text, "零食")) return "零食";
        if (containsAny(text, "购物", "淘宝", "京东", "买了")) return "购物";
        if (containsAny(text, "地铁", "公交", "打车", " taxi", "车费", "交通")) return "交通";
        if (containsAny(text, "房租", "房贷", "住房")) return "住房";
        if (containsAny(text, "水电", "日用", "纸巾")) return "日用";
        if (containsAny(text, "衣服", "鞋", "服饰")) return "服饰";
        if (containsAny(text, "医院", "药", "医疗")) return "医疗";
        if (containsAny(text, "书", "课程", "学习")) return "学习";
        if (containsAny(text, "电影", "游戏", "娱乐")) return "娱乐";
        if (containsAny(text, "电话", "话费", "流量", "通讯")) return "通讯";
        return "其他";
    }

    private long matchAccount(String text, boolean target) {
        List<DbHelper.Item> list = db.accounts();
        String sourceText = text;
        if (target) {
            int idx = Math.max(Math.max(text.indexOf("转到"), text.indexOf("转入")), text.indexOf("到"));
            if (idx >= 0) sourceText = text.substring(idx);
        }
        for (DbHelper.Item item : list) {
            if (sourceText.contains(item.name)) return item.id;
        }
        if (sourceText.contains("微信")) return accountIdByKeyword("微信");
        if (sourceText.contains("支付宝")) return accountIdByKeyword("支付宝");
        if (sourceText.contains("现金")) return accountIdByKeyword("现金");
        if (sourceText.contains("银行卡") || sourceText.contains("银行")) return accountIdByKeyword("银行卡");
        if (sourceText.contains("信用卡")) return accountIdByKeyword("信用卡");
        return 0;
    }

    private long accountIdByKeyword(String keyword) {
        for (DbHelper.Item item : db.accounts()) if (item.name.contains(keyword) || keyword.contains(item.name)) return item.id;
        return 0;
    }

    private String accountName(long id) {
        for (DbHelper.Item item : db.accounts()) if (item.id == id) return item.name;
        return "未识别";
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) if (text.contains(key)) return true;
        return false;
    }

    private String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private View petIntroCard() {
        LinearLayout card = card();
        TextView title = label("领养一只记账伙伴", 22, ink, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView sub = label("每次记账、添加图片票据，它都会陪你一起成长。", 14, muted, false);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub, new LinearLayout.LayoutParams(-1, dp(40)));
        return card;
    }

    private View petAdoptRow(View a, View b, View c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(a, new LinearLayout.LayoutParams(0, dp(190), 1));
        row.addView(b, new LinearLayout.LayoutParams(0, dp(190), 1));
        row.addView(c, new LinearLayout.LayoutParams(0, dp(190), 1));
        return row;
    }

    private View petAdoptCard(String type, String name, int bgColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6), dp(10), dp(6), dp(10));
        card.setBackground(round(Color.WHITE, dp(8)));
        card.addView(new PetView(this, type, bgColor), new LinearLayout.LayoutParams(-1, dp(104)));
        TextView title = label(name, 15, ink, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView choose = label("领养", 13, muted, false);
        choose.setGravity(Gravity.CENTER);
        card.addView(choose, new LinearLayout.LayoutParams(-1, dp(28)));
        touchable(card);
        card.setOnClickListener(v -> adoptPet(type, name));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(190), 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(lp);
        return card;
    }

    private View petHeroCard() {
        LinearLayout card = card();
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        TextView name = label(petName() + "  Lv." + petLevel(), 22, ink, true);
        name.setGravity(Gravity.CENTER);
        card.addView(name, new LinearLayout.LayoutParams(-1, dp(38)));
        card.addView(new PetView(this, petType(), Color.rgb(255, 248, 226)), new LinearLayout.LayoutParams(-1, dp(250)));
        TextView coins = label("金币 " + petCoins(), 16, Color.rgb(177, 126, 0), true);
        coins.setGravity(Gravity.CENTER);
        card.addView(coins, new LinearLayout.LayoutParams(-1, dp(30)));
        card.addView(petProgress("经验", petExp(), petNeedExp(), yellow));
        card.addView(petProgress("心情", petMood(), 100, green));
        card.addView(petProgress("亲密度", petAffinity(), 100, Color.rgb(213, 95, 138)));
        return card;
    }

    private View petProgress(String title, int value, int max, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView text = label(title + "  " + value + "/" + max, 13, muted, false);
        box.addView(text, new LinearLayout.LayoutParams(-1, dp(22)));
        box.addView(progressBar(max <= 0 ? 0 : Math.min(100, value * 100 / max), color), new LinearLayout.LayoutParams(-1, dp(8)));
        return box;
    }

    private View petActionButton(String title, String sub, Runnable action) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(round(Color.WHITE, dp(8)));
        btn.addView(label(title, 17, ink, true), new LinearLayout.LayoutParams(-2, dp(28)));
        btn.addView(label(sub, 12, muted, false), new LinearLayout.LayoutParams(-2, dp(22)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(74), 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        btn.setLayoutParams(lp);
        touchable(btn);
        btn.setOnClickListener(v -> action.run());
        return btn;
    }

    private View taskRow(String title, String reward) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label(title, 15, ink, false), new LinearLayout.LayoutParams(0, dp(38), 1));
        TextView r = label(reward, 14, green, false);
        r.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(r, new LinearLayout.LayoutParams(dp(120), dp(38)));
        return row;
    }

    private View shopRow(String title, String sub, int price, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title, 16, ink, false), new LinearLayout.LayoutParams(-1, dp(24)));
        text.addView(label(sub, 12, muted, false), new LinearLayout.LayoutParams(-1, dp(20)));
        row.addView(text, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button buy = outlineButton(hasPetItem(key) ? "已拥有" : price + "金币");
        buy.setEnabled(!hasPetItem(key) || "food".equals(key));
        buy.setOnClickListener(v -> buyPetItem(title, price, key));
        row.addView(buy, new LinearLayout.LayoutParams(dp(92), dp(42)));
        return row;
    }

    private View mineHero() {
        long[] month = monthRange(visibleMonth);
        long income = db.sum("income", month[0], month[1]);
        long expense = db.sum("expense", month[0], month[1]);
        long balance = 0;
        for (DbHelper.Item a : db.accounts()) balance += a.balance;
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(16), dp(18), dp(18));
        hero.setBackground(round(yellow, dp(8)));
        TextView title = label("我的", 28, ink, true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView asset = label("账户总资产\n" + money(balance), 30, ink, true);
        asset.setGravity(Gravity.CENTER);
        hero.addView(asset, new LinearLayout.LayoutParams(-1, dp(96)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(mineMiniMetric("本月收入", income, green), new LinearLayout.LayoutParams(0, dp(58), 1));
        row.addView(mineMiniMetric("本月支出", expense, red), new LinearLayout.LayoutParams(0, dp(58), 1));
        row.addView(mineMiniMetric("本月结余", income - expense, ink), new LinearLayout.LayoutParams(0, dp(58), 1));
        hero.addView(row);
        return hero;
    }

    private TextView mineMiniMetric(String title, long value, int color) {
        TextView tv = label(title + "\n" + money(value), 14, color, false);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(round(Color.argb(120, 255, 255, 255), dp(6)));
        return tv;
    }

    private View accountRow(DbHelper.Item account) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(iconBadge(account.name, "transfer", dp(44), false), new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView name = label(account.name, 16, ink, false);
        name.setPadding(dp(10), 0, 0, 0);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView amount = label(money(account.balance), 17, account.balance >= 0 ? green : red, true);
        amount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(amount, new LinearLayout.LayoutParams(dp(130), dp(44)));
        return row;
    }

    private View toolRow(String title, String sub, String icon, int iconBg, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView badge = label(icon, 18, ink, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(circle(iconBg, iconBg));
        row.addView(badge, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(10), 0, 0, 0);
        text.addView(label(title, 16, ink, false), new LinearLayout.LayoutParams(-1, dp(24)));
        text.addView(label(sub, 12, muted, false), new LinearLayout.LayoutParams(-1, dp(20)));
        row.addView(text, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView arrow = label(">", 20, muted, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(44)));
        touchable(row);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private void showSettings() {
        currentPage = "settings";
        LinearLayout page = page("设置");
        page.addView(settingsHero());
        page.addView(sectionTitle("音效设置"));
        LinearLayout soundCard = card();
        soundCard.addView(settingRow("效", Color.rgb(255, 238, 170), "按键音效", soundEnabled() ? "已开启，点击可关闭" : "已关闭，点击可开启", soundEnabled() ? "开" : "关", () -> {
            boolean next = !soundEnabled();
            prefs.edit().putBoolean("sound_enabled", next).apply();
            toast(next ? "按键音效已开启" : "按键音效已关闭");
            showSettings();
        }));
        soundCard.addView(settingDivider());
        soundCard.addView(settingRow("音", Color.rgb(229, 246, 239), "按键音色", "内置 " + (SOUND_NAMES.length - 1) + " 种音色，可试听切换", soundName(), this::chooseSoundTone));
        soundCard.addView(settingDivider());
        soundCard.addView(settingRow("录", Color.rgb(236, 241, 255), "录制按键音效", "录一段自己的提示音并设为按键音", "录制", this::openRecordSound));
        soundCard.addView(settingDivider());
        soundCard.addView(settingRow("震", Color.rgb(255, 232, 238), "按键震动", vibrationEnabled() ? "点击按钮时会轻微震动" : "关闭后点击只保留视觉反馈", vibrationEnabled() ? "开" : "关", () -> {
            boolean next = !vibrationEnabled();
            prefs.edit().putBoolean("vibration_enabled", next).apply();
            if (next) playClickVibration();
            toast(next ? "按键震动已开启" : "按键震动已关闭");
            showSettings();
        }));
        page.addView(soundCard);

        page.addView(sectionTitle("备份设置"));
        LinearLayout backupCard = card();
        backupCard.addView(settingRow("出", Color.rgb(230, 241, 255), "导出完整备份", "账单、分类、账户、预算、本地图片一起导出", "导出", this::createBackupDocument));
        backupCard.addView(settingDivider());
        backupCard.addView(settingRow("入", Color.rgb(235, 245, 232), "导入完整备份", "导入会覆盖当前本机数据，请先确认文件来源", "导入", this::confirmImportBackup));
        page.addView(backupCard);
        TextView tip = label("完整备份包含账单、分类、账户、预算和本地图片。导入会覆盖当前本机数据。", 14, muted, false);
        tip.setPadding(dp(4), dp(4), dp(4), dp(12));
        page.addView(tip);
        setPage(page);
    }

    private View settingsHero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(16), dp(18), dp(16));
        hero.setBackground(round(yellow, dp(8)));
        TextView title = label("设置中心", 24, ink, true);
        hero.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));
        TextView sub = label("音效、震动和完整备份都在这里管理。", 14, ink, false);
        hero.addView(sub, new LinearLayout.LayoutParams(-1, dp(28)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(settingsPill("音效", soundEnabled() ? "开" : "关"), new LinearLayout.LayoutParams(0, dp(54), 1));
        row.addView(settingsPill("音色", soundName()), new LinearLayout.LayoutParams(0, dp(54), 1));
        row.addView(settingsPill("震动", vibrationEnabled() ? "开" : "关"), new LinearLayout.LayoutParams(0, dp(54), 1));
        hero.addView(row);
        return hero;
    }

    private TextView settingsPill(String title, String value) {
        TextView tv = label(title + "\n" + value, 13, ink, false);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(round(Color.argb(130, 255, 255, 255), dp(6)));
        return tv;
    }

    private View settingRow(String icon, int iconBg, String title, String sub, String value, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView badge = label(icon, 16, ink, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(circle(iconBg, iconBg));
        row.addView(badge, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(12), 0, dp(8), 0);
        text.addView(label(title, 16, ink, true), new LinearLayout.LayoutParams(-1, dp(24)));
        text.addView(label(sub, 12, muted, false), new LinearLayout.LayoutParams(-1, dp(22)));
        row.addView(text, new LinearLayout.LayoutParams(0, dp(50), 1));
        TextView state = label(value, 14, muted, false);
        state.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(state, new LinearLayout.LayoutParams(dp(86), dp(50)));
        TextView arrow = label(">", 18, muted, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(18), dp(50)));
        touchable(row);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private View settingDivider() {
        View v = new View(this);
        v.setBackgroundColor(line);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
        lp.setMargins(dp(56), 0, 0, 0);
        v.setLayoutParams(lp);
        return v;
    }

    private void chooseSoundTone() {
        int checked = soundIndex();
        new AlertDialog.Builder(this)
                .setTitle("选择按键音效")
                .setSingleChoiceItems(SOUND_NAMES, checked, (dialog, which) -> {
                    if (SOUND_TONES[which] == CUSTOM_RECORD && !recordedSoundFile().exists()) {
                        dialog.dismiss();
                        toast("请先录制自己的音效");
                        openRecordSound();
                        return;
                    }
                    prefs.edit()
                            .putInt("sound_tone", which)
                            .putBoolean("sound_enabled", true)
                            .apply();
                    previewSound(which);
                    dialog.dismiss();
                    showSettings();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void askBudget() {
        EditText input = new EditText(this);
        input.setHint("例如 5000");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this)
                .setTitle("设置 " + monthKey(visibleMonth) + " 预算")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    long cents = parseAmount(input.getText().toString());
                    if (cents > 0) {
                        db.saveBudget(monthKey(visibleMonth), cents);
                        toast("预算已保存");
                        showHome();
                    }
                }).show();
    }

    private boolean petAdopted() {
        return prefs != null && prefs.getBoolean("pet_adopted", false);
    }

    private String petType() {
        return prefs == null ? "cat" : prefs.getString("pet_type", "cat");
    }

    private String petName() {
        return prefs == null ? "小秋猫" : prefs.getString("pet_name", "小秋猫");
    }

    private int petLevel() {
        return prefs == null ? 1 : prefs.getInt("pet_level", 1);
    }

    private int petExp() {
        return prefs == null ? 0 : prefs.getInt("pet_exp", 0);
    }

    private int petCoins() {
        return prefs == null ? 0 : prefs.getInt("pet_coins", 0);
    }

    private int petMood() {
        return prefs == null ? 70 : prefs.getInt("pet_mood", 70);
    }

    private int petAffinity() {
        return prefs == null ? 20 : prefs.getInt("pet_affinity", 20);
    }

    private int petNeedExp() {
        return 80 + petLevel() * 40;
    }

    private void adoptPet(String type, String name) {
        prefs.edit()
                .putBoolean("pet_adopted", true)
                .putString("pet_type", type)
                .putString("pet_name", name)
                .putInt("pet_level", 1)
                .putInt("pet_exp", 0)
                .putInt("pet_coins", 3)
                .putInt("pet_mood", 80)
                .putInt("pet_affinity", 25)
                .apply();
        toast("已领养 " + name);
        showPet();
    }

    private void petReward(boolean withImage) {
        if (!petAdopted()) return;
        addPetExp(2);
        if (withImage) addPetCoins(1);
        setPetMood(Math.min(100, petMood() + 1));
    }

    private void feedPet() {
        if (petCoins() < 2) {
            toast("金币不足，记账或添加图片可以获得金币");
            return;
        }
        setPetAction("feed");
        addPetCoins(-2);
        setPetMood(Math.min(100, petMood() + 8));
        setPetAffinity(Math.min(100, petAffinity() + 3));
        toast(petName() + " 吃饱啦");
        showPet();
    }

    private void playWithPet() {
        setPetAction("play");
        addPetExp(3);
        setPetMood(Math.min(100, petMood() + 5));
        toast(petName() + " 玩得很开心");
        showPet();
    }

    private void petPet() {
        setPetAction("touch");
        setPetAffinity(Math.min(100, petAffinity() + 4));
        setPetMood(Math.min(100, petMood() + 2));
        toast(petName() + " 更亲近你了");
        showPet();
    }

    private void buyPetItem(String name, int price, String key) {
        if (petCoins() < price) {
            toast("金币不足");
            return;
        }
        addPetCoins(-price);
        if (!"food".equals(key)) prefs.edit().putBoolean("pet_item_" + key, true).apply();
        setPetAction("food".equals(key) ? "feed" : "levelup");
        setPetAffinity(Math.min(100, petAffinity() + Math.max(2, price / 2)));
        toast("已购买 " + name);
        showPet();
    }

    private boolean hasPetItem(String key) {
        return prefs != null && prefs.getBoolean("pet_item_" + key, false);
    }

    private void setPetAction(String action) {
        petAction = action;
        petActionUntil = System.currentTimeMillis() + 1200;
    }

    private void addPetCoins(int delta) {
        prefs.edit().putInt("pet_coins", Math.max(0, petCoins() + delta)).apply();
    }

    private void setPetMood(int value) {
        prefs.edit().putInt("pet_mood", Math.max(0, Math.min(100, value))).apply();
    }

    private void setPetAffinity(int value) {
        prefs.edit().putInt("pet_affinity", Math.max(0, Math.min(100, value))).apply();
    }

    private void addPetExp(int delta) {
        int level = petLevel();
        int exp = petExp() + delta;
        int affinity = petAffinity();
        while (exp >= 80 + level * 40) {
            exp -= 80 + level * 40;
            level++;
            affinity = Math.min(100, affinity + 5);
        }
        prefs.edit()
                .putInt("pet_level", level)
                .putInt("pet_exp", exp)
                .putInt("pet_affinity", affinity)
                .apply();
    }

    private void askCategory() {
        EditText input = new EditText(this);
        input.setHint("分类名称");
        new AlertDialog.Builder(this)
                .setTitle("新增" + typeText(currentType) + "分类")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() > 0) {
                        selectedCategory = db.addCategory(name, currentType);
                        selectedCategoryName = name;
                        selectedCategoryIcon = name.substring(0, 1);
                        showEntryForm();
                    }
                }).show();
    }

    private void askAccount() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText name = new EditText(this);
        name.setHint("账户名称，例如 招商银行卡");
        EditText balance = new EditText(this);
        balance.setHint("初始余额，例如 1000");
        balance.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(name);
        box.addView(balance);
        new AlertDialog.Builder(this)
                .setTitle("新增账户")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.length() > 0) {
                        selectedAccount = db.addAccount(n, parseAmount(balance.getText().toString()));
                        showMine();
                    }
                }).show();
    }

    private void shareCsv() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TEXT, db.exportCsv());
        intent.putExtra(Intent.EXTRA_SUBJECT, "小秋记账导出");
        startActivity(Intent.createChooser(intent, "导出账单"));
    }

    private void createBackupDocument() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "小秋记账完整备份_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(Calendar.getInstance().getTime()) + ".zip");
        startActivityForResult(intent, CREATE_BACKUP);
    }

    private void confirmImportBackup() {
        new AlertDialog.Builder(this)
                .setTitle("导入完整备份")
                .setMessage("导入会覆盖当前账单、账户、预算和图片。请确认已经导出过当前数据。")
                .setNegativeButton("取消", null)
                .setPositiveButton("选择备份文件", (d, w) -> openBackupDocument())
                .show();
    }

    private void openBackupDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        startActivityForResult(intent, IMPORT_BACKUP);
    }

    private void exportFullBackup(Uri uri) {
        try {
            consolidateAttachments();
            checkpointDb();
            OutputStream raw = getContentResolver().openOutputStream(uri);
            if (raw == null) throw new Exception("no output");
            ZipOutputStream zip = new ZipOutputStream(raw);
            addFileToZip(zip, getDatabasePath(DbHelper.DB_NAME), "database/" + DbHelper.DB_NAME);
            File receipts = new File(getFilesDir(), "receipts");
            addDirToZip(zip, receipts, "receipts");
            File sounds = new File(getFilesDir(), "sounds");
            addDirToZip(zip, sounds, "sounds");
            addTextToZip(zip, "settings/settings.json", settingsJson());
            zip.close();
            toast("完整备份已导出");
        } catch (Exception e) {
            toast("备份导出失败");
        }
    }

    private void consolidateAttachments() {
        File receipts = new File(getFilesDir(), "receipts");
        if (!receipts.exists()) receipts.mkdirs();
        String receiptsPath = receipts.getAbsolutePath();
        for (DbHelper.Attachment attachment : db.allAttachments()) {
            if (attachment.path == null || attachment.path.length() == 0) continue;
            File source = new File(attachment.path);
            if (!source.exists() || !source.isFile()) continue;
            if (source.getAbsolutePath().startsWith(receiptsPath)) continue;
            File target = uniqueReceiptFile(source.getName());
            try {
                copyFile(source, target);
                db.updateAttachmentPath(attachment.id, target.getAbsolutePath());
            } catch (Exception ignored) {
            }
        }
    }

    private void importFullBackup(Uri uri) {
        File temp = new File(getCacheDir(), "backup_import");
        try {
            deleteRecursive(temp);
            if (!temp.mkdirs()) throw new Exception("temp");
            InputStream raw = getContentResolver().openInputStream(uri);
            if (raw == null) throw new Exception("no input");
            unzipTo(raw, temp);
            raw.close();

            File importedDb = new File(new File(temp, "database"), DbHelper.DB_NAME);
            if (!importedDb.exists()) throw new Exception("missing db");
            db.close();
            copyFile(importedDb, getDatabasePath(DbHelper.DB_NAME));
            deleteIfExists(new File(getDatabasePath(DbHelper.DB_NAME).getAbsolutePath() + "-wal"));
            deleteIfExists(new File(getDatabasePath(DbHelper.DB_NAME).getAbsolutePath() + "-shm"));

            File receipts = new File(getFilesDir(), "receipts");
            deleteRecursive(receipts);
            receipts.mkdirs();
            File importedReceipts = new File(temp, "receipts");
            copyDir(importedReceipts, receipts);
            File sounds = new File(getFilesDir(), "sounds");
            deleteRecursive(sounds);
            sounds.mkdirs();
            copyDir(new File(temp, "sounds"), sounds);
            restoreSettings(new File(new File(temp, "settings"), "settings.json"));
            db = new DbHelper(this);
            db.normalizeAttachmentPaths(receipts.getAbsolutePath());
            deleteRecursive(temp);
            toast("完整备份已导入");
            showHome();
        } catch (Exception e) {
            db = new DbHelper(this);
            toast("备份导入失败");
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                File dir = new File(getFilesDir(), "receipts");
                if (!dir.exists()) dir.mkdirs();
                File out = uniqueReceiptFile("receipt_" + System.currentTimeMillis() + ".jpg");
                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[8192];
                int len;
                while (in != null && (len = in.read(buf)) > 0) fos.write(buf, 0, len);
                if (in != null) in.close();
                fos.close();
                pendingImages.add(out.getAbsolutePath());
                if (imageHint != null) imageHint.setText("已添加 " + pendingImages.size() + " 张图片");
            } catch (Exception e) {
                toast("图片保存失败");
            }
        }
        if (requestCode == CREATE_BACKUP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            exportFullBackup(data.getData());
        }
        if (requestCode == IMPORT_BACKUP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importFullBackup(data.getData());
        }
        if (requestCode == SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) aiHandleUserText(results.get(0));
            else toast("没有识别到语音内容");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) showRecordSoundDialog();
            else toast("需要麦克风权限才能录制音效");
        }
        if (requestCode == SPEECH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startAiSpeechInput();
            else toast("需要麦克风权限才能语音记账");
        }
    }

    private LinearLayout monthSwitcher(Runnable refresh) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = outlineButton("<");
        prev.setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, -1);
            refresh.run();
        });
        Button next = outlineButton(">");
        next.setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, 1);
            refresh.run();
        });
        TextView month = label(monthKey(visibleMonth), 18, ink, true);
        month.setGravity(Gravity.CENTER);
        month.setOnClickListener(v -> pickMonth(refresh));
        row.addView(prev, new LinearLayout.LayoutParams(dp(54), dp(44)));
        row.addView(month, new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(next, new LinearLayout.LayoutParams(dp(54), dp(44)));
        return row;
    }

    private LinearLayout listDateSwitcher(Runnable refresh) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = outlineButton("<");
        prev.setOnClickListener(v -> {
            if (listDayMode) visibleDay.add(Calendar.DAY_OF_MONTH, -1);
            else visibleMonth.add(Calendar.MONTH, -1);
            refresh.run();
        });
        Button next = outlineButton(">");
        next.setOnClickListener(v -> {
            if (listDayMode) visibleDay.add(Calendar.DAY_OF_MONTH, 1);
            else visibleMonth.add(Calendar.MONTH, 1);
            refresh.run();
        });
        TextView current = label(listDayMode ? formatDateOnly(visibleDay.getTimeInMillis()) : monthKey(visibleMonth), 18, ink, true);
        current.setGravity(Gravity.CENTER);
        touchable(current);
        current.setOnClickListener(v -> pickListDate(refresh));
        row.addView(prev, new LinearLayout.LayoutParams(dp(54), dp(44)));
        row.addView(current, new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(next, new LinearLayout.LayoutParams(dp(54), dp(44)));
        box.addView(row);
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        Button month = listDayMode ? outlineButton("按月") : primaryButton("按月");
        month.setOnClickListener(v -> {
            listDayMode = false;
            refresh.run();
        });
        Button day = listDayMode ? primaryButton("按日") : outlineButton("按日");
        day.setOnClickListener(v -> {
            listDayMode = true;
            visibleMonth.setTimeInMillis(visibleDay.getTimeInMillis());
            refresh.run();
        });
        Button today = outlineButton("今天");
        today.setOnClickListener(v -> {
            listDayMode = true;
            visibleDay = Calendar.getInstance();
            visibleMonth.setTimeInMillis(visibleDay.getTimeInMillis());
            refresh.run();
        });
        modes.addView(month, new LinearLayout.LayoutParams(0, dp(42), 1));
        modes.addView(day, new LinearLayout.LayoutParams(0, dp(42), 1));
        modes.addView(today, new LinearLayout.LayoutParams(0, dp(42), 1));
        box.addView(modes);
        return box;
    }

    private LinearLayout statsDateSwitcher(Runnable refresh) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, dp(4));
        LinearLayout types = new LinearLayout(this);
        types.setOrientation(LinearLayout.HORIZONTAL);
        types.setBackground(round(Color.argb(120, 255, 255, 255), dp(6)));
        TextView expense = segmentChip("支出", "expense".equals(statsType));
        expense.setOnClickListener(v -> {
            statsType = "expense";
            refresh.run();
        });
        TextView income = segmentChip("收入", "income".equals(statsType));
        income.setOnClickListener(v -> {
            statsType = "income";
            refresh.run();
        });
        types.addView(expense, new LinearLayout.LayoutParams(0, dp(40), 1));
        types.addView(income, new LinearLayout.LayoutParams(0, dp(40), 1));
        box.addView(types);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev = arrowChip("<");
        prev.setOnClickListener(v -> {
            if ("month".equals(statsPeriod)) visibleMonth.add(Calendar.MONTH, -1);
            if ("year".equals(statsPeriod)) visibleMonth.add(Calendar.YEAR, -1);
            refresh.run();
        });
        TextView next = arrowChip(">");
        next.setOnClickListener(v -> {
            if ("month".equals(statsPeriod)) visibleMonth.add(Calendar.MONTH, 1);
            if ("year".equals(statsPeriod)) visibleMonth.add(Calendar.YEAR, 1);
            refresh.run();
        });
        TextView current = label(statsRangeLabel(), 24, ink, true);
        current.setGravity(Gravity.CENTER);
        touchable(current);
        current.setOnClickListener(v -> pickStatsDate(refresh));
        row.addView(prev, new LinearLayout.LayoutParams(dp(44), dp(44)));
        row.addView(current, new LinearLayout.LayoutParams(0, dp(58), 1));
        row.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44)));
        box.addView(row);

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setBackground(round(Color.argb(120, 255, 255, 255), dp(6)));
        TextView month = segmentChip("月", "month".equals(statsPeriod));
        month.setOnClickListener(v -> {
            statsPeriod = "month";
            refresh.run();
        });
        TextView year = segmentChip("年", "year".equals(statsPeriod));
        year.setOnClickListener(v -> {
            statsPeriod = "year";
            refresh.run();
        });
        TextView all = segmentChip("总", "all".equals(statsPeriod));
        all.setOnClickListener(v -> {
            statsPeriod = "all";
            refresh.run();
        });
        modes.addView(month, new LinearLayout.LayoutParams(0, dp(40), 1));
        modes.addView(year, new LinearLayout.LayoutParams(0, dp(40), 1));
        modes.addView(all, new LinearLayout.LayoutParams(0, dp(40), 1));
        box.addView(modes);
        return box;
    }

    private void pickListDate(Runnable refresh) {
        Calendar c = listDayMode ? (Calendar) visibleDay.clone() : (Calendar) visibleMonth.clone();
        new DatePickerDialog(this, (view, year, month, day) -> {
            visibleDay.set(year, month, day);
            visibleMonth.set(year, month, 1);
            listDayMode = true;
            refresh.run();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickStatsDate(Runnable refresh) {
        if ("all".equals(statsPeriod)) return;
        Calendar c = (Calendar) visibleMonth.clone();
        new DatePickerDialog(this, (view, year, month, day) -> {
            visibleMonth.set(year, month, 1);
            refresh.run();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickMonth(Runnable refresh) {
        Calendar c = (Calendar) visibleMonth.clone();
        new DatePickerDialog(this, (view, year, month, day) -> {
            visibleMonth.set(year, month, 1);
            refresh.run();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private TextView headerMetric(String title, long cents) {
        TextView tv = label(title + "\n" + money(cents), 18, ink, false);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private TextView metric(String title, long value, int color) {
        return metric(title, value, color, true);
    }

    private TextView metric(String title, long value, int color, boolean moneyValue) {
        TextView tv = label(title + "\n" + (moneyValue ? money(value) : String.valueOf(value)), 14, color, false);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private String[] trendLabels() {
        if ("all".equals(statsPeriod)) {
            int year = visibleMonth.get(Calendar.YEAR);
            return new String[]{String.valueOf(year - 4), String.valueOf(year - 3), String.valueOf(year - 2), String.valueOf(year - 1), String.valueOf(year)};
        }
        if ("year".equals(statsPeriod)) return new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        String[] labels = new String[6];
        Calendar c = (Calendar) visibleMonth.clone();
        c.add(Calendar.MONTH, -5);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = String.valueOf(c.get(Calendar.MONTH) + 1);
            c.add(Calendar.MONTH, 1);
        }
        return labels;
    }

    private long[] trendValues() {
        if ("all".equals(statsPeriod)) {
            long[] values = new long[5];
            Calendar c = (Calendar) visibleMonth.clone();
            c.add(Calendar.YEAR, -4);
            for (int i = 0; i < values.length; i++) {
                long[] r = yearRange(c);
                values[i] = db.sum(statsType, r[0], r[1]);
                c.add(Calendar.YEAR, 1);
            }
            return values;
        }
        if ("year".equals(statsPeriod)) {
            long[] values = new long[12];
            Calendar c = (Calendar) visibleMonth.clone();
            c.set(Calendar.MONTH, Calendar.JANUARY);
            for (int i = 0; i < values.length; i++) {
                long[] r = monthRange(c);
                values[i] = db.sum(statsType, r[0], r[1]);
                c.add(Calendar.MONTH, 1);
            }
            return values;
        }
        long[] values = new long[6];
        Calendar c = (Calendar) visibleMonth.clone();
        c.add(Calendar.MONTH, -5);
        for (int i = 0; i < values.length; i++) {
            long[] r = monthRange(c);
            values[i] = db.sum(statsType, r[0], r[1]);
            c.add(Calendar.MONTH, 1);
        }
        return values;
    }

    private View shortcut(String title, String icon, Runnable action) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView badge = label(icon, 22, ink, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(circle(soft, line));
        box.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView name = label(title, 13, muted, false);
        name.setGravity(Gravity.CENTER);
        box.addView(name, new LinearLayout.LayoutParams(-1, dp(30)));
        touchable(box);
        box.setOnClickListener(v -> action.run());
        return box;
    }

    private LinearLayout page(String title) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), statusBarHeight() + dp(12), dp(16), dp(110));
        scroll.addView(page);
        page.setTag(scroll);
        if (title != null && title.length() > 0) page.addView(label(title, 28, ink, true));
        return page;
    }

    private void setPage(LinearLayout page) {
        content.removeAllViews();
        content.addView((View) page.getTag(), new FrameLayout.LayoutParams(-1, -1));
    }

    private void setContent(View view) {
        content.removeAllViews();
        content.addView(view, new FrameLayout.LayoutParams(-1, -1));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(Color.WHITE, dp(8)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(lp);
        return card;
    }

    private View iconBadge(String name, String type, int size, boolean add) {
        return new IconBadge(this, name, type, add);
    }

    private View statRow(DbHelper.Stat stat, long total) {
        int pct = total <= 0 ? 0 : (int) (stat.amount * 100 / total);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(round(Color.WHITE, dp(8)));
        row.setPadding(dp(10), dp(10), dp(12), dp(10));
        row.addView(iconBadge(stat.name, statsType, dp(44), false), new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(10), 0, dp(10), 0);
        mid.addView(label(stat.name + "    " + pct + "%", 16, ink, false), new LinearLayout.LayoutParams(-1, dp(24)));
        mid.addView(progressBar(pct, "income".equals(statsType) ? green : red), new LinearLayout.LayoutParams(-1, dp(8)));
        row.addView(mid, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView amount = label(money(stat.amount), 16, "income".equals(statsType) ? green : red, false);
        amount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(amount, new LinearLayout.LayoutParams(dp(112), dp(44)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(lp);
        return row;
    }

    private View progressBar(int pct, int color) {
        LinearLayout outer = new LinearLayout(this);
        outer.setBackground(round(soft, dp(8)));
        View fill = new View(this);
        fill.setBackground(round(color, dp(8)));
        outer.addView(fill, new LinearLayout.LayoutParams(0, -1, Math.max(1, pct)));
        View rest = new View(this);
        outer.addView(rest, new LinearLayout.LayoutParams(0, -1, Math.max(1, 100 - pct)));
        return outer;
    }

    private TextView sectionTitle(String text) {
        TextView tv = label(text, 17, ink, true);
        tv.setPadding(0, dp(14), 0, dp(6));
        return tv;
    }

    private TextView empty(String text) {
        TextView tv = label(text, 14, muted, false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(24), 0, dp(24));
        return tv;
    }

    private Button primaryButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(ink);
        b.setBackgroundColor(yellow);
        b.setAllCaps(false);
        touchable(b);
        return b;
    }

    private Button outlineButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(ink);
        b.setBackground(round(soft, dp(4)));
        b.setAllCaps(false);
        touchable(b);
        return b;
    }

    private Button filterButton(String text, boolean selected) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(selected ? ink : muted);
        b.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        b.setBackground(selected ? round(yellow, dp(4)) : round(soft, dp(4)));
        b.setAllCaps(false);
        touchable(b);
        return b;
    }

    private TextView segmentChip(String text, boolean selected) {
        TextView tv = label(text, 16, selected ? ink : muted, selected);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(selected ? round(Color.WHITE, dp(6)) : round(Color.TRANSPARENT, dp(6)));
        touchable(tv);
        return tv;
    }

    private TextView arrowChip(String text) {
        TextView tv = label(text, 22, ink, false);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(round(Color.argb(120, 255, 255, 255), dp(8)));
        touchable(tv);
        return tv;
    }

    private TextView quickChip(String text) {
        TextView tv = label(text, 15, ink, false);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(round(soft, dp(4)));
        touchable(tv);
        return tv;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setIncludeFontPadding(true);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private <T extends View> T touchable(T view) {
        view.setClickable(true);
        view.setFocusable(true);
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        Drawable ripple = getDrawable(outValue.resourceId);
        view.setForeground(ripple);
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                playClickSound();
                playClickVibration();
            }
            return false;
        });
        return view;
    }

    private boolean soundEnabled() {
        return prefs == null || prefs.getBoolean("sound_enabled", true);
    }

    private void playClickSound() {
        if (soundEnabled()) previewSound(soundIndex());
    }

    private boolean vibrationEnabled() {
        return prefs != null && prefs.getBoolean("vibration_enabled", false);
    }

    private void playClickVibration() {
        if (!vibrationEnabled()) return;
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(18);
    }

    private int soundIndex() {
        if (prefs == null) return 0;
        int index = prefs.getInt("sound_tone", 0);
        return index >= 0 && index < SOUND_TONES.length ? index : 0;
    }

    private String soundName() {
        return SOUND_NAMES[soundIndex()];
    }

    private void previewSound(int index) {
        int safeIndex = index >= 0 && index < SOUND_TONES.length ? index : 0;
        int code = SOUND_TONES[safeIndex];
        if (code >= 0) {
            if (tone != null) tone.startTone(code, 45);
        } else if (code == CUSTOM_RECORD) {
            playRecordedSound();
        } else {
            playCustomSound(code);
        }
    }

    private File recordedSoundFile() {
        File dir = new File(getFilesDir(), "sounds");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "click_sound.m4a");
    }

    private void startRecording(TextView status, Button record) {
        try {
            stopRecording(false);
            releaseCustomPlayer();
            currentRecordingFile = recordedSoundFile();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(currentRecordingFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            status.setText("正在录音... 再点一次停止");
            record.setText("停止录音");
        } catch (Exception e) {
            stopRecording(false);
            toast("录音启动失败");
        }
    }

    private void stopRecording(boolean keepFile) {
        if (recorder == null) return;
        try {
            recorder.stop();
        } catch (Exception ignored) {
        }
        try {
            recorder.release();
        } catch (Exception ignored) {
        }
        recorder = null;
        if (!keepFile && currentRecordingFile != null && currentRecordingFile.exists()) currentRecordingFile.delete();
    }

    private void playRecordedSound() {
        File file = recordedSoundFile();
        if (!file.exists()) {
            toast("还没有录制音效");
            return;
        }
        try {
            releaseCustomPlayer();
            customPlayer = new MediaPlayer();
            customPlayer.setDataSource(file.getAbsolutePath());
            customPlayer.setOnCompletionListener(mp -> releaseCustomPlayer());
            customPlayer.prepare();
            customPlayer.start();
        } catch (Exception e) {
            releaseCustomPlayer();
            toast("录音播放失败");
        }
    }

    private void releaseCustomPlayer() {
        if (customPlayer == null) return;
        try {
            customPlayer.release();
        } catch (Exception ignored) {
        }
        customPlayer = null;
    }

    private void playCustomSound(int code) {
        new Thread(() -> {
            try {
                if (code == CUSTOM_CRYSTAL) playWave(new double[]{1760, 2349, 3136}, new int[]{28, 34, 42}, 0.55);
                else if (code == CUSTOM_BELL) playWave(new double[]{1568, 2093, 2637}, new int[]{45, 55, 65}, 0.5);
                else if (code == CUSTOM_CHIME) playWave(new double[]{1976, 2637, 3520, 2637}, new int[]{24, 30, 38, 42}, 0.48);
                else if (code == CUSTOM_COIN) playWave(new double[]{1661, 2217, 2959}, new int[]{18, 24, 52}, 0.6);
                else if (code == CUSTOM_POP) playSweep(520, 980, 62, 0.5, false);
                else if (code == CUSTOM_BUBBLE) playWave(new double[]{900, 1240, 1680}, new int[]{32, 30, 34}, 0.38);
                else if (code == CUSTOM_BIRD) playWave(new double[]{2600, 3300, 2800, 3600}, new int[]{22, 24, 20, 26}, 0.5);
                else if (code == CUSTOM_CAT) playSweep(760, 1120, 140, 0.48, true);
                else if (code == CUSTOM_DOG) {
                    playSweep(420, 260, 70, 0.62, false);
                    Thread.sleep(35);
                    playSweep(360, 230, 65, 0.55, false);
                } else if (code == CUSTOM_FROG) {
                    playSweep(260, 190, 95, 0.56, true);
                    Thread.sleep(30);
                    playSweep(310, 210, 82, 0.45, true);
                } else if (code == CUSTOM_SHEEP) {
                    playSweep(520, 690, 130, 0.44, true);
                    Thread.sleep(20);
                    playSweep(660, 500, 110, 0.38, true);
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void playWave(double[] freqs, int[] durations, double volume) {
        int total = 0;
        for (int duration : durations) total += duration;
        short[] samples = new short[total * 44100 / 1000];
        int pos = 0;
        for (int i = 0; i < freqs.length; i++) {
            int count = durations[i] * 44100 / 1000;
            for (int j = 0; j < count && pos < samples.length; j++, pos++) {
                double t = j / 44100.0;
                double env = 1.0 - (double) j / Math.max(1, count);
                samples[pos] = (short) (Math.sin(2 * Math.PI * freqs[i] * t) * 32767 * volume * env);
            }
        }
        playSamples(samples);
    }

    private void playSweep(double startFreq, double endFreq, int durationMs, double volume, boolean softVibrato) {
        int count = durationMs * 44100 / 1000;
        short[] samples = new short[count];
        double phase = 0;
        for (int i = 0; i < count; i++) {
            double p = (double) i / Math.max(1, count - 1);
            double freq = startFreq + (endFreq - startFreq) * p;
            if (softVibrato) freq += Math.sin(p * Math.PI * 10) * 55;
            phase += 2 * Math.PI * freq / 44100.0;
            double env = Math.sin(Math.PI * p);
            samples[i] = (short) (Math.sin(phase) * 32767 * volume * env);
        }
        playSamples(samples);
    }

    private void playSamples(short[] samples) {
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                samples.length * 2, AudioTrack.MODE_STATIC);
        track.write(samples, 0, samples.length);
        track.setNotificationMarkerPosition(samples.length);
        track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack audioTrack) {
                audioTrack.release();
            }

            @Override
            public void onPeriodicNotification(AudioTrack audioTrack) {
            }
        });
        track.play();
    }

    private GradientDrawable circle(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable round(int fill, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        return d;
    }

    private long parseAmount(String raw) {
        try {
            return Math.round(Double.parseDouble(raw.trim()) * 100);
        } catch (Exception e) {
            return 0;
        }
    }

    private long firstId(List<DbHelper.Item> items) {
        return items.isEmpty() ? 0 : items.get(0).id;
    }

    private long firstDifferentAccount(long accountId) {
        for (DbHelper.Item item : db.accounts()) {
            if (item.id != accountId) return item.id;
        }
        return 0;
    }

    private DbHelper.Item categoryById(String type, long id) {
        for (DbHelper.Item item : db.categories(type)) {
            if (item.id == id) return item;
        }
        return null;
    }

    private long[] monthRange(Calendar base) {
        Calendar c = (Calendar) base.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        return new long[]{start, c.getTimeInMillis()};
    }

    private long[] yearRange(Calendar base) {
        Calendar c = (Calendar) base.clone();
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.YEAR, 1);
        return new long[]{start, c.getTimeInMillis()};
    }

    private long[] statsRange() {
        if ("year".equals(statsPeriod)) return yearRange(visibleMonth);
        if ("all".equals(statsPeriod)) return new long[]{0, 0};
        return monthRange(visibleMonth);
    }

    private String statsRangeLabel() {
        if ("year".equals(statsPeriod)) return new SimpleDateFormat("yyyy年", Locale.CHINA).format(visibleMonth.getTime());
        if ("all".equals(statsPeriod)) return "全部";
        return monthKey(visibleMonth);
    }

    private long[] dayRange(Calendar base) {
        return dayRange(base.getTimeInMillis());
    }

    private long[] dayRange(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, 1);
        return new long[]{start, c.getTimeInMillis()};
    }

    private int daysInMonth(Calendar base) {
        return base.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private String typeText(String type) {
        if ("income".equals(type)) return "收入";
        if ("transfer".equals(type)) return "转账";
        return "支出";
    }

    private String monthKey(Calendar c) {
        return new SimpleDateFormat("yyyy-MM", Locale.CHINA).format(c.getTime());
    }

    private String formatDateTime(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(time);
    }

    private String formatDateOnly(long time) {
        return new SimpleDateFormat("yyyy-MM-dd E", Locale.CHINA).format(time);
    }

    private String shortDateLabel(long time) {
        long[] today = dayRange(System.currentTimeMillis());
        if (time >= today[0] && time < today[1]) return "今天";
        return new SimpleDateFormat("MM-dd E", Locale.CHINA).format(time);
    }

    private String formatDay(long time) {
        return new SimpleDateFormat("MM月dd日 E", Locale.CHINA).format(time);
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(time);
    }

    private String money(long cents) {
        return DbHelper.formatMoney(cents);
    }

    private void checkpointDb() {
        android.database.Cursor c = db.getReadableDatabase().rawQuery("PRAGMA wal_checkpoint(FULL)", null);
        if (c != null) c.close();
    }

    private void addFileToZip(ZipOutputStream zip, File file, String name) throws Exception {
        if (file == null || !file.exists() || !file.isFile()) return;
        zip.putNextEntry(new ZipEntry(name));
        FileInputStream in = new FileInputStream(file);
        copyStream(in, zip);
        in.close();
        zip.closeEntry();
    }

    private void addDirToZip(ZipOutputStream zip, File dir, String prefix) throws Exception {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = prefix + "/" + file.getName();
            if (file.isDirectory()) addDirToZip(zip, file, name);
            else addFileToZip(zip, file, name);
        }
    }

    private void addTextToZip(ZipOutputStream zip, String name, String text) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        byte[] bytes = text.getBytes("UTF-8");
        zip.write(bytes, 0, bytes.length);
        zip.closeEntry();
    }

    private void unzipTo(InputStream raw, File dest) throws Exception {
        ZipInputStream zip = new ZipInputStream(raw);
        ZipEntry entry;
        byte[] buf = new byte[8192];
        String root = dest.getCanonicalPath() + File.separator;
        while ((entry = zip.getNextEntry()) != null) {
            File out = new File(dest, entry.getName());
            String outPath = out.getCanonicalPath();
            if (!outPath.startsWith(root)) throw new Exception("bad zip");
            if (entry.isDirectory()) {
                out.mkdirs();
            } else {
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                FileOutputStream fos = new FileOutputStream(out);
                int len;
                while ((len = zip.read(buf)) > 0) fos.write(buf, 0, len);
                fos.close();
            }
            zip.closeEntry();
        }
        zip.close();
    }

    private void copyFile(File from, File to) throws Exception {
        File parent = to.getParentFile();
        if (parent != null) parent.mkdirs();
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        copyStream(in, out);
        in.close();
        out.close();
    }

    private File uniqueReceiptFile(String originalName) {
        File dir = new File(getFilesDir(), "receipts");
        if (!dir.exists()) dir.mkdirs();
        String clean = originalName == null ? "image.jpg" : originalName.replace("\\", "/");
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) clean = clean.substring(slash + 1);
        if (clean.length() == 0) clean = "image.jpg";
        File out = new File(dir, clean);
        if (!out.exists()) return out;
        String name = clean;
        String ext = "";
        int dot = clean.lastIndexOf('.');
        if (dot > 0) {
            name = clean.substring(0, dot);
            ext = clean.substring(dot);
        }
        int i = 1;
        do {
            out = new File(dir, name + "_" + System.currentTimeMillis() + "_" + i + ext);
            i++;
        } while (out.exists());
        return out;
    }

    private void copyDir(File from, File to) throws Exception {
        if (from == null || !from.exists()) return;
        if (from.isDirectory()) {
            to.mkdirs();
            File[] files = from.listFiles();
            if (files == null) return;
            for (File file : files) copyDir(file, new File(to, file.getName()));
        } else {
            copyFile(from, to);
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
    }

    private String readText(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copyStream(in, out);
        in.close();
        return new String(out.toByteArray(), "UTF-8");
    }

    private String settingsJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("sound_enabled", soundEnabled());
        json.put("sound_tone", soundIndex());
        json.put("vibration_enabled", vibrationEnabled());
        json.put("pet_adopted", petAdopted());
        json.put("pet_type", petType());
        json.put("pet_name", petName());
        json.put("pet_level", petLevel());
        json.put("pet_exp", petExp());
        json.put("pet_coins", petCoins());
        json.put("pet_mood", petMood());
        json.put("pet_affinity", petAffinity());
        json.put("pet_item_bell", hasPetItem("bell"));
        json.put("pet_item_ball", hasPetItem("ball"));
        json.put("pet_item_home", hasPetItem("home"));
        return json.toString();
    }

    private void restoreSettings(File file) throws Exception {
        if (file == null || !file.exists()) return;
        JSONObject json = new JSONObject(readText(file));
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("sound_enabled", json.optBoolean("sound_enabled", true));
        editor.putInt("sound_tone", json.optInt("sound_tone", 0));
        editor.putBoolean("vibration_enabled", json.optBoolean("vibration_enabled", false));
        editor.putBoolean("pet_adopted", json.optBoolean("pet_adopted", false));
        editor.putString("pet_type", json.optString("pet_type", "cat"));
        editor.putString("pet_name", json.optString("pet_name", "小秋猫"));
        editor.putInt("pet_level", json.optInt("pet_level", 1));
        editor.putInt("pet_exp", json.optInt("pet_exp", 0));
        editor.putInt("pet_coins", json.optInt("pet_coins", 0));
        editor.putInt("pet_mood", json.optInt("pet_mood", 70));
        editor.putInt("pet_affinity", json.optInt("pet_affinity", 20));
        editor.putBoolean("pet_item_bell", json.optBoolean("pet_item_bell", false));
        editor.putBoolean("pet_item_ball", json.optBoolean("pet_item_ball", false));
        editor.putBoolean("pet_item_home", json.optBoolean("pet_item_home", false));
        editor.apply();
    }

    private void deleteIfExists(File file) {
        if (file != null && file.exists()) file.delete();
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) deleteRecursive(child);
            }
        }
        file.delete();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int chartColor(int index) {
        int[] colors = {
                Color.rgb(229, 93, 79),
                Color.rgb(233, 158, 67),
                Color.rgb(64, 117, 210),
                Color.rgb(125, 97, 184),
                Color.rgb(47, 125, 104),
                Color.rgb(41, 156, 166),
                Color.rgb(213, 95, 138),
                Color.rgb(120, 126, 134)
        };
        return colors[index % colors.length];
    }

    private class DonutChartView extends View {
        private final List<DbHelper.Stat> stats;
        private final long total;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        DonutChartView(Activity context, List<DbHelper.Stat> stats, long total) {
            super(context);
            this.stats = stats;
            this.total = total;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.min(w, h) * 0.34f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeWidth(dp(24));
            RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            if (total <= 0 || stats.isEmpty()) {
                paint.setColor(line);
                canvas.drawArc(oval, -90, 360, false, paint);
            } else {
                float start = -90f;
                long drawn = 0;
                int limit = Math.min(stats.size(), 8);
                for (int i = 0; i < limit; i++) {
                    long amount = stats.get(i).amount;
                    drawn += amount;
                    float sweep = i == limit - 1 ? 360f - (start + 90f) : Math.max(2f, amount * 360f / total);
                    paint.setColor(chartColor(i));
                    canvas.drawArc(oval, start, sweep, false, paint);
                    start += sweep;
                }
                if (stats.size() > limit && drawn < total) {
                    paint.setColor(chartColor(limit));
                    canvas.drawArc(oval, start, (total - drawn) * 360f / total, false, paint);
                }
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(muted);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(dp(14));
            canvas.drawText("income".equals(statsType) ? "总收入" : "总支出", cx, cy - dp(8), paint);
            paint.setColor("income".equals(statsType) ? green : red);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dp(20));
            canvas.drawText(money(total), cx, cy + dp(20), paint);
        }
    }

    private class PetView extends View {
        private final String type;
        private final int sceneColor;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long startedAt = System.currentTimeMillis();

        PetView(Activity context, String type, int sceneColor) {
            super(context);
            this.type = type == null ? "cat" : type;
            this.sceneColor = sceneColor;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float t = (System.currentTimeMillis() - startedAt) / 1000f;
            float bob = (float) Math.sin(t * 2.4f) * dp(3);
            String action = System.currentTimeMillis() < petActionUntil ? petAction : "idle";
            if ("play".equals(action)) bob -= Math.abs((float) Math.sin(t * 8f)) * dp(12);
            float cy = h * 0.53f + bob;
            boolean happy = !petAdopted() || petMood() >= 70;
            paint.setStyle(Paint.Style.FILL);
            int roomColor = hasPetItem("home") ? Color.rgb(255, 241, 210) : sceneColor;
            paint.setShader(new LinearGradient(0, 0, 0, h, lighten(roomColor, 28), roomColor, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(new RectF(dp(10), dp(8), w - dp(10), h - dp(8)), dp(24), dp(24), paint);
            paint.setShader(null);
            paint.setColor(Color.argb(90, 255, 255, 255));
            canvas.drawCircle(cx - w * 0.24f, h * 0.2f, dp(22), paint);
            canvas.drawCircle(cx + w * 0.26f, h * 0.28f, dp(13), paint);
            paint.setColor(Color.argb(35, 255, 255, 255));
            canvas.drawCircle(cx + w * 0.12f, h * 0.16f, dp(30), paint);
            drawPetRoom(canvas, w, h, cx);

            paint.setColor(Color.argb(42, 40, 45, 52));
            canvas.drawOval(new RectF(cx - dp(62), h - dp(42), cx + dp(62), h - dp(20)), paint);

            int base = petBaseColor();
            int detailColor = petDetailColor();
            int dark = darken(base, 28);
            int light = lighten(base, 34);

            drawTail(canvas, cx, cy, base, dark);
            drawFeet(canvas, cx, cy, base, light, dark);
            paint.setShader(new RadialGradient(cx - dp(18), cy + dp(26), dp(78), light, dark, Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(cx - dp(48), cy + dp(20), cx + dp(48), cy + dp(88)), paint);
            paint.setShader(null);
            drawArms(canvas, cx, cy, base, light, dark, "feed".equals(action));

            drawEars(canvas, cx, cy, base, light, dark, detailColor);

            paint.setShader(new RadialGradient(cx - dp(20), cy - dp(18), dp(70), light, dark, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, dp(52), paint);
            paint.setShader(null);

            paint.setColor(Color.argb(72, 255, 255, 255));
            canvas.drawOval(new RectF(cx - dp(34), cy - dp(42), cx - dp(4), cy - dp(16)), paint);
            paint.setColor(Color.argb(32, 70, 50, 35));
            canvas.drawArc(new RectF(cx + dp(10), cy + dp(8), cx + dp(50), cy + dp(46)), 310, 80, false, paint);

            paint.setColor(Color.rgb(38, 45, 52));
            canvas.drawCircle(cx - dp(18), cy - dp(6), dp(5), paint);
            canvas.drawCircle(cx + dp(18), cy - dp(6), dp(5), paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx - dp(20), cy - dp(8), dp(1.5f), paint);
            canvas.drawCircle(cx + dp(16), cy - dp(8), dp(1.5f), paint);
            paint.setColor(detailColor);
            canvas.drawOval(new RectF(cx - dp(5), cy + dp(4), cx + dp(5), cy + dp(11)), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(cx, cy + dp(10), cx, cy + dp(16), paint);
            if ("feed".equals(action)) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawOval(new RectF(cx - dp(10), cy + dp(14), cx + dp(10), cy + dp(28)), paint);
                paint.setStyle(Paint.Style.STROKE);
            } else if ("touch".equals(action)) {
                canvas.drawArc(new RectF(cx - dp(18), cy + dp(12), cx, cy + dp(28)), 20, 120, false, paint);
                canvas.drawArc(new RectF(cx, cy + dp(12), cx + dp(18), cy + dp(28)), 40, 120, false, paint);
                paint.setColor(Color.rgb(38, 45, 52));
                canvas.drawLine(cx - dp(23), cy - dp(8), cx - dp(14), cy - dp(4), paint);
                canvas.drawLine(cx + dp(14), cy - dp(4), cx + dp(23), cy - dp(8), paint);
            } else if (happy) {
                canvas.drawArc(new RectF(cx - dp(18), cy + dp(9), cx, cy + dp(29)), 20, 120, false, paint);
                canvas.drawArc(new RectF(cx, cy + dp(9), cx + dp(18), cy + dp(29)), 40, 120, false, paint);
            } else {
                canvas.drawArc(new RectF(cx - dp(12), cy + dp(20), cx + dp(12), cy + dp(36)), 200, 140, false, paint);
            }
            if ("cat".equals(type) || "fox".equals(type)) {
                canvas.drawLine(cx - dp(26), cy + dp(8), cx - dp(48), cy + dp(2), paint);
                canvas.drawLine(cx - dp(26), cy + dp(16), cx - dp(48), cy + dp(18), paint);
                canvas.drawLine(cx + dp(26), cy + dp(8), cx + dp(48), cy + dp(2), paint);
                canvas.drawLine(cx + dp(26), cy + dp(16), cx + dp(48), cy + dp(18), paint);
            }
            if ("panda".equals(type)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(40, 45, 48));
                canvas.drawOval(new RectF(cx - dp(34), cy - dp(20), cx - dp(8), cy + dp(4)), paint);
                canvas.drawOval(new RectF(cx + dp(8), cy - dp(20), cx + dp(34), cy + dp(4)), paint);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(cx - dp(18), cy - dp(6), dp(5), paint);
                canvas.drawCircle(cx + dp(18), cy - dp(6), dp(5), paint);
                paint.setColor(Color.rgb(38, 45, 52));
                canvas.drawCircle(cx - dp(18), cy - dp(6), dp(3), paint);
                canvas.drawCircle(cx + dp(18), cy - dp(6), dp(3), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(85, 213, 95, 138));
            canvas.drawCircle(cx - dp(30), cy + dp(14), dp(7), paint);
            canvas.drawCircle(cx + dp(30), cy + dp(14), dp(7), paint);
            paint.setColor(Color.argb(38, 255, 255, 255));
            canvas.drawOval(new RectF(cx - dp(44), cy + dp(56), cx + dp(44), cy + dp(86)), paint);
            drawPetDecorations(canvas, cx, cy, action);
            postInvalidateDelayed(16);
        }

        private void drawPetRoom(Canvas canvas, float w, float h, float cx) {
            paint.setStyle(Paint.Style.FILL);
            if (hasPetItem("home")) {
                paint.setColor(Color.rgb(218, 174, 114));
                canvas.drawRoundRect(new RectF(cx - dp(72), h - dp(150), cx + dp(72), h - dp(54)), dp(20), dp(20), paint);
                paint.setColor(Color.rgb(184, 126, 82));
                Path roof = new Path();
                roof.moveTo(cx - dp(84), h - dp(142));
                roof.lineTo(cx, h - dp(198));
                roof.lineTo(cx + dp(84), h - dp(142));
                roof.close();
                canvas.drawPath(roof, paint);
                paint.setColor(Color.rgb(255, 232, 190));
                canvas.drawRoundRect(new RectF(cx - dp(30), h - dp(122), cx + dp(30), h - dp(54)), dp(18), dp(18), paint);
            }
            paint.setShader(new RadialGradient(cx, h - dp(36), dp(92), Color.rgb(255, 230, 146), Color.rgb(238, 188, 84), Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(cx - dp(96), h - dp(76), cx + dp(96), h - dp(18)), paint);
            paint.setShader(null);
            for (int i = 0; i < Math.min(8, petLevel()); i++) {
                paint.setColor(Color.argb(120, 255, 255, 255));
                float x = dp(34) + i * (w - dp(68)) / Math.max(1, Math.min(8, petLevel()));
                canvas.drawCircle(x, dp(26 + (i % 2) * 18), dp(3), paint);
            }
        }

        private void drawTail(Canvas canvas, float cx, float cy, int base, int dark) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(13));
            paint.setColor(darken(base, 18));
            if ("cat".equals(type)) {
                canvas.drawArc(new RectF(cx + dp(34), cy + dp(18), cx + dp(92), cy + dp(78)), 130, -235, false, paint);
            } else if ("fox".equals(type)) {
                paint.setStrokeWidth(dp(18));
                canvas.drawArc(new RectF(cx + dp(32), cy + dp(18), cx + dp(102), cy + dp(82)), 130, -220, false, paint);
                paint.setColor(Color.rgb(255, 250, 232));
                canvas.drawCircle(cx + dp(88), cy + dp(34), dp(10), paint);
            } else if ("dog".equals(type)) {
                canvas.drawArc(new RectF(cx + dp(34), cy + dp(22), cx + dp(84), cy + dp(68)), 180, 180, false, paint);
            } else if ("rabbit".equals(type) || "hamster".equals(type)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor("hamster".equals(type) ? lighten(base, 20) : Color.rgb(245, 245, 245));
                canvas.drawCircle(cx + dp(44), cy + dp(58), dp(13), paint);
            } else if ("panda".equals(type)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(40, 45, 48));
                canvas.drawCircle(cx + dp(44), cy + dp(58), dp(10), paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawFeet(Canvas canvas, float cx, float cy, int base, int light, int dark) {
            paint.setShader(new RadialGradient(cx - dp(26), cy + dp(84), dp(22), light, dark, Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(cx - dp(48), cy + dp(70), cx - dp(8), cy + dp(100)), paint);
            paint.setShader(new RadialGradient(cx + dp(26), cy + dp(84), dp(22), light, dark, Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(cx + dp(8), cy + dp(70), cx + dp(48), cy + dp(100)), paint);
            paint.setShader(null);
        }

        private void drawArms(Canvas canvas, float cx, float cy, int base, int light, int dark, boolean feeding) {
            paint.setShader(new RadialGradient(cx - dp(48), cy + dp(38), dp(26), light, dark, Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(cx - dp(66), cy + dp(18), cx - dp(30), cy + dp(62)), paint);
            paint.setShader(new RadialGradient(cx + dp(48), cy + dp(38), dp(26), light, dark, Shader.TileMode.CLAMP));
            canvas.drawOval(new RectF(cx + dp(30), cy + dp(18), cx + dp(66), cy + dp(62)), paint);
            paint.setShader(null);
        }

        private void drawPetDecorations(Canvas canvas, float cx, float cy, String action) {
            paint.setStyle(Paint.Style.FILL);
            if (hasPetItem("bell")) {
                paint.setColor(Color.rgb(241, 190, 48));
                canvas.drawCircle(cx, cy + dp(58), dp(10), paint);
                paint.setColor(Color.rgb(126, 86, 30));
                canvas.drawCircle(cx, cy + dp(61), dp(2), paint);
            }
            if (hasPetItem("ball") || "play".equals(action)) {
                float bx = cx + dp(86);
                float by = cy + dp(86) - ("play".equals(action) ? Math.abs((float) Math.sin((System.currentTimeMillis() - startedAt) / 140.0)) * dp(26) : 0);
                paint.setColor(Color.rgb(78, 130, 219));
                canvas.drawCircle(bx, by, dp(17), paint);
                paint.setColor(Color.argb(130, 255, 255, 255));
                canvas.drawArc(new RectF(bx - dp(13), by - dp(13), bx + dp(13), by + dp(13)), 210, 90, false, paint);
            }
            if ("feed".equals(action)) {
                paint.setColor(Color.rgb(246, 246, 246));
                canvas.drawOval(new RectF(cx - dp(100), cy + dp(78), cx - dp(48), cy + dp(104)), paint);
                paint.setColor(Color.rgb(228, 98, 77));
                canvas.drawOval(new RectF(cx - dp(90), cy + dp(74), cx - dp(58), cy + dp(94)), paint);
            }
            if ("touch".equals(action) || "levelup".equals(action)) {
                paint.setColor(Color.rgb(232, 92, 132));
                drawHeart(canvas, cx + dp(66), cy - dp(64), dp(10));
                drawHeart(canvas, cx - dp(70), cy - dp(42), dp(7));
            }
            if ("levelup".equals(action)) {
                paint.setColor(Color.rgb(255, 205, 64));
                canvas.drawCircle(cx - dp(84), cy - dp(82), dp(5), paint);
                canvas.drawCircle(cx + dp(88), cy - dp(86), dp(6), paint);
                canvas.drawCircle(cx + dp(74), cy + dp(6), dp(4), paint);
            }
        }

        private void drawHeart(Canvas canvas, float cx, float cy, float s) {
            Path p = new Path();
            p.moveTo(cx, cy + s);
            p.cubicTo(cx - s * 2, cy - s * 0.3f, cx - s, cy - s * 1.5f, cx, cy - s * 0.5f);
            p.cubicTo(cx + s, cy - s * 1.5f, cx + s * 2, cy - s * 0.3f, cx, cy + s);
            canvas.drawPath(p, paint);
        }

        private void drawEars(Canvas canvas, float cx, float cy, int base, int light, int dark, int detailColor) {
            if ("cat".equals(type) || "fox".equals(type)) {
                paint.setShader(new LinearGradient(cx - dp(58), cy - dp(78), cx - dp(20), cy - dp(28), light, dark, Shader.TileMode.CLAMP));
                Path left = new Path();
                left.moveTo(cx - dp(38), cy - dp(28));
                left.lineTo(cx - dp(58), cy - dp(78));
                left.lineTo(cx - dp(12), cy - dp(48));
                canvas.drawPath(left, paint);
                paint.setShader(new LinearGradient(cx + dp(58), cy - dp(78), cx + dp(20), cy - dp(28), light, dark, Shader.TileMode.CLAMP));
                Path right = new Path();
                right.moveTo(cx + dp(38), cy - dp(28));
                right.lineTo(cx + dp(58), cy - dp(78));
                right.lineTo(cx + dp(12), cy - dp(48));
                canvas.drawPath(right, paint);
                paint.setShader(null);
                paint.setColor(Color.argb(80, 213, 95, 138));
                canvas.drawCircle(cx - dp(38), cy - dp(52), dp(8), paint);
                canvas.drawCircle(cx + dp(38), cy - dp(52), dp(8), paint);
            } else if ("rabbit".equals(type)) {
                paint.setShader(new LinearGradient(cx, cy - dp(112), cx, cy - dp(34), light, dark, Shader.TileMode.CLAMP));
                canvas.drawOval(new RectF(cx - dp(34), cy - dp(112), cx - dp(8), cy - dp(34)), paint);
                canvas.drawOval(new RectF(cx + dp(8), cy - dp(112), cx + dp(34), cy - dp(34)), paint);
                paint.setShader(null);
                paint.setColor(Color.argb(115, 230, 169, 196));
                canvas.drawOval(new RectF(cx - dp(27), cy - dp(98), cx - dp(15), cy - dp(48)), paint);
                canvas.drawOval(new RectF(cx + dp(15), cy - dp(98), cx + dp(27), cy - dp(48)), paint);
            } else if ("hamster".equals(type) || "panda".equals(type)) {
                paint.setColor("panda".equals(type) ? Color.rgb(38, 45, 52) : dark);
                canvas.drawCircle(cx - dp(40), cy - dp(38), dp(22), paint);
                canvas.drawCircle(cx + dp(40), cy - dp(38), dp(22), paint);
                paint.setColor("panda".equals(type) ? Color.rgb(245, 245, 240) : Color.rgb(255, 211, 178));
                canvas.drawCircle(cx - dp(40), cy - dp(38), dp(10), paint);
                canvas.drawCircle(cx + dp(40), cy - dp(38), dp(10), paint);
            } else {
                paint.setShader(new RadialGradient(cx - dp(48), cy - dp(4), dp(34), light, dark, Shader.TileMode.CLAMP));
                canvas.drawOval(new RectF(cx - dp(70), cy - dp(36), cx - dp(34), cy + dp(24)), paint);
                paint.setShader(new RadialGradient(cx + dp(48), cy - dp(4), dp(34), light, dark, Shader.TileMode.CLAMP));
                canvas.drawOval(new RectF(cx + dp(34), cy - dp(36), cx + dp(70), cy + dp(24)), paint);
                paint.setShader(null);
            }
        }

        private int petBaseColor() {
            if ("dog".equals(type)) return Color.rgb(221, 165, 95);
            if ("rabbit".equals(type)) return Color.rgb(248, 248, 244);
            if ("fox".equals(type)) return Color.rgb(235, 130, 62);
            if ("panda".equals(type)) return Color.rgb(244, 244, 238);
            if ("hamster".equals(type)) return Color.rgb(226, 176, 112);
            return Color.rgb(244, 188, 112);
        }

        private int petDetailColor() {
            if ("dog".equals(type)) return Color.rgb(105, 72, 48);
            if ("rabbit".equals(type)) return Color.rgb(214, 128, 170);
            if ("fox".equals(type)) return Color.rgb(95, 54, 32);
            if ("panda".equals(type)) return Color.rgb(38, 45, 52);
            if ("hamster".equals(type)) return Color.rgb(124, 82, 48);
            return Color.rgb(106, 74, 48);
        }

        private int lighten(int color, int amount) {
            return Color.rgb(Math.min(255, Color.red(color) + amount), Math.min(255, Color.green(color) + amount), Math.min(255, Color.blue(color) + amount));
        }

        private int darken(int color, int amount) {
            return Color.rgb(Math.max(0, Color.red(color) - amount), Math.max(0, Color.green(color) - amount), Math.max(0, Color.blue(color) - amount));
        }
    }

    private class BarChartView extends View {
        private final String[] labels;
        private final long[] values;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        BarChartView(Activity context, String[] labels, long[] values) {
            super(context);
            this.labels = labels;
            this.values = values;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            int bottom = h - dp(26);
            int top = dp(14);
            long max = 0;
            for (long value : values) if (value > max) max = value;
            paint.setStrokeWidth(dp(1));
            paint.setColor(line);
            canvas.drawLine(dp(8), bottom, w - dp(8), bottom, paint);
            float gap = dp(8);
            float colW = (w - dp(16) - gap * (values.length - 1)) / Math.max(1, values.length);
            int barColor = "income".equals(statsType) ? green : red;
            for (int i = 0; i < values.length; i++) {
                float left = dp(8) + i * (colW + gap);
                float ratio = max == 0 ? 0 : (float) values[i] / max;
                float barH = Math.max(dp(4), (bottom - top) * ratio);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(max == 0 ? line : barColor);
                canvas.drawRoundRect(new RectF(left, bottom - barH, left + colW, bottom), dp(5), dp(5), paint);
                paint.setColor(muted);
                paint.setTextSize(dp(10));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(labels[i], left + colW / 2f, h - dp(8), paint);
            }
        }
    }

    private class IconBadge extends View {
        private final String name;
        private final String type;
        private final boolean add;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        IconBadge(Activity context, String name, String type, boolean add) {
            super(context);
            this.name = name == null ? "" : name;
            this.type = type == null ? "" : type;
            this.add = add;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) * 0.46f;
            int accent = iconAccent(name, type);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(add ? yellow : iconBg(type, accent));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(Math.max(3f, w * 0.045f));
            paint.setColor(add ? ink : Color.rgb(82, 88, 96));
            drawIcon(canvas, cx, cy, r * 0.95f, iconKey(name, type));
        }

        private void drawIcon(Canvas canvas, float cx, float cy, float s, String key) {
            if (add) {
                canvas.drawLine(cx - s * 0.35f, cy, cx + s * 0.35f, cy, paint);
                canvas.drawLine(cx, cy - s * 0.35f, cx, cy + s * 0.35f, paint);
                return;
            }
            if ("food".equals(key)) {
                canvas.drawLine(cx - s * 0.25f, cy - s * 0.35f, cx - s * 0.25f, cy + s * 0.35f, paint);
                canvas.drawLine(cx - s * 0.38f, cy - s * 0.35f, cx - s * 0.12f, cy - s * 0.35f, paint);
                canvas.drawLine(cx - s * 0.38f, cy - s * 0.15f, cx - s * 0.12f, cy - s * 0.15f, paint);
                canvas.drawArc(new RectF(cx + s * 0.05f, cy - s * 0.38f, cx + s * 0.45f, cy + s * 0.1f), -70, 300, false, paint);
                canvas.drawLine(cx + s * 0.25f, cy + s * 0.08f, cx + s * 0.25f, cy + s * 0.38f, paint);
            } else if ("shopping".equals(key)) {
                RectF bag = new RectF(cx - s * 0.35f, cy - s * 0.15f, cx + s * 0.35f, cy + s * 0.42f);
                canvas.drawRoundRect(bag, s * 0.08f, s * 0.08f, paint);
                canvas.drawArc(new RectF(cx - s * 0.2f, cy - s * 0.42f, cx + s * 0.2f, cy), 180, 180, false, paint);
            } else if ("transport".equals(key)) {
                canvas.drawRoundRect(new RectF(cx - s * 0.42f, cy - s * 0.25f, cx + s * 0.42f, cy + s * 0.25f), s * 0.1f, s * 0.1f, paint);
                canvas.drawCircle(cx - s * 0.24f, cy + s * 0.32f, s * 0.08f, paint);
                canvas.drawCircle(cx + s * 0.24f, cy + s * 0.32f, s * 0.08f, paint);
                canvas.drawLine(cx - s * 0.2f, cy - s * 0.25f, cx + s * 0.2f, cy - s * 0.25f, paint);
            } else if ("home".equals(key)) {
                Path p = new Path();
                p.moveTo(cx - s * 0.42f, cy);
                p.lineTo(cx, cy - s * 0.38f);
                p.lineTo(cx + s * 0.42f, cy);
                p.moveTo(cx - s * 0.3f, cy);
                p.lineTo(cx - s * 0.3f, cy + s * 0.38f);
                p.lineTo(cx + s * 0.3f, cy + s * 0.38f);
                p.lineTo(cx + s * 0.3f, cy);
                canvas.drawPath(p, paint);
            } else if ("phone".equals(key)) {
                canvas.drawRoundRect(new RectF(cx - s * 0.22f, cy - s * 0.42f, cx + s * 0.22f, cy + s * 0.42f), s * 0.08f, s * 0.08f, paint);
                canvas.drawCircle(cx, cy + s * 0.28f, s * 0.03f, paint);
            } else if ("clothes".equals(key)) {
                Path p = new Path();
                p.moveTo(cx - s * 0.15f, cy - s * 0.36f);
                p.lineTo(cx - s * 0.42f, cy - s * 0.12f);
                p.lineTo(cx - s * 0.25f, cy + s * 0.08f);
                p.lineTo(cx - s * 0.18f, cy + s * 0.42f);
                p.lineTo(cx + s * 0.18f, cy + s * 0.42f);
                p.lineTo(cx + s * 0.25f, cy + s * 0.08f);
                p.lineTo(cx + s * 0.42f, cy - s * 0.12f);
                p.lineTo(cx + s * 0.15f, cy - s * 0.36f);
                canvas.drawPath(p, paint);
            } else if ("medical".equals(key)) {
                canvas.drawLine(cx - s * 0.32f, cy, cx + s * 0.32f, cy, paint);
                canvas.drawLine(cx, cy - s * 0.32f, cx, cy + s * 0.32f, paint);
            } else if ("income".equals(key)) {
                canvas.drawCircle(cx, cy, s * 0.36f, paint);
                canvas.drawLine(cx, cy + s * 0.3f, cx, cy - s * 0.32f, paint);
                canvas.drawLine(cx, cy - s * 0.32f, cx - s * 0.18f, cy - s * 0.1f, paint);
                canvas.drawLine(cx, cy - s * 0.32f, cx + s * 0.18f, cy - s * 0.1f, paint);
            } else if ("transfer".equals(key)) {
                canvas.drawLine(cx - s * 0.36f, cy - s * 0.16f, cx + s * 0.28f, cy - s * 0.16f, paint);
                canvas.drawLine(cx + s * 0.28f, cy - s * 0.16f, cx + s * 0.08f, cy - s * 0.32f, paint);
                canvas.drawLine(cx + s * 0.28f, cy - s * 0.16f, cx + s * 0.08f, cy, paint);
                canvas.drawLine(cx + s * 0.36f, cy + s * 0.18f, cx - s * 0.28f, cy + s * 0.18f, paint);
                canvas.drawLine(cx - s * 0.28f, cy + s * 0.18f, cx - s * 0.08f, cy, paint);
                canvas.drawLine(cx - s * 0.28f, cy + s * 0.18f, cx - s * 0.08f, cy + s * 0.34f, paint);
            } else if ("book".equals(key)) {
                canvas.drawRoundRect(new RectF(cx - s * 0.35f, cy - s * 0.34f, cx, cy + s * 0.36f), s * 0.06f, s * 0.06f, paint);
                canvas.drawRoundRect(new RectF(cx, cy - s * 0.34f, cx + s * 0.35f, cy + s * 0.36f), s * 0.06f, s * 0.06f, paint);
            } else {
                canvas.drawCircle(cx, cy, s * 0.3f, paint);
                canvas.drawLine(cx - s * 0.18f, cy + s * 0.22f, cx + s * 0.22f, cy - s * 0.2f, paint);
            }
        }
    }

    private int iconBg(String type, int accent) {
        if ("income".equals(type)) return Color.rgb(229, 246, 239);
        if ("transfer".equals(type)) return Color.rgb(230, 241, 255);
        return Color.rgb(244, 244, 242);
    }

    private int iconAccent(String name, String type) {
        if ("income".equals(type)) return green;
        if ("transfer".equals(type)) return Color.rgb(64, 105, 210);
        return muted;
    }

    private String iconKey(String name, String type) {
        if ("transfer".equals(type) || name.contains("转")) return "transfer";
        if ("income".equals(type)) return "income";
        if (name.contains("餐") || name.contains("蔬") || name.contains("果") || name.contains("零") || name.contains("烟") || name.contains("酒")) return "food";
        if (name.contains("购") || name.contains("礼") || name.contains("日")) return "shopping";
        if (name.contains("交") || name.contains("车") || name.contains("旅")) return "transport";
        if (name.contains("住") || name.contains("家")) return "home";
        if (name.contains("讯") || name.contains("数")) return "phone";
        if (name.contains("服") || name.contains("美")) return "clothes";
        if (name.contains("医") || name.contains("运动")) return "medical";
        if (name.contains("书") || name.contains("学") || name.contains("办")) return "book";
        return "other";
    }

    private static class AiDraft {
        String raw;
        String type = "expense";
        long amount;
        long categoryId;
        String categoryName = "";
        long accountId;
        long targetAccountId;
        long time;
        String note = "";
        boolean ready;
        String missing = "";
    }
}
