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
+ Minecraft 1.16 or newer with...
  + Fabric Loader \+ Fabric API
  
This mod meant to replace the original MIDIOut, and it requires MIDI devices or SoundFont™ banks. Only Windows is supported.

## Releases

Use the link here to redirect to the [releases](https://github.com/FrankYang6921/midiout-/releases) page.

## HOWTO
These commands below are useful when you want to use the mod yourself. However, it's much easier and faster to use [MCDI](https://github.com/FrankYang6921/mcdi) for creating musics.

+ Print version information: `/mopp about`
+ Start playing a single MIDI file: `/mopp player play <path>`
+ Stop playing a single MIDI file: `/mopp player stop`
+ List all the MIDI devices: `/mopp device get`
+ Select a MIDI device: `/mopp device set <name>`
+ Reset the selected MIDI device: `/mopp device reset`
+ Panic the selected MIDI device: `/mopp device panic`
+ Write a raw MIDI message: `/mopp device send <bytes>`
    + Notice: these bytes should be sent in base64 form.
+ Send a short MIDI message:
    + `/mopp device short <status>`
    + `/mopp device short <status> <data1> <data2>`
    + `/mopp device short <status> <channel> <data1> <data2>`
    + Notice: 'status' can be either in integer or in string.
+ Send a SysEx MIDI message: 
    + `/mopp device sysex <data> <length>`
    + `/mopp device sysex <status> <data> <length>`

You can also try these 3 commands to test your installation:
```
/mopp device set "Microsoft GS Wavetable Synth"
/mopp device write "kDx/" (or /mopp device NOTE_ON 144 60 127)
/mopp device short NOTE_ON 60 127
/mopp device write "gDxA" (or /mopp device NOTE_OFF 128 60 127)
```
After typing the first 2 commands, you should hear a middle C note, and after typing the last command, the note should be stopped.

## License

MIDIOut++ is a free software by kworker(FrankYang6921) under GPLv3 license. There is no warranty for the program.
