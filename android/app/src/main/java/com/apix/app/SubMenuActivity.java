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

        // Build native player config
        com.apix.app.StreamConfig config = new com.apix.app.StreamConfig();
        config.title = channel.name;

        if (channel.androidStream != null && channel.androidStream.url != null) {
            config.url = channel.androidStream.url;
        } else if (channel.stream != null) {
            config.url = channel.stream.url;
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
