package app.lovable.tvplus;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

/**
 * Security Monitor - Anti-Tamper & Network Security System
 * 
 * Features:
 * - Continuous background monitoring (every 5-19ms)
 * - Sniffer/Proxy detection (HttpCanary, Charles, etc.)
 * - Emulator/VM detection (VMOS, BlueStacks, etc.)
 * - VPN detection with IP whitelist (172.19.0.x)
 * - Hosts file modification detection
 * - Signature verification (anti-repack)
 * - Private DNS ad-blocker detection
 */
public class SecurityMonitor {
    
    private static final String TAG = "SecurityMonitor";
    private static SecurityMonitor instance;
    private Context context;
    private Thread monitorThread;
    private volatile boolean running = false;
    
    // Known sniffer/proxy packages
    private static final String[] SNIFFER_PACKAGES = {
        "com.guoshi.httpcanary",
        "com.guoshi.httpcanary.premium",
        "com.minhui.networkcapture",
        "jp.co.because.network.analysis",
        "com.charles.proxy",
        "com.egorovandreyrm.pcapremote",
        "app.greyshirts.sslcapture",
        "com.reqbin.httpbin",
        "io.anyline.xposed",
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "me.weishu.exp",
        "com.saurik.substrate",
        "com.zachspong.temprootremovejb",
    };
    
    // Whitelisted VPN IP range (your custom VPN app)
    private static final String VPN_WHITELIST_PREFIX = "172.19.0.";
    
    // Expected signature hash - will be set on first run
    private String expectedSignatureHash = null;

    private SecurityMonitor(Context ctx) {
        this.context = ctx.getApplicationContext();
        // Store current signature as expected
        this.expectedSignatureHash = getCurrentSignatureHash();
    }
    
    public static synchronized SecurityMonitor getInstance(Context ctx) {
        if (instance == null) {
            instance = new SecurityMonitor(ctx);
        }
        return instance;
    }
    
    /**
     * Start the continuous security monitor
     * Runs checks every 5-19ms in a background thread
     */
    public void startMonitor() {
        if (running) return;
        running = true;
        
        monitorThread = new Thread(() -> {
            Log.d(TAG, "Security monitor started");
            
            while (running) {
                try {
                    // Run all security checks
                    if (detectSniffers()) {
                        killApp("Sniffer detected");
                        return;
                    }
                    
                    if (detectProxy()) {
                        killApp("Proxy detected");
                        return;
                    }
                    
                    if (detectEmulator()) {
                        killApp("Emulator detected");
                        return;
                    }
                    
                    if (detectHostsModification()) {
                        killApp("Hosts file modified");
                        return;
                    }
                    
                    if (detectUnauthorizedVPN()) {
                        killApp("Unauthorized VPN");
                        return;
                    }
                    
                    if (detectSignatureTampering()) {
                        killApp("Signature mismatch");
                        return;
                    }
                    
                    if (detectPrivateDNS()) {
                        killApp("Ad-blocking DNS detected");
                        return;
                    }
                    
                    // Random sleep interval 5-19ms to avoid pattern detection
                    Thread.sleep(5 + (long)(Math.random() * 14));
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Log.w(TAG, "Monitor check error: " + e.getMessage());
                }
            }
        }, "sec-monitor");
        
        monitorThread.setDaemon(true);
        monitorThread.setPriority(Thread.MAX_PRIORITY);
        monitorThread.start();
    }
    
    /**
     * Stop the monitor
     */
    public void stopMonitor() {
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
    }
    
