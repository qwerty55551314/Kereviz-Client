package me.ksyz.accountmanager;

import com.google.gson.*;
import kereviz.config.ClientFiles;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.utils.Nan0EventRegister;
import me.ksyz.accountmanager.utils.SSLUtils;
import net.minecraftforge.common.MinecraftForge;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.util.ArrayList;
import java.util.Optional;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class AccountManager {
    private static final File file = ClientFiles.accountFile("kereviz.accounts.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static final ArrayList<Account> accounts = new ArrayList<>();

    public static void init() {
        SSLContext ignored = SSLUtils.getSSLContext();
        Nan0EventRegister.register(MinecraftForge.EVENT_BUS,new Events());

        if (!file.exists()) {
            try {
                if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                    if (file.createNewFile()) {
                        System.out.print("Successfully created kereviz.accounts.json!");
                    }
                }
            } catch (IOException e) {
                System.err.print("Couldn't create kereviz.accounts.json!");
            }
        }
    }

    public static void load() {
        accounts.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            JsonElement json = new JsonParser().parse(reader);
            if (!(json instanceof JsonArray)) {
                return;
            }

            JsonArray jsonArray = json.getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                if (jsonElement == null || !jsonElement.isJsonObject()) {
                    continue;
                }

                JsonObject jsonObject = jsonElement.getAsJsonObject();
                accounts.add(new Account(
                        Optional.ofNullable(jsonObject.get("refreshToken")).map(JsonElement::getAsString).orElse(""),
                        Optional.ofNullable(jsonObject.get("accessToken")).map(JsonElement::getAsString).orElse(""),
                        Optional.ofNullable(jsonObject.get("username")).map(JsonElement::getAsString).orElse(""),
                        Optional.ofNullable(jsonObject.get("unban")).map(JsonElement::getAsLong).orElse(0L),
                        Optional.ofNullable(jsonObject.get("clientId")).map(JsonElement::getAsString).orElse(""),
                        Optional.ofNullable(jsonObject.get("scope")).map(JsonElement::getAsString).orElse(""),
                        Optional.ofNullable(jsonObject.get("type")).map(JsonElement::getAsString).orElse(Account.TYPE_MICROSOFT)
                ));
            }
        } catch (FileNotFoundException e) {
            System.err.print("Couldn't find kereviz.accounts.json!");
        } catch (Exception e) {
            System.err.print("Couldn't load kereviz.accounts.json!");
        }
    }

    public static void save() {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            JsonArray jsonArray = new JsonArray();
            for (Account account : accounts) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("refreshToken", account.getRefreshToken());
                jsonObject.addProperty("accessToken", account.getAccessToken());
                jsonObject.addProperty("username", account.getUsername());
                jsonObject.addProperty("unban", account.getUnban());
                jsonObject.addProperty("clientId", account.getClientId());
                jsonObject.addProperty("scope", account.getScope());
                jsonObject.addProperty("type", account.getType());
                jsonArray.add(jsonObject);
            }
            try (PrintWriter printWriter = new PrintWriter(new FileWriter(file))) {
                printWriter.println(gson.toJson(jsonArray));
            }
        } catch (IOException e) {
            System.err.print("Couldn't save kereviz.accounts.json!");
        }
    }
}
