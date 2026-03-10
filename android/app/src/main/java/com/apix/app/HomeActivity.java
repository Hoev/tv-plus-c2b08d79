package com.apix.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Native Home Activity - matches website design exactly
 * Portrait: bottom nav with icons, 2-col channels, APiX header + search
 * Landscape/TV: right sidebar with APiX logo + categories, clock + search top center, 2-col channels
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    // Portrait views
    private LinearLayout portraitLayout;
    private RecyclerView categoriesRecyclerPortrait;
    private RecyclerView channelsRecyclerPortrait;
    private TextView categoryTitlePortrait;

    // Landscape views
    private LinearLayout landscapeLayout;
    private RecyclerView categoriesRecyclerLandscape;
    private RecyclerView channelsRecyclerLandscape;
    private TextView categoryTitleLandscape;
    private TextView clockText;

    // Search
    private LinearLayout searchOverlay;
    private EditText searchInput;
    private TextView searchCancel;
    private RecyclerView searchResultsRecycler;

    private ProgressBar loadingBar;
    private LinearLayout errorLayout;
    private TextView errorText;

    private DatabaseReference dbRef;
    private Gson gson = new Gson();

    private List<FirebaseModels.Category> categories = new ArrayList<>();
    private Map<String, FirebaseModels.SideMenu> sideMenus = new HashMap<>();
    private FirebaseModels.Category selectedCategory;

    private List<FirebaseModels.Channel> allChannels = new ArrayList<>();

    private CategoryAdapter categoryAdapterPortrait;
    private CategoryAdapter categoryAdapterLandscape;
    private ChannelAdapter channelAdapterPortrait;
    private ChannelAdapter channelAdapterLandscape;
    private ChannelAdapter searchAdapter;

    private boolean isLandscape = false;

    // Clock updater
    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // NO fullscreen in home - let system bars show normally
        // Fullscreen is only for PlayerActivity

        initViews();
        initFirebase();
        loadData();
        startClock();
    }

    private void startClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (clockText != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    clockText.setText(sdf.format(new Date()));
                }
                clockHandler.postDelayed(this, 30000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void initViews() {
        loadingBar = findViewById(R.id.loading_bar);
        errorLayout = findViewById(R.id.error_layout);
        errorText = findViewById(R.id.error_text);

        // Portrait
        portraitLayout = findViewById(R.id.portrait_layout);
        categoriesRecyclerPortrait = findViewById(R.id.categories_recycler);
        channelsRecyclerPortrait = findViewById(R.id.channels_recycler);
        categoryTitlePortrait = findViewById(R.id.category_title);

        // Landscape
        landscapeLayout = findViewById(R.id.landscape_layout);
        categoriesRecyclerLandscape = findViewById(R.id.categories_recycler_landscape);
        channelsRecyclerLandscape = findViewById(R.id.channels_recycler_landscape);
        categoryTitleLandscape = findViewById(R.id.category_title_landscape);
        clockText = findViewById(R.id.clock_text);

        // Search
        searchOverlay = findViewById(R.id.search_overlay);
        searchInput = findViewById(R.id.search_input);
        searchCancel = findViewById(R.id.search_cancel);
        searchResultsRecycler = findViewById(R.id.search_results_recycler);

        // Search buttons
        ImageButton searchBtnPortrait = findViewById(R.id.search_button_portrait);
        ImageButton searchBtnLandscape = findViewById(R.id.search_button_landscape);

        searchBtnPortrait.setOnClickListener(v -> showSearch());
        searchBtnLandscape.setOnClickListener(v -> showSearch());
        searchCancel.setOnClickListener(v -> hideSearch());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchInput.getText().toString());
                return true;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                performSearch(s.toString());
            }
        });

        // Search results
        searchAdapter = new ChannelAdapter(this, new ArrayList<>(), this::onChannelClick);
        int searchSpan = getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE ? 2 : 2;
        searchResultsRecycler.setLayoutManager(new GridLayoutManager(this, searchSpan));
        searchResultsRecycler.setAdapter(searchAdapter);

        // Portrait: bottom nav horizontal, reversed for RTL feel (first from right)
        categoriesRecyclerPortrait.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
        categoryAdapterPortrait = new CategoryAdapter(this, categories, this::onCategorySelected);
        categoryAdapterPortrait.setSideMode(false);
        categoriesRecyclerPortrait.setAdapter(categoryAdapterPortrait);

        channelsRecyclerPortrait.setLayoutManager(new GridLayoutManager(this, 2));
        channelAdapterPortrait = new ChannelAdapter(this, new ArrayList<>(), this::onChannelClick);
        channelsRecyclerPortrait.setAdapter(channelAdapterPortrait);

        // Landscape: vertical side categories, 2 columns for bigger cards
        categoriesRecyclerLandscape.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        categoryAdapterLandscape = new CategoryAdapter(this, categories, this::onCategorySelected);
        categoryAdapterLandscape.setSideMode(true);
        categoriesRecyclerLandscape.setAdapter(categoryAdapterLandscape);

        channelsRecyclerLandscape.setLayoutManager(new GridLayoutManager(this, 2));
        channelAdapterLandscape = new ChannelAdapter(this, new ArrayList<>(), this::onChannelClick);
        channelsRecyclerLandscape.setAdapter(channelAdapterLandscape);

        applyLayout();
    }

    private void showSearch() {
        searchOverlay.setVisibility(View.VISIBLE);
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearch() {
        searchOverlay.setVisibility(View.GONE);
        searchInput.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchAdapter.updateData(new ArrayList<>());
            return;
        }

        String filter = query.toLowerCase().trim();
        List<FirebaseModels.Channel> results = new ArrayList<>();

        for (FirebaseModels.Channel ch : allChannels) {
            if (ch.name != null && ch.name.toLowerCase().contains(filter)) {
                results.add(ch);
            }
        }

        for (FirebaseModels.SideMenu menu : sideMenus.values()) {
            if (menu.channels != null) {
                for (FirebaseModels.SubChannel sc : menu.channels.values()) {
                    if (sc.name != null && sc.name.toLowerCase().contains(filter)) {
                        FirebaseModels.Channel ch = new FirebaseModels.Channel();
                        ch.id = sc.id;
                        ch.name = sc.name;
                        ch.imageUrl = sc.imageUrl;
                        ch.actionType = "direct_play";
                        ch.stream = sc.stream;
                        ch.androidStream = sc.androidStream;
                        ch.androidActionType = sc.androidActionType;
                        results.add(ch);
                    }
                }
            }
        }

        searchAdapter.updateData(results);
    }

    private void applyLayout() {
        isLandscape = getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            portraitLayout.setVisibility(View.GONE);
            landscapeLayout.setVisibility(View.VISIBLE);
        } else {
            portraitLayout.setVisibility(View.VISIBLE);
            landscapeLayout.setVisibility(View.GONE);
        }
    }

    private void onCategorySelected(FirebaseModels.Category category) {
        selectedCategory = category;

        categoryTitlePortrait.setText(category.name);
        categoryTitleLandscape.setText(category.name);

        int pos = categories.indexOf(category);
        if (pos >= 0) {
            categoryAdapterPortrait.setSelected(pos);
            categoryAdapterLandscape.setSelected(pos);
        }

        updateChannels();
    }

    private void initFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey("AIzaSyBxT35NMrvWYPJRvWek_NKeu8QtNInISC4")
                .setApplicationId("1:659730944639:web:1c00b6f7118bf85bdde54a")
                .setDatabaseUrl("https://cinema-plus-d1238-default-rtdb.firebaseio.com")
                .setProjectId("cinema-plus-d1238")
                .setStorageBucket("cinema-plus-d1238.firebasestorage.app")
                .build();
            FirebaseApp.initializeApp(this, options);
        }
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    private void loadData() {
        loadingBar.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);

        dbRef.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                allChannels.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        FirebaseModels.Category cat = child.getValue(FirebaseModels.Category.class);
                        if (cat != null) {
                            cat.id = child.getKey();
                            if (!cat.hidden) {
                                if (cat.channels == null) cat.channels = new HashMap<>();
                                DataSnapshot channelsSnap = child.child("channels");
                                for (DataSnapshot chSnap : channelsSnap.getChildren()) {
                                    FirebaseModels.Channel ch = chSnap.getValue(FirebaseModels.Channel.class);
                                    if (ch != null) {
                                        ch.id = chSnap.getKey();
                                        cat.channels.put(ch.id, ch);
                                        if (!ch.hidden) allChannels.add(ch);
                                    }
                                }
                                categories.add(cat);
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing category: " + e.getMessage());
                    }
                }

                Collections.sort(categories, (a, b) -> a.sortOrder - b.sortOrder);

                categoryAdapterPortrait.updateData(categories);
                categoryAdapterLandscape.updateData(categories);

                if (!categories.isEmpty() && selectedCategory == null) {
                    selectedCategory = categories.get(0);
                    categoryTitlePortrait.setText(selectedCategory.name);
                    categoryTitleLandscape.setText(selectedCategory.name);
                    categoryAdapterPortrait.setSelected(0);
                    categoryAdapterLandscape.setSelected(0);
                    updateChannels();
                }

                loadingBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingBar.setVisibility(View.GONE);
                errorLayout.setVisibility(View.VISIBLE);
                errorText.setText("خطأ في الاتصال: " + error.getMessage());
            }
        });

        dbRef.child("sideMenus").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sideMenus.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        FirebaseModels.SideMenu menu = child.getValue(FirebaseModels.SideMenu.class);
                        if (menu != null) {
                            menu.id = child.getKey();
                            sideMenus.put(menu.id, menu);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing side menu: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Side menus load error: " + error.getMessage());
            }
        });
    }

    private void updateChannels() {
        if (selectedCategory == null || selectedCategory.channels == null) return;

        List<FirebaseModels.Channel> channels = new ArrayList<>();
        for (FirebaseModels.Channel ch : selectedCategory.channels.values()) {
            if (!ch.hidden) channels.add(ch);
        }

        Collections.sort(channels, (a, b) -> a.sortOrder - b.sortOrder);

        channelAdapterPortrait.updateData(channels);
        channelAdapterLandscape.updateData(channels);
    }

    private void onChannelClick(FirebaseModels.Channel channel) {
        if (channel == null) return;

        if (searchOverlay.getVisibility() == View.VISIBLE) {
            hideSearch();
        }

        String actionType = channel.actionType != null ? channel.actionType : "direct_play";

        switch (actionType) {
            case "open_submenu":
                openSubMenu(channel);
                break;
            case "external_link":
                if (channel.externalUrl != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse(channel.externalUrl));
                    startActivity(browserIntent);
                }
                break;
            default:
                playChannel(channel);
                break;
        }
    }

    private void openSubMenu(FirebaseModels.Channel channel) {
        if (channel.sideMenuId == null) return;
        FirebaseModels.SideMenu menu = sideMenus.get(channel.sideMenuId);
        if (menu == null || menu.channels == null) {
            Toast.makeText(this, "لا توجد قنوات فرعية", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SubMenuActivity.class);
        intent.putExtra("menuId", channel.sideMenuId);
        intent.putExtra("menuName", channel.name);
        intent.putExtra("menuJson", gson.toJson(menu));
        startActivity(intent);
    }

    private void playChannel(FirebaseModels.Channel channel) {
        String androidAction = channel.androidActionType != null ? channel.androidActionType : "native";

        if ("intent".equals(androidAction) && channel.androidStream != null &&
            channel.androidStream.intentUri != null) {
            launchIntent(channel.androidStream.intentUri);
            return;
        }

        if ("webview".equals(androidAction)) {
            Intent intent = new Intent(this, WebViewActivity.class);
            String url = channel.androidStream != null ? channel.androidStream.url :
                (channel.stream != null ? channel.stream.url : null);
            intent.putExtra("url", url);
            intent.putExtra("title", channel.name);
            startActivity(intent);
            return;
        }

        StreamConfig config = buildStreamConfig(channel);
        if (config == null || config.url == null || config.url.isEmpty()) {
            Toast.makeText(this, "لا يوجد رابط بث", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("streamConfig", gson.toJson(config));
        startActivity(intent);
    }

    private StreamConfig buildStreamConfig(FirebaseModels.Channel channel) {
        StreamConfig config = new StreamConfig();
        config.title = channel.name;

        if (channel.androidStream != null && channel.androidStream.url != null) {
            config.url = channel.androidStream.url;
            config.actionType = channel.androidActionType;

            if (channel.androidStream.headers != null) {
                config.headers = new StreamConfig.Headers();
                config.headers.userAgent = channel.androidStream.headers.get("userAgent");
                config.headers.referer = channel.androidStream.headers.get("referrer");
                config.headers.cookie = channel.androidStream.headers.get("cookie");
                config.headers.origin = channel.androidStream.headers.get("origin");
            }

            if (channel.androidStream.drmScheme != null) {
                config.drm = new StreamConfig.DrmConfig();
                config.drm.scheme = channel.androidStream.drmScheme;
                config.drm.licenseUrl = channel.androidStream.drmLicenseUrl;

                String keyId = channel.androidStream.drmKeyId;
                String key = channel.androidStream.drmKey;
                if ("combined".equals(channel.androidStream.drmClearKeyMode) &&
                    channel.androidStream.drmClearKeyCombined != null) {
                    String[] parts = channel.androidStream.drmClearKeyCombined.split(":");
                    if (parts.length == 2) { keyId = parts[0]; key = parts[1]; }
                }
                config.drm.keyId = keyId;
                config.drm.key = key;
            }

            if (channel.androidStream.servers != null) {
                config.servers = new ArrayList<>();
                for (FirebaseModels.Server s : channel.androidStream.servers) {
                    StreamConfig.Server server = new StreamConfig.Server();
                    server.name = s.name;
                    server.url = s.url;
                    config.servers.add(server);
                }
            }
        } else if (channel.stream != null) {
            config.url = channel.stream.url;
            if (channel.stream.userAgent != null || channel.stream.referrer != null) {
                config.headers = new StreamConfig.Headers();
                config.headers.userAgent = channel.stream.userAgent;
                config.headers.referer = channel.stream.referrer;
                config.headers.cookie = channel.stream.cookies;
            }
        }

        return config;
    }

    private void launchIntent(String intentUri) {
        try {
            Intent intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                String packageName = intent.getPackage();
                if (packageName != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("market://details?id=" + packageName)));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "فشل التشغيل", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (searchOverlay.getVisibility() == View.VISIBLE) {
            hideSearch();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyLayout();

        if (!categories.isEmpty()) {
            categoryAdapterPortrait.updateData(categories);
            categoryAdapterLandscape.updateData(categories);
            updateChannels();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
}
