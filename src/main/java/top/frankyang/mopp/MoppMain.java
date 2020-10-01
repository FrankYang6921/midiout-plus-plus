package top.frankyang.mopp;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;


public class MoppMain implements ModInitializer {
    private static final int MAJOR_VERSION = 0;
    private static final int MINOR_VERSION = 1;
    private static final int REVISION = 1;

    private Sequencer sequencer;
    private MidiDevice midiDevice;
    private Receiver midiReceiver;


    private MidiDevice tryGetMidiDevice(MidiDevice.Info info) {
        try {
            return MidiSystem.getMidiDevice(info);
        } catch (MidiUnavailableException ignored) {
        }
        return null;
    }

    private void sendMidiMessage(String bytesString) {
        byte[] data = Base64.getDecoder().decode(bytesString);
        midiReceiver.send(new LooseMessage(data), -1);
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("about").executes(this::about)));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("player").then(CommandManager.literal("play").then(CommandManager.argument("path", string()).executes(this::playerPlay)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("player").then(CommandManager.literal("stop").executes(this::playerStop))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("get").executes(this::deviceGet))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("set").then(CommandManager.argument("name", string()).executes(this::deviceSet)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("reset").executes(this::deviceReset))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("panic").executes(this::devicePanic))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("send").then(CommandManager.argument("bytes", string()).executes(this::deviceSend)))));
        });
    }

    private int playerPlay(CommandContext<ServerCommandSource> context) {
        if (sequencer != null && sequencer.isRunning()) {
            context.getSource().sendFeedback(new LiteralText("§c上一个MIDI尚未结束或被停止。"), false);
            return 1;
        }
        try {
            Sequence sequence = MidiSystem.getSequence(new File(getString(context, "path")));
            sequencer = MidiSystem.getSequencer();
            if (sequencer == null) {
                context.getSource().sendFeedback(new LiteralText("§cMIDI设备无效。"), false);
                return 1;
            }
            sequencer.setSequence(sequence);
            sequencer.open();
            sequencer.start();
            context.getSource().sendFeedback(new LiteralText("已经开始播放这个MIDI。"), false);
        } catch (IOException e) {
            context.getSource().sendFeedback(new LiteralText("§c无法打开MIDI文件。"), false);
        } catch (InvalidMidiDataException e) {
            context.getSource().sendFeedback(new LiteralText("§c无法解析MIDI文件。"), false);
        } catch (MidiUnavailableException e) {
            context.getSource().sendFeedback(new LiteralText("§cMIDI设备正忙。"), false);
        }

        return 1;
    }

    private int playerStop(CommandContext<ServerCommandSource> context) {
        if (sequencer == null || !sequencer.isRunning()) {
            context.getSource().sendFeedback(new LiteralText("§c上一个MIDI已经结束或被停止。"), false);
            return 1;
        }

        sequencer.stop();
        sequencer.close();
        context.getSource().sendFeedback(new LiteralText("已经停止播放上个MIDI。"), false);

        return 1;
    }

    private int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText(
                String.format(
                        "§e§lMIDIOut++§r v%d.%d.%d 是§9§nkworker§r制作的的自由软件。遵循GPLv3协议。\n\n", MAJOR_VERSION, MINOR_VERSION, REVISION
                ) +
                        "§e§l集气瓶的FAQ：§r\n" +
                        "§eQ1§r：“遵循GPLv3协议的自由软件”是什么意思？\n" +
                        "§eA1§r：这意味着你有自由按自己的意愿使用软件，有自由按自己的需要修改软件，有自由把软件分享给友邻，以及有自由分享自己对软件的修改。简而言之，你可以用它做任何事，只须遵守§oGPLv3§r的条款。\n\n" +
                        "§eQ2§r：我使用本软件制作的作品属于红石音乐吗？\n" +
                        "§eA2§r：看情况。如果你使用本软件中的§oSoundFont™§r驱动功能，则你的作品是红石音乐。如果你使用本软件中的MIDI设备连接功能，则你的作品只是黑石音乐。我永远推荐你使用§oSoundFont™§r驱动。"
        ), false);

        return 1;
    }

    private int deviceGet(CommandContext<ServerCommandSource> context) {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        StringBuilder feedback = new StringBuilder();
        for (int i = 0; i < info.length; i++) {
            MidiDevice.Info piece = info[i];
            feedback.append(
                    String.format("§eMIDI设备[%d]：§r%s\n§e制造商§r：§9§n%s§r。\n§e详细信息：§r%s。\n\n", i, piece.getName(), piece.getVendor(), piece.getDescription())
            );
        }
        context.getSource().sendFeedback(new LiteralText(feedback.toString().trim()), false);

        return 1;
    }


    private int deviceSet(CommandContext<ServerCommandSource> context) {
        String deviceName = getString(context, "name");
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

        if (midiReceiver != null) {
            midiReceiver.close();
        }
        if (midiDevice != null) {
            midiDevice.close();
        }

        midiDevice = null;
        midiReceiver = null;
        for (MidiDevice.Info piece : info) {
            if (!deviceName.equals(piece.getName())) {
                continue;  // Ignores other MIDI devices.
            }
            midiDevice = tryGetMidiDevice(piece);
        }

        if (midiDevice == null) {
            if (info.length == 0) {
                context.getSource().sendFeedback(new LiteralText("§cMIDI设备选择失败。没有有效的MIDI设备。"), false);
                return 1;
            } else {
                deviceName = info[0].getName();  // Default
                midiDevice = tryGetMidiDevice(info[0]);
                context.getSource().sendFeedback(new LiteralText(String.format("§cMIDI设备选择失败。现在已选择“%s”", deviceName)), false);
            }
        } else {
            context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备选择成功。现在已选择“%s”。", deviceName)), false);
        }

        try {
            midiDevice.open();
            midiReceiver = midiDevice.getReceiver();
        } catch (MidiUnavailableException e) {
            context.getSource().sendFeedback(new LiteralText("§cMIDI设备初始化失败。没有有效的MIDI设备。"), false);
            return 1;
        }
        context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备初始化成功。现在正使用“%s”。", deviceName)), false);

        return 1;
    }


    private int deviceReset(CommandContext<ServerCommandSource> context) {
        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未选择或初始化MIDI设备，因此无法重置。"), false);
            return 1;
        }

        try {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 128; j++) {
                    midiReceiver.send(new ShortMessage(ShortMessage.NOTE_OFF, i, j, 0), -1);
                }
                midiReceiver.send(new ShortMessage(ShortMessage.PROGRAM_CHANGE, i, 0, 0), -1);
            }
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }

        context.getSource().sendFeedback(new LiteralText("重置了MIDI设备。"), false);
        return 1;
    }

    private int devicePanic(CommandContext<ServerCommandSource> context) {
        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未选择或初始化MIDI设备，因此无法复位。"), false);
            return 1;
        }

        try {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 128; j++) {
                    midiReceiver.send(new ShortMessage(ShortMessage.NOTE_OFF, i, j, 0), -1);
                }
            }
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }

        context.getSource().sendFeedback(new LiteralText("复位了MIDI设备。"), false);
        return 1;
    }


    private int deviceSend(CommandContext<ServerCommandSource> context) {
        String bytesString = getString(context, "bytes");

        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未选择或初始化MIDI设备，因此无法发送。"), false);
            return 1;
        }
        if (bytesString.isEmpty()) {
            context.getSource().sendFeedback(new LiteralText("§c消息不可为空。"), false);
            return 1;
        }

        sendMidiMessage(bytesString);

        context.getSource().sendFeedback(new LiteralText("发送了消息。"), false);
        return 1;
    }


    private static class LooseMessage extends ShortMessage {
        public LooseMessage(byte[] data) {
            super(data);  // Switched from protected to public.
        }
    }
}
