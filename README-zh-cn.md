# MIDIOut++;
```java
public class WhatIsThisThingLike {
    public var MOPP = new MidiOutPlusPlus();

    public void tell() {
        assert MOPP.author.equals("kworker");
        assert MOPP > new OriginalMidiOut();
        assert MOPP.isFast() && MOPP.isLight();
        assert MOPP.minecraftVersion.equals("1.16");
    }    
}
```

## 开始
要运行这个模组，你需要：
+ Minecraft 1.16.x：装有……
  + Fabric Loader 和 Fabric API

这个模组的目的是取代原版的MIDIOut，它需要MIDI设备或SoundFont™音色库。这个模组只支持Windows。

## 发行
使用这个链接来重定向到 [发行](https://github.com/FrankYang6921/midiout-/releases) 页。

## 教程
如果你想自己使用这个模组，那么下方的命令可能会帮到你。但是用 [MCDI](https://github.com/FrankYang6921/mcdi) 生成红石音乐更快也更简单。

+ 打印版本信息：`/mopp about`
+ 开始播放单个MIDI文件：`/mopp player play <path>`
+ 停止播放单个MIDI文件：`/mopp player stop`
+ 列出所有的MIDI设备：`/mopp device list`
+ 复位选定的MIDI设备：`/mopp device panic`
+ 重置选定的MIDI设备：`/mopp device reset`
+ 选择一个MIDI设备：`/mopp device select <name>`
    + 提示：用”.“作为设备名就可以选择默认设备。
+ 写入一个原生MIDI消息：`/mopp device send raw <bytes>`
    + 提示：这些字节应该用”base64“编码。
+ 发送一个一般MIDI消息：
    + `/mopp device send short <status>`
    + `/mopp device send short <status> <data1> <data2>`
    + `/mopp device send short <status> <channel> <data1> <data2>`
    + 提示：“<status>”既可以是整数，也可以是字符串。
+ 发送一个系统MIDI消息：
    + `/mopp device send sysex <data> <length>`
    + `/mopp device send sysex <status> <data> <length>`
+ 显示选定的MIDI设备：`/mopp device show`
+ 将 SoundFont™ 加载进内置MIDI设备“Gervill”： `/mopp vdev load <path>`
+ 将 SoundFont™ 重新加载进内置MIDI设备“Gervill”：`/mopp vdev reload`
+ 将 SoundFont™ 从内置MIDI设备“Gervill”卸载：`/mopp vdev unload`

如果要测试安装是否成功，您也可以尝试以下的3条命令：:
```
/mopp device select "Microsoft GS Wavetable Synth"
/mopp device send raw "kDx/" (或 /mopp device send short NOTE_ON 0 60 127)
/mopp device send raw "gDxA" (或 /mopp device send short NOTE_OFF 0 60 127)
```
在键入前两个命令之后，你应该听见一个中央C音符；在键入最后一个命令之后，音符应该停止。

有关细节，敬请参阅[技术细节](#技术细节)。

## 技术细节
提示：要了解基本概念，敬请参阅[教程](#教程).

### “player”子命令可以干什么？
它是用内置SoundFont™加载器来播放MIDI文件的最简方法之一（节奏完全准确）。它可以帮助您测试安装是否成功，也可以给您一个该MIDI在Minecraft中听感的第一印象。

### 一些特殊的MIDI设备……
当您键入 `/mopp device list`，你会看到一列MIDI设备。这是挑选MIDI设备的快速指南。如果您想要使用内置SoundFont™加载器，则您应该选择OpenJDK开发的“Gervill”。永远别选“Real Time Sequencer”，因为它根本不是一个可以发送MIDI消息的设备。此外，当您有另一个MIDI设备时，您也不应该选择“Microsoft GS Wavetable Synth”，因为其音质太差。

### “复位”和“重置”的不同……
“复位”只是关闭所有的音符，但“重置”不仅关闭所有的音符，还把所有的乐器重置到0（大钢琴）。

### 您可以使用的字符串……
对于`/mopp device send short <status> ...`和`/mopp device send sysex <status> <data> <length>`您可以把“\<status\>”设为字符串，而非整数。

一般消息：
+ MIDI_TIME_CODE = 0xF1
+ SONG_POSITION_POINTER = 0xF2
+ SONG_SELECT = 0xF3
+ TUNE_REQUEST = 0xF6
+ END_OF_EXCLUSIVE = 0xF7
+ TIMING_CLOCK = 0xF8
+ START = 0xFA
+ CONTINUE = 0xFB
+ STOP = 0xFC
+ ACTIVE_SENSING = 0xFE
+ SYSTEM_RESET = 0xFF
+ **NOTE_OFF = 0x80**
+ **NOTE_ON = 0x90**
+ POLY_PRESSURE = 0xA0
+ **CONTROL_CHANGE = 0xB0**
+ **PROGRAM_CHANGE = 0xC0**
+ CHANNEL_PRESSURE = 0xD0
+ PITCH_BEND = 0xE0

系统消息：
+ SYSTEM_EXCLUSIVE = 0xF0
+ SPECIAL_SYSTEM_EXCLUSIVE = 0xF7

### 如何使用SoundFont™加载器？
如果您想要使用内置SoundFont™加载器，则您应该选择OpenJDK开发的“Gervill”。记住，当您已经设定好了SoundFont™，把MIDI设备更改至“Gervill”会把SoundFont™重置至默认。这意味着您应该先选择设备，再设定SoundFont™，否则您必须重新加载SoundFont™（浪费时间！）。

## 授权
MIDIOut++ 是由kworker(FrankYang6921)制作，遵从GPLv3协议的自由软件。本软件没有担保。你可以前往红石音乐俱乐部或Minecraft视听俱乐部（591318869或1129026982）或加作者QQ（3450123872）以获取帮助。
