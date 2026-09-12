package cn.ethereal.config;

import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.ui.values.StringValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final Gson gson;
    private final File configFile;
    private final Path configPath;

    private ConfigManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        this.configFile = new File(Minecraft.getMinecraft().mcDataDir, "ethereal/config.json");
        this.configPath = configFile.toPath();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void saveConfig() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            ConfigData configData = new ConfigData();

            for (Module module : ModuleManager.getInstance().getModules()) {
                ModuleConfig moduleConfig = new ModuleConfig(
                        module.isEnabled(),
                        module.getKey()
                );

                for (BooleanValue value : module.getBooleanValues()) {
                    moduleConfig.booleanValues.put(value.getName(), value.getValue());
                }
                for (NumberValue value : module.getNumberValues()) {
                    moduleConfig.numberValues.put(value.getName(), value.getValue());
                }
                for (ModeValue value : module.getModeValues()) {
                    moduleConfig.modeValues.put(value.getName(), value.getValue());
                }
                for (StringValue value : module.getStringValues()) {
                    moduleConfig.stringValues.put(value.getName(), value.getValue());
                }

                configData.modules.put(module.getName(), moduleConfig);
            }

            configData.metadata.version = "1.0";
            configData.metadata.savedTime = System.currentTimeMillis();

            String json = gson.toJson(configData);
            Files.write(configPath, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("[Ethereal] Config saved to " + configFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("[Ethereal] Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        try {
            if (!configFile.exists()) {
                System.out.println("[Ethereal] Config file not found, creating default config");
                saveConfig();
                return;
            }

            String json = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            ConfigData configData = gson.fromJson(json, ConfigData.class);

            if (configData == null || configData.modules == null) {
                System.err.println("[Ethereal] Invalid config file, creating new one");
                saveConfig();
                return;
            }

            int loadedCount = 0;
            for (Map.Entry<String, ModuleConfig> entry : configData.modules.entrySet()) {
                Module module = ModuleManager.getInstance().getModule(entry.getKey());
                if (module == null) continue;

                ModuleConfig moduleConfig = entry.getValue();

                module.setEnabled(moduleConfig.enabled);
                module.setKey(moduleConfig.keyBind);

                for (BooleanValue value : module.getBooleanValues()) {
                    Boolean saved = moduleConfig.booleanValues.get(value.getName());
                    if (saved != null) value.setValue(saved);
                }
                for (NumberValue value : module.getNumberValues()) {
                    Double saved = moduleConfig.numberValues.get(value.getName());
                    if (saved != null) value.setValue(saved);
                }
                for (ModeValue value : module.getModeValues()) {
                    String saved = moduleConfig.modeValues.get(value.getName());
                    if (saved != null) value.setValue(saved);
                }
                for (StringValue value : module.getStringValues()) {
                    String saved = moduleConfig.stringValues.get(value.getName());
                    if (saved != null) value.setValue(saved);
                }

                loadedCount++;
            }

            System.out.println("[Ethereal] Config loaded: " + loadedCount + " modules restored");

        } catch (IOException e) {
            System.err.println("[Ethereal] Failed to load config: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[Ethereal] Error parsing config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void resetConfig() {
        try {
            if (configFile.exists()) {
                Files.delete(configPath);
                System.out.println("[Ethereal] Config file deleted");
            }

            for (Module module : ModuleManager.getInstance().getModules()) {
                module.setEnabled(false);
                module.setKey(0);
                for (BooleanValue v : module.getBooleanValues()) v.reset();
                for (NumberValue v : module.getNumberValues()) v.reset();
                for (ModeValue v : module.getModeValues()) v.reset();
                for (StringValue v : module.getStringValues()) v.reset();
            }

            saveConfig();

        } catch (IOException e) {
            System.err.println("[Ethereal] Failed to reset config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public File getConfigFile() {
        return configFile;
    }

    private static class ConfigData {
        Metadata metadata = new Metadata();
        Map<String, ModuleConfig> modules = new HashMap<>();
    }

    private static class Metadata {
        String version;
        long savedTime;
    }

    private static class ModuleConfig {
        boolean enabled;
        int keyBind;
        Map<String, Boolean> booleanValues = new HashMap<>();
        Map<String, Double> numberValues = new HashMap<>();
        Map<String, String> modeValues = new HashMap<>();
        Map<String, String> stringValues = new HashMap<>();

        ModuleConfig() {}

        ModuleConfig(boolean enabled, int keyBind) {
            this.enabled = enabled;
            this.keyBind = keyBind;
        }
    }
}