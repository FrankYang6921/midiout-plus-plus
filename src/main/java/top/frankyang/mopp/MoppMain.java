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
import java.util.Objects;

import static com.mojang.brigadier.arguments.StringArgumentType.*;

public class MoppMain implements ModInitializer {
    private static final int MAJOR_VERSION = 0;
    private static final int MINOR_VERSION = 2;
    private static final int REVISION = 2;
    private static final MidiDevice virtualMidi;
    private static final Synthesizer virtualSynth;
    private static final MidiDevice.Info virtualInfo;
    private static Sequencer sequencer;
    private static MidiDevice midiDevice;
    private static Receiver midiReceiver;
    private static Soundbank soundBank;

    static {
        virtualMidi = getMidiDeviceByRawName("Gervill");
        virtualSynth = (Synthesizer) Objects.requireNonNull(virtualMidi);
        virtualInfo = Objects.requireNonNull(virtualMidi).getDeviceInfo();
    }

    private static short mapShortMessageStat(String data) {
        try {
            return Short.parseShort(data);
        } catch (NumberFormatException e) {
            Class<ShortMessage> klass = ShortMessage.class;
            try {
                return (short) klass.getField(data).getInt(new ShortMessage());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                throw new IllegalArgumentException();  // Neither a number nor parsed
            }
        }
    }

    private static short mapSysExMessageStat(String data) {
        try {
            return Short.parseShort(data);
        } catch (NumberFormatException e) {
            Class<SysexMessage> klass = SysexMessage.class;
            try {
                return (short) klass.getField(data).getInt(new SysexMessage());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                throw new IllegalArgumentException();  // Neither a number nor parsed
            }
        }
    }

    private static String showMidiDeviceInfo(MidiDevice.Info info) {
        return String.format(
                "§e友好名称：§r%s\n§e制造商§r：§9§n%s§r。\n§e设备描述：§r%s。\n\n", info.getName(), info.getVendor(), info.getDescription()
        );
    }

    private static MidiDevice getMidiDeviceByInfo(MidiDevice.Info info) {
        try {
            return info.equals(virtualInfo) ? virtualMidi : MidiSystem.getMidiDevice(info);
        } catch (Exception e) {
            return null;
        }
    }

