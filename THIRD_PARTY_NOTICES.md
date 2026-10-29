# Third-party notices

NextCoinflip's original source is GPL-3.0-or-later, copyright (c) 2026 Muhammad Daffa (aglerr) and contributors. Dependency licenses and copyright notices continue to apply to their respective components.

## Bundled libraries

- **MDLib 2.4.8**, Muhammad Daffa and upstream contributors. [Public source](https://github.com/mdaffa48/MDLib/tree/440fedb39f45f5dd86822c46e9950283f2f35d8c), [Maven artifact](https://repo.blocksmith.studio/releases/com/muhammaddaffa/MDLib/2.4.8/). The library includes GPL-3.0-or-later SpigotUpdateChecker code, copyright (c) 2022 Alexander Majka (mfnalex), JEFF Media GbR. [Upstream source](https://github.com/JEFF-Media-GbR/SpigotUpdateChecker). The GNU GPLv3 text is in `LICENSE`.
- **FastInv**, bundled inside MDLib, copyright (c) 2018–2021 MrMicky, MIT License. [Source](https://github.com/MrMicky-FR/FastInv). Its full notice is preserved in `src/main/resources/META-INF/licenses/FastInv.txt` and the built JAR.
- **XSeries 13.7.1**, Crypto Morin, MIT License. [Source](https://github.com/CryptoMorin/XSeries).
- **FastBoard 2.1.5**, MrMicky, MIT License. [Source](https://github.com/MrMicky-FR/FastBoard).
- **CustomBlockData 2.2.3**, JEFF Media, GPL-3.0. [Source](https://github.com/JEFF-Media-GbR/CustomBlockData).

These three components are embedded in MDLib. Their upstream license texts are retained in `src/main/resources/META-INF/licenses` and the built JAR.
- **HikariCP 7.1.0**, Apache License 2.0. [Source](https://github.com/brettwooldridge/HikariCP/tree/HikariCP-7.1.0).
- **bStats 3.2.1**, MIT License. [Source](https://github.com/Bastian/bStats).
- **SLF4J API 2.0.17**, MIT License. [Source](https://github.com/qos-ch/slf4j/tree/v_2.0.17).

The Shade plugin preserves Apache license/notice resources. Additional notices and the GPL text are packaged under `META-INF/licenses/`.

## Server-provided APIs

Paper and PlaceholderAPI are provided by the server and are not bundled in NextCoinflip's JAR. Their respective licenses apply. Test dependencies JUnit and Mockito are not bundled.
