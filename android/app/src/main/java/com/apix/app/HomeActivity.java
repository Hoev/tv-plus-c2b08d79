package com.apix.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native Home Activity - replaces WebView with native Android UI
 * Fetches data from Firebase and displays categories + channels
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private RecyclerView categoriesRecycler;
    private RecyclerView channelsRecycler;
    private TextView categoryTitle;
    private ProgressBar loadingBar;
    private LinearLayout errorLayout;
    private TextView errorText;

    private DatabaseReference dbRef;
    private Gson gson = new Gson();

    private List<FirebaseModels.Category> categories = new ArrayList<>();
    private Map<String, FirebaseModels.SideMenu> sideMenus = new HashMap<>();
    private FirebaseModels.Category selectedCategory;

    private CategoryAdapter categoryAdapter;
    private ChannelAdapter channelAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        enableFullscreen();
        initViews();
        initFirebase();
        loadData();
    }

    private void enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
            getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void initViews() {
        categoriesRecycler = findViewById(R.id.categories_recycler);
        channelsRecycler = findViewById(R.id.channels_recycler);
        categoryTitle = findViewById(R.id.category_title);
        loadingBar = findViewById(R.id.loading_bar);
        errorLayout = findViewById(R.id.error_layout);
        errorText = findViewById(R.id.error_text);

        // Categories - horizontal list
        categoriesRecycler.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(this, categories, category -> {
            selectedCategory = category;
            categoryTitle.setText(category.name);
            updateChannels();
        });
        categoriesRecycler.setAdapter(categoryAdapter);

        // Channels - grid
        int spanCount = getResources().getConfiguration().orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        channelsRecycler.setLayoutManager(new GridLayoutManager(this, spanCount));
        channelAdapter = new ChannelAdapter(this, new ArrayList<>(), this::onChannelClick);
        channelsRecycler.setAdapter(channelAdapter);
    }

    private void initFirebase() {
        // Initialize Firebase programmatically (no google-services.json needed)
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

        // Load categories
        dbRef.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        FirebaseModels.Category cat = child.getValue(FirebaseModels.Category.class);
                        if (cat != null) {
                            cat.id = child.getKey();
                            if (!cat.hidden) {
                                // Parse channels
                                if (cat.channels == null) cat.channels = new HashMap<>();
                                DataSnapshot channelsSnap = child.child("channels");
                                for (DataSnapshot chSnap : channelsSnap.getChildren()) {
                                    FirebaseModels.Channel ch = chSnap.getValue(FirebaseModels.Channel.class);
                                    if (ch != null) {
                                        ch.id = chSnap.getKey();
                                        cat.channels.put(ch.id, ch);
                                    }
                                }
                                categories.add(cat);
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing category: " + e.getMessage());
                    }
                }

                // Sort
                Collections.sort(categories, (a, b) -> a.sortOrder - b.sortOrder);
                categoryAdapter.updateData(categories);

                // Select first category
                if (!categories.isEmpty() && selectedCategory == null) {
                    selectedCategory = categories.get(0);
                    categoryTitle.setText(selectedCategory.name);
                    categoryAdapter.setSelected(0);
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

        // Load side menus
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
        channelAdapter.updateData(channels);
    }

    private void onChannelClick(FirebaseModels.Channel channel) {
        if (channel == null) return;

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
            default: // direct_play
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
        // Build StreamConfig for PlayerActivity
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

        // Native player
        com.apix.app.StreamConfig config = buildStreamConfig(channel);
        if (config == null || config.url == null || config.url.isEmpty()) {
            Toast.makeText(this, "لا يوجد رابط بث", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("streamConfig", gson.toJson(config));
        startActivity(intent);
    }

    private com.apix.app.StreamConfig buildStreamConfig(FirebaseModels.Channel channel) {
        com.apix.app.StreamConfig config = new com.apix.app.StreamConfig();
        config.title = channel.name;

        if (channel.androidStream != null && channel.androidStream.url != null) {
            config.url = channel.androidStream.url;
            config.actionType = channel.androidActionType;

            // Headers
            if (channel.androidStream.headers != null) {
                config.headers = new com.apix.app.StreamConfig.Headers();
                config.headers.userAgent = channel.androidStream.headers.get("userAgent");
                config.headers.referer = channel.androidStream.headers.get("referrer");
                config.headers.cookie = channel.androidStream.headers.get("cookie");
                config.headers.origin = channel.androidStream.headers.get("origin");
            }

            // DRM
            if (channel.androidStream.drmScheme != null) {
                config.drm = new com.apix.app.StreamConfig.DrmConfig();
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

            // Servers
            if (channel.androidStream.servers != null) {
                config.servers = new ArrayList<>();
                for (FirebaseModels.Server s : channel.androidStream.servers) {
                    com.apix.app.StreamConfig.Server server = new com.apix.app.StreamConfig.Server();
                    server.name = s.name;
                    server.url = s.url;
                    config.servers.add(server);
                }
            }
        } else if (channel.stream != null) {
            // Fallback to web stream config
            config.url = channel.stream.url;
            if (channel.stream.userAgent != null || channel.stream.referrer != null) {
                config.headers = new com.apix.app.StreamConfig.Headers();
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
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int spanCount = newConfig.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        ((GridLayoutManager) channelsRecycler.getLayoutManager()).setSpanCount(spanCount);
    }
}
