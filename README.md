[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]


<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/cbartram/Hydra-Toggle">
    <img src="src/main/resources/com/kraken/images/kraken.png" alt="Logo" width="90" height="90">
  </a>

<h3 align="center">Hydra Toggle</h3>

  <p align="center">
   A GUI for toggling between the standard RuneLite client and the Hydra Tech client for 
Old School RuneScape.
    <br />
</div>


# Kraken Loader Plugin

This is a RuneLite plugin which side loads the Kraken suite of essential plugins into the RuneLite client. Much of the 
UI code is taken from [RuneLite](https://github.com/runelite/runelite/tree/master) as the Kraken loader plugin has to do the same thing: load plugins, display them in a list,
and provide editable configuration for each plugin. 

## Getting Started

To get started clone this project and run a gradle build:

```shell
git clone https://github.com/cbartram/kraken-loader-plugin.git

gradle build
```

### Prerequisites

You will need to have [Gradle installed](https://gradle.org/install/) on your system in order to build and run this project. This
project may not work with all versions of the Java JDK. Specifically it has been tested on:
- [Eclipse Temurin 11.0.23](https://adoptium.net/temurin/releases/?version=11)
- [OpenJDK 11.0.2](https://www.openlogic.com/openjdk-downloads)

### Running

To run the project setup a new run configuration with the following args:
- VM Args: `-ea`
- CLI Args: `--developer-mode` (optionally `--debug` for more logs)
- Main class: `com.kraken.KrakenLoaderPluginTest`

It should launch RuneLite normally and you will be able to see a small green "Kraken" icon in the Navbar
which contains your Kraken plugins.

## Running the tests

No tests yet.

## Deployment

Deployment will come later in this project's lifecycle.

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

## Authors

  - **C. Bartram** - *Initial Project implementation* - [RuneWraith](https://github.com/cbartram)

See also the list of
[contributors](https://github.com/PurpleBooth/a-good-readme-template/contributors)
who participated in this project.

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
