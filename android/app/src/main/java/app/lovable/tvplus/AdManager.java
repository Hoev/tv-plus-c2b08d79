package app.lovable.tvplus;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * AdManager - Handles AdMob ad loading and display.
 * Configuration is fetched from Firebase Realtime Database (adConfig node).
 * Supports: Banner, Interstitial, and Rewarded Video ads.
 */
public class AdManager {
    private static final String TAG = "AdManager";
    
    private static AdManager instance;
    private boolean initialized = false;
    
    // Ad Unit IDs from Firebase
    private String bannerId = "";
    private String interstitialId = "";
    private String rewardedId = "";
    private boolean adsEnabled = false;
    
    // Loaded ads
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    
    public interface AdCallback {
        void onAdCompleted();
        void onAdFailed();
    }
    
    private AdManager() {}
    
    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }
    
    /**
     * Initialize AdMob SDK and listen for config changes from Firebase
     */
    public void init(Activity activity) {
        if (initialized) return;
        initialized = true;
        
        MobileAds.initialize(activity, initializationStatus -> {
            Log.d(TAG, "AdMob SDK initialized");
        });
        
        // Listen for ad config from Firebase
        DatabaseReference adConfigRef = FirebaseDatabase.getInstance().getReference("adConfig");
        adConfigRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    adsEnabled = Boolean.TRUE.equals(snapshot.child("adsEnabled").getValue(Boolean.class));
                    bannerId = getStringValue(snapshot, "admobBannerId");
                    interstitialId = getStringValue(snapshot, "admobInterstitialId");
                    rewardedId = getStringValue(snapshot, "admobRewardedId");
                    
                    Log.d(TAG, "Ad config updated - enabled: " + adsEnabled);
                    
                    if (adsEnabled) {
                        preloadInterstitial(activity);
                        preloadRewarded(activity);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load ad config: " + error.getMessage());
            }
        });
    }
    
    private String getStringValue(DataSnapshot snapshot, String key) {
        Object val = snapshot.child(key).getValue();
        return val != null ? val.toString() : "";
    }
    
    public boolean isAdsEnabled() {
        return adsEnabled;
    }
    
    /**
     * Check if a category has ad gate enabled (requires watching rewarded ad)
     */
    public void checkAdGate(String categoryId, Activity activity, AdCallback callback) {
        if (!adsEnabled || rewardedId.isEmpty()) {
            callback.onAdCompleted();
            return;
        }
        
        DatabaseReference catRef = FirebaseDatabase.getInstance()
                .getReference("categories").child(categoryId).child("adGateEnabled");
        
        catRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean gateEnabled = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(gateEnabled)) {
                    showRewardedAd(activity, callback);
                } else {
                    callback.onAdCompleted();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onAdCompleted();
            }
        });
    }
    
    // === Interstitial Ads ===
    
    private void preloadInterstitial(Activity activity) {
        if (interstitialId.isEmpty()) return;
        
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, interstitialId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                Log.d(TAG, "Interstitial ad loaded");
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                interstitialAd = null;
                Log.e(TAG, "Interstitial failed to load: " + error.getMessage());
            }
        });
    }
    
    public void showInterstitial(Activity activity, AdCallback callback) {
        if (!adsEnabled || interstitialAd == null) {
            if (callback != null) callback.onAdCompleted();
            return;
        }
        
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                preloadInterstitial(activity);
                if (callback != null) callback.onAdCompleted();
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                interstitialAd = null;
                preloadInterstitial(activity);
                if (callback != null) callback.onAdFailed();
            }
        });
        
        interstitialAd.show(activity);
    }
    
    // === Rewarded Video Ads ===
    
    private void preloadRewarded(Activity activity) {
        if (rewardedId.isEmpty()) return;
        
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(activity, rewardedId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                Log.d(TAG, "Rewarded ad loaded");
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                rewardedAd = null;
                Log.e(TAG, "Rewarded ad failed to load: " + error.getMessage());
            }
        });
    }
    
    public void showRewardedAd(Activity activity, AdCallback callback) {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded ad not ready, allowing access");
            if (callback != null) callback.onAdCompleted();
            preloadRewarded(activity);
            return;
        }
        
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                preloadRewarded(activity);
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                rewardedAd = null;
                preloadRewarded(activity);
                if (callback != null) callback.onAdFailed();
            }
        });
        
        rewardedAd.show(activity, rewardItem -> {
            Log.d(TAG, "User earned reward: " + rewardItem.getAmount());
            if (callback != null) callback.onAdCompleted();
        });
    }
}
