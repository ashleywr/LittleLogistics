![Little Logistics](https://user-images.githubusercontent.com/31002977/162084973-b37fd259-e3c5-4c91-81e5-193f32e1ef91.png)
###### banner by naj77


# Little Logistics Mod for Minecraft Forge

<video src="https://user-images.githubusercontent.com/31002977/150042817-908a75b5-2802-4c83-a13d-0fbecc0ec94c.mp4"></video>

### Download

https://www.curseforge.com/minecraft/mc-mods/little-logistics

https://modrinth.com/mod/little-logistics

### Discord

https://discord.gg/e6U2CFruVC

### Features

- Trains with train cars 
  - transport entities, items, fluids
  - routing AI
  - collision avoidance AI
- Tugs with barges
  - transport players, items, fluids
  - routing AI
  - fish automatically
- Shared directed transport networks
  - author rail or water graphs with the Network Chart
  - public dispatcher and docking-service APIs for demand-driven addons
  - temporary vehicle itineraries coexist with classic cyclic routes

### License 

#### Source Code / java files

LGPLv3 
https://www.gnu.org/licenses/lgpl-3.0.en.html

#### Artwork, textures, and models

All right reserved, availiable in the repository linked below

https://github.com/MuradAkh/LittleLogisticsBlockBench

You may use this to create a texture pack for Little Logistics without further permission

#### "Little Logistics" name

All right reserved, if you would like to port this mod to another version or loader, please reach out on discord.

### Contributors

Contributions welcome, for simple fixes just send a PR, If you are interested in contributing features please open an issue first to discuss.

<!-- ashley-release-information -->

## Ashley's builds

This is an unofficial maintenance fork. Original authors retain ownership and credit. See the [upstream project](https://github.com/muradakh/LittleLogistics) for the original mod. Local changes do not imply upstream endorsement.

Licensing: **LGPL-3.0; Little Contraptions MIT**. Existing license files, copyright notices, attribution and any separate asset terms remain in force. This fork does not relicense upstream work.

Provided **AS IS**, without warranty of any kind. Maintenance, updates and support are best effort; compatibility with every modpack is not guaranteed.

### Downloads and installation

Download a runtime `.jar` from [GitHub Releases](https://github.com/ashleywr/LittleLogistics/releases). Use the Minecraft version and loader named in that release and install its required dependencies. Do not install `-sources`, `-dev`, `-slim`, `-common` or test JARs as the mod.

Close Minecraft, back up your world, and place the matching runtime JAR in the instance's `mods/` directory. Replace older versions of this same mod rather than loading duplicates. For content mods, install matching versions on the server and its clients unless the release explicitly says client-only or server-only.

### Building and reporting problems

Use Java 21 and the checked-in Gradle wrapper:

```powershell
.\gradlew.bat build
```

Build outputs are under `build/libs/` or each loader subproject's `build/libs/`. Some projects require the local development dependencies described above; those dependencies are not included in the download.

For a problem report, include the exact release filename, Minecraft and loader versions, relevant mod versions, `logs/latest.log`, and a crash report if one was written. Remove private information before sharing logs. Remove the JAR to uninstall; worlds containing this mod's blocks, entities or world generation may require restoring a backup.
