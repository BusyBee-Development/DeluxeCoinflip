## DeluxeCoinflip
This plugin was made open source to help in development and bug fixes by accepting pull requests from the community. Anyone is free to compile this plugin and use it.

# Support
If you need support for this plugin you must purchase it from [SpigotMC](https://www.spigotmc.org/resources/deluxecoinflip.79965/) or [BuiltByBit](https://builtbybit.com/resources/deluxecoinflip.10475/)

# Building
To build this plugin you need to add the missing APIs to the /libs/ folder then it's as simple as typing
`./gradlew shadowJar`

# Used Libraries
Here is a list of the various libraries used in this plugin.
- [Triump-GUI](https://github.com/TriumphTeam/triumph-gui)
- [ACF](https://github.com/aikar/commands) (Aikar Command Framework)

## DeluxeCoinflip
This plugin was made open source to help in development and bug fixes by accepting pull requests from the community. Anyone is free to compile this plugin and use it.

# Support
If you need support for this plugin you must purchase it from [SpigotMC](https://www.spigotmc.org/resources/deluxecoinflip.79965/) or [BuiltByBit](https://builtbybit.com/resources/deluxecoinflip.10475/)

# Building
To build this plugin you need to add the missing APIs to the /libs/ folder then it's as simple as typing
`./gradlew shadowJar`

# Used Libraries
Here is a list of the various libraries used in this plugin.
- [Triump-GUI](https://github.com/TriumphTeam/triumph-gui)
- [ACF](https://github.com/aikar/commands) (Aikar Command Framework)

# Placeholders
DeluxeCoinflip now supports two styles of placeholders:

- Brace-style (built-in): {KEY} where KEY = [A-Z0-9_]+
- PlaceholderAPI style: %deluxecoinflip_key%

Both styles are supported in config/messages. The plugin converts PAPI-style keys to brace-style internally and applies all known values in one pass.

Pre-registered keys include:
- {HEARTS_WON} → Hearts won
- {HEARTS_LOST} → Hearts lost
- {HEARTS_BET} → Hearts bet
- {WIN} → Win
- {LOSSES} → Losses

With PlaceholderAPI, the equivalents are:
- %deluxecoinflip_hearts_won%
- %deluxecoinflip_hearts_lost%
- %deluxecoinflip_hearts_bet%
- %deluxecoinflip_wins%
- %deluxecoinflip_losses%

Examples in messages.yml:
- "You have {HEARTS_WON}!"
- "You have %deluxecoinflip_hearts_won%!"

# PlaceholderAPI Integration
This plugin ships a PlaceholderExpansion with the identifier "deluxecoinflip". If PlaceholderAPI is present, it will auto-register on startup.

Supported identifiers:
- hearts_won, hearts_lost, hearts_bet (labels)
- wins, losses (from player stats)

# Developer API (Bukkit Services)
External plugins can register their own placeholders via Bukkit Services.

Service interface: net.zithium.deluxecoinflip.api.DeluxePlaceholdersApi

Example usage:

```java
DeluxePlaceholdersApi api = Bukkit.getServicesManager()
    .load(DeluxePlaceholdersApi.class);
if (api != null) {
    api.register("MY_CUSTOM_KEY", player -> "some dynamic value");
}
```

Apply placeholders to a message:
```java
String result = api.apply("Hello {MY_CUSTOM_KEY}", player);
```

Notes:
- Keys are case-insensitive but normalized to upper-case.
- Unknown keys are replaced with an empty string by default.
- The registry is thread-safe and performs fast single-pass replacement.
