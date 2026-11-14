<div align="center">

<img src="assets/icon.jpg" alt="Seoga app icon" width="120" height="120">

# Seoga

**Your personal manga and comics library, organized your way.**

An offline-first Android catalog for manga, manhwa, manhua, and comics. Track chapters, rate your favorites, and organize your collection — without an account.

[Features](#features) · [Screenshots](#screenshots) · [Technology](#technology) · [Getting started](#getting-started)

</div>

> [!IMPORTANT]
> **Seoga is a catalog, not a reader.** It does not download or display manga or comic pages.

## Features

- **Personal library:** Catalog manga, manhwa, manhua, and comics with covers and metadata.
- **Reading progress:** Track chapters and assign decimal ratings.
- **Custom collections:** Organize titles into multiple collections. The default collection includes every work and can be renamed, but not deleted.
- **Bulk management:** Select multiple titles to manage collections or delete entries.
- **Optional online discovery:** Look up metadata through MangaDex and AniList, or add entries manually.
- **Content controls:** Optional adult-content search with 18+ cover labels.
- **Local-first:** Your library is stored on your device, with no account required. Online metadata search requires an internet connection.
- **Portable backups:** Export and restore your library using ZIP archives.
- **Languages:** English, Brazilian Portuguese, and Spanish.

## Screenshots

The screenshots below show the Android interface in dark mode.

| Library | Online search |
|:---:|:---:|
| ![Library screen](screenshots/library.jpg) | ![Online search screen](screenshots/search.jpg) |
| **Collections** | **Settings** |
| ![Collections screen](screenshots/collections.jpg) | ![Settings screen](screenshots/settings.jpg) |

## Technology

- **Language:** Kotlin
- **UI:** Jetpack Compose and Material 3
- **Architecture:** MVVM
- **Local database:** Room (SQLite)
- **Metadata providers:** MangaDex and AniList

## Getting started

Download the source code from this repository and open the Android project in a compatible Android development environment.

For prebuilt packages, check the [Releases](https://github.com/joa-no/Seoga/releases) page if a release is available.

## Privacy and data

Seoga keeps your catalog data on your device. Online metadata lookup is optional and requires network access to the selected provider. Use the ZIP export feature to create a backup before switching devices or uninstalling the app.

## Name

**Seoga** comes from the Korean word **서가**, meaning *bookshelf*.

## License

Distributed under the [MIT License](LICENSE). See the license file for terms and attribution.

---

<div align="center">
Made by <a href="https://github.com/joa-no">joa-no</a>
</div>
