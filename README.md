# VibeTest

Two independent Kotlin/JVM applications that talk to each other over a MIDI port:

- **controller** — captures global keyboard and mouse input and sends MIDI notes out.
- **synthesizer** — reads MIDI notes in and renders them to audio in real time.

Both use only the JDK's built-in `javax.sound.midi` / `javax.sound.sampled` packages
for MIDI and audio. The controller additionally uses
[jnativehook](https://github.com/kwhat/jnativehook) for global input capture, since
the JDK has no cross-platform global keyboard/mouse hook.

## Requirements

- A JDK 21 installation. You do **not** need to set `JAVA_HOME`: Gradle picks a
  matching JDK automatically via `gradle/gradle-daemon-jvm.properties`, and will
  download one if none is installed.
- A virtual MIDI loopback port, e.g. [loopMIDI](https://www.tobias-erichsen.de/software/loopmidi.html)
  on Windows. Create a port named `loopMIDI` (or pass `--port` to match your name).
  On macOS use the IAC Driver; on Linux use `snd-virmidi` or `virmidi`.

## Build

```sh
./gradlew build
```

## Run

Start the synthesizer first so the port is open, then the controller:

```sh
./gradlew :synthesizer:run --args="--list"     # see available inputs
./gradlew :synthesizer:run --args="--port loopMIDI"

./gradlew :controller:run --args="--list"      # see available outputs
./gradlew :controller:run --args="--port loopMIDI"
```

## Controls

| Input            | Action                        |
| ---------------- | ----------------------------- |
| `Z X C V B N M`  | White keys (C D E F G A B)    |
| `S D` / `G H J`  | Black keys (C# D# / F# G# A#) |
| `,`              | C of the next octave          |
| `Up` / `Down`    | Octave up / down              |
| Mouse L / M / R  | C / D / E                     |

## Options

Both apps accept `--list` to print ports and `--port <name-or-index>` to select one
by case-insensitive name substring or by index from the listing.

- controller: `--channel <0-15>` sets the MIDI channel (default 0).
- synthesizer: `--rate <hz>` sets the sample rate (default 44100).
