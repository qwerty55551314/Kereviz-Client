package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.regex.Pattern;

/*
 * Offline/cracked accounts only set a local legacy session.
 * They do not authenticate against Mojang or Microsoft services.
 */
public class GuiCrackedAccount extends GuiScreen {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final GuiScreen previousScreen;
    private GuiTextField usernameField;
    private GuiButton addButton;
    private GuiButton backButton;
    private String status = "&7Enter a cracked username.&r";

    public GuiCrackedAccount(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        usernameField = new GuiTextField(1, mc.fontRendererObj, centerX - 100, centerY - 8, 200, 20);
        usernameField.setMaxStringLength(16);
        usernameField.setFocused(true);

        buttonList.add(addButton = new GuiButton(0, centerX - 100, centerY + 24, 200, 20, "Add Cracked"));
        buttonList.add(backButton = new GuiButton(1, centerX - 100, centerY + 48, 200, 20, "Back"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        usernameField.updateCursorCounter();
        addButton.enabled = !usernameField.getText().trim().isEmpty();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        drawCenteredString(fontRendererObj, "Cracked Account", centerX, centerY - 42, 11184810);
        drawCenteredString(fontRendererObj, TextFormatting.translate(status), centerX, centerY - 28, -1);
        usernameField.drawTextBox();

        if (usernameField.getText().isEmpty() && !usernameField.isFocused()) {
            drawString(fontRendererObj, TextFormatting.translate("&7Username"), centerX - 96, centerY - 2, -1);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }

        switch (button.id) {
            case 0:
                addCrackedAccount();
                break;
            case 1:
                mc.displayGuiScreen(new GuiAccountManager(previousScreen));
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            actionPerformed(backButton);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            actionPerformed(addButton);
            return;
        }

        usernameField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        usernameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void addCrackedAccount() {
        String username = usernameField.getText().trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            status = "&cUsername must be 3-16 letters, numbers or underscores.&r";
            return;
        }

        AccountManager.load();
        for (Account account : AccountManager.accounts) {
            if (account.isCracked() && username.equalsIgnoreCase(account.getUsername())) {
                status = "&cThis cracked account already exists.&r";
                return;
            }
        }

        Account account = Account.cracked(username);
        AccountManager.accounts.add(account);
        AccountManager.save();
        SessionManager.set(new Session(username, "", "", "legacy"));
        mc.displayGuiScreen(new GuiAccountManager(
                previousScreen,
                new Notification(TextFormatting.translate(String.format(
                        "&aSuccessful cracked login! (%s)&r", username
                )), 5000L)
        ));
    }
}
