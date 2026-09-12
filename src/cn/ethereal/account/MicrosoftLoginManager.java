package cn.ethereal.account;

import cn.ethereal.util.chat.ChatUtil;
import net.lenni0451.commons.httpclient.HttpClient;
import net.minecraft.client.Minecraft;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import java.util.function.Consumer;

public class MicrosoftLoginManager {

    private static boolean loggingIn = false;

    public static boolean isLoggingIn() { return loggingIn; }

    public static void startLogin() {
        if (loggingIn) {
            ChatUtil.chat("§e[正版登录] 已有一个登录流程在进行了");
            return;
        }
        loggingIn = true;

        new Thread(() -> {
            try {
                HttpClient httpClient = MinecraftAuth.createHttpClient("EtherealClient/1.0");
                // 或者：HttpClient httpClient = new HttpClient();

                // 1. 先创建 Builder
                JavaAuthManager.Builder builder = JavaAuthManager.create(httpClient);

// 2. 在 Builder 上调用 login，传入服务工厂和设备代码回调
                JavaAuthManager authManager = builder.login(
                        DeviceCodeMsaAuthService::new,
                        new Consumer<MsaDeviceCode>() {
                            @Override
                            public void accept(MsaDeviceCode deviceCode) {
                                Minecraft.getMinecraft().addScheduledTask(() ->
                                        showDeviceCode(deviceCode)
                                );
                            }
                        }
                );

                String accessToken = authManager.getMinecraftToken().getUpToDate().getToken();
                String username = authManager.getMinecraftProfile().getUpToDate().getName();
                String uuid = authManager.getMinecraftProfile().getUpToDate().getId().toString();

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    boolean ok = SessionSetter.setSession(username, accessToken, uuid);  // ★ 改成 SessionSetter
                    if (ok) {
                        ChatUtil.chat("§a[正版登录] 成功! 欢迎, §f" + username);
                    } else {
                        ChatUtil.chat("§c[正版登录] Session 设置失败");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Minecraft.getMinecraft().addScheduledTask(() ->
                        ChatUtil.chat("§c[正版登录] 失败: " + e.getMessage())
                );
            } finally {
                loggingIn = false;
            }
        }, "Ethereal-MicrosoftLogin").start();
    }

    private static void showDeviceCode(MsaDeviceCode code) {
        ChatUtil.chat("§6========================================");
        ChatUtil.chat("§e请访问: §f" + code.getVerificationUri());
        ChatUtil.chat("§e输入代码: §a§l" + code.getUserCode());
        ChatUtil.chat("§7或直接点击: §f" + code.getDirectVerificationUri());
        ChatUtil.chat("§6========================================");
    }
}