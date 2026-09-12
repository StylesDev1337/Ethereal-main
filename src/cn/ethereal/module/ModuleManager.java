package cn.ethereal.module;

import cn.ethereal.module.client.Rotations;
import cn.ethereal.module.combat.KillAura;
import cn.ethereal.module.combat.Target;
import cn.ethereal.module.movement.Noslow;
import cn.ethereal.module.movement.Scaffold;
import cn.ethereal.module.movement.Sprint;
//import cn.ethereal.module.player.ThrowableAura;
import cn.ethereal.module.player.ThrowableAura;
import cn.ethereal.module.render.Animations;
import cn.ethereal.module.render.HUD;
import cn.ethereal.module.render.FullBright;
import cn.ethereal.module.render.TargetHud;
import cn.ethereal.ui.ClickGUI;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private static ModuleManager instance;
    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {}

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    public void registerModules() {
        addModule(new Target());
        addModule(new HUD());
        addModule(new Sprint());
        addModule(new FullBright());
        addModule(new KillAura());
        addModule(new Noslow());
        addModule(new ThrowableAura());
        addModule(new Rotations());
        addModule(new TargetHud());
        addModule(new Scaffold());
        addModule(new Animations());
    }

    public void addModule(Module module) {
        if (module != null && !modules.contains(module)) {
            modules.add(module);
        }
    }

    public List<Module> getModules() {
        return new ArrayList<>(modules);
    }

    public int getModuleCount() {
        return modules.size();
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Module getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Module getModuleByKey(int key) {
        return modules.stream()
                .filter(m -> m.getKey() == key && key != 0)
                .findFirst()
                .orElse(null);
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }

    public void onKeyPress(int key) {
        if (key == 0) return;
        for (Module module : modules) {
            if (module.getKey() == key) {
                module.toggle();
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_RSHIFT) {
                Minecraft.getMinecraft().displayGuiScreen(new ClickGUI());
            }
        }
    }
}