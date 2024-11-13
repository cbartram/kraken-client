[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]


<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/cbartram/kraken-loader-plugin">
    <img src="src/main/resources/com/kraken/images/kraken.png" alt="Logo" width="120" height="120">
  </a>

<h3 align="center">Kraken Client</h3>

  <p align="center">
   A RuneLite plugin which side-loads the Kraken essential plugins.
    <br />
</div>

This is a RuneLite plugin which side loads the Kraken suite of essential plugins into the RuneLite client. This client is not associated with Jagex or RuneLite in any way.
Currently, all plugins are released for free at [our discord here](https://discord.gg/Jxfxr3Zme2). Some features this client offers are:

- Discord authentication and sign up / sign in
- Auto plugin loading (and automatic plugin updates)
- Plugin license key validation
- Direct Jagex launcher compatibility.
- Native RuneLite Client (no injection or client modification)

Although the Kraken client is safe and doesn't modify RuneLite in any way the plugins are unofficial. **We are not responsible for any bans you may incur for using this client.**
For more information about the Kraken Client see: [Kraken Client](#about-kraken-client).

# Current Plugins

Currently, we have the following plugins enabled on the client:

| Plugin Name       | Plugin Description                                                               | Version |
|-------------------|----------------------------------------------------------------------------------|---------|
| Alchemical Hydra  | Tracks your prayers, special attacks and when to switch for Hydra.               | 1.0.0   |
| Chambers of Xeric | Tracks Olm rotations, specials, tick counters, and various boss helpers for CoX. | 1.0.1   |

# QuickStart

Follow the steps below to start using the Kraken client.

## Downloading the Client 
- Download the latest version of this client from the [GitHub releases](https://github.com/cbartram/kraken-client/releases).
- Make sure you have Java 11 installed. [Install here](https://adoptium.net/temurin/releases/?version=11).
- Make sure your JAVA_HOME environment variable points to your installation of Java 11. See [troubleshooting](#troubleshooting) for help setting your JAVA_HOME.
  - Windows: `C:\Program Files\Eclipse Adoptium\jdk-11.0.25.9-hotspot`
  - Mac: `/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home`
- Double-click the client to run

## Getting Access to Plugins

- Within the Kraken client in the sidebar click "Sign in with Discord" to link your discord account
- [Join our discord here](https://discord.gg/Jxfxr3Zme2). 
- Create a ticket for free access to the plugins. You will receive a license key which you enter into the plugins configuration to use the plugin.

**Note: There are security restrictions in place for this client. Plugin usage is limited to 1 machine per Discord account. When you link your Discord
your hardware id is captured. Any license keys you receive for your plugins will only be valid from the machine you linked your Discord on.**

## Jagex Launcher
This client runs as the normal RuneLite (no additional injections or anything like that). If you use the Jagex launcher then simply go to C:\Users\YOUR_USER\AppData\Local\RuneLite and copy the kraken client there. Re-name the client to RuneLite.jar and when you launch RuneLite via the Jagex launcher it will launch the Kraken client.

To switch back just rename RuneLite.jar back to Kraken-Client.jar and rename the original runelite jar back to RuneLite.jar

## Troubleshooting
If you are having trouble or the plugins aren't loading/starting properly make sure your JAVA_HOME env variable is pointing to the right installation and that you are using
Java 11 (preferably [Eclipse Temurin JDK](https://adoptium.net/temurin/releases/?version=11) since that is what RuneLite uses).

### Windows

On Windows you can open Powershell and type `$env:JAVA_HOME` to see its current value. Set your java home by searching "Environment variables" in the windows start menu. 
If you installed the JDK from the link above you can edit your environment variable for java home and set it to: `C:\Program Files\Eclipse Adoptium\jdk-11.0.25.9-hotspot`.

Re-launch the Kraken client.

### Mac
On Mac you can check your java home location in a terminal window by using: echo $JAVA_HOME. You can set your java home variable by
creating a file called: `.bash_profile` in your home directory (`/Users/your-name/.bash_profile`). In the bash profile file add the following line:

`export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home`

_Note: Your installed Eclipse Temurin JDK location may vary. Double check._

# Getting Started (Development)

To get started clone this project and run a gradle build:

```shell
git clone https://github.com/cbartram/kraken-loader-plugin.git

gradle build

# Create an executable JAR with

gradle shadowJar

# Run with

java -jar ./build/libs/kraken-client-{version}-all.jar
```

## Prerequisites

You will need to have [Gradle installed](https://gradle.org/install/) on your system in order to build and run this project. This
project may not work with all versions of the Java JDK. Specifically it has been tested on:
- [Eclipse Temurin 11.0.23](https://adoptium.net/temurin/releases/?version=11)
- [OpenJDK 11.0.2](https://www.openlogic.com/openjdk-downloads)

## Running

To run the project set up a new run configuration with the following args:
- VM Args: `-ea`
- CLI Args: `--developer-mode` (optionally `--debug` for more logs)
- Main class: `com.kraken.KrakenClient`

It should launch RuneLite normally, and you will be able to see a small green "Kraken" icon in the Navbar
which contains your Kraken plugins.

# About Kraken Client

The Kraken client is more like the Kraken plugin because it loads using the native RuneLite plugin loader. This has some pro's and con's. For one,
the Kraken client isn't technically a third party client. It's RuneLite, with a plugin that loads other plugins. All Kraken plugins are fully compatible with RuneLite,
use only the RuneLite API, and are loaded the same way as any other external plugin you would download on the plugin hub! This makes Kraken virtually undetectable from a client perspective.

The downside to this approach is that Kraken plugins can't automate tasks in game. It's not a bot, and doesn't modify RuneLite in any way. That's not to say the plugins Kraken provides
aren't extremely overpowered because they are! It just means it's not going to play the game for you i.e. switch prayers, walk here, or interact with game objects. Your basically trading
off automation for account security. That being said we still aren't responsible for any bans on your accounts. RuneLite could introduce an update to detect client's like this in the future.
If you care about an account do NOT play on third party clients.

_Note: Much of the UI code is taken from [RuneLite](https://github.com/runelite/runelite/tree/master) as the Kraken loader plugin has to do the same thing: load plugins, display them in a list,
and provide editable configuration for each plugin._

## Adding Plugins

In order for plugins to be compatible with Kraken they must follow the standard [RuneLite plugin configuration](https://github.com/runelite/plugin-hub/blob/master/README.md):
- Be nested a package called `com.krakenplugins`. Any other package nesting within `com.krakenplugins` is fine.
- Include a license field in the RuneLite config. The keyName **MUST** be "licenseKey". It should look like this:
```java
@ConfigItem(
        keyName = "licenseKey",
        name = "License Key",
        description = "License key required to enable the plugin.",
        position = 0,
        secret = true
)
default String licenseKey() {
    return "";
}
```
- A shaded JAR of the plugin must be built and uploaded to the `/plugins` prefix of the S3 bucket on the backend. You cannot currently side-load other plugins with Kraken. 
- The `rootProject.name` field in `settings.gradle` **MUST** be the same name as the `@PluginDescriptor` name. It should also replace any spaces with dashes "-".
- (Optional) Ideally JAR file names for plugins are as small as possible. Replacing words like `snapshot-all` with a simple semantic version is helpful.

## Running the tests

No tests yet.

## Deployment

To create a release for the client run:

```shell
gradle shadowJar
```

Create a new [release in GitHub](https://github.com/cbartram/kraken-client/releases), upload the shaded JAR from `./build/libs/kraken-client-{version}-all.jar`
as part of the release.

Give the release a semantic version like `v1.0.2` in the title and write a description about what has changed. 

## Built With

  - [Java 11](https://www.openlogic.com/openjdk-downloads) - Programming Language
  - [RuneLite](https://github.com/runelite/runelite/tree/master) - Base Client and API

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code
of conduct, and the process for submitting pull requests to us.

## Versioning

We use [Semantic Versioning](http://semver.org/) for versioning. For the versions
available, see the [tags on this
repository](https://github.com/cbartram/kraken-loader-plugin/tags).

## License

This project is licensed under the [CC0 1.0 Universal](LICENSE.md)
Creative Commons License - see the [LICENSE.md](LICENSE.md) file for
details

## Acknowledgments

  - RuneLite for making an incredible piece of software and API.

[contributors-shield]: https://img.shields.io/github/contributors/cbartram/kraken-loader-plugin.svg?style=for-the-badge
[contributors-url]: https://github.com/cbartram/kraken-loader-plugin/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/cbartram/kraken-loader-plugin.svg?style=for-the-badge
[forks-url]: https://github.com/cbartram/kraken-loader-plugin/network/members
[stars-shield]: https://img.shields.io/github/stars/cbartram/kraken-loader-plugin.svg?style=for-the-badge
[stars-url]: https://github.com/cbartram/kraken-loader-plugin/stargazers
[issues-shield]: https://img.shields.io/github/issues/cbartram/kraken-loader-plugin.svg?style=for-the-badge
[issues-url]: https://github.com/cbartram/kraken-loader-plugin/issues
[license-shield]: https://img.shields.io/github/license/cbartram/kraken-loader-plugin.svg?style=for-the-badge
[license-url]: https://github.com/cbartram/kraken-loader-plugin/blob/master/LICENSE.txt
