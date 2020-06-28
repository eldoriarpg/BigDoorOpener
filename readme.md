# Big Door Opener

This is an add-on for the amazing plugin [Big Doors](https://www.spigotmc.org/resources/big-doors.58669/).

It allows you to create doors, which open and close based on various conditions.


# Features
* Timed door. Define at which times doors should be open and closed.
* Closed door. A door which is always closed except when a player approaches it.
* Player approach. A closed door can open when a player approaches it.
* Permission door. A closed door will only open when a player with a specific permission approaches it.

# Setup
Simply drop the jar in your plugin folder.

Make sure you have [Big Doors](https://www.spigotmc.org/resources/big-doors.58669/) installed as well.

# Commands

Every door created will have a range of 10 blocks to open.

All commands have full tab completion support. So you can probably skip this part and start using it.
I highly recommend to not set an autoclose value or manage this door with redstone.

#### Create a timed door
`/bdo setTimed <doorId> <open> <close>`  
This creates a new automatic door which opens and closes at the specific time.
It will open when a player approaches it. [See permissions to change this.](#Door-with-permission)

*You can enter a time (E.g. 6:30), or a tick amount (E.g. 500) as time.*

![timed door](http://chojo.u.catgirlsare.sexy/OJikflxg.gif)

#### Create a closed door
`/bdo setClosed <doorId>`  
This creates a closed door which only opens on player approach.

![closed door](http://chojo.u.catgirlsare.sexy/ekZpqt_T.gif)

#### Set the approach range
`/bdo setRange <doorId> <range>`  
The door will open if a player is inside this range around the door.  
The center is the center of the door.  
*To disable approach opening set the range to 0*

#### Invert open
`/bdo invertOpen <doorId> <true|false>`  
This inverts the state of the door.  
*Big Doors assumes that a created door is in closed state. If you created it in open state you need to set this to `true`*

#### Door with permission
`/bdo requiresPermission <doorId> <true|false|permission>`  
This sets a permission for the door.  
True will use the permission `bdo.use.<doorId>`.  
You can also enter your own permission.  
To remove the permission set this to `false`.  

If a permission is set, the door will only open for players with this permission, when it's closed.

#### Unregister a door
`/bdo unregister <doorId>`  
Removes a door. It is than no longer managed by this plugin.

#### Door info
`/bdo info <doorId>`  
This will give you information about a door.

#### Reload plugin
`/bdo about`  
This will reload the plugin with the config.

#### About
`/bdo about`
Some information about the plugin.

# Permissions

To use this plugin you will need `bdo.use`

To reload this plugin you wil need `bdo.reload`