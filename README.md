# Better Ladders

A Fabric mod that gives ladders rails and rungs, lets them stand on their own, and adds a rope
ladder that unrolls itself down a drop.

## Ladders Are Not Flat

Vanilla's ladder is one plane with a ladder drawn on it, which is why a ladder seen edge-on is not
there at all. This is the same picture given depth: two rails standing a pixel off the wall, four
rungs between them.

**It is vanilla's own texture, rearranged.** Each face takes the part of `block/ladder.png` that
already shows that part - rails from the texture's rail columns, rungs from its rung rows - so a
resource pack that retextures ladders still retextures these.

## Ladders Hold Themselves Up

Vanilla wants a solid face behind every rung, which turns a climb down a shaft into a building job:
you build a wall you did not want, to hang a ladder on, in a hole you are about to leave.

Here a ladder stays where you put it. Placed in open air it takes the direction you were facing;
placed on a wall it still takes the wall's, because a ladder that has a wall should agree with it.
And a ladder whose wall is later mined stays hanging rather than popping off.

## Rope Ladder

Its own block, not a ladder wearing a different name: rope rails with slats between them, a good
deal quicker and quieter to cut than nailed timber. Made from four string and a stick, which is a
rope ladder's whole bill of materials.

Throw it at a wall or off a ledge and it unrolls downward, one piece of the stack per rung, until
it reaches the ground or you run out.

- **Off a wall** it hangs on the face you clicked, the way one placed by hand would.
- **Off the top of a block** it goes over the edge you are looking across, which is what anybody
  standing at a cliff holding a rope is about to do.
- **It stops at the first thing in its way** - ground, a chest, somebody's build - rather than
  pushing past it, so a rope thrown down a shaft lands on the floor of the shaft.

**Cut it anywhere and everything under it comes down.** The only thing holding the rest of the rope
up was the rung you just took, so the whole drop lands on the floor as rope ladder again - which is
also how you get one back down off a cliff you have finished with, in one hit rather than sixty.

Nothing is lost doing it. Every rung dropped is a rung returned, so the coil and what it becomes
are the same thing in two shapes.

This mod is also the reason a ladder needs no wall, and the rope needs that more than anything
else does: thrown into open air, it stays where it lands.

## Pandorical

Better Ladders ships the 3D ladder model and the rope ladder's own model through Pandorical's
content sync.

**The Pandorical mod must be installed client-side** to see either. Without it a client sees
vanilla's flat ladder and an untextured rope ladder; everything still works, including ladders
standing unsupported and ropes unrolling, because all of that is decided on the server.

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients
need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API)
and `fabric.mod.json` (Java).

## Key Files

| File | Responsibility |
|------|---------------|
| `Main.java` | Entry point; the rope ladder, and shipping the ladder model |
| `RopeLadderItem.java` | A coil of ladder that unrolls itself |
| `RopeLadder.java` | Unrolling one down whatever is under it |
| `RopeLadderBlock.java` | A ladder that hangs, and what happens when it is cut |
| `mixin/LadderSupportMixin.java` | A ladder holds itself up |

## Art

`generate_models.py` cuts the 3D ladder model, and `generate_icon.py` and `generate_textures.py`
cut the icon and rope ladder sprite out of the vanilla jar. All three are deterministic; re-run
them after a Minecraft version bump.

## License

MIT, see [LICENSE](LICENSE).
