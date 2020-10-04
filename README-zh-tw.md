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

## 開始
要運行這個模組，妳需要：
+ Minecraft 1.16.x：裝有……
  + Fabric Loader 和 Fabric API

這個模組的目的是取代原版的MIDIOut，它需要MIDI設備或SoundFont™音色庫。這個模組只支持Windows。

## 發行
使用這個鏈接來重定向到 [發行](https://github.com/FrankYang6921/midiout-/releases) 頁。

## 教程
如果妳想自己使用這個模組，那麽下方的命令可能會幫到妳。但是用 [MCDI](https://github.com/FrankYang6921/mcdi) 生成紅石音樂更快也更簡單。

+ 打印版本信息：`/mopp about`
+ 開始播放單個MIDI文件：`/mopp player play <path>`
+ 停止播放單個MIDI文件：`/mopp player stop`
+ 列出所有的MIDI設備：`/mopp device list`
+ 復位選定的MIDI設備：`/mopp device panic`
+ 重置選定的MIDI設備：`/mopp device reset`
+ 選擇壹個MIDI設備：`/mopp device select <name>`
    + 提示：用”.“作為設備名就可以選擇默認設備。
+ 寫入壹個原生MIDI消息：`/mopp device send raw <bytes>`
    + 提示：這些字節應該用”base64“編碼。
+ 發送壹個壹般MIDI消息：
    + `/mopp device send short <status>`
    + `/mopp device send short <status> <data1> <data2>`
    + `/mopp device send short <status> <channel> <data1> <data2>`
    + 提示：“<status>”既可以是整數，也可以是字符串。
+ 發送壹個系統MIDI消息：
    + `/mopp device send sysex <data> <length>`
    + `/mopp device send sysex <status> <data> <length>`
+ 顯示選定的MIDI設備：`/mopp device show`
+ 將 SoundFont™ 加載進內置MIDI設備“Gervill”： `/mopp vdev load <path>`
+ 將 SoundFont™ 重新加載進內置MIDI設備“Gervill”：`/mopp vdev reload`
+ 將 SoundFont™ 從內置MIDI設備“Gervill”卸載：`/mopp vdev unload`

如果要測試安裝是否成功，您也可以嘗試以下的3條命令：:
```
/mopp device select "Microsoft GS Wavetable Synth"
/mopp device send raw "kDx/" (或 /mopp device send short NOTE_ON 0 60 127)
/mopp device send raw "gDxA" (或 /mopp device send short NOTE_OFF 0 60 127)
```
在鍵入前兩個命令之後，妳應該聽見壹個中央C音符；在鍵入最後壹個命令之後，音符應該停止。

有關細節，敬請參閱[技術細節](#技術細節)。

## 技術細節
提示：要了解基本概念，敬請參閱[教程](#教程).

### “player”子命令可以幹什麽？
它是用內置SoundFont™加載器來播放MIDI文件的最簡方法之壹（節奏完全準確）。它可以幫助您測試安裝是否成功，也可以給您壹個該MIDI在Minecraft中聽感的第壹印象。

### 壹些特殊的MIDI設備……
當您鍵入 `/mopp device list`，妳會看到壹列MIDI設備。這是挑選MIDI設備的快速指南。如果您想要使用內置SoundFont™加載器，則您應該選擇OpenJDK開發的“Gervill”。永遠別選“Real Time Sequencer”，因為它根本不是壹個可以發送MIDI消息的設備。此外，當您有另壹個MIDI設備時，您也不應該選擇“Microsoft GS Wavetable Synth”，因為其音質太差。

### “復位”和“重置”的不同……
“復位”只是關閉所有的音符，但“重置”不僅關閉所有的音符，還把所有的樂器重置到0（大鋼琴）。

### 您可以使用的字符串……
對於`/mopp device send short <status> ...`和`/mopp device send sysex <status> <data> <length>`您可以把“\<status\>”設為字符串，而非整數。

壹般消息：
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

系統消息：
+ SYSTEM_EXCLUSIVE = 0xF0
+ SPECIAL_SYSTEM_EXCLUSIVE = 0xF7

### 如何使用SoundFont™加載器？
如果您想要使用內置SoundFont™加載器，則您應該選擇OpenJDK開發的“Gervill”。記住，當您已經設定好了SoundFont™，把MIDI設備更改至“Gervill”會把SoundFont™重置至默認。這意味著您應該先選擇設備，再設定SoundFont™，否則您必須重新加載SoundFont™（浪費時間！）。

## 授權
MIDIOut++ 是由kworker(FrankYang6921)制作，遵從GPLv3協議的自由軟件。本軟件沒有擔保。
