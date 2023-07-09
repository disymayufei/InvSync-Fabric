package cn.disy920.invsync;

import cn.disy920.invsync.access.PlayerAccess;
import cn.disy920.invsync.database.PlayerDatabase;
import cn.disy920.invsync.fabric.FabricMod;
import cn.disy920.invsync.utils.Logger;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends FabricMod {
    private final Map<UUID, PlayerDatabase> workingDatabase = new ConcurrentHashMap<>(64);
    private Thread autoSaveThread = null;

    public static Main MOD_INSTANCE = null;

    @Override
    public void onInit() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerUuid = player.getUuid();

            ((PlayerAccess)player).setWaiting(true);
            PlayerInventory inventory = player.getInventory();
            PlayerInventory inventoryClone = new PlayerInventory(player);
            inventoryClone.clone(inventory);

            inventory.clear();

            player.sendMessageToClient(Text.literal("正在读取背包数据...").setStyle(Style.EMPTY.withColor(Formatting.GOLD)), true);

            new Thread(() -> {
                PlayerDatabase database = null;

                try {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ignore){}

                    boolean success = false;
                    Exception exception = null;
                    for (int i = 0; i < 3; i++) {
                        try {
                            database = new PlayerDatabase(playerUuid);
                            success = true;
                            break;
                        }
                        catch (Exception e) {
                            exception = e;
                            Thread.sleep(1000);
                        }
                    }

                    if (!success) {
                        throw exception;
                    }

                    if (database.isError()) {
                        handler.disconnect(Text.literal("您的背包数据疑似在保存时出现损坏，请立即停止尝试进入服务器，并及时联系管理员处理！").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                        return;
                    }

                    NbtCompound nbtData = database.getInventoryNbt();

                    if (nbtData != null) {
                        for (int i = 0; i < inventory.size(); i++) {
                            ItemStack itemStack = ItemStack.fromNbt(nbtData.getCompound(String.valueOf(i)));
                            inventory.insertStack(i, itemStack);
                        }

                        workingDatabase.put(playerUuid, database);

                        player.sendMessageToClient(Text.literal("背包数据读取成功").setStyle(Style.EMPTY.withColor(Formatting.GREEN)), true);

                        ((PlayerAccess)player).setWaiting(false);
                    }
                    else {  // 如果没数据那就先把玩家背包存一下
                        if (!getConfig().getBoolean("default-server")) {
                            handler.disconnect(Text.literal("您的背包数据尚未被创建，请先进入主生存服完成创建工作！").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                            database.close();
                            return;
                        }

                        NbtCompound data = new NbtCompound();

                        for (int i = 0; i < inventory.size(); i++) {
                            ItemStack itemStack = inventory.getStack(i);

                            NbtCompound emptyCompound = new NbtCompound();
                            NbtCompound itemCompound = itemStack.writeNbt(emptyCompound);

                            data.put(String.valueOf(i), itemCompound);
                        }

                        try {
                            database.putInventoryNbt(data);

                            player.sendMessageToClient(Text.literal("未检测到背包数据，已自动创建！").setStyle(Style.EMPTY.withColor(Formatting.GREEN)), true);
                            ((PlayerAccess)player).setWaiting(false);
                        }
                        catch (Exception e) {
                            database.close();

                            database.makeBackupLog(inventoryClone);

                            handler.disconnect(Text.literal("您的背包保存失败，若您看到该消息，请立即联系管理员查看后台报错！"));
                            Logger.warn(player.getName().getString() + "的背包保存失败，以下是错误的堆栈信息:");
                            e.printStackTrace();
                        }
                    }
                }
                catch (InterruptedException ignored) {}
                catch (Exception e) {
                    if (database != null) {
                        database.close();
                    }
                    handler.disconnect(Text.literal("背包同步时发生错误，错误信息: " + e.getMessage()).setStyle(Style.EMPTY.withColor(Formatting.RED)));
                    Logger.error("加载" + player.getName().getString() + "的数据库的过程中发生错误，以下是错误的堆栈信息: ");
                    e.printStackTrace();
                }
            }).start();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerUuid = player.getUuid();

            new Thread(() -> {
                PlayerDatabase database = workingDatabase.get(playerUuid);
                if (database != null) {
                    PlayerInventory inventory = player.getInventory();

                    NbtCompound data = new NbtCompound();

                    for (int i = 0; i < inventory.size(); i++) {
                        ItemStack itemStack = inventory.getStack(i);

                        NbtCompound emptyCompound = new NbtCompound();
                        NbtCompound itemCompound = itemStack.writeNbt(emptyCompound);

                        data.put(String.valueOf(i), itemCompound);
                    }

                    try {
                        database.putInventoryNbt(data);
                    }
                    catch (Exception e) {
                        database.markError(e);
                    }
                    database.close();

                    workingDatabase.remove(playerUuid);
                }
            }).start();
        });

        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        MOD_INSTANCE = this;

        int autoSaveInterval = getConfig().getInt("auto-save-interval", 120);

        autoSaveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                for (Map.Entry<UUID, PlayerDatabase> entry : workingDatabase.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    ServerPlayerEntity player = FabricMod.getServer().getPlayerManager().getPlayer(playerUuid);
                    if (player != null) {
                        PlayerDatabase database = entry.getValue();
                        if (database != null) {
                            PlayerInventory inventory = player.getInventory();

                            NbtCompound data = new NbtCompound();

                            for (int i = 0; i < inventory.size(); i++) {
                                ItemStack itemStack = inventory.getStack(i);

                                NbtCompound emptyCompound = new NbtCompound();
                                NbtCompound itemCompound = itemStack.writeNbt(emptyCompound);

                                data.put(String.valueOf(i), itemCompound);
                            }

                            try {
                                database.putInventoryNbt(data);
                            }
                            catch (Exception e) {
                                player.sendMessage(Text.literal("您的背包保存失败，若您看到该消息，请立即联系管理员查看后台报错！"));
                                Logger.warn(player.getName().getString() + "的背包保存失败，以下是错误的堆栈信息:");
                                e.printStackTrace();
                            }
                        }
                    }
                }

                try {
                    Thread.sleep(autoSaveInterval * 1000L);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "InvSync-AutoSave");
    }

    @Override
    public void onDisable() {
        if (autoSaveThread != null) {
            autoSaveThread.interrupt();
        }

        Logger.info("跨服背包Mod已关闭，有缘再见！");
    }
}
