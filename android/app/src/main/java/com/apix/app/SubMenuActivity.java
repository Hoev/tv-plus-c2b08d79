package com.apix.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sub Menu Activity - shows sub-channels for a category with open_submenu action
 * Matches the website's ChannelPlayer sub-channel page
 */
public class SubMenuActivity extends AppCompatActivity {

    private RecyclerView channelsRecycler;
    private TextView titleText;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submenu);

        enableFullscreen();

        titleText = findViewById(R.id.submenu_title);
        channelsRecycler = findViewById(R.id.submenu_channels_recycler);
        ImageButton backButton = findViewById(R.id.submenu_back);

        String menuName = getIntent().getStringExtra("menuName");
        String menuJson = getIntent().getStringExtra("menuJson");

        titleText.setText(menuName != null ? menuName : "");
        backButton.setOnClickListener(v -> finish());

        if (menuJson == null) {
            Toast.makeText(this, "خطأ في تحميل القائمة", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseModels.SideMenu menu = gson.fromJson(menuJson, FirebaseModels.SideMenu.class);
        if (menu == null || menu.channels == null) {
            Toast.makeText(this, "لا توجد قنوات فرعية", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Convert sub-channels to channel format for adapter reuse
        List<FirebaseModels.Channel> channels = new ArrayList<>();
        for (FirebaseModels.SubChannel sc : menu.channels.values()) {
            if (!sc.hidden) {
                FirebaseModels.Channel ch = new FirebaseModels.Channel();
                ch.id = sc.id;
                ch.name = sc.name;
                ch.imageUrl = sc.imageUrl;
                ch.sortOrder = sc.sortOrder;
                ch.actionType = "direct_play";
                ch.stream = sc.stream;
                ch.androidStream = sc.androidStream;
                ch.androidActionType = sc.androidActionType;
                ch.preferredPlayer = sc.preferredPlayer;
                channels.add(ch);
            }
        }

        Collections.sort(channels, (a, b) -> a.sortOrder - b.sortOrder);

        int spanCount = getResources().getConfiguration().orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        channelsRecycler.setLayoutManager(new GridLayoutManager(this, spanCount));

        ChannelAdapter adapter = new ChannelAdapter(this, channels, this::playSubChannel);
        channelsRecycler.setAdapter(adapter);
    }

    private void enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
            getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void playSubChannel(FirebaseModels.Channel channel) {
        String androidAction = channel.androidActionType != null ? channel.androidActionType : "native";

        if ("intent".equals(androidAction) && channel.androidStream != null &&
            channel.androidStream.intentUri != null) {
            try {
                Intent intent = Intent.parseUri(channel.androidStream.intentUri, Intent.URI_INTENT_SCHEME);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            } catch (Exception e) {
                Toast.makeText(this, "فشل التشغيل", Toast.LENGTH_SHORT).show();
            }
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

        // Build native player config with full DRM support
        com.apix.app.StreamConfig config = new com.apix.app.StreamConfig();
        config.title = channel.name;

        if (channel.androidStream != null && channel.androidStream.url != null) {
            config.url = channel.androidStream.url;
            config.actionType = channel.androidActionType;

            if (channel.androidStream.headers != null) {
                config.headers = new com.apix.app.StreamConfig.Headers();
                config.headers.userAgent = channel.androidStream.headers.get("userAgent");
                config.headers.referer = channel.androidStream.headers.get("referrer");
                config.headers.cookie = channel.androidStream.headers.get("cookie");
                config.headers.origin = channel.androidStream.headers.get("origin");
            }

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

            if (channel.androidStream.servers != null) {
                config.servers = new java.util.ArrayList<>();
                for (FirebaseModels.Server s : channel.androidStream.servers) {
                    com.apix.app.StreamConfig.Server server = new com.apix.app.StreamConfig.Server();
                    server.name = s.name;
                    server.url = s.url;
                    config.servers.add(server);
                }
            }
        } else if (channel.stream != null) {
            config.url = channel.stream.url;
            if (channel.stream.userAgent != null || channel.stream.referrer != null) {
                config.headers = new com.apix.app.StreamConfig.Headers();
                config.headers.userAgent = channel.stream.userAgent;
                config.headers.referer = channel.stream.referrer;
                config.headers.cookie = channel.stream.cookies;
            }
        }

        if (config.url == null || config.url.isEmpty()) {
            Toast.makeText(this, "لا يوجد رابط بث", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("streamConfig", gson.toJson(config));
        startActivity(intent);
    }
}
