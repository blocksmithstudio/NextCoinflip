# Coinflip dialog

The optional dialog presentation targets Minecraft Java 26.3 and requires the matching resource pack. The creator is Heads, and the challenger is Tails. Both see the same outcome. The regular coinflip task picks the winner and settles the wager, and the reveal displays that result.

## Install

1. Install the rebuilt NextCoinflip JAR on a Paper 26.3 server.
2. Zip the **contents** of `resource-pack` so `pack.mcmeta` and `assets` are at the ZIP root. Serve that ZIP through the server's resource pack delivery.
3. Set `coinflip-dialog.resource-pack-id` in NextCoinflip's `config.yml` to the exact UUID sent with the pack. For built-in delivery, this is the `resource-pack-id` in `server.properties`. Restart the server and reconnect to apply the pack.

Each participant needs a successful load acknowledgement for the configured UUID. Players without that acknowledgement see the inventory animation. Leaving the UUID blank disables the dialog presentation.

The pack overrides the 26.3 core text shaders. If an existing server pack already overrides `assets/minecraft/shaders/core/text.vsh` or `text.fsh`, merge the marked-glyph logic into those shader entry points before shipping one combined pack. A pack placed higher in the stack supplies the entire file for each path.

## Rendering

The supplied HallowPrison reference routes player-head text through its core text shaders. Its vertex shader identifies marked skin glyphs and sets up the render quad and pose. Its fragment shader samples the whole player skin and traces model parts, with additional passes for equipment and unrelated UI effects. The text vertex and fragment programs route through h_bm.glsl and h_bl.glsl. World entities and HUD post-processing use separate programs. The archive contains the client resources. The server-side dialog construction and its configuration were outside the archive.

The dialog header contains three marked player-head glyphs. The vertex shader places the portraits at equal distances from the viewport center and adjusts their size to fit narrow windows. The coin's full silhouette, including its rim, rests at the viewport center and flips along the same vertical line. The fragment shader leaves the space around the scene transparent. The vanilla dialog backdrop and safety icon remain visible. The result closes automatically 60 ticks after the reveal, after the last jump and settle pose. Escape can dismiss the result earlier.

The Paper 26.3 packet adapter matches the exact result dialog sent to the player and checks ownership on the connection's event loop before sending the close packet. A replacement sent by the server stays open. If packet tracking is unavailable, the result can be dismissed with Escape.

The portrait markers and center coin share the same tick clock. Both players stay planted during the face-off, swing as the coin launches, and brace before it lands. The winner raises both arms and makes three jumps in front of rotating translucent gold rays. The loser lowers their arms, bows their head, and fades to grayscale. The badges and center coin share a coarse pixel grid with dark outlines, yellow faces, and pale diagonal highlights. The coin pauses edge-on at tick 44 and shows the settled face at tick 62. The face-off draws from Kakegurui's opening duel, and the held beat draws from the Restricted Rock–Paper–Scissors arc in Kaiji. [Kakegurui episode 1](https://kakegurui-anime.com/1st/story/detail.php?id=1000217), [Kaiji episode guide](https://www.ntv.co.jp/kaiji/static/story.html)
