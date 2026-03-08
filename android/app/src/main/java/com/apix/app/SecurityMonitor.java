package com.apix.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Debug;
import android.view.Display;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Security Monitor - Advanced Anti-Tamper & Anti-Reverse-Engineering System
 * 
 * Features:
 * - 3-second splash security check before app loads
 * - Continuous background monitoring (5-19ms)
 * - Cloud phone / virtual device detection
 * - Secondary display / VMOS detection
 * - Sniffer/Proxy/Emulator detection
 * - Debugger/Frida/Root detection
 * - APK integrity & signature verification
 */
public class SecurityMonitor {
    
    private static SecurityMonitor instance;
    private Context context;
    private Thread monitorThread;
    private volatile boolean running = false;
    
    // HARDCODED signature hash - set for production
    private static final String RELEASE_SIGNATURE_HASH = null;
    private String expectedSignatureHash = null;
    private int expectedDexCount = -1;
    
    // Dangerous packages
    private static final String[] DANGEROUS_PACKAGES = {
        // Sniffers
        "com.guoshi.httpcanary", "com.guoshi.httpcanary.premium",
        "com.minhui.networkcapture", "jp.co.because.network.analysis",
        "com.charles.proxy", "com.egorovandreyrm.pcapremote",
        "app.greyshirts.sslcapture", "com.reqbin.httpbin",
        // Xposed / Root
        "io.anyline.xposed", "de.robv.android.xposed.installer",
        "org.lsposed.manager", "com.topjohnwu.magisk",
        "eu.chainfire.supersu", "me.weishu.exp",
        "com.saurik.substrate", "com.zachspong.temprootremovejb",
        // Reverse engineering tools
        "com.mt.mtmanager", "bin.mt.plus",
        "com.apktool.apktools", "com.jrummyapps.rootbrowser",
        "com.noshufou.android.su", "com.koushikdutta.superuser",
        // Game/App modifiers
        "com.chelpus.lackypatch", "com.android.vending.billing.InAppBillingService.LACK",
        "com.ramdroid.appquarantine",
        // Frida
        "re.frida.server", "com.frida",
        // Cloud phones & virtual environments
        "com.redfinger.app", "com.redfinger.cloud",
        "com.nowgg.cloud", "com.netease.mumu",
        "com.microvirt.memuime", "com.bignox.appcenter",
        "com.ldmnq.launcher3", "com.ldmnq.launcher",
        "com.kaopu.gameassistant", "com.excelliance.multiaccount",
        "com.parallel.space", "com.parallel.space.lite",
        "com.lbe.parallel.intl", "com.jumobile.smartapp.dual",
    };
    
    // Cloud phone indicators in build properties
    private static final String[] CLOUD_PHONE_INDICATORS = {
        "vmos", "redfinger", "nowgg", "cloudphone", "remotegaming",
        "cloud_phone", "virtual_phone", "phonecloud", "genymotion",
        "tencent_cloud", "huawei_cloud", "alicloud", "aws_device_farm",
    };
    
    private static final int[] FRIDA_PORTS = {27042, 27043};
    private static final String VPN_WHITELIST_PREFIX = "172.19.0.";

    public interface SecurityCheckCallback {
        void onCheckComplete(boolean passed, String failReason);
    }

    private SecurityMonitor(Context ctx) {
        this.context = ctx.getApplicationContext();
        if (RELEASE_SIGNATURE_HASH != null) {
            this.expectedSignatureHash = RELEASE_SIGNATURE_HASH;
        } else {
            this.expectedSignatureHash = getCurrentSignatureHash();
        }
        this.expectedDexCount = countDexFiles();
    }
    
    public static synchronized SecurityMonitor getInstance(Context ctx) {
        if (instance == null) {
            instance = new SecurityMonitor(ctx);
        }
        return instance;
    }
    
    /**
     * Run initial security check (blocking, used during splash screen)
     * Returns null if passed, or failure reason string
     */
    public String runInitialCheck() {
        if (detectSniffers()) return "تم اكتشاف برنامج مراقبة";
        if (detectCloudPhone()) return "لا يمكن تشغيل التطبيق على هاتف سحابي";
        if (detectSecondaryDisplay()) return "لا يمكن تشغيل التطبيق على شاشة ثانوية";
        // Emulator & Root allowed
        // if (detectEmulator()) return "لا يمكن تشغيل التطبيق على محاكي";
        if (detectProxy()) return "تم اكتشاف بروكسي";
        if (detectDebugger()) return "تم اكتشاف مصحح أخطاء";
        if (detectSignatureTampering()) return "تم التلاعب بالتطبيق";
        if (detectApkTampering()) return "تم تعديل ملفات التطبيق";
        if (detectFrida()) return "تم اكتشاف أداة اختراق";
        // if (detectRootFiles()) return "الجهاز مروت";
        return null; // All checks passed
    }

