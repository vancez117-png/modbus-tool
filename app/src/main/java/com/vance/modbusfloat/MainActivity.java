package com.vance.modbusfloat;

import android.app.Activity;
import android.app.AlertDialog;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String APP_VERSION = "v1.1.16";
    private static final String DATA_TYPE_HEADER_INTEGER = "整數";
    private static final String DATA_TYPE_HEADER_FLOAT = "浮點數";
    private static final String DATA_TYPE_HEADER_TEXT = "文字與編碼";
    private enum OperationState {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        STALE
    }

    private enum DecodeInputMode {
        REGISTER,
        HEX,
        BINARY
    }

    private ScrollView mainScrollView;
    private EditText register1Input;
    private EditText register2Input;
    private EditText register3Input;
    private EditText register4Input;
    private EditText binaryInput;
    private EditText hexInput;
    private EditText rawDecodeInput;
    private EditText floatInput;
    private TextView register1Message;
    private TextView register2Message;
    private TextView register3Message;
    private TextView register4Message;
    private TextView rawDecodeMessage;
    private TextView rawDecodePreview;
    private Spinner registerModeSpinner;
    private Spinner dataTypeSpinner;
    private Spinner asciiNullModeSpinner;
    private Spinner reverseTypeSpinner;
    private TextView statusText;
    private LinearLayout converterPage;
    private LinearLayout forwardConvertPage;
    private LinearLayout reverseConvertPage;
    private LinearLayout helpPage;
    private Button forwardModeButton;
    private Button reverseModeButton;
    private Button converterTabButton;
    private Button packetTabButton;
    private Button helpTabButton;
    private Button registerInputModeButton;
    private Button rawBinaryModeButton;
    private Button rawHexModeButton;
    private LinearLayout packetPage;
    private LinearLayout registerInputContainer;
    private LinearLayout rawInputContainer;
    private LinearLayout registerRow2;
    private LinearLayout registerRowsContainer;
    private LinearLayout variableRegisterActions;
    private EditText packetHexInput;
    private RadioGroup packetTypeGroup;
    private RadioGroup packetDirectionGroup;
    private Button packetTransferButton;
    private TextView packetSummaryText;
    private TextView packetStatusText;
    private TextView packetExampleText;
    private ModbusPacketSamples.SamplePacket selectedPacketExample;
    private PacketParseResult latestPacketResult;
    private boolean formattingPacketHex;
    private boolean rawDecodeBinaryMode = true;
    private boolean formattingRawDecodeInput;
    private String lastValidRawBinary32 = "";
    private TableLayout encodeTable;
    private TableLayout registerFormatTable;
    private TableLayout dataTypeTable;
    private LinearLayout forwardResultList;
    private LinearLayout reverseResultList;
    private LinearLayout forwardOperationStatusBar;
    private TextView forwardOperationTitle;
    private TextView forwardOperationDetail;
    private LinearLayout reverseOperationStatusBar;
    private LinearLayout reverseExampleRow;
    private TextView reverseOperationTitle;
    private TextView reverseOperationDetail;
    private LinearLayout forwardFormatTabs;
    private LinearLayout reverseFormatTabs;
    private View dataTypeBlock;
    private View encodeBlock;
    private final List<Button> topTabButtons = new ArrayList<>();
    private final List<Button> forwardFormatButtons = new ArrayList<>();
    private final List<Button> reverseFormatButtons = new ArrayList<>();
    private final List<EditText> registerInputs = new ArrayList<>();
    private final List<TextView> registerMessages = new ArrayList<>();
    private List<DataTypeResult> latestForwardRows = new ArrayList<>();
    private List<ReverseResult> latestReverseRows = new ArrayList<>();
    private ResultFormatMode forwardResultMode = ResultFormatMode.ALL;
    private ResultFormatMode reverseResultMode = ResultFormatMode.ALL;
    private boolean suppressLiveUpdate;
    private DecodeInputMode currentDecodeInputMode = DecodeInputMode.REGISTER;
    private int activeRegisterCount = 2;
    private int lastVariableRegisterCount = 2;
    private TextWatcher registerTextWatcher;
    private boolean asciiTrimTrailingNull;

    private RegisterInputMode currentRegisterInputMode = RegisterInputMode.UNSIGNED;

    private int bgColor;
    private int panelColor;
    private int textColor;
    private int mutedColor;
    private int accentColor;
    private int dangerColor;
    private int borderColor;
    private int successColor;
    private int glassFillColor;
    private int instrumentFillColor;
    private int glassBorderColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadColors();
        buildUi();
        showExample(10.0f);
    }

    private void loadColors() {
        bgColor = getColor(R.color.bg);
        panelColor = getColor(R.color.panel);
        textColor = getColor(R.color.text);
        mutedColor = getColor(R.color.muted);
        accentColor = getColor(R.color.accent);
        dangerColor = getColor(R.color.danger);
        borderColor = getColor(R.color.border);
        successColor = 0xFF65E5B5;
        glassFillColor = 0x7716253A;
        instrumentFillColor = 0xE807111E;
        glassBorderColor = 0x38FFFFFF;
    }

    private void buildUi() {
        Window window = getWindow();
        window.setStatusBarColor(bgColor);
        window.setNavigationBarColor(bgColor);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mainScrollView = new ScrollView(this);
        mainScrollView.setBackground(pageBackground());
        mainScrollView.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(20));
        mainScrollView.addView(root);
        mainScrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();
            root.setPadding(dp(12) + left, dp(10) + top, dp(12) + right, dp(20) + bottom);
            return insets;
        });

        LinearLayout navBar = stack();
        navBar.setBackground(glassBackground(dp(22)));
        navBar.setPadding(dp(14), dp(10), dp(14), dp(10));
        navBar.setElevation(dp(8));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        navParams.setMargins(0, 0, 0, dp(10));
        navBar.setLayoutParams(navParams);
        TextView title = text("Modbus 資料轉換工具 " + APP_VERSION, 21, true);
        navBar.addView(title);
        TextView subtitle = text("PLC / DCS 現場用，支援四種 byte / word order", 13, false);
        subtitle.setTextColor(mutedColor);
        subtitle.setPadding(0, dp(2), 0, 0);
        navBar.addView(subtitle);
        root.addView(navBar);

        LinearLayout tabs = equalRow(3);
        converterTabButton = addSegmentButton(tabs, "資料轉換", v -> showPage(0));
        packetTabButton = addSegmentButton(tabs, "通訊封包", v -> showPage(1));
        helpTabButton = addSegmentButton(tabs, "格式說明", v -> showPage(2));
        topTabButtons.add(converterTabButton);
        topTabButtons.add(packetTabButton);
        topTabButtons.add(helpTabButton);
        root.addView(tabs);

        converterPage = stack();
        LinearLayout convertTabs = equalRow(2);
        forwardModeButton = addSegmentButton(convertTabs, "正向解析", v -> showConverterMode(true));
        reverseModeButton = addSegmentButton(convertTabs, "反向轉換", v -> showConverterMode(false));
        converterPage.addView(convertTabs);

        forwardConvertPage = stack();
        forwardConvertPage.addView(section("資料解碼", buildDecodePanel()));
        forwardResultList = stack();
        forwardFormatTabs = buildResultFormatTabs(true);
        LinearLayout forwardResults = stack();
        forwardResults.addView(operationStatusBar(true));
        forwardResults.addView(forwardFormatTabs);
        forwardResults.addView(forwardResultList);
        dataTypeBlock = collapsibleSection("解析結果比較", forwardResults, true);
        forwardConvertPage.addView(dataTypeBlock);
        converterPage.addView(forwardConvertPage);

        reverseConvertPage = stack();
        reverseConvertPage.addView(section("資料反向轉換", buildEncodePanel()));
        reverseResultList = stack();
        reverseFormatTabs = buildResultFormatTabs(false);
        LinearLayout reverseResults = stack();
        reverseResults.addView(operationStatusBar(false));
        reverseResults.addView(reverseFormatTabs);
        reverseResults.addView(reverseResultList);
        encodeBlock = collapsibleSection("反向結果比較", reverseResults, true);
        reverseConvertPage.addView(encodeBlock);
        reverseConvertPage.setVisibility(View.GONE);
        converterPage.addView(reverseConvertPage);
        root.addView(converterPage);

        packetPage = stack();
        packetPage.addView(section("Modbus RTU / TCP 封包解析", buildPacketPanel()));
        packetPage.setVisibility(View.GONE);
        root.addView(packetPage);

        helpPage = stack();
        helpPage.addView(section("格式說明", buildHelpPanel()));
        helpPage.setVisibility(View.GONE);
        root.addView(helpPage);

        statusText = text("", 14, true);
        statusText.setTextColor(dangerColor);
        statusText.setPadding(0, dp(8), 0, 0);
        root.addView(statusText);

        setContentView(mainScrollView);
        showPage(0);
        showConverterMode(true);
    }

    private void showPage(int page) {
        converterPage.setVisibility(page == 0 ? View.VISIBLE : View.GONE);
        packetPage.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        helpPage.setVisibility(page == 2 ? View.VISIBLE : View.GONE);
        setActiveSegment(topTabButtons, page);
    }

    private void showConverterMode(boolean forward) {
        forwardConvertPage.setVisibility(forward ? View.VISIBLE : View.GONE);
        reverseConvertPage.setVisibility(forward ? View.GONE : View.VISIBLE);
        setActiveSegment(List.of(forwardModeButton, reverseModeButton), forward ? 0 : 1);
    }

    private View buildDecodePanel() {
        LinearLayout box = stack();

        TextView inputModeTitle = helperText("輸入格式");
        box.addView(inputModeTitle);
        LinearLayout inputModeTabs = equalRow(3);
        registerInputModeButton = addSegmentButton(inputModeTabs, "Register", v -> setDecodeInputMode(DecodeInputMode.REGISTER));
        rawHexModeButton = addSegmentButton(inputModeTabs, "Hex", v -> setDecodeInputMode(DecodeInputMode.HEX));
        rawBinaryModeButton = addSegmentButton(inputModeTabs, "Binary", v -> setDecodeInputMode(DecodeInputMode.BINARY));
        box.addView(inputModeTabs);

        registerInputContainer = stack();
        ArrayAdapter<String> modeAdapter = glassAdapter(labels(RegisterInputMode.values()));
        registerModeSpinner = glassDropdown(modeAdapter, "Register 輸入格式");
        registerModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (register1Input == null || suppressLiveUpdate) {
                    return;
                }
                RegisterInputMode nextMode = RegisterInputMode.values()[position];
                RegisterInputMode previousMode = currentRegisterInputMode;
                convertRegisterInputs(previousMode, nextMode);
                currentRegisterInputMode = nextMode;
                syncRegisterToRawIfReady();
                updateDataTypeTables();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        registerInputContainer.addView(helperText("Register 輸入格式（只影響 Register 欄位輸入方式）"));
        registerInputContainer.addView(registerModeSpinner);

        registerTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateRegisterInputMessages();
                if (currentDecodeInputMode == DecodeInputMode.REGISTER) {
                    syncRegisterToRawIfReady();
                    updateDataTypeTables();
                }
            }
        };
        registerRowsContainer = stack();
        registerInputContainer.addView(registerRowsContainer);
        variableRegisterActions = row();
        variableRegisterActions.addView(smallButton("+ Register", v -> addVariableRegister()));
        variableRegisterActions.addView(smallButton("- Register", v -> removeVariableRegister()));
        registerInputContainer.addView(variableRegisterActions);
        ensureRegisterInputCount(4);
        renderRegisterFields();
        box.addView(registerInputContainer);

        rawInputContainer = stack();
        rawDecodeInput = input("8 位 Hex，例如 41200234", InputType.TYPE_CLASS_TEXT, 512);
        rawDecodeInput.setTypeface(Typeface.MONOSPACE);
        rawDecodeInput.setSingleLine(false);
        rawDecodeInput.setMinLines(1);
        rawDecodeInput.setMaxLines(3);
        rawDecodeInput.setHorizontallyScrolling(false);
        rawInputContainer.addView(rawDecodeInput);
        rawDecodeMessage = helperText("");
        rawInputContainer.addView(rawDecodeMessage);
        rawDecodePreview = helperText("");
        rawDecodePreview.setTypeface(Typeface.MONOSPACE);
        rawDecodePreview.setTextIsSelectable(true);
        rawInputContainer.addView(rawDecodePreview);
        box.addView(rawInputContainer);

        binaryInput = rawDecodeInput;
        hexInput = new EditText(this);
        rawDecodeInput.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!formattingRawDecodeInput && currentDecodeInputMode != DecodeInputMode.REGISTER) {
                    updateRawDecode(false);
                }
            }
        });
        rawDecodeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                formatRawDecodeInputIfComplete();
            }
        });

        ArrayAdapter<String> dataTypeAdapter = glassDataTypeAdapter();
        dataTypeSpinner = glassDropdown(dataTypeAdapter, "解析資料型態");
        selectDataType(dataTypeSpinner, DataTypeMode.FLOAT32);
        dataTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRegisterFieldVisibility();
                if (currentDecodeInputMode == DecodeInputMode.REGISTER) {
                    updateDataTypeTables();
                } else {
                    setRawDecodeMode(rawDecodeBinaryMode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        box.addView(helperText("解析資料型態"));
        box.addView(dataTypeSpinner);

        ArrayAdapter<String> asciiModeAdapter = glassAdapter(new String[]{"ASCII：保留所有 Byte", "ASCII：忽略尾端 NULL"});
        asciiNullModeSpinner = glassDropdown(asciiModeAdapter, "ASCII NULL 顯示模式");
        asciiNullModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                asciiTrimTrailingNull = position == 1;
                if (currentDecodeInputMode == DecodeInputMode.REGISTER) {
                    updateDataTypeTables();
                } else {
                    updateRawDecode(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        box.addView(asciiNullModeSpinner);

        LinearLayout actions = row();
        addGridButton(actions, "解析資料", v -> decodeCurrentInput());
        addGridButton(actions, "清除", v -> clearDecodeInputs());
        box.addView(actions);
        setDecodeInputMode(DecodeInputMode.REGISTER);
        return box;
    }

    private View buildEncodePanel() {
        LinearLayout box = stack();
        ArrayAdapter<String> reverseAdapter = glassDataTypeAdapter();
        reverseTypeSpinner = glassDropdown(reverseAdapter, "反向轉換資料型態");
        selectDataType(reverseTypeSpinner, DataTypeMode.FLOAT32);
        reverseTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateReverseInputForType();
                liveEncodeInput();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        box.addView(reverseTypeSpinner);

        floatInput = input("資料值，例如 10.0、hello、00123456", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 128);
        floatInput.setSingleLine(false);
        floatInput.setMinLines(1);
        floatInput.setMaxLines(3);
        floatInput.setHorizontallyScrolling(false);
        box.addView(floatInput);

        reverseExampleRow = equalRow(5);
        box.addView(reverseExampleRow);
        box.addView(primaryWideButton("反向轉換", v -> encodeInput()));
        floatInput.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                liveEncodeInput();
            }
        });
        updateReverseInputForType();
        return box;
    }

    private View buildPacketPanel() {
        LinearLayout box = stack();
        packetTypeGroup = new RadioGroup(this);
        packetTypeGroup.setOrientation(RadioGroup.HORIZONTAL);
        android.widget.RadioButton rtu = new android.widget.RadioButton(this);
        rtu.setText("RTU");
        rtu.setTextColor(textColor);
        rtu.setTextSize(18);
        rtu.setId(1);
        android.widget.RadioButton tcp = new android.widget.RadioButton(this);
        tcp.setText("TCP");
        tcp.setTextColor(textColor);
        tcp.setTextSize(18);
        tcp.setId(2);
        packetTypeGroup.addView(rtu);
        packetTypeGroup.addView(tcp);
        packetTypeGroup.check(1);
        box.addView(packetTypeGroup);
        packetDirectionGroup = new RadioGroup(this);
        packetDirectionGroup.setOrientation(RadioGroup.HORIZONTAL);
        android.widget.RadioButton request = new android.widget.RadioButton(this);
        request.setText("Request");
        request.setTextColor(textColor);
        request.setTextSize(18);
        request.setId(3);
        android.widget.RadioButton response = new android.widget.RadioButton(this);
        response.setText("Response");
        response.setTextColor(textColor);
        response.setTextSize(18);
        response.setId(4);
        packetDirectionGroup.addView(request);
        packetDirectionGroup.addView(response);
        packetDirectionGroup.check(3);
        box.addView(packetDirectionGroup);
        box.addView(primaryWideButton("選擇測試範例", v -> showPacketExampleChooser()));

        packetHexInput = input("封包 HEX，例如 01 03 04 42 48 00 00 6E 5D", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, 240);
        packetHexInput.setSingleLine(false);
        packetHexInput.setMinLines(2);
        packetHexInput.setMaxLines(4);
        packetHexInput.setHorizontallyScrolling(false);
        packetHexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (formattingPacketHex) {
                    return;
                }
                formattingPacketHex = true;
                String formatted = formatHexPairs(s.toString());
                packetHexInput.setText(formatted);
                packetHexInput.setSelection(formatted.length());
                formattingPacketHex = false;
            }
        });
        box.addView(packetHexInput);
        LinearLayout packetActions = stack();
        LinearLayout firstPacketActionRow = buttonGridRow();
        addGridButton(firstPacketActionRow, "解析封包", v -> parsePacket(packetTypeGroup.getCheckedRadioButtonId() == 2));
        addGridButton(firstPacketActionRow, "清除", v -> clearPacket());
        LinearLayout secondPacketActionRow = buttonGridRow();
        packetTransferButton = addGridButton(secondPacketActionRow, "Register Data 轉資料解碼", v -> loadPacketRegisterDataToDecoder());
        packetTransferButton.setEnabled(false);
        packetTransferButton.setAlpha(0.55f);
        packetActions.addView(firstPacketActionRow);
        packetActions.addView(secondPacketActionRow);
        box.addView(packetActions);

        packetStatusText = text("", 17, true);
        packetStatusText.setPadding(0, dp(8), 0, dp(8));
        box.addView(packetStatusText);
        packetExampleText = helperText("");
        packetExampleText.setTextIsSelectable(true);
        box.addView(packetExampleText);
        packetSummaryText = selectableResultText("尚未解析封包。");
        packetSummaryText.setBackground(cardBackground(instrumentFillColor, 0x334DA3FF, dp(14)));
        packetSummaryText.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.addView(packetSummaryText);
        box.addView(button("通訊封包說明", v -> showHelp("通訊封包說明", packetHelp())));
        return box;
    }

    private LinearLayout section(String label, View content) {
        LinearLayout section = stack();
        section.setBackground(glassBackground(dp(22)));
        section.setPadding(dp(14), dp(12), dp(14), dp(14));
        section.setElevation(dp(6));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(10), 0, 0);
        section.setLayoutParams(params);
        TextView heading = text(label, 18, true);
        heading.setPadding(0, 0, 0, dp(6));
        section.addView(heading);
        section.addView(content);
        return section;
    }

    private LinearLayout collapsibleSection(String label, View content, boolean expanded) {
        LinearLayout section = section(label, content);
        TextView heading = (TextView) section.getChildAt(0);
        heading.setText((expanded ? "▼ " : "▶ ") + label);
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        View.OnClickListener toggle = v -> {
            boolean nextExpanded = content.getVisibility() != View.VISIBLE;
            if (nextExpanded) {
                content.setAlpha(0f);
                content.setVisibility(View.VISIBLE);
                content.animate().alpha(1f).setDuration(180).start();
            } else {
                content.animate().alpha(0f).setDuration(160).withEndAction(() -> content.setVisibility(View.GONE)).start();
            }
            heading.setText((nextExpanded ? "▼ " : "▶ ") + label);
        };
        heading.setOnClickListener(toggle);
        return section;
    }

    private LinearLayout stack() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.START);
        return layout;
    }

    private View operationStatusBar(boolean forward) {
        LinearLayout bar = stack();
        bar.setBackground(cardBackground(0x6618273B, glassBorderColor, dp(16)));
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        bar.setLayoutParams(params);

        TextView title = text("尚未執行", 15, true);
        TextView detail = text("按下解析後，這裡會顯示本次操作狀態。", 13, false);
        detail.setTextColor(mutedColor);
        bar.addView(title);
        bar.addView(detail);

        if (forward) {
            forwardOperationStatusBar = bar;
            forwardOperationTitle = title;
            forwardOperationDetail = detail;
        } else {
            reverseOperationStatusBar = bar;
            reverseOperationTitle = title;
            reverseOperationDetail = detail;
        }
        return bar;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.START);
        layout.setPadding(0, dp(4), 0, dp(2));
        return layout;
    }

    private EditText input(String hint, int inputType, int maxLength) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(textColor);
        editText.setHintTextColor(mutedColor);
        editText.setTextSize(16);
        editText.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        editText.setInputType(inputType);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        editText.setBackground(cardBackground(instrumentFillColor, 0x554DA3FF, dp(12)));
        editText.setPadding(dp(12), dp(6), dp(12), dp(6));
        editText.setMinHeight(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(3), 0, dp(4));
        editText.setLayoutParams(params);
        return editText;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(buttonBackground(accentColor, accentColor, dp(14)));
        button.setStateListAnimator(null);
        button.setElevation(dp(2));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
        params.setMargins(0, 0, dp(8), dp(8));
        button.setLayoutParams(params);
        return button;
    }

    private Button primaryWideButton(String label, View.OnClickListener listener) {
        Button button = button(label, listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        params.setMargins(0, dp(4), 0, dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout equalRow(int weightSum) {
        LinearLayout layout = row();
        layout.setWeightSum(weightSum);
        return layout;
    }

    private LinearLayout buttonGridRow() {
        return equalRow(2);
    }

    private Button addGridButton(LinearLayout row, String label, View.OnClickListener listener) {
        Button button = button(label, listener);
        button.setTextSize(14);
        button.setSingleLine(false);
        button.setMaxLines(2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        params.setMargins(0, 0, dp(8), dp(8));
        button.setLayoutParams(params);
        row.addView(button);
        return button;
    }

    private Button addSegmentButton(LinearLayout row, String label, View.OnClickListener listener) {
        Button button = addGridButton(row, label, listener);
        button.setTextSize(13);
        return button;
    }

    private Button addChipButton(LinearLayout row, String label, View.OnClickListener listener) {
        Button button = button(label, listener);
        button.setTextColor(accentColor);
        button.setBackground(buttonBackground(glassFillColor, glassBorderColor, dp(20)));
        button.setTextSize(13);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1f);
        params.setMargins(0, 0, dp(6), dp(6));
        button.setLayoutParams(params);
        row.addView(button);
        return button;
    }

    private Button smallButton(String label, View.OnClickListener listener) {
        Button button = button(label, listener);
        button.setTextSize(12);
        button.setMinWidth(dp(64));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
        params.setMargins(dp(8), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private Spinner glassDropdown(ArrayAdapter<String> adapter, String contentDescription) {
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        spinner.setAdapter(adapter);
        spinner.setContentDescription(contentDescription);
        styleGlassDropdown(spinner);
        return spinner;
    }

    private void styleGlassDropdown(Spinner spinner) {
        spinner.setBackground(cardBackground(instrumentFillColor, 0x664DA3FF, dp(16)));
        spinner.setPopupBackgroundDrawable(cardBackground(0xF0101B2E, 0x884DA3FF, dp(16)));
        spinner.setPadding(0, 0, 0, 0);
        spinner.setMinimumHeight(dp(54));
        spinner.setDropDownVerticalOffset(dp(4));
        spinner.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        spinner.setFocusable(true);
        spinner.setClickable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        params.setMargins(0, dp(3), 0, dp(6));
        spinner.setLayoutParams(params);
    }

    private ArrayAdapter<String> glassAdapter(String[] items) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                styleSpinnerItem(view, false);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                styleSpinnerItem(view, true);
                return view;
            }
        };
    }

    private ArrayAdapter<String> glassDataTypeAdapter() {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dataTypeMenuItems()) {
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return !isDataTypeHeader(getItem(position));
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                styleSpinnerItem(view, false);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                boolean header = isDataTypeHeader(getItem(position));
                styleSpinnerItem(view, true);
                if (header) {
                    view.setTextColor(mutedColor);
                    view.setTextSize(13);
                    view.setTypeface(Typeface.DEFAULT_BOLD);
                    view.setBackground(cardBackground(0xF018273B, 0x335E6B83, dp(10)));
                }
                return view;
            }
        };
    }

    private String[] dataTypeMenuItems() {
        return new String[]{
                DATA_TYPE_HEADER_INTEGER,
                DataTypeMode.INT16.label(),
                DataTypeMode.UINT16.label(),
                DataTypeMode.INT32.label(),
                DataTypeMode.UINT32.label(),
                DATA_TYPE_HEADER_FLOAT,
                DataTypeMode.FLOAT32.label(),
                DataTypeMode.FLOAT64.label(),
                DATA_TYPE_HEADER_TEXT,
                DataTypeMode.ASCII.label(),
                DataTypeMode.BCD.label()
        };
    }

    private boolean isDataTypeHeader(String value) {
        return DATA_TYPE_HEADER_INTEGER.equals(value)
                || DATA_TYPE_HEADER_FLOAT.equals(value)
                || DATA_TYPE_HEADER_TEXT.equals(value);
    }

    private void selectDataType(Spinner spinner, DataTypeMode mode) {
        String label = mode.label();
        for (int i = 0; i < spinner.getCount(); i++) {
            if (label.equals(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void styleSpinnerItem(TextView view, boolean dropdown) {
        view.setTextColor(textColor);
        view.setTextSize(dropdown ? 17 : 16);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setMinHeight(dp(dropdown ? 52 : 54));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(16), 0, dp(16), 0);
        if (dropdown) {
            view.setBackground(cardBackground(0xCC101B2E, 0x334DA3FF, dp(12)));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void setActiveSegment(List<Button> buttons, int activeIndex) {
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            boolean active = i == activeIndex;
            button.setTextColor(active ? 0xFFFFFFFF : textColor);
            button.setBackground(buttonBackground(active ? accentColor : glassFillColor, active ? 0xAA9AD0FF : glassBorderColor, dp(14)));
        }
    }

    private GradientDrawable cardBackground(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable buttonBackground(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        if (fill == accentColor) {
            drawable.setOrientation(GradientDrawable.Orientation.TL_BR);
            drawable.setColors(new int[]{0xFF75BFFF, accentColor, 0xFF2F6CFF});
        } else {
            drawable.setColor(fill);
        }
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable pageBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF08111F, bgColor, 0xFF101B2E, 0xFF07101C});
        return drawable;
    }

    private GradientDrawable glassBackground(int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x881F334B, glassFillColor, 0x66101B2E});
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), glassBorderColor);
        return drawable;
    }

    private void addWeightedView(LinearLayout row, View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(0, 0, dp(8), dp(4));
        view.setLayoutParams(params);
        row.addView(view);
    }

    private void ensureRegisterInputCount(int count) {
        while (registerInputs.size() < count) {
            int index = registerInputs.size() + 1;
            EditText input = input("R" + index, InputType.TYPE_CLASS_NUMBER, 17);
            TextView message = helperText("");
            input.addTextChangedListener(registerTextWatcher);
            attachRegisterBinaryFormatter(input);
            registerInputs.add(input);
            registerMessages.add(message);
        }
        assignRegisterAliases();
        convertRegisterInputWidgets(currentRegisterInputMode);
    }

    private void assignRegisterAliases() {
        register1Input = registerInputs.size() > 0 ? registerInputs.get(0) : null;
        register2Input = registerInputs.size() > 1 ? registerInputs.get(1) : null;
        register3Input = registerInputs.size() > 2 ? registerInputs.get(2) : null;
        register4Input = registerInputs.size() > 3 ? registerInputs.get(3) : null;
        register1Message = registerMessages.size() > 0 ? registerMessages.get(0) : null;
        register2Message = registerMessages.size() > 1 ? registerMessages.get(1) : null;
        register3Message = registerMessages.size() > 2 ? registerMessages.get(2) : null;
        register4Message = registerMessages.size() > 3 ? registerMessages.get(3) : null;
    }

    private void renderRegisterFields() {
        if (registerRowsContainer == null) {
            return;
        }
        ensureRegisterInputCount(activeRegisterCount);
        registerRowsContainer.removeAllViews();
        for (int i = 0; i < activeRegisterCount; i += 2) {
            LinearLayout row = buttonGridRow();
            addWeightedView(row, registerField("Register " + (i + 1), registerInputs.get(i), registerMessages.get(i)));
            if (i + 1 < activeRegisterCount) {
                addWeightedView(row, registerField("Register " + (i + 2), registerInputs.get(i + 1), registerMessages.get(i + 1)));
            } else {
                View spacer = new View(this);
                addWeightedView(row, spacer);
            }
            registerRowsContainer.addView(row);
        }
        if (variableRegisterActions != null) {
            variableRegisterActions.setVisibility(isVariableRegisterMode() ? View.VISIBLE : View.GONE);
        }
        updateRegisterInputMessages();
    }

    private void addVariableRegister() {
        if (!isVariableRegisterMode()) {
            return;
        }
        activeRegisterCount++;
        lastVariableRegisterCount = activeRegisterCount;
        ensureRegisterInputCount(activeRegisterCount);
        renderRegisterFields();
        syncRegisterToRawIfReady();
        updateDataTypeTables();
    }

    private void removeVariableRegister() {
        if (!isVariableRegisterMode() || activeRegisterCount <= 1) {
            return;
        }
        EditText target = registerInputs.get(activeRegisterCount - 1);
        if (hasText(target.getText().toString())) {
            new AlertDialog.Builder(this)
                    .setTitle("移除 Register")
                    .setMessage("Register " + activeRegisterCount + " 目前有資料，確定要移除嗎？資料會保留但不參與本次 ASCII / BCD 解析。")
                    .setPositiveButton("移除", (dialog, which) -> removeLastVariableRegister())
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            removeLastVariableRegister();
        }
    }

    private void removeLastVariableRegister() {
        activeRegisterCount = Math.max(1, activeRegisterCount - 1);
        lastVariableRegisterCount = activeRegisterCount;
        renderRegisterFields();
        syncRegisterToRawIfReady();
        updateDataTypeTables();
    }

    private LinearLayout registerField(String label, EditText input, TextView message) {
        LinearLayout box = stack();
        TextView title = text(label, 12, true);
        title.setTextColor(mutedColor);
        box.addView(title);
        detachFromParent(input);
        detachFromParent(message);
        box.addView(input);
        box.addView(message);
        return box;
    }

    private void detachFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(textColor);
        textView.setTextSize(sp);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private HorizontalScrollView tableScroller(TableLayout table) {
        table.setStretchAllColumns(false);
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.addView(table);
        return scroll;
    }

    private void decodeFromRegisters() {
        if (updateDataTypeTables(true)) {
            setForwardOperationState(OperationState.SUCCESS, "解析完成", "已更新四種 Byte Order 的解析結果。");
            scrollToOperationResult(true);
        }
    }

    private void decodeCurrentInput() {
        if (currentDecodeInputMode == DecodeInputMode.REGISTER) {
            decodeFromRegisters();
        } else {
            updateRawDecode(true);
        }
    }

    private void decodeFromBinary() {
        try {
            applyRawBits(rawDecodeInput.getText().toString(), 2);
            setForwardOperationState(OperationState.SUCCESS, "解析完成", "已更新四種 Byte Order 的解析結果。");
            scrollToOperationResult(true);
        } catch (IllegalArgumentException ex) {
            setForwardOperationState(OperationState.ERROR, "解析失敗", toUserMessage(ex.getMessage()));
            showError(ex.getMessage());
        }
    }

    private void decodeFromHex() {
        try {
            applyRawHex(rawDecodeInput.getText().toString());
            setForwardOperationState(OperationState.SUCCESS, "解析完成", "已更新四種 Byte Order 的解析結果。");
            scrollToOperationResult(true);
        } catch (IllegalArgumentException ex) {
            setForwardOperationState(OperationState.ERROR, "解析失敗", toUserMessage(ex.getMessage()));
            showError(ex.getMessage());
        }
    }

    private void setRawDecodeMode(boolean binaryMode) {
        if (rawDecodeInput == null) {
            return;
        }
        String current = rawDecodeInput.getText().toString();
        String binary = rawDecodeBinaryMode ? cleanBinary(current) : binaryFromHexSafe(current);
        rawDecodeBinaryMode = binaryMode;
        int expectedBits = expectedRawBits();
        rawDecodeInput.setHint(binaryMode
                ? rawInputHint(true, expectedBits)
                : rawInputHint(false, expectedBits));
        rawDecodeInput.setMinLines(binaryMode ? 2 : 1);
        rawDecodeInput.setMaxLines(binaryMode ? 8 : 4);
        if (isAllowedRawBits(binary.length())) {
            formattingRawDecodeInput = true;
            rawDecodeInput.setText(binaryMode ? displayBinaryForEditor(binary) : hexFromBinary(binary));
            rawDecodeInput.setSelection(rawDecodeInput.getText().length());
            formattingRawDecodeInput = false;
            updateRawDecode(false);
        } else {
            updateRawDecode(false);
        }
    }

    private void setDecodeInputMode(DecodeInputMode mode) {
        if (registerInputContainer == null || rawInputContainer == null) {
            currentDecodeInputMode = mode;
            return;
        }
        if (currentDecodeInputMode == DecodeInputMode.REGISTER && mode != DecodeInputMode.REGISTER) {
            syncRegisterToRawIfReady();
        }
        currentDecodeInputMode = mode;
        registerInputContainer.setVisibility(mode == DecodeInputMode.REGISTER ? View.VISIBLE : View.GONE);
        rawInputContainer.setVisibility(mode == DecodeInputMode.REGISTER ? View.GONE : View.VISIBLE);
        setActiveSegment(List.of(registerInputModeButton, rawHexModeButton, rawBinaryModeButton),
                mode == DecodeInputMode.REGISTER ? 0 : mode == DecodeInputMode.HEX ? 1 : 2);
        if (mode == DecodeInputMode.HEX) {
            setRawDecodeMode(false);
        } else if (mode == DecodeInputMode.BINARY) {
            setRawDecodeMode(true);
        } else {
            updateDataTypeTables(false);
        }
    }

    private void syncRegisterToRawIfReady() {
        if (rawDecodeInput == null || register1Input == null || !hasText(register1Input.getText().toString())) {
            return;
        }
        try {
            int[] registers = readRegistersAllowBlank();
            StringBuilder hexBuilder = new StringBuilder();
            for (int register : registers) {
                hexBuilder.append(String.format(Locale.US, "%04X", register & 0xFFFF));
            }
            String hex = hexBuilder.toString();
            updateRawDecodeDisplay(hex, binaryFromHex(hex));
        } catch (Exception ignored) {
        }
    }

    private boolean updateRawDecode(boolean manual) {
        if (rawDecodeInput == null || rawDecodeMessage == null) {
            return false;
        }
        String raw = rawDecodeInput.getText().toString();
        String binary = rawDecodeBinaryMode ? cleanBinary(raw) : binaryFromHexSafe(raw);
        int expectedBits = expectedRawBits();
        if (!rawDecodeBinaryMode) {
            String compactHex = cleanHex(raw);
            if (!compactHex.matches("[0-9A-Fa-f]*")) {
                setRawDecodeMessage("Hex 只能輸入 0-9、A-F", true);
                if (manual) {
                    setForwardOperationState(OperationState.ERROR, "解析失敗", "Hex 只能輸入 0-9、A-F。");
                }
                return false;
            }
            int expectedHex = expectedBits / 4;
            if (!isVariableRawMode() && compactHex.length() < expectedHex) {
                setRawDecodeMessage("Hex 目前 " + compactHex.length() + " / " + expectedHex + " 碼，尚缺 " + (expectedHex - compactHex.length()) + " 碼。", false);
                updateRawPreview("", "");
                if (manual) {
                    setForwardOperationState(OperationState.ERROR, "解析失敗", "Hex 目前 " + compactHex.length() + " / " + expectedHex + " 碼，尚缺 " + (expectedHex - compactHex.length()) + " 碼。");
                }
                return false;
            }
            if (compactHex.isEmpty() || !isAllowedRawHex(compactHex)) {
                setRawDecodeMessage(rawLengthMessage("Hex", compactHex.length()), true);
                if (manual) {
                    setForwardOperationState(OperationState.ERROR, "解析失敗", rawLengthMessage("Hex", compactHex.length()));
                }
                return false;
            }
            binary = binaryFromHex(compactHex);
        } else {
            if (!raw.replaceAll("\\s+", "").matches("[01]*")) {
                setRawDecodeMessage("Binary 只能輸入 0 或 1，可包含空白分組。", true);
                if (manual) {
                    setForwardOperationState(OperationState.ERROR, "解析失敗", "Binary 只能輸入 0 或 1，可包含空白分組。");
                }
                return false;
            }
            if (!isVariableRawMode() && binary.length() < expectedBits) {
                setRawDecodeMessage("Binary 目前 " + binary.length() + " / " + expectedBits + " 位，尚缺 " + (expectedBits - binary.length()) + " 位。", false);
                updateRawPreview("", "");
                if (manual) {
                    setForwardOperationState(OperationState.ERROR, "解析失敗", "Binary 目前 " + binary.length() + " / " + expectedBits + " 位，尚缺 " + (expectedBits - binary.length()) + " 位。");
                }
                return false;
            }
            if (binary.isEmpty() || !isAllowedRawBits(binary.length())) {
                setRawDecodeMessage(rawLengthMessage("Binary", binary.length()), true);
                if (manual) {
                    setForwardOperationState(OperationState.ERROR, "解析失敗", rawLengthMessage("Binary", binary.length()));
                }
                return false;
            }
        }
        String hex = hexFromBinary(binary);
        lastValidRawBinary32 = binary;
        updateRawPreview(hex, binary);
        setRawDecodeMessage(rawDataLengthText(hex, binary), false);
        applyRawRegistersFromRaw(hex, binary);
        if (manual) {
            formatRawDecodeInputIfComplete();
            setForwardOperationState(OperationState.SUCCESS, "解析完成", "已更新四種 Byte Order 的解析結果。");
            scrollToOperationResult(true);
        } else {
            setForwardOperationState(OperationState.STALE, "待確認", "輸入已即時更新；按解析資料可確認本次資料。");
        }
        return true;
    }

    private void applyRawRegistersFromRaw(String hex, String binary) {
        String paddedHex = padHexToRegister(hex);
        int registerCount = paddedHex.length() / 4;
        int[] registers = new int[registerCount];
        for (int i = 0; i < registerCount; i++) {
            registers[i] = Integer.parseInt(paddedHex.substring(i * 4, i * 4 + 4), 16);
        }
        suppressLiveUpdate = true;
        setRegisterInputs(registers, registerCount);
        if (hexInput != null) {
            hexInput.setText(hex);
        }
        suppressLiveUpdate = false;
        updateDataTypeTables();
    }

    private String padHexToRegister(String hex) {
        String compact = cleanHex(hex);
        if (compact.length() % 4 == 0) {
            return compact;
        }
        StringBuilder builder = new StringBuilder(compact);
        while (builder.length() % 4 != 0) {
            builder.append('0');
        }
        return builder.toString();
    }

    private void updateRawDecodeDisplay(String hex, String binary) {
        if (rawDecodeInput == null || !hasText(hex) || !hasText(binary)) {
            return;
        }
        String compactBinary = cleanBinary(binary);
        formattingRawDecodeInput = true;
        rawDecodeInput.setText(rawDecodeBinaryMode ? displayBinaryForEditor(compactBinary) : hex);
        rawDecodeInput.setSelection(rawDecodeInput.getText().length());
        formattingRawDecodeInput = false;
        updateRawPreview(hex, compactBinary);
        if (isAllowedRawBits(compactBinary.length())) {
            lastValidRawBinary32 = compactBinary;
            setRawDecodeMessage(rawDataLengthText(hex, compactBinary), false);
        }
    }

    private void formatRawDecodeInputIfComplete() {
        if (rawDecodeInput == null) {
            return;
        }
        String raw = rawDecodeInput.getText().toString();
        String binary = rawDecodeBinaryMode ? cleanBinary(raw) : binaryFromHexSafe(raw);
        if (!isAllowedRawBits(binary.length())) {
            return;
        }
        formattingRawDecodeInput = true;
        rawDecodeInput.setText(rawDecodeBinaryMode ? groupBinaryForInput(binary) : hexFromBinary(binary));
        rawDecodeInput.setSelection(rawDecodeInput.getText().length());
        formattingRawDecodeInput = false;
    }

    private String displayBinaryForEditor(String binary) {
        return rawDecodeInput != null && rawDecodeInput.hasFocus() ? cleanBinary(binary) : groupBinaryForInput(binary);
    }

    private DataTypeMode selectedDataTypeMode() {
        if (dataTypeSpinner == null) {
            return DataTypeMode.FLOAT32;
        }
        String selected = String.valueOf(dataTypeSpinner.getSelectedItem());
        return isDataTypeHeader(selected) ? DataTypeMode.FLOAT32 : DataTypeMode.fromLabel(selected);
    }

    private int expectedRawBits() {
        DataTypeMode mode = selectedDataTypeMode();
        switch (mode) {
            case INT16:
            case UINT16:
                return 16;
            case FLOAT64:
                return 64;
            case ASCII:
            case BCD:
                int currentBits = currentRawBitsLength();
                if (currentBits <= 0) {
                    return Math.max(32, activeRegisterCount * 16);
                }
                return Math.max(8, currentBits);
            case INT32:
            case UINT32:
            case FLOAT32:
            default:
                return 32;
        }
    }

    private int currentRawBitsLength() {
        if (rawDecodeInput == null) {
            return 0;
        }
        String raw = rawDecodeInput.getText().toString();
        return rawDecodeBinaryMode ? cleanBinary(raw).length() : cleanHex(raw).length() * 4;
    }

    private boolean isAllowedRawBits(int bits) {
        DataTypeMode mode = selectedDataTypeMode();
        switch (mode) {
            case INT16:
            case UINT16:
                return bits == 16;
            case FLOAT64:
                return bits == 64;
            case ASCII:
            case BCD:
                return bits >= 8 && bits % 8 == 0;
            case INT32:
            case UINT32:
            case FLOAT32:
            default:
                return bits == 32;
        }
    }

    private String rawLengthMessage(String label, int length) {
        DataTypeMode mode = selectedDataTypeMode();
        if (mode == DataTypeMode.ASCII || mode == DataTypeMode.BCD) {
            if ("Hex".equals(label)) {
                return "Hex 長度必須為偶數，每 2 碼代表 1 Byte。";
            }
            return "Binary 長度必須為 8 的倍數，每 8 bits 代表 1 Byte。";
        }
        int expected = expectedRawBits();
        if ("Hex".equals(label)) {
            return "Hex 必須剛好 " + (expected / 4) + " 碼。";
        }
        return "Binary 必須剛好 " + expected + " bits。";
    }

    private boolean isVariableRawMode() {
        DataTypeMode mode = selectedDataTypeMode();
        return mode == DataTypeMode.ASCII || mode == DataTypeMode.BCD;
    }

    private boolean isAllowedRawHex(String hex) {
        String compact = cleanHex(hex);
        if (isVariableRawMode()) {
            return compact.length() >= 2 && compact.length() % 2 == 0;
        }
        return isAllowedRawBits(compact.length() * 4);
    }

    private String rawInputHint(boolean binaryMode, int expectedBits) {
        if (isVariableRawMode()) {
            return binaryMode
                    ? "Binary，可貼上任意完整 Byte，例如 01000001 01000010"
                    : "Hex，可貼上任意 Byte，例如 4142 4344 4546";
        }
        return binaryMode
                ? expectedBits + "-bit Binary，例如 01000001 00100000 ..."
                : (expectedBits / 4) + " 位 Hex，例如 4024000000000000";
    }

    private String rawDataLengthText(String hex, String binary) {
        int hexChars = cleanHex(hex).length();
        int bits = cleanBinary(binary).length();
        int bytes = bits / 8;
        int registers = (bytes + 1) / 2;
        String padding = bytes % 2 == 1 ? " · Padding 1 Byte：尾端 00" : "";
        return (rawDecodeBinaryMode ? "Binary " + bits + " bits" : "Hex " + hexChars + " 碼")
                + " · " + bytes + " Bytes · " + registers + " Registers" + padding + "\n✓ 完整";
    }

    private int[] registersFromHexCompact(String hex) {
        String compact = cleanHex(hex);
        int registerCount = compact.length() / 4;
        int[] registers = new int[registerCount];
        for (int i = 0; i < registerCount; i++) {
            registers[i] = Integer.parseInt(compact.substring(i * 4, i * 4 + 4), 16);
        }
        return registers;
    }

    private String registerPreviewText(int[] registers) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < registers.length; i++) {
            values.add("R" + (i + 1) + "  " + (registers[i] & 0xFFFF) + " / 0x" + String.format(Locale.US, "%04X", registers[i] & 0xFFFF));
        }
        return String.join("    ", values);
    }

    private void updateRawPreview(String hex, String binary) {
        if (rawDecodePreview == null) {
            return;
        }
        if (!hasText(hex) || !hasText(binary)) {
            rawDecodePreview.setText("");
            return;
        }
        String paddedHex = padHexToRegister(hex);
        int[] registers = registersFromHexCompact(paddedHex);
        DataTypeMode mode = selectedDataTypeMode();
        String value = "";
        try {
            List<DataTypeResult> rows = parseDataType(registers, mode);
            if (!rows.isEmpty()) {
                value = rows.get(0).dataType + "  " + rows.get(0).value;
            }
        } catch (Exception ignored) {
            value = "尚未符合 " + mode.label() + " 長度";
        }
        rawDecodePreview.setText(rawDataLengthText(hex, binary)
                + "\nHex  " + groupHexForDisplay(hex)
                + "\nBinary\n" + groupBinaryForInput(binary)
                + "\n" + registerPreviewText(registers)
                + "\n" + value);
    }

    private void setRawDecodeMessage(String message, boolean error) {
        rawDecodeMessage.setText(message);
        rawDecodeMessage.setTextColor(error ? dangerColor : mutedColor);
    }

    private void encodeInput() {
        try {
            DataTypeMode mode = selectedReverseDataTypeMode();
            fillReverseTable(DataTypeConverter.encode(floatInput.getText().toString(), mode));
            setReverseOperationState(OperationState.SUCCESS, "轉換完成", "已產生 " + mode.label() + " 的 Register、Hex 與 Binary 數值。");
            scrollToOperationResult(false);
        } catch (Exception ex) {
            String message = toUserMessage(ex.getMessage());
            setReverseOperationState(OperationState.ERROR, "轉換失敗", message);
            showError(message);
        }
    }

    private void liveEncodeInput() {
        if (reverseResultList == null || floatInput == null || !hasText(floatInput.getText().toString())) {
            if (reverseResultList != null) {
                setReverseOperationState(OperationState.IDLE, "尚未執行", "輸入資料後會即時預覽，按反向轉換可確認本次結果。");
            }
            return;
        }
        try {
            DataTypeMode mode = selectedReverseDataTypeMode();
            fillReverseTable(DataTypeConverter.encode(floatInput.getText().toString(), mode));
            setReverseOperationState(OperationState.STALE, "待確認", "輸入已即時更新；按反向轉換可確認本次結果。");
        } catch (Exception ignored) {
            latestReverseRows = new ArrayList<>();
            renderReverseResults();
            setReverseOperationState(OperationState.IDLE, "等待輸入", "目前輸入尚未形成可轉換資料。");
        }
    }

    private void showExample(float value) {
        floatInput.setText(formatFloat(value));
        if (reverseTypeSpinner != null) {
            selectDataType(reverseTypeSpinner, DataTypeMode.FLOAT32);
        }
        List<ConversionResult> encoded = ModbusFloatConverter.encodeFloat(value);
        fillReverseTable(DataTypeConverter.encodeFloat32(value));
        ConversionResult abcd = encoded.get(0);
        register1Input.setText(DataTypeConverter.formatRegisterInput(abcd.register1, currentRegisterInputMode));
        register2Input.setText(DataTypeConverter.formatRegisterInput(abcd.register2, currentRegisterInputMode));
        updateRawDecodeDisplay(abcd.hex, abcd.binary);
        updateDataTypeTables();
    }

    private DataTypeMode selectedReverseDataTypeMode() {
        if (reverseTypeSpinner == null) {
            return DataTypeMode.FLOAT32;
        }
        String selected = String.valueOf(reverseTypeSpinner.getSelectedItem());
        return isDataTypeHeader(selected) ? DataTypeMode.FLOAT32 : DataTypeMode.fromLabel(selected);
    }

    private void updateReverseInputForType() {
        if (floatInput == null || reverseExampleRow == null) {
            return;
        }
        DataTypeMode mode = selectedReverseDataTypeMode();
        switch (mode) {
            case INT16:
                floatInput.setHint("Int16，例如 -1、32767");
                rebuildReverseExamples(new String[]{"-1", "0", "1", "32767", "-32768"});
                break;
            case UINT16:
                floatInput.setHint("UInt16，例如 65535");
                rebuildReverseExamples(new String[]{"0", "1", "10", "1234", "65535"});
                break;
            case INT32:
                floatInput.setHint("Int32，例如 -1、2147483647");
                rebuildReverseExamples(new String[]{"-1", "0", "1", "1000", "2147483647"});
                break;
            case UINT32:
                floatInput.setHint("UInt32，例如 4294967295");
                rebuildReverseExamples(new String[]{"0", "1", "10", "65535", "4294967295"});
                break;
            case FLOAT64:
                floatInput.setHint("Float64，例如 10.0、-0.0、NaN、Infinity");
                rebuildReverseExamples(new String[]{"0.0", "1.0", "10.0", "100.0", "-1.0"});
                break;
            case ASCII:
                floatInput.setHint("ASCII 文字，例如 hello、TEST、ABCD");
                rebuildReverseExamples(new String[]{"hello", "TEST", "ABCD"});
                break;
            case BCD:
                floatInput.setHint("Packed BCD，例如 1234、00123456、98765432");
                rebuildReverseExamples(new String[]{"1234", "00123456", "98765432"});
                break;
            case FLOAT32:
            default:
                floatInput.setHint("Float32，例如 10.0、-0.0、NaN、Infinity");
                rebuildReverseExamples(new String[]{"0.0", "1.0", "10.0", "100.0", "-1.0"});
                break;
        }
    }

    private void rebuildReverseExamples(String[] values) {
        reverseExampleRow.removeAllViews();
        for (String value : values) {
            addChipButton(reverseExampleRow, value, v -> floatInput.setText(value));
        }
    }

    private void fillReverseTable(List<ReverseResult> rows) {
        latestReverseRows = new ArrayList<>(rows);
        expandBlock(encodeBlock);
        renderReverseResults();
    }

    private void parsePacket(boolean tcp) {
        latestPacketResult = tcp
                ? ModbusPacketParser.parseTcp(packetHexInput.getText().toString())
                : ModbusPacketParser.parseRtu(packetHexInput.getText().toString());
        renderPacketResult(latestPacketResult);
        scrollToView(packetStatusText);
    }

    private void showPacketExampleChooser() {
        boolean tcp = packetTypeGroup != null && packetTypeGroup.getCheckedRadioButtonId() == 2;
        ModbusPacketSamples.Direction direction = currentPacketDirection();
        List<ModbusPacketSamples.SamplePacket> examples = ModbusPacketSamples.filter(tcp, direction);
        if (examples.isEmpty()) {
            showPacketStatus("目前模式沒有可用測試範例", true);
            return;
        }
        String[] labels = new String[examples.size()];
        for (int i = 0; i < examples.size(); i++) {
            ModbusPacketSamples.SamplePacket example = examples.get(i);
            labels[i] = example.name + "\n" + direction.zh + "  FC" + String.format(Locale.US, "%02X", example.functionCode & 0xFF) + "  " + example.purpose;
        }
        new AlertDialog.Builder(this)
                .setTitle("選擇" + (tcp ? " TCP " : " RTU ") + direction.zh + "範例")
                .setItems(labels, (dialog, which) -> loadPacketExample(examples.get(which), true))
                .show();
    }

    private ModbusPacketSamples.Direction currentPacketDirection() {
        return packetDirectionGroup != null && packetDirectionGroup.getCheckedRadioButtonId() == 4
                ? ModbusPacketSamples.Direction.RESPONSE
                : ModbusPacketSamples.Direction.REQUEST;
    }

    private void loadPacketExample(ModbusPacketSamples.SamplePacket example, boolean parseImmediately) {
        selectedPacketExample = example;
        packetHexInput.setText(formatHexPairs(example.hex));
        updatePacketExampleText();
        if (parseImmediately) {
            parsePacket(packetTypeGroup != null && packetTypeGroup.getCheckedRadioButtonId() == 2);
        }
    }

    private void updatePacketExampleText() {
        if (packetExampleText == null || selectedPacketExample == null) {
            return;
        }
        packetExampleText.setText("測試範例：" + selectedPacketExample.name
                + "\n用途：" + selectedPacketExample.purpose
                + "\n預期解析結果：\n" + selectedPacketExample.expected);
    }

    private void appendPacketField(SpannableStringBuilder block, String label, String value) {
        if (block.length() > 0 && block.charAt(block.length() - 1) != '\n') {
            block.append('\n');
        }
        appendStyled(block, label + "：\n", mutedColor, 0.88f, Typeface.NORMAL, false);
        appendStyled(block, value == null ? "" : value, textColor, 0.96f, Typeface.NORMAL, true);
        block.append("\n\n");
    }

    private void renderPacketResult(PacketParseResult result) {
        boolean failed = result.error != null || (result.crcPresent && !result.crcValid);
        showPacketStatus(failed
                ? "！ 解析失敗\n" + toUserMessage(result.error != null ? result.error : "CRC Check 錯誤")
                : "✓ 解析完成\n已更新 Modbus 封包欄位與動作說明。", failed);
        SpannableStringBuilder block = new SpannableStringBuilder();
        appendStyled(block, "封包欄位\n", accentColor, 1.18f, Typeface.BOLD, false);
        if (result.tcp) {
            appendPacketField(block, "Transaction ID", result.transactionId);
            appendPacketField(block, "Protocol ID", result.protocolId);
            appendPacketField(block, "MBAP Length", result.length);
            if (hasText(result.mbapLengthCheck)) {
                appendPacketField(block, "Length 驗證", result.mbapLengthCheck);
            }
            appendPacketField(block, "Unit ID", result.unitId);
        } else {
            appendPacketField(block, "Slave ID", result.slaveId);
        }
        appendPacketField(block, "Function Code", result.functionCode);
        appendPacketField(block, "Frame", result.frameKind);
        appendPacketField(block, "Action", result.action);
        for (String[] detail : result.details) {
            appendPacketField(block, detail[0], detail[1]);
        }
        if (hasText(result.byteCount)) {
            appendPacketField(block, "Byte Count", result.byteCount);
        }
        if (hasText(result.registerDataHex)) {
            appendPacketField(block, "Register Data", formatHexPairs(result.registerDataHex));
            if (hasText(result.registerDataSource)) {
                appendPacketField(block, "Register Data 來源", result.registerDataSource);
            }
            if (result.registers.size() >= 2) {
                List<DataTypeResult> floats = DataTypeConverter.parse(new int[]{
                        result.registers.get(0),
                        result.registers.get(1),
                        result.registers.size() > 2 ? result.registers.get(2) : 0,
                        result.registers.size() > 3 ? result.registers.get(3) : 0
                }, DataTypeMode.FLOAT32);
                appendPacketField(block, "ABCD Float32", floats.get(0).value);
            }
        }
        appendPacketField(block, "可轉入資料解碼", result.registerDataDecodable ? "可以" : "不可以：" + result.transferBlockedReason);
        if (result.crcPresent) {
            appendPacketField(block, "CRC Received", result.crcReceived);
            appendPacketField(block, "CRC Calculated", result.crcCalculated);
            appendPacketField(block, "CRC Check", result.crcValid ? "正確" : "錯誤");
        }
        packetSummaryText.setText(block);
        if (packetTransferButton != null) {
            packetTransferButton.setEnabled(result.registerDataDecodable);
            packetTransferButton.setAlpha(result.registerDataDecodable ? 1f : 0.55f);
        }
        updatePacketExampleText();
    }

    private void clearPacket() {
        packetHexInput.setText("");
        if (packetSummaryText != null) {
            packetSummaryText.setText("尚未解析封包。");
        }
        latestPacketResult = null;
        if (packetTransferButton != null) {
            packetTransferButton.setEnabled(false);
            packetTransferButton.setAlpha(0.55f);
        }
        selectedPacketExample = null;
        if (packetExampleText != null) {
            packetExampleText.setText("");
        }
        showPacketStatus("已清除", false);
    }

    private void loadPacketRegisterDataToDecoder() {
        if (latestPacketResult == null || !latestPacketResult.registerDataDecodable || latestPacketResult.registers.isEmpty()) {
            showPacketStatus(latestPacketResult != null && hasText(latestPacketResult.transferBlockedReason)
                    ? latestPacketResult.transferBlockedReason
                    : "沒有可供資料解碼的 Register Data", true);
            return;
        }
        showPage(0);
        showConverterMode(true);
        setDecodeInputMode(DecodeInputMode.REGISTER);
        suppressLiveUpdate = true;
        int count = latestPacketResult.registers.size();
        int[] registers = new int[count];
        for (int i = 0; i < count; i++) {
            registers[i] = latestPacketResult.registers.get(i);
        }
        setRegisterInputs(registers, count);
        if (dataTypeSpinner != null) {
            selectDataType(dataTypeSpinner, count >= 4 ? DataTypeMode.FLOAT64 : count >= 2 ? DataTypeMode.FLOAT32 : DataTypeMode.UINT16);
        }
        suppressLiveUpdate = false;
        updateRegisterFieldVisibility();
        updateDataTypeTables(true);
        setForwardOperationState(OperationState.SUCCESS, "解析完成", "已從封包 Register Data 更新資料解碼結果。");
        scrollToOperationResult(true);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String formatHexPairs(String value) {
        String compact = value == null ? "" : value.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.US);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < compact.length(); i++) {
            if (i > 0 && i % 2 == 0) {
                builder.append(' ');
            }
            builder.append(compact.charAt(i));
        }
        return builder.toString();
    }

    private void showPacketStatus(String message, boolean error) {
        packetStatusText.setTextColor(error ? dangerColor : successColor);
        packetStatusText.setText(message);
    }

    private void showHelp(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("關閉", null)
                .show();
    }

    private String converterHelp() {
        return "資料轉換分頁用於驗證 register、Binary、Hex 與 IEEE754 浮點數。\n\n"
                + "1. Register 可用 Unsigned、Signed、Hex、Binary 輸入。\n"
                + "2. 資料型態可切換 Int16、UInt16、Int32、UInt32、Float32、Float64、ASCII、BCD。\n"
                + "3. Binary / Hex 支援 32-bit 與 64-bit，會自動填入 R1-R4。\n"
                + "4. 資料反向轉換支援 Int16、UInt16、Int32、UInt32、Float32、Float64、ASCII 與 Packed BCD。";
    }

    private String packetHelp() {
        return "通訊封包分頁用於解析 Modbus RTU / TCP 回應封包。\n\n"
                + "RTU：輸入完整 HEX，例如 01 03 04 42 48 00 00 6E 5D，會解析 Slave ID、Function、Byte Count、Register Data 與 CRC。\n"
                + "TCP：輸入含 MBAP Header 的 HEX，會解析 Transaction ID、Protocol ID、Length、Unit ID、Function 與資料。\n"
                + "程式會判斷封包是讀取或寫入，並顯示讀寫位址、數量或寫入值。資料轉換與 IEEE754 運算請使用資料轉換分頁。";
    }

    private View buildHelpPanel() {
        LinearLayout box = stack();
        TextView help = text(converterHelp() + "\n\n" + packetHelp()
                + "\n\nByte Order：ABCD 為 IEEE754 原始位元組順序；CDAB 為 Word Swap；BADC 為 Byte Swap；DCBA 為 Byte + Word Swap。", 14, false);
        help.setTextColor(textColor);
        help.setPadding(0, 0, 0, dp(8));
        box.addView(help);
        box.addView(primaryWideButton("資料轉換說明", v -> showHelp("資料轉換說明", converterHelp())));
        box.addView(primaryWideButton("通訊封包說明", v -> showHelp("通訊封包說明", packetHelp())));
        return box;
    }

    private void applyRawHex(String rawHex) {
        String hex = cleanHex(rawHex);
        if (!hex.matches("[0-9A-F]+") || !isAllowedRawHex(hex)) {
            throw new IllegalArgumentException(rawLengthMessage("Hex", hex.length()));
        }
        String paddedHex = padHexToRegister(hex);
        int registerCount = paddedHex.length() / 4;
        int[] registers = new int[registerCount];
        for (int i = 0; i < registerCount; i++) {
            registers[i] = Integer.parseInt(paddedHex.substring(i * 4, i * 4 + 4), 16);
        }
        applyRawRegisters(registers, registerCount, hex, binaryFromHex(hex));
    }

    private void applyRawBits(String rawBinary, int ignored) {
        String binary = rawBinary == null ? "" : rawBinary.replaceAll("\\s+", "");
        if (!binary.matches("[01]+") || !isAllowedRawBits(binary.length())) {
            throw new IllegalArgumentException(rawLengthMessage("Binary", binary.length()));
        }
        String hex = hexFromBinary(binary);
        String paddedHex = padHexToRegister(hex);
        int registerCount = paddedHex.length() / 4;
        int[] registers = new int[registerCount];
        for (int i = 0; i < registerCount; i++) {
            registers[i] = Integer.parseInt(paddedHex.substring(i * 4, i * 4 + 4), 16);
        }
        applyRawRegisters(registers, registerCount, hex, binary);
    }

    private void applyRawRegisters(int[] registers, int registerCount, String hex, String binary) {
        suppressLiveUpdate = true;
        setRegisterInputs(registers, registerCount);
        if (hexInput != null) {
            hexInput.setText(hex);
        }
        if (binaryInput != null && binaryInput != rawDecodeInput) {
            binaryInput.setText(groupBinaryAny(binary));
        }
        updateRawDecodeDisplay(hex, binary);
        suppressLiveUpdate = false;
        updateDataTypeTables(false);
    }

    private void setRegisterInputs(int[] registers, int registerCount) {
        ensureRegisterInputCount(registerCount);
        if (isVariableRegisterMode()) {
            activeRegisterCount = Math.max(1, registerCount);
            lastVariableRegisterCount = activeRegisterCount;
        }
        renderRegisterFields();
        for (int i = 0; i < registerInputs.size(); i++) {
            registerInputs.get(i).setText(i < registerCount ? DataTypeConverter.formatRegisterInput(registers[i], currentRegisterInputMode) : "");
        }
    }

    private void addRow(TableLayout table, boolean header, String... values) {
        TableRow row = new TableRow(this);
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            TextView cell = text(value == null ? "" : value, header ? 15 : 14, header);
            cell.setTextColor(header ? accentColor : textColor);
            cell.setTextIsSelectable(!header);
            cell.setPadding(dp(8), dp(7), dp(8), dp(7));
            cell.setMinWidth(dp(i == 1 ? 245 : header ? 92 : 110));
            if (i == 1 && !header) {
                cell.setTypeface(Typeface.MONOSPACE);
                cell.setTextSize(13);
            }
            cell.setBackgroundColor(header ? bgColor : panelColor);
            row.addView(cell);
        }
        table.addView(row);
    }

    private boolean updateDataTypeTables() {
        return updateDataTypeTables(false);
    }

    private boolean updateDataTypeTables(boolean manual) {
        if (suppressLiveUpdate || dataTypeSpinner == null || forwardResultList == null) {
            return false;
        }
        try {
            if (!updateRegisterInputMessages()) {
                latestForwardRows = new ArrayList<>();
                forwardResultList.removeAllViews();
                setForwardOperationState(manual ? OperationState.ERROR : OperationState.IDLE,
                        manual ? "解析失敗" : "等待輸入",
                        "請先完成 Register Binary 輸入。");
                return false;
            }
            if (!hasRequiredForwardInputs()) {
                latestForwardRows = new ArrayList<>();
                forwardResultList.removeAllViews();
                setForwardOperationState(manual ? OperationState.ERROR : OperationState.IDLE,
                        manual ? "解析失敗" : "等待輸入",
                        "請輸入目前資料型態需要的 Register。");
                return false;
            }
            int[] registers = readRegistersAllowBlank();
            DataTypeMode mode = DataTypeMode.fromLabel(String.valueOf(dataTypeSpinner.getSelectedItem()));
            fillDataTypeTable(parseDataType(registers, mode));
            if (!manual) {
                setForwardOperationState(OperationState.STALE, "待確認", "輸入已即時更新；按解析可確認本次結果。");
            }
            return true;
        } catch (Exception ex) {
            if (manual) {
                setForwardOperationState(OperationState.ERROR, "解析失敗", toUserMessage(ex.getMessage()));
            }
            showError(ex.getMessage());
            return false;
        }
    }

    private int[] readRegistersAllowBlank() {
        int count = requiredRegisterCount();
        int[] registers = new int[count];
        for (int i = 0; i < count; i++) {
            registers[i] = parseVisibleRegister(registerInputs.get(i));
        }
        return registers;
    }

    private int parseVisibleRegister(EditText input) {
        return DataTypeConverter.parseRegister(input.getText().toString(), currentRegisterInputMode);
    }

    private void fillDataTypeTable(List<DataTypeResult> rows) {
        latestForwardRows = new ArrayList<>(rows);
        expandBlock(dataTypeBlock);
        renderForwardResults();
    }

    private List<DataTypeResult> parseDataType(int[] registers, DataTypeMode mode) {
        if (mode == DataTypeMode.ASCII) {
            return DataTypeConverter.parseAscii(registers, asciiTrimTrailingNull);
        }
        return DataTypeConverter.parse(registers, mode);
    }

    private boolean hasRequiredForwardInputs() {
        if (dataTypeSpinner == null) {
            return false;
        }
        DataTypeMode mode = DataTypeMode.fromLabel(String.valueOf(dataTypeSpinner.getSelectedItem()));
        if (!hasText(register1Input.getText().toString())) {
            return false;
        }
        int count = requiredRegisterCount();
        for (int i = 0; i < count; i++) {
            if (!hasText(registerInputs.get(i).getText().toString())) {
                return false;
            }
        }
        return true;
    }

    private int requiredRegisterCount() {
        DataTypeMode mode = selectedDataTypeMode();
        if (asciiNullModeSpinner != null) {
            asciiNullModeSpinner.setVisibility(mode == DataTypeMode.ASCII ? View.VISIBLE : View.GONE);
        }
        if (mode == DataTypeMode.INT16 || mode == DataTypeMode.UINT16) {
            return 1;
        }
        if (mode == DataTypeMode.FLOAT64) {
            return 4;
        }
        if (mode == DataTypeMode.ASCII || mode == DataTypeMode.BCD) {
            return Math.max(1, activeRegisterCount);
        }
        return 2;
    }

    private boolean isVariableRegisterMode() {
        DataTypeMode mode = selectedDataTypeMode();
        return mode == DataTypeMode.ASCII || mode == DataTypeMode.BCD;
    }

    private int highestNonBlankRegisterIndex() {
        int highest = 0;
        for (int i = 0; i < registerInputs.size(); i++) {
            if (hasText(registerInputs.get(i).getText().toString())) {
                highest = i + 1;
            }
        }
        return highest;
    }

    private void updateRegisterFieldVisibility() {
        if (dataTypeSpinner == null || registerRowsContainer == null) {
            return;
        }
        DataTypeMode mode = selectedDataTypeMode();
        if (mode == DataTypeMode.INT16 || mode == DataTypeMode.UINT16) {
            activeRegisterCount = 1;
        } else if (mode == DataTypeMode.FLOAT64) {
            activeRegisterCount = 4;
        } else if (mode == DataTypeMode.ASCII || mode == DataTypeMode.BCD) {
            activeRegisterCount = Math.max(2, Math.max(lastVariableRegisterCount, highestNonBlankRegisterIndex()));
        } else {
            activeRegisterCount = 2;
        }
        ensureRegisterInputCount(activeRegisterCount);
        renderRegisterFields();
    }

    private boolean updateRegisterInputMessages() {
        if (registerMessages.isEmpty()) {
            return true;
        }
        boolean ok = true;
        for (int i = 0; i < registerInputs.size(); i++) {
            TextView message = registerMessages.get(i);
            EditText input = registerInputs.get(i);
            message.setText("");
            if (i >= activeRegisterCount || currentRegisterInputMode != RegisterInputMode.BINARY || input.getVisibility() == View.GONE) {
                continue;
            }
            String raw = input.getText().toString();
            String compact = cleanBinary(raw);
            if (!raw.replaceAll("\\s+", "").matches("[01]*")) {
                message.setText("Register " + (i + 1) + "：只能輸入 0 或 1。");
                message.setTextColor(dangerColor);
                ok = false;
            } else if (hasText(raw) && compact.length() < 16) {
                message.setText("Register " + (i + 1) + "：目前輸入 " + compact.length() + " / 16 位，尚缺 " + (16 - compact.length()) + " 位。");
                message.setTextColor(mutedColor);
                ok = false;
            } else if (compact.length() > 16) {
                message.setText("Register " + (i + 1) + "：Binary 只能輸入 16 bits。");
                message.setTextColor(dangerColor);
                ok = false;
            } else if (compact.length() == 16) {
                message.setText("Register " + (i + 1) + "：16 / 16 位。");
                message.setTextColor(mutedColor);
            }
        }
        return ok;
    }

    private LinearLayout buildResultFormatTabs(boolean forward) {
        LinearLayout group = equalRow(4);
        List<Button> buttons = forward ? forwardFormatButtons : reverseFormatButtons;
        buttons.clear();
        ResultFormatMode[] modes = ResultFormatMode.values();
        for (ResultFormatMode mode : modes) {
            Button button = addSegmentButton(group, mode.label, v -> {
                if (forward) {
                    forwardResultMode = mode;
                    setActiveSegment(forwardFormatButtons, mode.ordinal());
                    renderForwardResults();
                } else {
                    reverseResultMode = mode;
                    setActiveSegment(reverseFormatButtons, mode.ordinal());
                    renderReverseResults();
                }
            });
            buttons.add(button);
        }
        setActiveSegment(buttons, ResultFormatMode.ALL.ordinal());
        return group;
    }

    private void renderForwardResults() {
        if (forwardResultList == null) {
            return;
        }
        forwardResultList.removeAllViews();
        if (latestForwardRows.isEmpty()) {
            forwardResultList.addView(helperText("請輸入完整 Register、Binary 或 Hex。"));
            return;
        }
        for (DataTypeResult row : latestForwardRows) {
            forwardResultList.addView(resultRow(row.order, row.dataType, row.value, row.hex, row.binary,
                    registersFromHex(row.hex, row.registers), forwardResultMode));
        }
    }

    private void renderReverseResults() {
        if (reverseResultList == null) {
            return;
        }
        reverseResultList.removeAllViews();
        if (latestReverseRows.isEmpty()) {
            reverseResultList.addView(helperText("輸入資料後會即時顯示反向轉換結果。"));
            return;
        }
        for (ReverseResult row : latestReverseRows) {
            String hex = reverseHex(row);
            String binary = groupBinaryAny(binaryFromHex(hex));
            String registers = reverseRegisters(row);
            reverseResultList.addView(resultRow(row.order, row.dataType, row.value, hex, binary, registers, reverseResultMode));
        }
    }

    private View resultRow(String order, String type, String value, String hex, String binary, String registers, ResultFormatMode mode) {
        return resultCard(order, type, value, hex, binary, registers, mode, 0x5565E5B5, dp(18));
    }

    private View textDecodeResultRow(String order, String type, String value, String hex, String binary, String registers) {
        return resultCard(order, type, value, hex, binary, registers, ResultFormatMode.ALL, 0x5565E5B5, dp(18));
    }

    private View floatResultRow(String order, String type, String value, String hex, String binary, String registers) {
        return resultCard(order, type, value, hex, binary, registers, ResultFormatMode.ALL, 0x6665E5B5, dp(18));
    }

    private View resultCard(String order, String type, String value, String hex, String binary, String registers, ResultFormatMode mode, int strokeColor, int radius) {
        LinearLayout card = stack();
        card.setBackground(cardBackground(instrumentFillColor, strokeColor, radius));
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setElevation(dp(3));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);
        card.addView(selectableResultText(resultSpannable(order, type, value, hex, binary, registers, mode)));
        return card;
    }

    private SpannableStringBuilder resultSpannable(String order, String type, String value, String hex, String binary, String registers, ResultFormatMode mode) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        appendStyled(builder, order + "\n", textColor, 1.12f, Typeface.BOLD, false);
        if (hasText(type)) {
            appendStyled(builder, type + "\n", mutedColor, 0.88f, Typeface.NORMAL, false);
        }
        switch (mode) {
            case HEX:
                appendSection(builder, "Hex", groupHexForDisplay(hex), true);
                break;
            case REGISTER:
                appendSection(builder, "Register", registerLines(registers), true);
                appendSection(builder, "Hex Register", registerHexLines(hex), true);
                break;
            case BINARY:
                appendSection(builder, "Binary", groupBinaryAny(binary), true);
                break;
            case ALL:
            default:
                if (isPrimaryTextType(type)) {
                    appendStyled(builder, value + "\n", successColor, 1.7f, Typeface.BOLD, true);
                } else {
                    appendSection(builder, "Value", value, true);
                }
                appendSection(builder, "Hex", groupHexForDisplay(hex), true);
                appendSection(builder, "Register", registerLines(registers), true);
                if (isTextPayloadType(type)) {
                    appendSection(builder, "資料長度", (cleanHex(hex).length() / 2) + " Bytes · " + ((cleanHex(hex).length() + 3) / 4) + " Registers", false);
                }
                appendSection(builder, "Binary", groupBinaryAny(binary), true);
                break;
        }
        return builder;
    }

    private void appendSection(SpannableStringBuilder builder, String label, String value, boolean monoValue) {
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        builder.append('\n');
        appendStyled(builder, label + "：\n", mutedColor, 0.88f, Typeface.NORMAL, false);
        appendStyled(builder, value == null ? "" : value, textColor, 0.95f, Typeface.NORMAL, monoValue);
        builder.append('\n');
    }

    private boolean isPrimaryTextType(String type) {
        return type != null && (type.startsWith("Float") || type.startsWith("ASCII") || type.startsWith("Packed BCD") || type.startsWith("BCD"));
    }

    private boolean isTextPayloadType(String type) {
        return type != null && (type.startsWith("ASCII") || type.startsWith("Packed BCD") || type.startsWith("BCD"));
    }

    private TextView selectableResultText(CharSequence value) {
        TextView view = text(value == null ? "" : value.toString(), 14, false);
        view.setText(value);
        view.setTextIsSelectable(true);
        view.setSingleLine(false);
        view.setHorizontallyScrolling(false);
        view.setTextColor(textColor);
        view.setLineSpacing(dp(2), 1.0f);
        return view;
    }

    private void appendStyled(SpannableStringBuilder builder, String text, int color, float relativeSize, int style, boolean monospace) {
        int start = builder.length();
        builder.append(text == null ? "" : text);
        int end = builder.length();
        if (end <= start) {
            return;
        }
        builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new RelativeSizeSpan(relativeSize), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (style != Typeface.NORMAL) {
            builder.setSpan(new StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (monospace) {
            builder.setSpan(new TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private Button tinyButton(String label, View.OnClickListener listener) {
        Button button = button(label, listener);
        button.setTextSize(11);
        button.setMinWidth(dp(52));
        button.setPadding(dp(4), 0, dp(4), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(34), 1f);
        params.setMargins(0, dp(6), dp(5), 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView helperText(String value) {
        TextView text = text(value, 14, false);
        text.setTextColor(mutedColor);
        text.setPadding(0, dp(6), 0, dp(6));
        return text;
    }

    private String reverseHex(ReverseResult row) {
        if (hasText(row.hex)) {
            return row.hex;
        }
        StringBuilder builder = new StringBuilder();
        appendRegisterHex(builder, row.register1);
        appendRegisterHex(builder, row.register2);
        appendRegisterHex(builder, row.register3);
        appendRegisterHex(builder, row.register4);
        return builder.length() > 0 ? builder.toString() : row.hex;
    }

    private String reverseRegisters(ReverseResult row) {
        if (hasText(row.registers)) {
            return row.registers;
        }
        List<String> values = new ArrayList<>();
        addIfText(values, row.register1);
        addIfText(values, row.register2);
        addIfText(values, row.register3);
        addIfText(values, row.register4);
        return String.join(" / ", values);
    }

    private String registersFromHex(String hex, String fallback) {
        String compact = hex == null ? "" : hex.replaceAll("\\s+", "");
        if (!compact.matches("[0-9A-Fa-f]+") || compact.length() % 4 != 0) {
            return fallback == null ? "" : fallback;
        }
        List<String> registers = new ArrayList<>();
        for (int i = 0; i + 4 <= compact.length(); i += 4) {
            registers.add(String.valueOf(Integer.parseInt(compact.substring(i, i + 4), 16)));
        }
        return String.join(" / ", registers);
    }

    private String registerLines(String registers) {
        if (!hasText(registers)) {
            return "";
        }
        String normalized = registers.replace(",", "/");
        String[] parts = normalized.split("/");
        List<String> rows = new ArrayList<>();
        for (String part : parts) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            rows.add("Register " + (rows.size() + 1) + "：" + value);
        }
        return String.join("\n", rows);
    }

    private String registerHexText(String hex) {
        String compact = hex == null ? "" : hex.replaceAll("\\s+", "").toUpperCase(Locale.US);
        List<String> registers = new ArrayList<>();
        for (int i = 0; i + 4 <= compact.length(); i += 4) {
            registers.add(compact.substring(i, i + 4));
        }
        return String.join(" / ", registers);
    }

    private String registerHexLines(String hex) {
        String compact = hex == null ? "" : hex.replaceAll("\\s+", "").toUpperCase(Locale.US);
        List<String> registers = new ArrayList<>();
        for (int i = 0; i + 4 <= compact.length(); i += 4) {
            registers.add("Register " + (registers.size() + 1) + "：0x" + compact.substring(i, i + 4));
        }
        return String.join("\n", registers);
    }

    private void appendRegisterHex(StringBuilder builder, String value) {
        if (!hasText(value)) {
            return;
        }
        builder.append(String.format(Locale.US, "%04X", Integer.parseInt(value) & 0xFFFF));
    }

    private void addIfText(List<String> values, String value) {
        if (hasText(value)) {
            values.add(value);
        }
    }

    private void expandBlock(View block) {
        if (!(block instanceof LinearLayout)) {
            return;
        }
        LinearLayout section = (LinearLayout) block;
        if (section.getChildCount() < 2 || !(section.getChildAt(0) instanceof TextView)) {
            return;
        }
        TextView heading = (TextView) section.getChildAt(0);
        View content = section.getChildAt(1);
        content.setVisibility(View.VISIBLE);
        String label = heading.getText().toString().replace("▶ ", "").replace("▼ ", "");
        heading.setText("▼ " + label);
    }

    private void convertRegisterInputs(RegisterInputMode previousMode, RegisterInputMode nextMode) {
        if (registerInputs.isEmpty()) {
            return;
        }
        suppressLiveUpdate = true;
        for (EditText input : registerInputs) {
            String original = input.getText().toString();
            Integer parsedValue = null;
            if (hasText(original)) {
                try {
                    parsedValue = DataTypeConverter.parseRegister(original, previousMode);
                } catch (Exception ignored) {
                    parsedValue = null;
                }
            }
            if (parsedValue != null) {
                input.setText(DataTypeConverter.formatRegisterInput(parsedValue, nextMode));
            }
        }
        convertRegisterInputWidgets(nextMode);
        suppressLiveUpdate = false;
        updateRegisterInputMessages();
    }

    private void convertRegisterInputWidgets(RegisterInputMode mode) {
        for (EditText input : registerInputs) {
            input.setInputType(mode == RegisterInputMode.SIGNED
                    ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED
                    : mode == RegisterInputMode.UNSIGNED
                    ? InputType.TYPE_CLASS_NUMBER
                    : InputType.TYPE_CLASS_TEXT);
            boolean binary = mode == RegisterInputMode.BINARY;
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(binary ? 24 : 17)});
            input.setTypeface(binary ? Typeface.MONOSPACE : Typeface.DEFAULT);
            input.setSingleLine(!binary);
            input.setMinLines(binary ? 2 : 1);
            input.setMaxLines(binary ? 2 : 1);
            input.setHorizontallyScrolling(false);
        }
    }

    private void attachRegisterBinaryFormatter(EditText input) {
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus || currentRegisterInputMode != RegisterInputMode.BINARY) {
                return;
            }
            String compact = cleanBinary(input.getText().toString());
            if (compact.length() == 16) {
                input.setText(groupBinary16ForInput(compact));
            }
        });
    }

    private String[] labels(RegisterInputMode[] modes) {
        String[] labels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            labels[i] = modes[i].label();
        }
        return labels;
    }

    private String[] labels(DataTypeMode[] modes) {
        String[] labels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            labels[i] = modes[i].label();
        }
        return labels;
    }

    private int parseRegister(EditText input, String label) {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException(label + " 不可空白");
        }
        try {
            int value = Integer.parseInt(raw);
            ModbusFloatConverter.validateRegister(value, label);
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " 必須是整數");
        }
    }

    private void clearDecodeInputs() {
        suppressLiveUpdate = true;
        for (EditText input : registerInputs) {
            input.setText("");
        }
        if (rawDecodeInput != null) {
            rawDecodeInput.setText("");
        }
        if (hexInput != null) {
            hexInput.setText("");
        }
        lastValidRawBinary32 = "";
        suppressLiveUpdate = false;
        latestForwardRows = new ArrayList<>();
        if (forwardResultList != null) {
            forwardResultList.removeAllViews();
        }
        showStatus("已清除");
        setForwardOperationState(OperationState.IDLE, "尚未執行", "按下解析後，這裡會顯示本次操作狀態。");
    }

    private void setForwardOperationState(OperationState state, String title, String detail) {
        setOperationState(forwardOperationTitle, forwardOperationDetail, state, title, detail);
    }

    private void setReverseOperationState(OperationState state, String title, String detail) {
        setOperationState(reverseOperationTitle, reverseOperationDetail, state, title, detail);
    }

    private void setOperationState(TextView titleView, TextView detailView, OperationState state, String title, String detail) {
        if (titleView == null || detailView == null) {
            return;
        }
        int color;
        String prefix;
        switch (state) {
            case SUCCESS:
                color = successColor;
                prefix = "✓ ";
                break;
            case ERROR:
                color = dangerColor;
                prefix = "！ ";
                break;
            case RUNNING:
                color = accentColor;
                prefix = "";
                break;
            case STALE:
                color = mutedColor;
                prefix = "";
                break;
            case IDLE:
            default:
                color = mutedColor;
                prefix = "";
                break;
        }
        titleView.setText(prefix + title);
        titleView.setTextColor(color);
        detailView.setText(detail);
        detailView.setTextColor(state == OperationState.ERROR ? dangerColor : mutedColor);
        if (titleView.getParent() instanceof View) {
            View bar = (View) titleView.getParent();
            int stroke = state == OperationState.SUCCESS ? 0x8865E5B5 : state == OperationState.ERROR ? 0x88FF7A8A : glassBorderColor;
            bar.setBackground(cardBackground(0x6618273B, stroke, dp(16)));
            if (state == OperationState.SUCCESS) {
                bar.setAlpha(0.82f);
                bar.animate().alpha(1f).setDuration(180).start();
            }
        }
        if (statusText != null && (state == OperationState.SUCCESS || state == OperationState.STALE || state == OperationState.IDLE)) {
            statusText.setText("");
        }
    }

    private void scrollToOperationResult(boolean forward) {
        View target = forward ? forwardOperationStatusBar : reverseOperationStatusBar;
        if (target == null) {
            target = forward ? dataTypeBlock : encodeBlock;
        }
        scrollToView(target);
    }

    private void scrollToView(View target) {
        if (mainScrollView == null || target == null) {
            return;
        }
        mainScrollView.post(() -> {
            int y = 0;
            View current = target;
            while (current != null && current != mainScrollView) {
                y += current.getTop();
                if (current.getParent() instanceof View) {
                    current = (View) current.getParent();
                } else {
                    break;
                }
            }
            int scrollY = Math.max(0, y - dp(12));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()) {
                mainScrollView.smoothScrollTo(0, scrollY);
            } else {
                mainScrollView.scrollTo(0, scrollY);
            }
        });
    }

    private void showError(String message) {
        statusText.setTextColor(dangerColor);
        statusText.setText(toUserMessage(message));
    }

    private void showStatus(String message) {
        if (statusText == null) {
            return;
        }
        statusText.setTextColor(accentColor);
        statusText.setText(message);
    }

    private String toUserMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "輸入格式尚未完成";
        }
        if (message.startsWith("For input string")) {
            return "輸入格式尚未完成，請確認位數與格式。";
        }
        if (message.contains("must be")) {
            return message.replace("must be exactly 8 characters, 0-9 or A-F", "必須剛好 8 碼，且只能包含 0-9 A-F")
                    .replace("must be exactly 32 bits and only 0 or 1", "必須剛好 32 bits，且只能包含 0 或 1")
                    .replace("must be 0 to 65535", "必須介於 0 到 65535");
        }
        return message;
    }

    private String formatFloat(float value) {
        return String.format(Locale.US, "%.7g", value);
    }

    private String exampleLabel(float value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String groupBinary(String binary) {
        if (binary == null) {
            return "";
        }
        String compact = binary.replaceAll("\\s+", "");
        if (compact.length() != 32) {
            return binary;
        }
        return compact.substring(0, 8) + " " + compact.substring(8, 16) + "\n"
                + compact.substring(16, 24) + " " + compact.substring(24, 32);
    }

    private String cleanBinary(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private String cleanHex(String value) {
        return value == null ? "" : value.replace("0x", "").replace("0X", "").replaceAll("[\\s,_:-]+", "").toUpperCase(Locale.US);
    }

    private String groupHexForDisplay(String hex) {
        String compact = cleanHex(hex);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < compact.length(); i += 4) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(compact, i, Math.min(i + 4, compact.length()));
        }
        return builder.toString();
    }

    private String groupBinaryForInput(String binary) {
        String compact = cleanBinary(binary);
        if (compact.length() <= 16) {
            return groupBinary16ForInput(compact);
        }
        if (compact.length() == 32) {
            return compact.substring(0, 8) + " " + compact.substring(8, 16) + "\n"
                    + compact.substring(16, 24) + " " + compact.substring(24, 32);
        }
        if (compact.length() == 64) {
            return compact.substring(0, 8) + " " + compact.substring(8, 16) + "\n"
                    + compact.substring(16, 24) + " " + compact.substring(24, 32) + "\n"
                    + compact.substring(32, 40) + " " + compact.substring(40, 48) + "\n"
                    + compact.substring(48, 56) + " " + compact.substring(56, 64);
        }
        return groupBinaryAny(compact);
    }

    private String groupBinary16ForInput(String binary) {
        String compact = cleanBinary(binary);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < compact.length(); i += 4) {
            if (i > 0) {
                builder.append(i == 8 ? '\n' : ' ');
            }
            builder.append(compact, i, Math.min(i + 4, compact.length()));
        }
        return builder.toString();
    }

    private String groupBinaryAny(String binary) {
        String compact = binary == null ? "" : binary.replaceAll("\\s+", "");
        if (compact.length() < 16 || compact.length() % 8 != 0) {
            return binary;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < compact.length(); i += 8) {
            if (i > 0) {
                builder.append(i % 16 == 0 ? '\n' : ' ');
            }
            builder.append(compact, i, i + 8);
        }
        return builder.toString();
    }

    private String binaryFromHex(String hex) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            int value = Integer.parseInt(hex.substring(i, i + 1), 16);
            builder.append(String.format(Locale.US, "%4s", Integer.toBinaryString(value)).replace(' ', '0'));
        }
        return builder.toString();
    }

    private String binaryFromHexSafe(String hex) {
        String compact = cleanHex(hex);
        if (!compact.matches("[0-9A-F]+") || compact.length() % 4 != 0 || !isAllowedRawBits(compact.length() * 4)) {
            return "";
        }
        return binaryFromHex(compact);
    }

    private String hexFromBinary(String binary) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 4) {
            int value = Integer.parseInt(binary.substring(i, i + 4), 2);
            builder.append(Integer.toHexString(value).toUpperCase(Locale.US));
        }
        return builder.toString();
    }

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private enum ResultFormatMode {
        HEX("Hex"),
        REGISTER("Register"),
        BINARY("Binary"),
        ALL("全部");

        private final String label;

        ResultFormatMode(String label) {
            this.label = label;
        }
    }
}
