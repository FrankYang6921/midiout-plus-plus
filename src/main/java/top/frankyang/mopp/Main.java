package top.frankyang.mopp;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import javax.sound.midi.*;
import javax.sound.midi.MidiDevice.Info;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

import static com.mojang.brigadier.arguments.StringArgumentType.*;

public final class Main implements ClientModInitializer {
    private static final int MAJOR_VERSION = 0;
    private static final int MINOR_VERSION = 2;
    private static final int REVISION = 3;
    private static final MidiDevice virtualMidi;
    private static final Synthesizer virtualSynth;
    private static final Info virtualInfo;
    private static Sequencer sequencer;
    private static MidiDevice midiDevice;
    private static Receiver midiReceiver;
    private static Soundbank soundBank;

    static {
        virtualMidi = getMidiDeviceByName("Gervill");
        virtualSynth = (Synthesizer) Objects.requireNonNull(virtualMidi);
        virtualInfo = Objects.requireNonNull(virtualMidi).getDeviceInfo();
    }

    private static short mapShortMessageStat(String data) {
        try {
            return Short.parseShort(data);
        } catch (NumberFormatException e) {
            Class<ShortMessage> clazz = ShortMessage.class;
            try {
                return (short) clazz.getField(data).getInt(new ShortMessage());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                throw new IllegalArgumentException();  // Neither a number nor parsed
            }
        }
    }

    private static short mapSysExMessageStat(String data) {
        try {
            return Short.parseShort(data);
        } catch (NumberFormatException e) {
            Class<SysexMessage> clazz = SysexMessage.class;
            try {
                return (short) clazz.getField(data).getInt(new SysexMessage());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                throw new IllegalArgumentException();  // Neither a number nor parsed
            }
        }
    }

    private static String getMidiDeviceInfoString(Info info) {
        return String.format(
                "友好名称：%s；\n设备描述：%s；\n制造商：%s；版本号：%s。\n\n", info.getName(), info.getVendor(), info.getDescription(), info.getVersion()
        );
    }

    private static MidiDevice getMidiDeviceByInfo(Info info) {
        try {
            return info.equals(virtualInfo) ? virtualMidi : MidiSystem.getMidiDevice(info);
        } catch (Exception e) {
            return null;
        }
    }

