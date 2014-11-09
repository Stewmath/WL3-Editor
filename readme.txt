TABLE OF CONTENTS

-   ABOUT
-   USING THE LEVEL EDITOR
-   EDITING WARPS
-   EDITING REGIONS
-   ADDING REGIONS
-   EDITING TILESETS
-   EDITING OBJECT SETS
-   LEVEL VERSIONS
-   MUSIC
-   EDITING TEXT
-   EDITING CREDITS
-   ABOUT THE "REF" FOLDER
-   CONTACT ME

ABOUT

	This is an editor for Wario Land 3, written by Drenn. This is version 0.4. 
	Source code is on github:
		https://github.com/Drenn1/WL3-Editor

USING THE LEVEL EDITOR

    First, you must choose a level to edit. Every level has 8 versions of itself
(see below), so numbers 0-7 are for the first level, Out of the Woods.  The
Peaceful Village picks up at 8 and continues to F. You can either choose the
level from the drop-down menu or change the number in brackets to select a
level. Note that hexadecimal is used almost exclusively in this editor, so if
you see a number or entry field, assume it's in hex!

Usage:
- Left click on the tileset or object viewers (left) to select a tile or an object
- Or, right click on a tile in the level viewer (right) to select a tile
- Left click and drag in the level viewer to place tiles or objects
- Ctrl+Left click to fill the area in a rectangle with the selected tile
- Ctrl+Z to undo
- Ctrl+Y to redo

	If "view->objects" is selected, you can also move objects, which are keys, 
chests, music coins, and enemies. They are represented by blue boxes with a 
number in them. A chest is "1", a key is "2", a musical coin is "3". Further 
numbers are usually enemies. You can check which enemies are which in the
object editor.

EDITING WARPS

    First, some quick terminology - a sector is a 16x16 area of tiles which is
the lowest unit a region can be divided into. They are outlined in green. A
region is a division of the level into something like a room. They are outlined
in red.

    It is highly recommended to enable "view->sectors" before continuing. Warps
can be edited by clicking "Edit warps & regions".

    Every sector can be given a single destination sector to warp to - only one.
All "warp tiles" placed in that sector will warp to its destination sector. Warp
tiles don't always look the same, but they are usually either doors, or they are
off to the side where the screen does not scroll. In Out of the Woods they
mostly look like ladders from the editor's point of view.  In the destination
sector, Wario will be warped to the first blue "W" it finds (at his feet). (I
call this a "warp object", since it's treated like an object - whereas a warp
tile comes from the tileset.) There's no point in putting more than one warp
object in a sector, because there's no way of specifying which one to warp to.

    To summarize: warp tiles initiate a warp. The sector containing this warp
tile has a "Destination" field, which is the sector he will be warped to. His
exact destination is marked by a warp object, the blue "W" in the destination
sector.

    You may notice that sector 0 is never used. This is because its warp
destination is Wario's spawn point. If you use it, any warp tiles placed there
will warp to the beginning of the level.
    
EDITING REGIONS

    As stated before, a region is a division of the level which could be thought
of as a room. There are several fields which define a region, most of which are
8-bit values. Here is a brief description of each. Press enter to make sure the
changes are applied.

- Top-left sector: The top-left sector of the region.
- Width & Height: Use with top-left sector to specify the size of the region.
- Scroll: Affects how the screen follows wario. it's half a byte, meaning values
  from 0 to F can be entered. Sorry the text is so compact, but it barely fits
  in. Here is the description of the important values:
    - V Norm: No horizontal scrolling, follows wario vertically.
    - H, V Norm: follows wario horizontally and vertically.
    - H, V segscrl: follows wario horizontally, and scroll vertically as an edge
      is approached.
    - V segscrl: No horizontal scrolling, but vertical scrolling as an edge is
      approached.  (segscrl stands for segmented scrolling.) Note: Ticking V
      segscrl, crop left, and crop right (without other cropping) causes very strange,
      special-case behaviour used in the boss room.
- Object Set: The object set is a description of the types of items (chests,
  keys) and enemies in an area. They can be modified if you switch back to "Edit
  level" and click on "Edit" under the object set box, just be careful if other
  regions (or even other levels!) use that object set.
- Tile Set: The tileset to use.
- Animations: I don't know much about this, but if a tile is made up of numbers,
  it's probably supposed to have an animation. This includes the doors behind
  chests, and water. Value of "1" keeps the exit door flashing, "0" stops even
  that.
- Palette Flashing: This makes palettes flash. It's used in "above the clouds"
  to make platforms fade in and out, but surprisingly, not for the exit doors.
  "0" disables palette flashing, other values probably reference sets of palettes
  to use.
- Crop: This is the other half byte of "scroll mode". If checked, 2 tiles will
  be cut off from the specified end to hide ugly warp tiles. This is purely
  visual.

ADDING REGIONS

    Level->Add region, and enter an empty sector where the new region will
reside (its top-left sector). You'll need to set most of its attributes after
adding it. You must remember to set a sector's "destination" field to somewhere
within that region, or your region will be gone after saving and reloading.
    
EDITING TILESETS

    Clicking the edit button under the tileset viewer will give you the tileset
editor. Near the top is the property field - which actually WORKS in version
0.3. I'm afraid it was bugged before.

    Closely related tilesets often share some of the same properties, so editing
