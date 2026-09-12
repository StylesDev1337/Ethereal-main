package cn.ethereal.hud.impl;

import cn.ethereal.hud.HudElement;
import cn.ethereal.module.render.HUD;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScoreboardHud extends HudElement {

    private static final int PADDING = 5;
    private static final int RADIUS = 4;
    private static final int BAR_WIDTH = 2;
    private static final int LINE_HEIGHT = 11;
    private static final int HEADER_GAP = 4;

    private static final int BG_COLOR = 0xB0101010;
    private static final int BORDER_COLOR = 0xFF2A2A2A;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFFCCCCCC;
    private static final int SCORE_COLOR = 0xFFFF5555;

    private List<Row> cachedRows = new ArrayList<>();
    private String cachedTitle = null;

    public ScoreboardHud() {
        super("Scoreboard", "自定义计分板",
                HUD.getInstance().scoreboardShow,
                HUD.getInstance().scoreboardPosX,
                HUD.getInstance().scoreboardPosY);
    }

    public static boolean hasSidebar() {
        if (mc.theWorld == null) return false;
        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) return false;
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        return obj != null;
    }

    @Override
    public int[] measure() {
        collectRows();

        if (cachedRows.isEmpty() && cachedTitle == null) {
            width = 0;
            height = 0;
            return new int[] { 0, 0 };
        }

        int maxW = 0;
        if (cachedTitle != null) {
            maxW = Math.max(maxW, font.getStringWidth(cachedTitle));
        }
        for (Row row : cachedRows) {
            int rowW = font.getStringWidth(row.name) + 4 + font.getStringWidth(row.score);
            maxW = Math.max(maxW, rowW);
        }

        width = maxW + PADDING * 2 + BAR_WIDTH + 4;
        height = (cachedTitle != null ? font.FONT_HEIGHT + HEADER_GAP : 0)
                + cachedRows.size() * LINE_HEIGHT
                + PADDING * 2;

        return new int[] { width, height };
    }

    @Override
    public int getDefaultX(ScaledResolution sr) {
        return sr.getScaledWidth() - width - 4;
    }

    @Override
    public int getDefaultY(ScaledResolution sr) {
        return (sr.getScaledHeight() - height) / 2;
    }

    @Override
    public void render(int x, int y) {
        if (cachedRows.isEmpty() && cachedTitle == null) return;

        int themeColor = HUD.getInstance().getThemeColor();

        RenderUtil.drawRoundedRect(x, y, width, height, RADIUS, BG_COLOR);
        RenderUtil.drawRoundedRect(x + 2, y + 3, BAR_WIDTH, height - 6, BAR_WIDTH / 2F, themeColor);

        int textX = x + 2 + BAR_WIDTH + 4;
        int cursorY = y + PADDING;

        if (cachedTitle != null) {
            int titleW = font.getStringWidth(cachedTitle);
            int titleX = x + (width - titleW) / 2;
            font.drawStringWithShadow(cachedTitle, titleX, cursorY, 0xFFFF5555);
            cursorY += font.FONT_HEIGHT + HEADER_GAP;

            RenderUtil.drawRect(x + 6, cursorY - 2, width - 12, 1, 0x40FFFFFF);
        }

        for (Row row : cachedRows) {
            font.drawStringWithShadow(row.name, textX, cursorY, TEXT_COLOR);

            int scoreW = font.getStringWidth(row.score);
            int scoreX = x + width - PADDING - scoreW;
            font.drawStringWithShadow(row.score, scoreX, cursorY, SCORE_COLOR);

            cursorY += LINE_HEIGHT;
        }
    }

    private void collectRows() {
        cachedRows.clear();
        cachedTitle = null;

        if (mc.theWorld == null) return;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return;

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return;

        String displayName = objective.getDisplayName();
        cachedTitle = StringUtils.stripControlCodes(displayName);

        Collection<Score> scores = scoreboard.getSortedScores(objective);

        List<Score> list = new ArrayList<>();
        for (Score s : scores) {
            if (list.size() >= 15) break;
            list.add(s);
        }

        for (Score score : list) {
            String playerName = score.getPlayerName();
            if (playerName == null || playerName.startsWith("#")) continue;

            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            String formatted = ScorePlayerTeam.formatPlayerName(team, playerName);
            String cleanName = StringUtils.stripControlCodes(formatted);

            cachedRows.add(new Row(cleanName, String.valueOf(score.getScorePoints())));
        }
    }

    private static class Row {
        final String name;
        final String score;

        Row(String name, String score) {
            this.name = name;
            this.score = score;
        }
    }
}