[discord-invite]: https://discord.hawolt.com

[discord-shield]: https://discordapp.com/api/guilds/1130517263280246907/widget.png?style=shield

# JamAlong

[![Hits](https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2Fhawolt%2FJamAlong&count_bg=%2379C83D&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=hits&edge_flat=false)](https://hits.seeyoufarm.com)
[ ![discord-shield][] ][discord-invite]

## Bugs & Feature Requests

If you are experiences any troubles or would like a feature please open a new Issue and chose the correct template.

You can click [here](https://github.com/hawolt/JamAlong/issues/new/choose) to open a new issue.

## Dependencies

- [soundcloud-downloader](https://github.com/hawolt/soundcloud-downloader)
- [discord-game-sdk4j](https://github.com/JnCrMx/discord-game-sdk4j)
- [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket)
- [logback-classic](https://github.com/qos-ch/logback)
- [hawolt-core](https://github.com/hawolt/hawolt-core)
- [mp3spi](https://github.com/umjammer/mp3spi)
- [Javalin](https://github.com/javalin/javalin)
- [jcef](https://github.com/chromiumembedded/java-cef)

## Discord

Since this code lacks documentation the best help you can get is my knowledge, proper questions can be asked in
this [discord](https://discord.hawolt.com) server, please note that I will not guide you to achieve something or
answer beginner level questions.

## Getting Started

### 1. Download the JAR file

Go to [releases](https://github.com/hawolt/JamAlong/releases/latest) and download the JAR file of the program.

### 2. Verify Java 17 installation

Before you can run the program, make sure you have Java 17 installed on your computer. To check if it's already
installed, open a terminal or command prompt and run the following command:

```sh
java -version
```

If you see output that mentions "17" (e.g., "openjdk version 17"), you're good to go.
If not, you'll need to install Java 17. (the internet is full of tutorials on how to do this)

### 3. Run the program

Once you have Java 17 installed and the JAR file downloaded, simply open the JAR file (e.g., `JamAlong-*version*`).

## How to setup the project using IntelliJ

1. Within Intellij select `File` -> `New` -> `Project from Version Control...`
2. Insert `git@github.com:hawolt/JamAlong.git` for the `URL` field and hit `Clone`
3. IntelliJ should automatically detect the Maven framework, if this is not the case you can rightclick the
   JamAlong folder in the Project hierarchy and select `Add Framework Support...` then select `Maven`
4. Make sure you are actually using a compatible Java version by selecting `File` -> `Project Structure`, navigate
   to `Project` within `Project Settings` and make sure both `SDK` and `Language level` have Java Version 17 or higher
   selected, hit `OK`
5. To run the Code navigate to `Client/src/main/java/com.hawolt` and rightclick `Main`,
   select `Run Main.main()`

## Contributions

Pull requests are always appreciated, before writing a larger chunk of code please communicate on Discord if it is needed.