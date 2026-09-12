package cn.ethereal.account;

import cn.ethereal.ui.notification.Notification;
import cn.ethereal.ui.notification.NotificationManager;
import net.lenni0451.commons.httpclient.HttpClient;
import net.minecraft.client.Minecraft;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import java.util.function.Consumer;

public class MicrosoftLoginManager {

    // ==================== 状态 ====================
    public enum State {
        IDLE,        // 未开始
        REQUESTING,  // 正在向微软请求设备代码
        WAITING,     // 已拿到设备代码，等待用户在浏览器里授权
        SUCCESS,     // 登录成功
        FAILED       // 失败
    }

    private static volatile State state = State.IDLE;
    private static volatile String deviceCode = null;
    private static volatile String verificationUri = null;
    private static volatile String directUri = null;
    private static volatile String errorMessage = null;
    private static volatile String successName = null;

    // UI 可注册的状态监听（可选）
    private static volatile Consumer<State> stateListener = null;

    // ==================== 对外 API ====================

    public static State getState() { return state; }
    public static String getDeviceCode() { return deviceCode; }
    public static String getVerificationUri() { return verificationUri; }
    public static String getDirectUri() { return directUri; }
    public static String getErrorMessage() { return errorMessage; }
    public static String getSuccessName() { return successName; }

    public static final NotificationManager noti = NotificationManager.getInstance();

    public static boolean isLoggingIn() {
        return state == State.REQUESTING || state == State.WAITING;
    }

    public static void setStateListener(Consumer<State> listener) {
        stateListener = listener;
    }

    /** 重置为初始状态（取消/重试时用） */
    public static void reset() {
        if (isLoggingIn()) return;   // 正在跑时不允许重置
        state = State.IDLE;
        deviceCode = null;
        verificationUri = null;
        directUri = null;
        errorMessage = null;
        successName = null;
    }

    // ==================== 登录流程 ====================

    public static void startLogin() {
        if (isLoggingIn()) {
            noti.notify("§e[正版登录]", "已有一个登录流程在进行了", Notification.Type.WARN);
            return;
        }

        // 重置状态
        deviceCode = null;
        verificationUri = null;
        directUri = null;
        errorMessage = null;
        successName = null;
        setState(State.REQUESTING);

        new Thread(() -> {
            try {
                HttpClient httpClient = MinecraftAuth.createHttpClient("EtherealClient/1.0");

                JavaAuthManager.Builder builder = JavaAuthManager.create(httpClient);

                JavaAuthManager authManager = builder.login(
                        DeviceCodeMsaAuthService::new,
                        new Consumer<MsaDeviceCode>() {
                            @Override
                            public void accept(MsaDeviceCode code) {
                                // 保存设备代码到字段（线程安全，volatile）
                                deviceCode = code.getUserCode();
                                verificationUri = code.getVerificationUri();
                                directUri = code.getDirectVerificationUri();
                                setState(State.WAITING);
                                openBrowser(code.getDirectVerificationUri());
                            }
                        }
                );

                String accessToken = authManager.getMinecraftToken().getUpToDate().getToken();
                String username = authManager.getMinecraftProfile().getUpToDate().getName();
                String uuid = authManager.getMinecraftProfile().getUpToDate().getId().toString();

                boolean ok = SessionSetter.setSession(username, accessToken, uuid);
                if (ok) {
                    successName = username;
                    setState(State.SUCCESS);
                    Minecraft.getMinecraft().addScheduledTask(() ->
                            noti.notify("§a[正版登录] 成功!", " 欢迎, §f" + username, Notification.Type.SUCCESS)
                    );
                } else {
                    errorMessage = "Session 设置失败";
                    setState(State.FAILED);
                    Minecraft.getMinecraft().addScheduledTask(() ->
                            noti.notify("§c[正版登录]", "Session 设置失败", Notification.Type.ERROR)
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                setState(State.FAILED);
                Minecraft.getMinecraft().addScheduledTask(() ->
                        noti.notify( "§c[正版登录]", "失败:" + errorMessage, Notification.Type.ERROR)
                );
            }
        }, "Ethereal-MicrosoftLogin").start();
    }

    private static void setState(State s) {
        state = s;
        Consumer<State> l = stateListener;
        if (l != null) {
            Minecraft.getMinecraft().addScheduledTask(() -> l.accept(s));
        }
    }

    private static void openBrowser(String uri) {
        if (uri == null || uri.isEmpty()) return;
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                // 用独立线程避免某些系统下 browse 阻塞
                new Thread(() -> {
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(uri));
                    } catch (Exception e) {
                        System.err.println("[Ethereal] 打开浏览器失败: " + e.getMessage());
                    }
                }, "Ethereal-OpenBrowser").start();
            }
        } catch (Throwable t) {
            // HeadlessException 或类不存在
            System.err.println("[Ethereal] 当前环境不支持打开浏览器: " + t.getMessage());
        }
    }
}