    /**
     * Kill the app immediately - prevents any data capture
     */
    private void killApp(String reason) {
        Log.e(TAG, "SECURITY VIOLATION: " + reason);
        running = false;
        
        try {
            // Kill all processes
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo proc : processes) {
                        if (proc.processName.contains(context.getPackageName())) {
                            android.os.Process.killProcess(proc.pid);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
    
    // ======== DETECTION MODULES ========
    
    /**
     * Detect known sniffer/proxy/root applications
     */
    private boolean detectSniffers() {
        PackageManager pm = context.getPackageManager();
        for (String pkg : SNIFFER_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                Log.w(TAG, "Sniffer app found: " + pkg);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
                // App not installed - safe
            }
        }
        return false;
    }
    
    /**
     * Detect system proxy settings
     */
    private boolean detectProxy() {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        
        if (proxyHost != null && !proxyHost.isEmpty()) {
            Log.w(TAG, "Proxy detected: " + proxyHost + ":" + proxyPort);
            return true;
        }
        
        // Check global proxy
        try {
            String globalProxy = android.provider.Settings.Global.getString(
                context.getContentResolver(), "http_proxy");
            if (globalProxy != null && !globalProxy.isEmpty() && !globalProxy.equals(":0")) {
                Log.w(TAG, "Global proxy detected: " + globalProxy);
                return true;
            }
        } catch (Exception ignored) {}
        
        return false;
    }
    
    /**
     * Detect emulator/VM environments
     */
    private boolean detectEmulator() {
        String model = Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        String device = Build.DEVICE.toLowerCase();
        String product = Build.PRODUCT.toLowerCase();
        String hardware = Build.HARDWARE.toLowerCase();
        String fingerprint = Build.FINGERPRINT.toLowerCase();
        
        // VMOS detection
        if (model.contains("vmos") || manufacturer.contains("vmos") || 
            product.contains("vmos") || brand.contains("vmos")) {
            return true;
        }
        
        // Generic emulator detection
        if (fingerprint.contains("generic") || fingerprint.contains("unknown") ||
            fingerprint.contains("sdk") || fingerprint.contains("google_sdk") ||
            fingerprint.contains("emulator") || fingerprint.contains("vbox")) {
            // Allow some SDK builds for development but block known emulators
            if (model.contains("sdk") || model.contains("emulator") || 
                model.contains("android sdk") || device.contains("generic")) {
                return true;
            }
        }
        
        // BlueStacks detection
        if (model.contains("bluestacks") || manufacturer.contains("bluestacks")) {
            return true;
        }
        
        // NoxPlayer detection
        if (model.contains("nox") || manufacturer.contains("nox") || brand.contains("nox")) {
            return true;
        }
        
        // Check for emulator files
        File qemuFile = new File("/dev/qemu_pipe");
        File goldfish = new File("/sys/qemu_trace");
        if (qemuFile.exists() || goldfish.exists()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Detect hosts file modifications (ad-blocking)
     */
    private boolean detectHostsModification() {
        try {
            File hostsFile = new File("/etc/hosts");
            if (hostsFile.exists()) {
                long size = hostsFile.length();
                // Normal hosts file is usually < 500 bytes
                // Modified (ad-blocking) hosts files are typically > 10KB
                if (size > 10240) { // > 10KB
                    Log.w(TAG, "Hosts file suspicious size: " + size + " bytes");
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    /**
     * Detect unauthorized VPN connections
     * Allows whitelisted IP range (172.19.0.x)
     */
    private boolean detectUnauthorizedVPN() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        // VPN detected - check if it's our whitelisted VPN
                        if (isWhitelistedVPN()) {
                            return false; // Our VPN, allow
                        }
                        Log.w(TAG, "Unauthorized VPN detected");
                        return true;
                    }
                }
            } else {
                // Fallback for older APIs
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface ni : interfaces) {
                    if (ni.getName().startsWith("tun") || ni.getName().startsWith("ppp")) {
                        if (isWhitelistedVPN()) return false;
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    /**
     * Check if current VPN is whitelisted (by IP range)
     */
    private boolean isWhitelistedVPN() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                if (ni.getName().startsWith("tun") || ni.getName().startsWith("ppp")) {
                    List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                    for (InetAddress addr : addresses) {
                        String ip = addr.getHostAddress();
                        if (ip != null && ip.startsWith(VPN_WHITELIST_PREFIX)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    /**
     * Detect app signature tampering (anti-repack)
     */
    private boolean detectSignatureTampering() {
        if (expectedSignatureHash == null) return false;
        
        String currentHash = getCurrentSignatureHash();
        if (currentHash == null) return true; // Can't verify = suspicious
        
        return !expectedSignatureHash.equals(currentHash);
    }
    
    /**
     * Get SHA-256 hash of app's signing certificate
     */
    @SuppressWarnings("deprecation")
    private String getCurrentSignatureHash() {
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo != null) {
                    Signature[] sigs = info.signingInfo.getApkContentsSigners();
                    if (sigs != null && sigs.length > 0) {
                        return hashSignature(sigs[0]);
                    }
                }
            } else {
                info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
                if (info.signatures != null && info.signatures.length > 0) {
                    return hashSignature(info.signatures[0]);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting signature: " + e.getMessage());
        }
        return null;
    }
    
    private String hashSignature(Signature sig) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sig.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Detect Private DNS settings (ad-blocking DNS)
     */
    private boolean detectPrivateDNS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                String privateDnsMode = android.provider.Settings.Global.getString(
                    context.getContentResolver(), "private_dns_mode");
                
                if ("hostname".equals(privateDnsMode)) {
                    String hostname = android.provider.Settings.Global.getString(
                        context.getContentResolver(), "private_dns_specifier");
                    
                    if (hostname != null) {
                        String lower = hostname.toLowerCase();
                        // Known ad-blocking DNS providers
                        if (lower.contains("adguard") || lower.contains("nextdns") ||
                            lower.contains("dns.adblock") || lower.contains("dnsforge") ||
                            lower.contains("dns.quad9") || lower.contains("dns.switch") ||
                            lower.contains("blahdns") || lower.contains("controld")) {
                            Log.w(TAG, "Ad-blocking DNS detected: " + hostname);
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
