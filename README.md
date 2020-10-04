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

## Getting Started
To run this mod, you need:
+ Minecraft 1.16.x with...
  + Fabric Loader \+ Fabric API
  
This mod is designed to replace the original MIDIOut, and it requires MIDI devices or SoundFont™ banks. Only Windows is supported.

## Releases
Use the link here to redirect to the [releases](https://github.com/FrankYang6921/midiout-/releases) page.

## HOWTO
These commands below are useful when you want to use the mod yourself. However, it's much easier and faster to use [MCDI](https://github.com/FrankYang6921/mcdi) for creating musics.

+ Print version information: `/mopp about`
+ Start playing a single MIDI file: `/mopp player play <path>`
+ Stop playing a single MIDI file: `/mopp player stop`
+ List all the MIDI devices: `/mopp device list`
+ Panic the selected MIDI device: `/mopp device panic`
+ Reset the selected MIDI device: `/mopp device reset`
+ Select a MIDI device: `/mopp device select <name>`
    + Notice: use '.' as a device name to use the default one.
+ Write a raw MIDI message: `/mopp device send raw <bytes>`
    + Notice: these bytes should be sent in base64 form.
+ Send a short MIDI message:
    + `/mopp device send short <status>`
    + `/mopp device send short <status> <data1> <data2>`
    + `/mopp device send short <status> <channel> <data1> <data2>`
    + Notice: 'status' can be either in integer or in string.
+ Send a SysEx MIDI message: 
    + `/mopp device send sysex <data> <length>`
    + `/mopp device send sysex <status> <data> <length>`
+ Show the selected MIDI device: `/mopp device show`
+ Load a SoundFont™ file to the virtual MIDI device 'Gervill': `/mopp vdev load <path>`
+ Reload the loaded one to the virtual MIDI device 'Gervill': `/mopp vdev reload`
+ Unload the loaded one from the virtual MIDI device 'Gervill': `/mopp vdev unload`

You can also try these 3 commands to test your installation:
```
/mopp device select "Microsoft GS Wavetable Synth"
/mopp device send raw "kDx/" (or /mopp device send short NOTE_ON 0 60 127)
/mopp device send raw "gDxA" (or /mopp device send short NOTE_OFF 0 60 127)
```
After typing in the first 2 commands, you should hear a middle C note, and after typing in the last command, the note should be stopped.

For more details, read [Details](#Details).

## Details
Notice: I won't repeat the basic concepts in [HOWTO](#HOWTO).

### What can the 'player' sub-command do?
It is the easiest way to playback a single MIDI file in accurate tempo with the internal SoundFont™ loader. It can help you test your installation and give you a brief impression of the performance of that MIDI file in Minecraft.

### Some special MIDI devices...
When you type in `/mopp device list`, you may see a list of MIDI devices. Here's a quick guide for you to pick these MIDI devices. If you'd like to use the internal SoundFont™ loader, then you have to choose 'Gervill' by OpenJDK. You should never choose 'Real Time Sequencer' as it's not a proper device to send MIDI messages. In addition, you should never choose 'Microsoft GS Wavetable Synth' either when you have another MIDI device for its poor sound quality.

### The difference between 'panic' and 'reset'...
'Panic' turns off all the notes, in comparison, 'reset' not only turns off all the notes, but also resets all the instruments to 0.

### The string shortcuts you can use...
For `/mopp device send short <status> ...` and `/mopp device send sysex <status> <data> <length>`, you can use string shortcuts for the '\<status\>' instead of an integer.

For short messages:
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

For sysex messages:
+ SYSTEM_EXCLUSIVE = 0xF0
+ SPECIAL_SYSTEM_EXCLUSIVE = 0xF7

### How to use SoundFont™ loader?
If you'd like to use the internal SoundFont™ loader, then you have to choose 'Gervill' by OpenJDK. Keep in mind that when you have already set the SoundFont™, changing the MIDI device to 'Gervill' will override that setting with the default one. That means you should set the device first then load the SoundFont™, or you have to reload the SoundFont™ again, which is time-consuming.

## License
MIDIOut++ is a free software by kworker(FrankYang6921) under GPLv3 license. There is no warranty for the program.
