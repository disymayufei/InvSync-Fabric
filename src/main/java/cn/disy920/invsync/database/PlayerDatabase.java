package cn.disy920.invsync.database;

import cn.disy920.invsync.exception.DatabaseAlreadyClosedException;
import cn.disy920.invsync.exception.DatabaseCreateException;
import cn.disy920.invsync.exception.DatabaseReadException;
import cn.disy920.invsync.exception.DatabaseWriteException;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static cn.disy920.invsync.Main.MOD_INSTANCE;

public class PlayerDatabase {
    private DB database;
    private final File dbDir;
    private final UUID playerUuid;

    public PlayerDatabase(UUID uuid) throws DatabaseCreateException, DatabaseReadException {
        this.playerUuid = uuid;

        Options options = new Options();
        options.createIfMissing(true);

        dbDir = new File(MOD_INSTANCE.getConfig().getString("database-path") + "/Players/" + playerUuid.toString());

        if (!dbDir.exists() || dbDir.isFile()) {
            if (!dbDir.mkdirs()) {
                throw new DatabaseCreateException("数据库文件夹创建失败！");
            }
        }

        DBFactory factory = new Iq80DBFactory();

        try {
            database = factory.open(dbDir, options);
        } catch (Exception e) {
            throw new DatabaseReadException("数据库初始化失败，可能是未获得锁", e);
        }
    }

    @Nullable
    public NbtCompound getInventoryNbt() throws DatabaseReadException, DatabaseAlreadyClosedException {
        if (database == null) {
            throw new DatabaseAlreadyClosedException("数据库已关闭");
        }

        byte[] rawData = database.get(toByte("inventory"));
        if (rawData == null) {
            return null;
        }

        InputStream byteStream = new ByteArrayInputStream(rawData);

        try {
            return NbtIo.readCompressed(byteStream);
        } catch (Exception e) {
            throw new DatabaseReadException("背包数据读取数据库发生错误", e);
        }
    }

    public void putInventoryNbt(NbtCompound nbt) throws DatabaseWriteException, DatabaseAlreadyClosedException {

        if (database == null) {
            throw new DatabaseAlreadyClosedException("数据库已关闭");
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(nbt, byteStream);
            database.put(toByte("inventory"), byteStream.toByteArray());
        } catch (IOException e) {
            throw new DatabaseWriteException("背包数据写入数据库发生错误", e);
        }
    }

    public void close() {
        if (database != null) {
            try {
                database.close();
            } catch (IOException ignored) {}
            database = null;
        }
    }

    /**
     * 将玩家背包清空前的数据的NBT创建字符串形式的备份，以防止数据库损坏导致的背包永久丢失
     * @param inventory 玩家背包
     */
    public void makeBackupLog(PlayerInventory inventory) {
        if (inventory.isEmpty()) {
            return;
        }

        NbtCompound data = new NbtCompound();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);

            NbtCompound emptyCompound = new NbtCompound();
            NbtCompound itemCompound = itemStack.writeNbt(emptyCompound);

            data.put(String.valueOf(i), itemCompound);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        File logFile = new File(dbDir, "log/" + sdf.format(new Date()) + " backup.log");
        try {
            if (!logFile.exists() || logFile.isDirectory()) {
                logFile.createNewFile();
            }

            Files.writeString(logFile.toPath(), data.asString());
        }
        catch (Exception ignored){}
    }

    public void markError(Throwable throwable) {
        File errorFile = new File(dbDir, "error.log");
        if (!errorFile.exists() || errorFile.isDirectory()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            try {
                errorFile.createNewFile();
                StringBuilder errorLog = new StringBuilder("Error Log\n================");
                errorLog.append("\nServer Name:");
                errorLog.append(MOD_INSTANCE.getConfig().getString("server-name"));
                errorLog.append("\nTime:");
                errorLog.append(sdf.format(new Date()));
                errorLog.append("\nClass: ");
                errorLog.append(throwable.getClass());
                errorLog.append("\nMessage: ");
                errorLog.append(throwable.getMessage());
                errorLog.append("\nCause: ");
                errorLog.append(throwable.getCause());
                errorLog.append("\nStackTrance: ");
                for (StackTraceElement element : throwable.getStackTrace()) {
                    errorLog.append("\n");
                    errorLog.append(element.toString());
                }

                Files.writeString(errorFile.toPath(), errorLog.toString());
            }
            catch (Exception ignored) {}
        }
    }

    public boolean isError() {
        File errorFile = new File(dbDir, "error.log");
        return errorFile.exists() && errorFile.isFile();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    private byte[] toByte(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
