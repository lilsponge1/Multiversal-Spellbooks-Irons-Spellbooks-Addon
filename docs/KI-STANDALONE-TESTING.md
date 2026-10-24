# Independent Ki School validation

The exact `ki-school-1.0.0.jar` passed **446 checks, zero failed**: 410 native Saiyan server checks, 9 coexistence checks and 27 native client checks. Server and client both exited with code 0, and the disposable world was saved. No installed profile or production server was edited.

The fixture added Ki beside the **unchanged published Omega Form Ritual 0.2.4 combined JAR**, which still supplies Grand Explosion, Susanoo, Ignis compatibility and Omega. The existing Cataclysm/Travel Optics compatibility patches were retained. Forge 47.4.10, Iron's Spells 3.16.3, Curios, Player Animator and the matching pack dependencies were used.

The hidden client used Solas V3.7b, Embeddium 0.3.31 and Oculus 1.8.0. Actual independent network messages completed SSJ1, SSJ2 and the fifteen-second SSJ3 charge, then a held input returned to Base. Checks also covered the input mixin, charge animation, item model, Curios renderer, spell/aura resources, all ten sound registrations, independent wig IDs and the still-registered Omega scarf. SSJ2, SSJ3 charging and completed SSJ3 captures were visually reviewed.

The canonical gameplay, hair and encoded audio are retained. A second standalone build reproduced the release bytes exactly. The package declares only `ki_school` and contains only `local/kischool` classes; test helpers and other addons are excluded. The new item/spell namespace is `ki_school`. Automatic migration of experimental items from the earlier bundled review version is not included.

This is a focused compatibility fixture rather than every mod combination or a whole-game performance benchmark. The first fixture omitted required existing pack compatibility patches and exited before diagnostics; the corrected fixture passed. Human audio listening was not repeated because the encoded audio bytes are unchanged.

See [the machine-readable artifact audit](KI-STANDALONE-VALIDATION.json).