    private static MidiDevice getMidiDeviceByName(String name) {
        try {
            return name.equals("Gervill") ? virtualMidi : getRawMidiDeviceByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static MidiDevice getRawMidiDeviceByName(String name) {
        Info[] info = MidiSystem.getMidiDeviceInfo();

        for (Info piece : info) {
            if (!piece.getName().equals(name)) {
                continue;  // Ignores other MIDI devices.
            }
            return getMidiDeviceByInfo(piece);
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static void sendRawMidiMessage(MidiMessage msg) {
        Objects.requireNonNull(midiReceiver).send(msg, -1);
    }

    private static void sendRawMidiMessage(String bytesString) {
        sendRawMidiMessage(new LooseMessage(
                Base64.getDecoder().decode(bytesString)  // Accepts BASE64
        ));
    }

    private static String[] devicePreSendProc(CommandContext<ServerCommandSource> context) throws IllegalArgumentException {
        String dataString = getString(context, "data");

        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendError(new LiteralText("尚未选择或初始化MIDI设备，因此无法发送消息。"));
            throw new IllegalArgumentException();
        }
        if (dataString.trim().isEmpty()) {
            context.getSource().sendError(new LiteralText("消息（BASE64）不可为空，且不可仅包含空白字符。"));
            throw new IllegalArgumentException();
        }

        return dataString.split("\\s+");
    }

    private static int playerPlay(CommandContext<ServerCommandSource> context) {
        if (sequencer != null && sequencer.isRunning()) {
            context.getSource().sendError(new LiteralText("上一个MIDI尚未结束或被停止。"));
            return 1;
        }
        try {
            Sequence sequence = MidiSystem.getSequence(new File(getString(context, "path")));
            sequencer = MidiSystem.getSequencer();
            if (sequencer == null) {
                context.getSource().sendError(new LiteralText("MIDI设备无效。"));
                return 1;
            }
            sequencer.setSequence(sequence);
            sequencer.open();
            sequencer.start();
        } catch (IOException e) {
            context.getSource().sendError(new LiteralText("无法打开MIDI文件。"));
            return 1;
        } catch (InvalidMidiDataException e) {
            context.getSource().sendError(new LiteralText("无法解析MIDI文件。"));
            return 1;
        } catch (MidiUnavailableException e) {
            context.getSource().sendError(new LiteralText("MIDI设备正忙。"));
            return 1;
        }

        context.getSource().sendFeedback(new LiteralText("已经开始播放这个MIDI。"), false);

        return 1;
    }

    private static int playerStop(CommandContext<ServerCommandSource> context) {
        if (sequencer == null || !sequencer.isRunning()) {
            context.getSource().sendError(new LiteralText("上一个MIDI已经结束或被停止。"));
            return 1;
        }

        sequencer.stop();
        sequencer.close();
        context.getSource().sendFeedback(new LiteralText("已经停止播放上个MIDI。"), false);

        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText(
                String.format(
                        "§e§lMIDIOut Plus Plus§r v%d.%d.%d 是§9§nkworker§r制作的的自由软件。遵循GPLv3协议。", MAJOR_VERSION, MINOR_VERSION, REVISION
                )
        ), false);
        return 1;
    }

    private static int deviceList(CommandContext<ServerCommandSource> context) {
        Info[] info = MidiSystem.getMidiDeviceInfo();
        StringBuilder feedback = new StringBuilder();

        for (Info piece : info) {
            feedback.append(getMidiDeviceInfoString(piece));
        }
        context.getSource().sendFeedback(new LiteralText(feedback.toString().trim()), false);

        return 1;
    }

    private static int devicePanic(CommandContext<ServerCommandSource> context) {
        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendError(new LiteralText("尚未选择或初始化MIDI设备，因此无法复位。"));
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

    private static int deviceReset(CommandContext<ServerCommandSource> context) {
        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendError(new LiteralText("尚未选择或初始化MIDI设备，因此无法重置。"));
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

    private static int deviceSelect(CommandContext<ServerCommandSource> context) {
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
            context.getSource().sendError(new LiteralText("MIDI设备选择失败。不是有效的MIDI设备。"));
            return 1;
        } else if (deviceName.equals(".")) {
            context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备选择成功。现在已选择“%s”。", "Gervill")), false);
        } else {
            context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备选择成功。现在已选择“%s”。", deviceName)), false);
        }

        try {
            midiDevice.open();
            midiReceiver = midiDevice.getReceiver();
        } catch (MidiUnavailableException e) {
            context.getSource().sendError(new LiteralText("MIDI设备选择失败。不是有效的MIDI设备。"));
            return 1;
        }

        if (deviceName.equals(".")) {
            context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备初始化成功。现在正使用“%s”。", "Gervill")), false);
        } else {
            context.getSource().sendFeedback(new LiteralText(String.format("MIDI设备初始化成功。现在正使用“%s”。", deviceName)), false);
        }

        return 1;
    }

    private static int deviceRawSend(CommandContext<ServerCommandSource> context) {
        String bytesString = getString(context, "bytes");

        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendError(new LiteralText("尚未选择或初始化MIDI设备，因此无法发送消息。"));
            return 1;
        }
        if (bytesString.isEmpty()) {
            context.getSource().sendError(new LiteralText("消息不可为空。"));
            return 1;
        }

        sendRawMidiMessage(bytesString);

        context.getSource().sendFeedback(new LiteralText("发送了原生消息。"), false);
        return 1;
    }

    private static int deviceShortSend(CommandContext<ServerCommandSource> context) {
        String[] data;
        try {
            data = devicePreSendProc(context);
        } catch (IllegalArgumentException e) {
            return 1;
        }

        if (data.length == 0) {
            context.getSource().sendError(new LiteralText("一般消息至少接受一个参数。"));
            return 1;
        }
        if (data.length > 4) {
            context.getSource().sendError(new LiteralText("一般消息至多接受四个参数。"));
            return 1;
        }
        if (data.length == 1) {
            try {
                short a = mapShortMessageStat(data[0]);
                midiReceiver.send(new ShortMessage(a), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                context.getSource().sendError(new LiteralText("参数不合法。"));
                return 1;
            }
        } else if (data.length == 2) {
            context.getSource().sendError(new LiteralText("一般消息不可接受两个参数。"));
            return 1;
        } else if (data.length == 3) {
            try {
                short a = mapShortMessageStat(data[0]);
                short b = Short.parseShort(data[1]);
                short c = Short.parseShort(data[2]);
                midiReceiver.send(new ShortMessage(a, b, c), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                e.printStackTrace();
                context.getSource().sendError(new LiteralText("参数不合法。"));
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
                context.getSource().sendError(new LiteralText("参数不合法。"));
                return 1;
            }
        }
        context.getSource().sendFeedback(new LiteralText("发送了一般消息。"), false);

        return 1;
    }

    private static int deviceSysExSend(CommandContext<ServerCommandSource> context) {
        String[] data;
        try {
            data = devicePreSendProc(context);
        } catch (IllegalArgumentException e) {
            return 1;
        }

        if (data.length < 2) {
            context.getSource().sendError(new LiteralText("系统消息至少接受两个参数。"));
            return 1;
        }
        if (data.length > 3) {
            context.getSource().sendError(new LiteralText("系统消息至多接受三个参数。"));
            return 1;
        }

        if (data.length == 2) {
            try {
                byte[] a = Base64.getDecoder().decode(data[0]);
                short b = Short.parseShort(data[1]);
                midiReceiver.send(new SysexMessage(a, b), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                context.getSource().sendError(new LiteralText("参数不合法。"));
                return 1;
            }
        } else {
            try {
                short a = mapSysExMessageStat(data[0]);
                byte[] b = Base64.getDecoder().decode(data[1]);
                short c = Short.parseShort(data[2]);
                midiReceiver.send(new SysexMessage(a, b, c), -1);
            } catch (IllegalArgumentException | InvalidMidiDataException e) {
                context.getSource().sendError(new LiteralText("参数不合法。"));
                return 1;
            }
        }

        context.getSource().sendFeedback(new LiteralText("发送了一般消息。"), false);
        return 1;
    }

    private static int deviceShow(CommandContext<ServerCommandSource> context) {
        if (midiDevice == null || midiReceiver == null) {
            context.getSource().sendError(new LiteralText("尚未选择或初始化MIDI设备，因此无法查看。"));
            return 1;
        }

        Info piece = midiDevice.getDeviceInfo();
        context.getSource().sendFeedback(new LiteralText(getMidiDeviceInfoString(piece).trim()), false);

        return 1;
    }

    private static int vDevSF2Load(CommandContext<ServerCommandSource> context) {
        try {
            soundBank = MidiSystem.getSoundbank(new File(getString(context, "path")));
        } catch (IOException e) {
            context.getSource().sendError(new LiteralText("无法打开SF2文件。"));
            return 1;
        } catch (InvalidMidiDataException e) {
            context.getSource().sendError(new LiteralText("无法解析SF2文件。"));
            return 1;
        }

        if (virtualSynth == null) {
            context.getSource().sendError(new LiteralText("没有有效的虚拟MIDI设备。请重新安装JRE（或JDK）和本模组。"));
            return 1;
        }
        virtualSynth.loadAllInstruments(soundBank);

        context.getSource().sendFeedback(new LiteralText("SF2音色库加载成功。"), false);

        return 1;
    }

    private static int vDevSF2Reload(CommandContext<ServerCommandSource> context) {
        if (soundBank == null) {
            context.getSource().sendError(new LiteralText("尚未加载SF2音色库，因此无法重新加载。"));
            return 1;
        }

        if (virtualSynth == null) {
            context.getSource().sendError(new LiteralText("没有有效的虚拟MIDI设备。请重新安装JRE（或JDK）和本模组。"));
            return 1;
        }
        virtualSynth.unloadAllInstruments(soundBank);
        virtualSynth.loadAllInstruments(soundBank);

        context.getSource().sendFeedback(new LiteralText("SF2音色库重新加载成功。"), false);

        return 1;
    }

    private static int vDevSF2Unload(CommandContext<ServerCommandSource> context) {
        if (soundBank == null) {
            context.getSource().sendError(new LiteralText("尚未加载SF2音色库，因此无法卸载。"));
            return 1;
        }

        if (virtualSynth == null) {
            context.getSource().sendError(new LiteralText("没有有效的虚拟MIDI设备。请重新安装JRE（或JDK）和本模组。"));
            return 1;
        }
        virtualSynth.unloadAllInstruments(soundBank);
        soundBank = null;

        context.getSource().sendFeedback(new LiteralText("SF2音色库卸载成功。"), false);

        return 1;
    }

    @Override
    public void onInitializeClient() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("about").executes(Main::about)));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("player").then(CommandManager.literal("play").then(CommandManager.argument("path", string()).executes(Main::playerPlay)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("player").then(CommandManager.literal("stop").executes(Main::playerStop))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("list").executes(Main::deviceList))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("panic").executes(Main::devicePanic))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("reset").executes(Main::deviceReset))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("select").then(CommandManager.argument("name", string()).executes(Main::deviceSelect)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("send").then(CommandManager.literal("raw").then(CommandManager.argument("bytes", string()).executes(Main::deviceRawSend))))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("send").then(CommandManager.literal("short").then(CommandManager.argument("data", greedyString()).executes(Main::deviceShortSend))))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("send").then(CommandManager.literal("sysex").then(CommandManager.argument("data", greedyString()).executes(Main::deviceSysExSend))))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("device").then(CommandManager.literal("show").executes(Main::deviceShow))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("vdev").then(CommandManager.literal("load").then(CommandManager.argument("path", string()).executes(Main::vDevSF2Load)))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("vdev").then(CommandManager.literal("reload").executes(Main::vDevSF2Reload))));
            dispatcher.register(CommandManager.literal("mopp").then(CommandManager.literal("vdev").then(CommandManager.literal("unload").executes(Main::vDevSF2Unload))));
        });
    }

    private static class LooseMessage extends ShortMessage {
        public LooseMessage(byte[] data) {
            super(data);  // Changed from protected to public.
        }
    }
}
