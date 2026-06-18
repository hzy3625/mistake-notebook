package com.mistakenotebook.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int REQ_GALLERY = 1001;
    private static final int REQ_CAMERA = 1002;
    private static final int COLOR_BG = 0xFFF4F6F5;
    private static final int COLOR_CARD = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF1D2327;
    private static final int COLOR_MUTED = 0xFF6B747C;
    private static final int COLOR_PRIMARY = 0xFF1F7A5C;
    private static final int COLOR_PRIMARY_DARK = 0xFF155C45;
    private static final int COLOR_BORDER = 0xFFE3E7E5;
    private static final int COLOR_DANGER = 0xFFB0413E;

    private SecurePrefs securePrefs;
    private MistakeDatabase database;
    private ImageStore imageStore;
    private final BailianClient bailianClient = new BailianClient();
    private String currentOriginalPath;
    private String currentProcessedPath;
    private BailianAnalysisResult currentAnalysis = BailianAnalysisResult.manual();
    private String analysisProgress = "";
    private boolean analysisInProgress = false;
    private Subject selectedSubject = Subject.OTHER;
    private Subject libraryFilterSubject = null;
    private Uri pendingCameraUri;
    private final Set<Long> selectedForExport = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        securePrefs = new SecurePrefs(this);
        database = new MistakeDatabase(this);
        imageStore = new ImageStore(this);
        showHome();
    }

    private void showHome() {
        LinearLayout root = base("错题本");
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_app_logo);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        logoParams.setMargins(0, 0, dp(12), 0);
        logo.setLayoutParams(logoParams);
        brand.addView(logo);
        TextView brandText = subtitle("拍照提取干净题目，按科目整理并导出打印");
        brandText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        brand.addView(brandText);
        root.addView(brand);

        LinearLayout status = card();
        status.addView(sectionTitle("百炼配置"));
        status.addView(body("API Key: " + securePrefs.apiKeyPreview()));
        status.addView(secondaryButton("系统设置", v -> showSettings()));
        root.addView(status);

        LinearLayout actions = card();
        actions.addView(sectionTitle("录入错题"));
        actions.addView(primaryButton("拍照记录错题", v -> capturePhoto()));
        actions.addView(secondaryButton("从相册导入", v -> pickImage()));
        actions.addView(secondaryButton("错题库 / 导出 PDF", v -> showLibrary()));
        root.addView(actions);

        root.addView(note("V0.1 以题目文本提取为主；提取失败时仍可保存原图，并按原图排版导出 PDF。"));
        setRoot(root);
    }

    private void showSettings() {
        LinearLayout root = base("系统设置");
        root.addView(subtitle("配置百炼模型调用信息"));
        EditText apiKey = input("百炼 API Key");
        EditText model = input("模型名称，默认 " + SecurePrefs.DEFAULT_MODEL_NAME);
        model.setText(securePrefs.loadModel());

        LinearLayout form = card();
        form.addView(body("当前 Key: " + securePrefs.apiKeyPreview()));
        form.addView(apiKey);
        form.addView(model);
        form.addView(primaryButton("保存设置", v -> {
            try {
                securePrefs.saveApiKey(apiKey.getText().toString());
                securePrefs.saveModel(model.getText().toString());
                toast("已保存");
                showHome();
            } catch (Exception e) {
                toast("保存失败: " + e.getMessage());
            }
        }));
        form.addView(secondaryButton("测试连接", v -> testConnection(apiKey.getText().toString(), model.getText().toString())));
        form.addView(dangerButton("清空 API Key", v -> {
            try {
                securePrefs.saveApiKey("");
                toast("已清空");
                showHome();
            } catch (Exception e) {
                toast("清空失败");
            }
        }));
        root.addView(form);
        root.addView(secondaryButton("返回", v -> showHome()));
        setRoot(root);
    }

    private void testConnection(String apiKeyText, String modelName) {
        toast("正在测试百炼连接...");
        new Thread(() -> {
            try {
                if (apiKeyText != null && !apiKeyText.trim().isEmpty()) {
                    securePrefs.saveApiKey(apiKeyText.trim());
                }
                securePrefs.saveModel(modelName);
                String key = securePrefs.loadApiKey();
                String result = bailianClient.testConnection(key, securePrefs.loadModel());
                runOnUiThread(() -> toast(result + "，API Key 已保存"));
            } catch (Exception e) {
                runOnUiThread(() -> toast(e.getMessage() == null ? "测试连接失败" : e.getMessage()));
            }
        }).start();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择错题图片"), REQ_GALLERY);
    }

    private void capturePhoto() {
        try {
            File dir = new File(getFilesDir(), "capture");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "capture-" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = SimpleFileProvider.uriFor(file, getFilesDir());
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (Exception e) {
            toast("无法启动相机: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        try {
            Uri uri = requestCode == REQ_CAMERA ? pendingCameraUri : data.getData();
            currentOriginalPath = imageStore.saveFromUri(uri);
            currentProcessedPath = null;
            currentAnalysis = BailianAnalysisResult.manual();
            analysisProgress = "";
            analysisInProgress = false;
            selectedSubject = Subject.OTHER;
            showConfirm(false);
        } catch (Exception e) {
            toast("图片读取失败: " + e.getMessage());
        }
    }

    private void showConfirm(boolean analyzed) {
        LinearLayout root = base("确认错题");
        root.addView(subtitle("提取图片中的干净题目文本，用于后续排版打印"));
        Bitmap original = imageStore.load(currentOriginalPath);
        if (original != null) root.addView(previewCard("原图", original));
        if (analysisProgress.length() > 0 || analysisInProgress) {
            root.addView(progressCard());
        }
        root.addView(note(analysisInProgress ? "正在提取题目，请等待结果。" : (analyzed ? analysisText() : "尚未提取。可以先点击“提取题目”，也可以手动选择科目保存原图。")));

        Spinner subjectSpinner = new Spinner(this);
        subjectSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Subject.labels()));
        subjectSpinner.setSelection(selectedSubject.ordinal());
        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSubject = Subject.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        LinearLayout actions = card();
        actions.addView(sectionTitle("科目与操作"));
        actions.addView(subjectSpinner);
        actions.addView(primaryButton(analysisInProgress ? "提取中..." : "提取题目", v -> {
            if (!analysisInProgress) analyzeCurrent();
        }));
        if (analyzed) {
            actions.addView(primaryButton("保存题目文本", v -> saveMistake(false)));
        } else {
            actions.addView(secondaryButton("保存原图", v -> saveMistake(false)));
        }
        root.addView(actions);
        if (currentAnalysis.cleanQuestionText.length() > 0) {
            root.addView(textCard("干净题目文本", currentAnalysis.cleanQuestionText));
        }
        root.addView(secondaryButton("返回首页", v -> showHome()));
        setRoot(root);
    }

    private void analyzeCurrent() {
        Bitmap source = imageStore.load(currentOriginalPath);
        if (source == null) {
            toast("图片不存在");
            return;
        }
        toast("正在提取题目...");
        analysisInProgress = true;
        analysisProgress = "";
        currentAnalysis = BailianAnalysisResult.manual();
        appendAnalysisStep("准备图片：压缩到适合模型识别的尺寸");
        new Thread(() -> {
            try {
                Bitmap analysisImage = imageStore.limitSize(source, 1600);
                appendAnalysisStep("调用模型：" + SecurePrefs.DEFAULT_MODEL_NAME + " (" + SecurePrefs.normalizeModel(securePrefs.loadModel()) + ")");
                BailianAnalysisResult result = bailianClient.analyze(
                        securePrefs.loadApiKey(),
                        securePrefs.loadModel(),
                        analysisImage
                );
                appendAnalysisStep("接收响应：提取干净题目文本");
                appendAnalysisStep(result.cleanQuestionText.length() > 0 ? "文本提取：已生成干净题目文本" : "文本提取：模型未返回可用题目文本");
                appendAnalysisStep("完成：请检查文本并保存");
                runOnUiThread(() -> {
                    currentAnalysis = result;
                    analysisInProgress = false;
                    if (result.parsed) {
                        selectedSubject = result.subjectConfidence >= 0.75 ? result.subject : selectedSubject;
                    }
                    currentProcessedPath = null;
                    showConfirm(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    toast("提取失败: " + e.getMessage());
                    currentAnalysis = BailianAnalysisResult.manual();
                    analysisInProgress = false;
                    appendAnalysisStep("失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage()));
                    showConfirm(false);
                });
            }
        }).start();
    }

    private void appendAnalysisStep(String step) {
        analysisProgress = analysisProgress + (analysisProgress.length() == 0 ? "" : "\n") + "• " + step;
        runOnUiThread(() -> showConfirm(true));
    }

    private void saveMistake(boolean useProcessed) {
        Mistake mistake = new Mistake();
        mistake.subject = selectedSubject;
        mistake.originalImagePath = currentOriginalPath;
        mistake.processedImagePath = currentProcessedPath;
        mistake.useProcessedImage = useProcessed && currentProcessedPath != null;
        mistake.analysisJson = currentAnalysis.rawJson;
        mistake.cleanQuestionText = currentAnalysis.cleanQuestionText;
        mistake.createdAt = System.currentTimeMillis();
        mistake.printed = false;
        database.insert(mistake);
        toast("已保存");
        showLibrary();
    }

    private void showLibrary() {
        selectedForExport.clear();
        renderLibrary(database.listAll());
    }

    private void renderLibrary(List<Mistake> mistakes) {
        LinearLayout root = base("错题库");
        List<Mistake> visibleMistakes = filterMistakes(mistakes);
        root.addView(subtitle("共 " + mistakes.size() + " 道错题，当前显示 " + visibleMistakes.size() + " 道，已选择 " + selectedForExport.size() + " 道"));

        LinearLayout filterPanel = card();
        filterPanel.addView(sectionTitle("筛选"));
        Spinner filterSpinner = new Spinner(this);
        String[] filterLabels = subjectFilterLabels();
        filterSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterLabels));
        filterSpinner.setSelection(libraryFilterSubject == null ? 0 : libraryFilterSubject.ordinal() + 1);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Subject next = position == 0 ? null : Subject.values()[position - 1];
                if (next != libraryFilterSubject) {
                    libraryFilterSubject = next;
                    renderLibrary(database.listAll());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        filterPanel.addView(filterSpinner);
        root.addView(filterPanel);

        LinearLayout exportPanel = card();
        exportPanel.addView(sectionTitle("导出"));
        exportPanel.addView(body("先勾选错题，再选择 A4 排版方式。"));
        exportPanel.addView(primaryButton("导出 A4 PDF - 每页 2 题", v -> exportPdf(2)));
        exportPanel.addView(secondaryButton("导出 A4 PDF - 每页 4 题", v -> exportPdf(4)));
        root.addView(exportPanel);

        for (Mistake mistake : visibleMistakes) {
            root.addView(mistakeCard(mistake, mistakes));
        }
        root.addView(secondaryButton("返回首页", v -> showHome()));
        setRoot(root);
    }

    private List<Mistake> filterMistakes(List<Mistake> mistakes) {
        if (libraryFilterSubject == null) return mistakes;
        ArrayList<Mistake> result = new ArrayList<>();
        for (Mistake mistake : mistakes) {
            if (mistake.subject == libraryFilterSubject) result.add(mistake);
        }
        return result;
    }

    private String[] subjectFilterLabels() {
        String[] labels = new String[Subject.values().length + 1];
        labels[0] = "全部科目";
        String[] subjectLabels = Subject.labels();
        for (int i = 0; i < subjectLabels.length; i++) labels[i + 1] = subjectLabels[i];
        return labels;
    }

    private View mistakeCard(Mistake mistake, List<Mistake> sourceList) {
        boolean selected = selectedForExport.contains(mistake.id);
        LinearLayout row = card(selected);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOnClickListener(v -> showMistakeDetail(mistake));

        Bitmap preview = imageStore.load(mistake.exportImagePath());
        if (preview != null) {
            row.addView(thumbnail(imageStore.limitSize(preview, 320), 92, 68));
        }

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, dp(6), 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        String date = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(mistake.createdAt));
        info.addView(sectionTitle("#" + mistake.id + " " + mistake.subject.label));
        TextView meta = body(date + " · " + (hasCleanText(mistake) ? "文本" : "图片") + (mistake.printed ? " · 已打印" : ""));
        meta.setSingleLine(true);
        info.addView(meta);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(selected ? "已选择" : "点卡片看详情", selected));
        if (hasCleanText(mistake)) chips.addView(chip("纯文本", false));
        info.addView(chips);
        row.addView(info);

        Button select = compactButton(selected ? "取消" : "选择", v -> {
            if (selectedForExport.contains(mistake.id)) selectedForExport.remove(mistake.id);
            else selectedForExport.add(mistake.id);
            renderLibrary(sourceList);
        });
        row.addView(select);
        return row;
    }

    private void showMistakeDetail(Mistake mistake) {
        LinearLayout root = base("错题详情");
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(mistake.createdAt));
        root.addView(subtitle("#" + mistake.id + " · " + mistake.subject.label + " · " + date));
        Bitmap image = imageStore.load(mistake.exportImagePath());
        if (image != null) root.addView(previewCard("原图", image));
        if (hasCleanText(mistake)) root.addView(textCard("干净题目文本", mistake.cleanQuestionText));
        root.addView(note(mistake.analysisJson == null || "{}".equals(mistake.analysisJson) ? "未保存题目提取结果。" : "已保存题目提取结果。"));
        root.addView(primaryButton(selectedForExport.contains(mistake.id) ? "取消选择导出" : "选择导出", v -> {
            if (selectedForExport.contains(mistake.id)) selectedForExport.remove(mistake.id);
            else selectedForExport.add(mistake.id);
            showMistakeDetail(mistake);
        }));
        root.addView(dangerButton("删除错题", v -> confirmDelete(mistake.id)));
        root.addView(secondaryButton("返回错题库", v -> renderLibrary(database.listAll())));
        setRoot(root);
    }

    private void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setTitle("删除错题")
                .setMessage("确定删除这道错题及本地图片吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    database.delete(id);
                    showLibrary();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void exportPdf(int perPage) {
        List<Mistake> selected = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (Mistake mistake : database.listAll()) {
            if (selectedForExport.contains(mistake.id)) {
                selected.add(mistake);
                ids.add(mistake.id);
            }
        }
        try {
            File pdf = new A4PdfExporter(this, imageStore).export(selected, perPage);
            database.markPrinted(ids);
            sharePdf(pdf);
        } catch (Exception e) {
            toast("导出失败: " + e.getMessage());
        }
    }

    private void sharePdf(File pdf) {
        Uri uri = SimpleFileProvider.uriFor(pdf, getFilesDir());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TITLE, pdf.getName());
        intent.putExtra(Intent.EXTRA_SUBJECT, pdf.getName());
        intent.setClipData(ClipData.newUri(getContentResolver(), pdf.getName(), uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "分享或打印 PDF"));
    }

    private String analysisText() {
        if (!currentAnalysis.parsed) {
            return "题目提取异常\n"
                    + "未能提取可用题目文本。\n"
                    + "建议: " + currentAnalysis.suggestion;
        }
        return "科目: " + currentAnalysis.subject.label
                + "\n置信度: " + currentAnalysis.subjectConfidence
                + "\n干净文本: " + (currentAnalysis.cleanQuestionText.length() > 0 ? "已提取" : "未提取")
                + "\n建议: " + currentAnalysis.suggestion;
    }

    private boolean hasCleanText(Mistake mistake) {
        return mistake.cleanQuestionText != null && mistake.cleanQuestionText.trim().length() > 0;
    }

    private View progressCard() {
        LinearLayout panel = card();
        panel.addView(sectionTitle(analysisInProgress ? "提取过程" : "提取过程与结果"));
        panel.addView(body("模型: " + SecurePrefs.DEFAULT_MODEL_NAME + " (" + SecurePrefs.normalizeModel(securePrefs.loadModel()) + ")"));
        TextView steps = body(analysisProgress.length() == 0 ? "等待开始" : analysisProgress);
        steps.setTextColor(COLOR_TEXT);
        panel.addView(steps);
        return panel;
    }

    private LinearLayout base(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(26), dp(18), dp(26));
        root.setBackgroundColor(COLOR_BG);
        TextView heading = label(title);
        heading.setTextSize(26);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(heading);
        return root;
    }

    private void setRoot(LinearLayout root) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BG);
        scrollView.addView(root);
        setContentView(scrollView);
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setPadding(0, dp(6), 0, dp(6));
        view.setTextColor(COLOR_TEXT);
        return view;
    }

    private Button button(String text, View.OnClickListener listener) {
        return styledButton(text, COLOR_PRIMARY, Color.WHITE, listener);
    }

    private Button primaryButton(String text, View.OnClickListener listener) {
        return styledButton(text, COLOR_PRIMARY, Color.WHITE, listener);
    }

    private Button secondaryButton(String text, View.OnClickListener listener) {
        return styledButton(text, 0xFFE8EFEC, COLOR_PRIMARY_DARK, listener);
    }

    private Button dangerButton(String text, View.OnClickListener listener) {
        return styledButton(text, 0xFFF4E7E6, COLOR_DANGER, listener);
    }

    private Button compactButton(String text, View.OnClickListener listener) {
        Button button = styledButton(text, selectedForExportLabel(text) ? COLOR_PRIMARY : 0xFFE8EFEC, selectedForExportLabel(text) ? Color.WHITE : COLOR_PRIMARY_DARK, listener);
        button.setTextSize(14);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(62), dp(42)));
        return button;
    }

    private boolean selectedForExportLabel(String text) {
        return "选择".equals(text);
    }

    private Button styledButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(roundRect(bgColor, 12, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        params.setMargins(0, dp(8), 0, 0);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setTextSize(16);
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackground(roundRect(0xFFF7F9F8, 10, COLOR_BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        params.setMargins(0, dp(8), 0, 0);
        editText.setLayoutParams(params);
        return editText;
    }

    private ImageView image(Bitmap bitmap) {
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(900);
        image.setPadding(0, 8, 0, 8);
        return image;
    }

    private View previewCard(String title, Bitmap bitmap) {
        LinearLayout panel = card();
        panel.addView(sectionTitle(title));
        ImageView image = image(bitmap);
        image.setMaxHeight(dp(360));
        panel.addView(image);
        return panel;
    }

    private View textCard(String title, String text) {
        LinearLayout panel = card();
        panel.addView(sectionTitle(title));
        TextView content = body(text == null || text.trim().isEmpty() ? "暂无文本" : text.trim());
        content.setTextColor(COLOR_TEXT);
        content.setTextSize(16);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));
        content.setBackground(roundRect(0xFFF7F9F8, 10, COLOR_BORDER));
        panel.addView(content);
        return panel;
    }

    private ImageView thumbnail(Bitmap bitmap, int widthDp, int heightDp) {
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(roundRect(0xFFE9EEEC, 8, COLOR_BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(widthDp), dp(heightDp));
        image.setLayoutParams(params);
        return image;
    }

    private LinearLayout card() {
        return card(false);
    }

    private LinearLayout card(boolean selected) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(roundRect(selected ? 0xFFE8F4EF : COLOR_CARD, 14, selected ? COLOR_PRIMARY : COLOR_BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private TextView subtitle(String text) {
        TextView view = label(text);
        view.setTextSize(15);
        view.setTextColor(COLOR_MUTED);
        view.setPadding(0, dp(2), 0, dp(8));
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = label(text);
        view.setTextSize(17);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, 0, 0, dp(4));
        return view;
    }

    private TextView body(String text) {
        TextView view = label(text);
        view.setTextSize(14);
        view.setTextColor(COLOR_MUTED);
        view.setPadding(0, dp(2), 0, dp(2));
        return view;
    }

    private TextView note(String text) {
        TextView view = body(text);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setBackground(roundRect(0xFFEAF2EF, 12, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(10), 0, 0);
        view.setLayoutParams(params);
        return view;
    }

    private TextView chip(String text, boolean selected) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        view.setTextColor(selected ? Color.WHITE : COLOR_MUTED);
        view.setPadding(dp(8), dp(3), dp(8), dp(3));
        view.setBackground(roundRect(selected ? COLOR_PRIMARY : 0xFFEFF2F1, 999, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), dp(6), 0);
        view.setLayoutParams(params);
        return view;
    }

    private GradientDrawable roundRect(int color, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