    private static MidiDevice getMidiDeviceByName(String name) {
        try {
            return name.equals("Gervill") ? virtualMidi : getMidiDeviceByRawName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static MidiDevice getMidiDeviceByRawName(String name) {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info piece : info) {
            if (!piece.getName().equals(name)) {
                continue;  // Ignores other MIDI devices.
            }
            return getMidiDeviceByInfo(piece);
        }
        return null;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("about").executes(this::about)));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("player").then(CommandManager.literal("play").then(CommandManager.argument("path", string()).executes(this::playerPlay)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("player").then(CommandManager.literal("stop").executes(this::playerStop))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("list").executes(this::deviceList))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("panic").executes(this::devicePanic))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("reset").executes(this::deviceReset))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("select").then(CommandManager.argument("name", string()).executes(this::deviceSelect)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("send").then(CommandManager.literal("raw").then(CommandManager.argument("bytes", string()).executes(this::deviceRawSend))))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("send").then(CommandManager.literal("short").then(CommandManager.argument("data", greedyString()).executes(this::deviceShortSend))))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("send").then(CommandManager.literal("sysex").then(CommandManager.argument("data", greedyString()).executes(this::deviceSysExSend))))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("show").executes(this::deviceShow))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("vdev").then(CommandManager.literal("load").then(CommandManager.argument("path", string()).executes(this::vDevSF2Load)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("vdev").then(CommandManager.literal("reload").executes(this::vDevSF2Reload))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("vdev").then(CommandManager.literal("unload").executes(this::vDevSF2Unload))));
        });
    }

    private void sendRawMidiMessage(String bytesString) {
        byte[] data = Base64.getDecoder().decode(bytesString);
        midiReceiver.send(new LooseMessage(data), -1);
    }

    private String[] devicePreSendProc(CommandContext<ServerCommandSource> context) throws IllegalArgumentException {
        String dataString = getString(context, "data");

        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未选择或初始化MIDI设备，因此无法发送。"), false);
            throw new IllegalArgumentException();
        }
        if (dataString.isEmpty()) {
            context.getSource().sendFeedback(new LiteralText("§c消息不可为空。"), false);
            throw new IllegalArgumentException();
        }

        return dataString.split("\\s+");
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
        } catch (IOException e) {
            context.getSource().sendFeedback(new LiteralText("§c无法打开MIDI文件。"), false);
            return 1;
        } catch (InvalidMidiDataException e) {
            context.getSource().sendFeedback(new LiteralText("§c无法解析MIDI文件。"), false);
            return 1;
        } catch (MidiUnavailableException e) {
            context.getSource().sendFeedback(new LiteralText("§cMIDI设备正忙。"), false);
            return 1;
        }

        context.getSource().sendFeedback(new LiteralText("已经开始播放这个MIDI。"), false);

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
                        "§e§lMIDIOut++§r v%d.%d.%d 是§9§nkworker§r制作的的自由软件。遵循GPLv3协议。", MAJOR_VERSION, MINOR_VERSION, REVISION
                )
        ), false);

        return 1;
    }

    private int deviceList(CommandContext<ServerCommandSource> context) {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        StringBuilder feedback = new StringBuilder();

        for (MidiDevice.Info piece : info) {
            feedback.append(showMidiDeviceInfo(piece));
        }
        context.getSource().sendFeedback(new LiteralText(feedback.toString().trim()), false);

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

    private int deviceSelect(CommandContext<ServerCommandSource> context) {
        String deviceName = getString(context, "name");

        if (midiReceiver != null) {
            midiReceiver.close();
        }
        if (midiDevice != null) {
            midiDevice.close();
        }

        midiDevice = null;
        midiReceiver = null;
        if (deviceName.equals(".")) {
            midiDevice = getMidiDeviceByName("Gervill");
        } else {
            midiDevice = getMidiDeviceByName(deviceName);
        }


        if (midiDevice == null) {
            context.getSource().sendFeedback(new LiteralText("§cMIDI设备选择失败。不是有效的MIDI设备。"), false);
            context.getSource().sendFeedback(new LiteralText("§3*你可以键入'/mopp device list'来获取有效的MIDI设备列表。"), false);
            return 1;
        } else if (deviceName.equals(".")) {
            context.getSource().sendFeedback(new LiteralText("MIDI设备选择成功。现在已选择“Gervill”。"), false);
        } else {
            context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备选择成功。现在已选择“%s”。", deviceName)), false);
        }

        try {
            midiDevice.open();
            midiReceiver = midiDevice.getReceiver();
        } catch (MidiUnavailableException e) {
            context.getSource().sendFeedback(new LiteralText("§cMIDI设备初始化失败。不是有效的MIDI设备。"), false);
            context.getSource().sendFeedback(new LiteralText("§3*你可以键入'/mopp device list'来获取有效的MIDI设备列表。"), false);
            return 1;
        }
        if (deviceName.equals(".")) {
            context.getSource().sendFeedback(new LiteralText("MIDI设备初始化成功。现在正使用“Gervill”。"), false);
        } else {
            context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备初始化成功。现在正使用“%s”。", deviceName)), false);
        }
        if (midiDevice.getDeviceInfo().getName().equals("Gervill") && soundBank == null) {  // SF2 loaded hint
            context.getSource().sendFeedback(new LiteralText("§3* 你未加载SF2音色库，但选择了虚拟MIDI设备（不推荐）。"), false);
        }

        return 1;
    }

    private int deviceRawSend(CommandContext<ServerCommandSource> context) {
        String bytesString = getString(context, "bytes");

        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未选择或初始化MIDI设备，因此无法发送。"), false);
            return 1;
        }
        if (bytesString.isEmpty()) {
            context.getSource().sendFeedback(new LiteralText("§c消息不可为空。"), false);
            return 1;
        }

        sendRawMidiMessage(bytesString);

        context.getSource().sendFeedback(new LiteralText("发送了原生消息。"), false);
        return 1;
    }

    private int deviceShortSend(CommandContext<ServerCommandSource> context) {
        String[] data;
        try {
            data = devicePreSendProc(context);
        } catch (IllegalArgumentException e) {
            return 1;
        }

        if (data.length == 0) {
            context.getSource().sendFeedback(new LiteralText("§c一般消息至少接受一个参数。"), false);
            return 1;
        }
        if (data.length > 4) {
            context.getSource().sendFeedback(new LiteralText("§c一般消息至多接受四个参数。"), false);
            return 1;
        }
        if (data.length == 1) {
            try {
                short a = mapShortMessageStat(data[0]);
                midiReceiver.send(new ShortMessage(a), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                context.getSource().sendFeedback(new LiteralText("§c参数不合法。"), false);
                return 1;
            }
        } else if (data.length == 2) {
            context.getSource().sendFeedback(new LiteralText("§c一般消息不可接受两个参数。"), false);
            return 1;
        } else if (data.length == 3) {
            try {
                short a = mapShortMessageStat(data[0]);
                short b = Short.parseShort(data[1]);
                short c = Short.parseShort(data[2]);
                midiReceiver.send(new ShortMessage(a, b, c), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                e.printStackTrace();
                context.getSource().sendFeedback(new LiteralText("§c参数不合法。"), false);
                return 1;
            }
        } else {
            try {
                short a = mapShortMessageStat(data[0]);
                short b = Short.parseShort(data[1]);
                short c = Short.parseShort(data[2]);
                short d = Short.parseShort(data[3]);
                midiReceiver.send(new ShortMessage(a, b, c, d), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                context.getSource().sendFeedback(new LiteralText("§c参数不合法。"), false);
                return 1;
            }
        }
        context.getSource().sendFeedback(new LiteralText("发送了一般消息。"), false);

        return 1;
    }

    private int deviceSysExSend(CommandContext<ServerCommandSource> context) {
        String[] data;
        try {
            data = devicePreSendProc(context);
        } catch (IllegalArgumentException e) {
            return 1;
        }

        if (data.length < 2) {
            context.getSource().sendFeedback(new LiteralText("§c系统消息至少接受两个参数。"), false);
            return 1;
        }
        if (data.length > 3) {
            context.getSource().sendFeedback(new LiteralText("§c系统消息至多接受三个参数。"), false);
            return 1;
        }

        if (data.length == 2) {
            try {
                byte[] a = Base64.getDecoder().decode(data[0]);
                short b = Short.parseShort(data[1]);
                midiReceiver.send(new SysexMessage(a, b), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                context.getSource().sendFeedback(new LiteralText("§c参数不合法。"), false);
                return 1;
            }
        } else {
            try {
                short a = mapSysExMessageStat(data[0]);
                byte[] b = Base64.getDecoder().decode(data[1]);
                short c = Short.parseShort(data[2]);
                midiReceiver.send(new SysexMessage(a, b, c), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                context.getSource().sendFeedback(new LiteralText("§c参数不合法。"), false);
                return 1;
            }
        }

        context.getSource().sendFeedback(new LiteralText("发送了一般消息。"), false);
        return 1;
    }

    private int deviceShow(CommandContext<ServerCommandSource> context) {
        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未选择或初始化MIDI设备，因此无法查看。"), false);
            return 1;
        }

        MidiDevice.Info piece = midiDevice.getDeviceInfo();
        context.getSource().sendFeedback(new LiteralText(showMidiDeviceInfo(piece).trim()), false);

        return 1;
    }

    private int vDevSF2Load(CommandContext<ServerCommandSource> context) {
        try {
            soundBank = MidiSystem.getSoundbank(new File(getString(context, "path")));
        } catch (IOException e) {
            context.getSource().sendFeedback(new LiteralText("§c无法打开SF2文件。"), false);
            return 1;
        } catch (InvalidMidiDataException e) {
            context.getSource().sendFeedback(new LiteralText("§c无法解析SF2文件。"), false);
            return 1;
        }

        if (virtualSynth == null) {
            context.getSource().sendFeedback(new LiteralText("§c没有有效的虚拟MIDI设备。请重新安装JRE（或JDK）和本模组。"), false);
            return 1;
        }
        virtualSynth.loadAllInstruments(soundBank);

        context.getSource().sendFeedback(new LiteralText("SF2音色库加载成功。"), false);
        if (midiDevice == null || !midiDevice.getDeviceInfo().getName().equals("Gervill")) {  // Internal device hint
            context.getSource().sendFeedback(new LiteralText("§3* 你加载了SF2音色库，但未选择虚拟MIDI设备（不推荐）。"), false);
        }

        return 1;
    }

    private int vDevSF2Reload(CommandContext<ServerCommandSource> context) {
        if (soundBank == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未加载SF2音色库，因此无法重新加载。"), false);
            return 1;
        }

        if (virtualSynth == null) {
            context.getSource().sendFeedback(new LiteralText("§c没有有效的虚拟MIDI设备。请重新安装JRE（或JDK）和本模组。"), false);
            return 1;
        }
        virtualSynth.unloadAllInstruments(soundBank);
        virtualSynth.loadAllInstruments(soundBank);

        context.getSource().sendFeedback(new LiteralText("SF2音色库重新加载成功。"), false);
        return 1;
    }

    private int vDevSF2Unload(CommandContext<ServerCommandSource> context) {
        if (soundBank == null) {
            context.getSource().sendFeedback(new LiteralText("§c尚未加载SF2音色库，因此无法卸载。"), false);
            return 1;
        }

        if (virtualSynth == null) {
            context.getSource().sendFeedback(new LiteralText("§c没有有效的虚拟MIDI设备。请重新安装JRE（或JDK）和本模组。"), false);
            return 1;
        }
        virtualSynth.unloadAllInstruments(soundBank);
        soundBank = null;

        context.getSource().sendFeedback(new LiteralText("SF2音色库卸载成功。"), false);
        if (midiDevice != null && midiDevice.getDeviceInfo().getName().equals("Gervill")) {  // Internal device hint
            context.getSource().sendFeedback(new LiteralText("§3* 你卸载了SF2音色库，但选择了虚拟MIDI设备（不推荐）。"), false);
        }
        return 1;
    }

    private static class LooseMessage extends ShortMessage {
        public LooseMessage(byte[] data) {
            super(data);  // Switched from protected to public.
        }
    }
}