one will modify the other. The first 2 fields, metatiles and flags, should not
be changed blindly - it is better to copy values from other tilesets. As for the
others, it will probably be obvious when you make the values too high. When you
do, don't use those values, because they may be data used in completely
unrelated areas of the game.

    Each tileset has 2 sets of subtiles, called "bank 0" and "bank 1" subtiles.
They can be edited in this program, but it's easier to open tile layer pro,
which was built for this sort of thing. Offsets are provided so you don't need
to search for the tiles in TLP.

    Each metatile is made up of 4 subtiles. Select a quarter of the tile with
the mouse or arrow keys. Subtiles can each be given several properties. They are
assigned a palette, and they can be flipped horizontally and vertically. The
sprite priority bit displays 3 of their 4 colors in front of sprites rather than
behind. The bank bit is put there for completeness, but it is handled
automatically - no need to change it.

    A long list of tile effects is included. Most are self-
explanitory. There are many which involve breaking into a different metatile.
They look something like, for instance, "Hard breaks (78)", which means it is a
hard, breakable tile which turns into tile 7a when broken. Only tiles 78-7f (the 
last 8 tiles in the tileset) can be used as the new identity of a breakable 
tile.

EDITING OBJECT SETS

    An object set consists of an itemSet and an enemySet. Editing the itemSet is
pretty straightforward - just select the colour of keys and chests.
Unfortunately you can't have a key and a chest of different colours in the same
region, as far as I know. Also there's this "pinkish" colour which acts
identical to grey. This is used to color a single paragoom's umbrella pink.

	Each enemySet has a nickname to maybe make them easier to keep track of. The 
"base bank" field should be either 68 or 6c - this is the first of 4 banks where 
graphics are taken from. Then there are 4 graphics slots and a ton of object 
slots. Generally the first graphic slot corresponds to object 4, the second to 
object 5, etc. But this isn't enforced by anything.

LEVEL VERSIONS

    Every level has 8 versions of itself. That's right, 8: 4 in the day, 4 in
the night. As of version 0.2, they can be edited distinctly. They could not be
before, because often, different versions of a level use the same data to save
space. So if you edit one version, you could end up editing half of the other
versions - or worse yet, have the changes to tiles carried over to other
versions, but not the changes to regions.

    To manage this, use Level->Compare Level, and enter another version of the
level being edited to compare it with. You will be told whether their "level
data" (tiles and objects) and "warp data" (regions, and the "Destination" field)
are separate or joined. If they are joined, you will be offered to separate them
- if not, you'll be offered to merge them. Only the level currently being edited
will be modified by doing this - if they are merged, then the level being
edited will be changed to reference the other level's data, rather than
vice-versa.

    Here's a tip: day and night versions nearly always use different warp data.
Because regions are an abstraction of the warp data, warp data determines what
tilesets are used in the level, and in the night, the tilesets are usually
different. So they must use different warp data to use different tilesets.

MUSIC

    Changing the music used in a level is a straightforward process, and you can
change music in other areas of the game with "other->Misc Music". I'd like to
note one thing, though: all of these music fields are 16-bit values. They appear
to be 8-bit because there are less than 0x100 songs. Starting at 0x101 are sound
effects. You can enter such a value simply by replacing the text with the number
you want.

EDITING TEXT

    You can edit text from the game, but its format is very strict. The text can
be parsed in different ways, but the following is most common:

    Text is arranged in groups of 4 lines, with each line having at most 16
characters. In the first group, the first line is the name of the speaker,
followed by 3 blank lines. Every subsequent group of 4 lines is a block of text,
but the 4th line in the group isn't normally used.  When the text is over, a
special character denoted by {end} tells the game to stop reading text. This
character must be the first character in a group of 4 lines. If in doubt, follow
the format of the original text.

    Also note that arbitrary bytes can be inserted into the text. If you wanted
to insert byte 0x26, which is a japanese character of some sort, you would type
{26}.

EDITING CREDITS

    Credit text is very different from other text, so it has its own editor.
Editing the text itself is easy. If the new text is long, you'll see a
horizontal blue line. Past this line, the screen scrolls twice as fast, so you
should try to fit everything in before that. If your text is REALLY long, you'll
see a red line which is the hard limit to your text length.

    Letters can be given palettes too, which is why there is a preview. You can
modify the palettes by selecting the text in the preview and editing the
"palette" field.

    You can also edit the font to insert your own characters or images, but you
can't use tile layer pro since it's compressed. Say you made a new character
just after the "0" character. If you click on the tile, you'll see text at the
bottom saying you've selected tile 174 (in hex). Subtract 0x100 from this number
resulting in 0x74. Then in the text box, write {74} where you want that
character to appear. Finally, if that character can be mapped to something from
your keyboard, open "creditTextTable.txt" and insert that value, following the
format of the other lines. 

ABOUT THE "REF" FOLDER

    All the files in the "ref" folder are processed by this program for various
reasons. You can add lines to these files if you have some idea what you're
doing - most usefully, you can modify "tileEffects.txt" to add any tile effects
I may have missed. Formatting is important though: make sure that any 2 values
on the same line are separated by an equals sign or a tab; and comments must 
begin the line with a semicolon.

CONTACT ME

	My email is stewartmatthew6@gmail.com.
