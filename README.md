Development client
==================
The `runClient` task builds and launches the JarJar archive from `build/libs` in the isolated
`run/client-jarjar` directory. In VS Code, select **Client (Debugger + HCR)** and press F5.
This runs the client with the debugger enabled and attaches the Java debugger when it is ready.
Hot Code Replace is enabled automatically.
In this HCR client, `.html` and `.js` files under `src/main/resources/web` are served from
the source directory, so refresh the browser page manually after editing them.

```text
.\gradlew.bat runClient --debug-jvm
```

Edit the files under `src`; the JAR in the run directory is generated and replaced on each launch.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