    /**
     * Run initial check asynchronously with callback
     */
    public void runInitialCheckAsync(SecurityCheckCallback callback) {
        new Thread(() -> {
            String result = runInitialCheck();
            if (callback != null) {
                callback.onCheckComplete(result == null, result);
            }
        }).start();
    }
    
    public void startMonitor() {
        if (running) return;
        running = true;
        
        monitorThread = new Thread(() -> {
            while (running) {
                try {
                    if (detectSniffers()) { killApp(); return; }
                    if (detectCloudPhone()) { killApp(); return; }
                    if (detectSecondaryDisplay()) { killApp(); return; }
                    // Emulator & Root allowed
                    // if (detectEmulator()) { killApp(); return; }
                    if (detectProxy()) { killApp(); return; }
                    if (detectHostsModification()) { killApp(); return; }
                    if (detectUnauthorizedVPN()) { killApp(); return; }
                    if (detectSignatureTampering()) { killApp(); return; }
                    if (detectPrivateDNS()) { killApp(); return; }
                    if (detectDebugger()) { killApp(); return; }
                    if (detectFrida()) { killApp(); return; }
                    if (detectApkTampering()) { killApp(); return; }
                    // if (detectRootFiles()) { killApp(); return; }
                    
                    Thread.sleep(5 + (long)(Math.random() * 14));
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        }, "t1");
        
        monitorThread.setDaemon(true);
        monitorThread.setPriority(Thread.MAX_PRIORITY);
        monitorThread.start();
    }
    
    public void stopMonitor() {
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
    }
    
    private void killApp() {
        running = false;
        try {
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
    
    // ======== CLOUD PHONE & VIRTUAL ENVIRONMENT DETECTION ========
    
    private boolean detectCloudPhone() {
        String model = Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        String product = Build.PRODUCT.toLowerCase();
        String device = Build.DEVICE.toLowerCase();
        String hardware = Build.HARDWARE.toLowerCase();
        String fingerprint = Build.FINGERPRINT.toLowerCase();
        String board = Build.BOARD.toLowerCase();
        
        // Check all build properties against cloud phone indicators
        for (String indicator : CLOUD_PHONE_INDICATORS) {
            if (model.contains(indicator) || manufacturer.contains(indicator) ||
                brand.contains(indicator) || product.contains(indicator) ||
                device.contains(indicator) || hardware.contains(indicator) ||
                fingerprint.contains(indicator) || board.contains(indicator)) {
                return true;
            }
        }
        
        // Check for VMOS specifically
        if (model.contains("vmos") || Build.DISPLAY.toLowerCase().contains("vmos") ||
            new File("/data/data/com.vmos.pro").exists() ||
            new File("/data/data/com.vmos.app").exists()) {
            return true;
        }
        
        // Check for cloud phone files
        String[] cloudPhoneFiles = {
            "/data/data/com.redfinger.app",
            "/data/data/com.redfinger.cloud",
            "/data/data/com.nowgg.cloud",
            "/system/app/VMOSFakeGps",
            "/data/vmos",
        };
        for (String path : cloudPhoneFiles) {
            if (new File(path).exists()) return true;
        }
        
        // Check system properties for cloud indicators
        try {
            Process process = Runtime.getRuntime().exec("getprop");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String lower = line.toLowerCase();
                if (lower.contains("vmos") || lower.contains("cloud.phone") ||
                    lower.contains("redfinger") || lower.contains("virtual.device")) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (Exception ignored) {}
        
        return false;
    }
    
    /**
     * Detect secondary/virtual displays (screen mirroring, dual screen apps)
     */
    private boolean detectSecondaryDisplay() {
        try {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            if (dm != null) {
                Display[] displays = dm.getDisplays();
                // More than 1 display and any is not the default = suspicious
                if (displays.length > 1) {
                    for (Display display : displays) {
                        if (display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                            // Check if it's a presentation display or virtual
                            int flags = display.getFlags();
                            boolean isVirtual = (flags & Display.FLAG_PRESENTATION) != 0;
                            boolean isPrivate = (flags & Display.FLAG_PRIVATE) != 0;
                            
                            // Virtual/presentation displays are suspicious
                            if (isVirtual || isPrivate) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    // ======== EXISTING DETECTION MODULES ========
    
    private boolean detectSniffers() {
        PackageManager pm = context.getPackageManager();
        for (String pkg : DANGEROUS_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return false;
    }
    
    private boolean detectProxy() {
        String proxyHost = System.getProperty("http.proxyHost");
        if (proxyHost != null && !proxyHost.isEmpty()) return true;
        try {
            String globalProxy = android.provider.Settings.Global.getString(
                context.getContentResolver(), "http_proxy");
            if (globalProxy != null && !globalProxy.isEmpty() && !globalProxy.equals(":0")) return true;
        } catch (Exception ignored) {}
        return false;
    }
    
    private boolean detectEmulator() {
        String model = Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        String device = Build.DEVICE.toLowerCase();
        String product = Build.PRODUCT.toLowerCase();
        String fingerprint = Build.FINGERPRINT.toLowerCase();
        
        if (model.contains("vmos") || manufacturer.contains("vmos") || 
            product.contains("vmos") || brand.contains("vmos")) return true;
        if (fingerprint.contains("generic") || fingerprint.contains("unknown") ||
            fingerprint.contains("sdk") || fingerprint.contains("emulator") ||
            fingerprint.contains("vbox")) {
            if (model.contains("sdk") || model.contains("emulator") || 
                model.contains("android sdk") || device.contains("generic")) return true;
        }
        if (model.contains("bluestacks") || manufacturer.contains("bluestacks")) return true;
        if (model.contains("nox") || manufacturer.contains("nox") || brand.contains("nox")) return true;
        if (model.contains("memu") || manufacturer.contains("microvirt")) return true;
        if (model.contains("ldplayer") || manufacturer.contains("ldmnq")) return true;
        
        File qemuFile = new File("/dev/qemu_pipe");
        File goldfish = new File("/sys/qemu_trace");
        if (qemuFile.exists() || goldfish.exists()) return true;
        
        return false;
    }
    
    private boolean detectHostsModification() {
        try {
            File hostsFile = new File("/etc/hosts");
            if (hostsFile.exists() && hostsFile.length() > 10240) return true;
        } catch (Exception ignored) {}
        return false;
    }
    
    private boolean detectUnauthorizedVPN() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return !isWhitelistedVPN();
                    }
                }
            } else {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface ni : interfaces) {
                    if (ni.getName().startsWith("tun") || ni.getName().startsWith("ppp")) {
                        return !isWhitelistedVPN();
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    private boolean isWhitelistedVPN() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                if (ni.getName().startsWith("tun") || ni.getName().startsWith("ppp")) {
                    List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                    for (InetAddress addr : addresses) {
                        String ip = addr.getHostAddress();
                        if (ip != null && ip.startsWith(VPN_WHITELIST_PREFIX)) return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    private boolean detectSignatureTampering() {
        if (expectedSignatureHash == null) return false;
        String currentHash = getCurrentSignatureHash();
        if (currentHash == null) return true;
        return !expectedSignatureHash.equals(currentHash);
    }
    
    @SuppressWarnings("deprecation")
    private String getCurrentSignatureHash() {
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo != null) {
                    Signature[] sigs = info.signingInfo.getApkContentsSigners();
                    if (sigs != null && sigs.length > 0) return hashSignature(sigs[0]);
                }
            } else {
                info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
                if (info.signatures != null && info.signatures.length > 0)
                    return hashSignature(info.signatures[0]);
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private String hashSignature(Signature sig) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sig.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return null; }
    }
    
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
                        if (lower.contains("adguard") || lower.contains("nextdns") ||
                            lower.contains("dns.adblock") || lower.contains("dnsforge") ||
                            lower.contains("dns.quad9") || lower.contains("blahdns") ||
                            lower.contains("controld")) return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
    
    private boolean detectDebugger() {
        // Skip debugger check in debug builds (Android Studio testing)
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // Debug build - skip all debugger checks to allow testing
                return false;
            }
        } catch (Exception ignored) {}
        
        // Release build - full debugger detection
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) return true;
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(Runtime.getRuntime().exec("cat /proc/self/status").getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("TracerPid:")) {
                    int pid = Integer.parseInt(line.substring(10).trim());
                    if (pid != 0) return true;
                }
            }
            reader.close();
        } catch (Exception ignored) {}
        return false;
    }
    
    private boolean detectFrida() {
        for (int port : FRIDA_PORTS) {
            try {
                java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
                socket.close();
                return true;
            } catch (Exception ignored) {}
        }
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(Runtime.getRuntime().exec("cat /proc/self/maps").getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("frida") || line.contains("gadget")) return true;
            }
            reader.close();
        } catch (Exception ignored) {}
        File fridaPipe = new File("/data/local/tmp/frida-server");
        File fridaPipe2 = new File("/data/local/tmp/re.frida.server");
        if (fridaPipe.exists() || fridaPipe2.exists()) return true;
        return false;
    }
    
    private boolean detectApkTampering() {
        if (expectedDexCount <= 0) return false;
        return countDexFiles() != expectedDexCount;
    }
    
    private int countDexFiles() {
        try {
            String apkPath = context.getApplicationInfo().sourceDir;
            ZipFile zipFile = new ZipFile(apkPath);
            int count = 0;
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".dex")) count++;
            }
            zipFile.close();
            return count;
        } catch (Exception e) { return -1; }
    }
    
    private boolean detectRootFiles() {
        String[] rootPaths = {
            "/system/app/Superuser.apk", "/system/xbin/su", "/system/bin/su",
            "/sbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/data/local/su", "/su/bin/su", "/system/bin/.ext/.su",
        };
        for (String path : rootPaths) {
            if (new File(path).exists()) return true;
        }
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "su"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            reader.close();
            if (result != null && !result.isEmpty()) return true;
        } catch (Exception ignored) {}
        return false;
    }
}
