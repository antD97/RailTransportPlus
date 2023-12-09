# Rail Transport Plus

[Modrinth]() |
[Planet Minecraft]() |
[CurseForge]() |
[Video Demo]() |
[Downloads](https://github.com/antD97/RailTransportPlus/releases)

A [Minecraft](https://www.minecraft.net) mod that adds linkable minecarts and faster than vanilla minecart speed while
trying to stay true to the vanilla experience.

# Cart Linking

Two carts can be linked by holding a chain and crouch right-clicking a minecart with a chain, and then crouch 
right-clicking another minecart. The second minecart that was right-clicked will then ignore all vanilla minecart 
movement mechanics and follow the first right-clicked minecart.

A train of linked carts can be made by shift right-clicking the rear most minecart and then shift right-clicking 
another minecart. There is a max train length of four minecarts, unless furnace minecarts are added to the front of 
the train. The max length of the train per furnace minecart is as follows:
- 0 or 1 furnace minecart: 4 minecart limit
- 2 furnace minecarts: 8 minecart limit
- 3 furnace minecarts: 12 minecart limit
- 4 furnace minecarts: 16 minecart limit

Note: If one or more minecarts in a train end up in unloaded chunks, the entire train stops moving.

# Furnace Minecart Boosting

Fueled furnace minecarts rolling on powered rails will continue to increase speed until they reach 65 m/s. Boosted 
furnace minecarts can be combined with cart linking to move players and items across long distances quickly. If a 
minecart train uses multiple furnace minecarts, all furnace minecarts in the train must be fueled for the boosting 
to activate.

Furnace minecarts also have the added ability to be refueled without player interaction. If the cart immediately 
following the furnace carts of train is a chest cart, the furnace carts will automatically take coal from the chest 
cart to refuel themselves.


Takes 7.5s for a non boosted furnace cart to reach max boost while on a powered rail
Takes 5s for a max boost furnace cart to return to zero total boost




# Design, Objectives, and Balance

Improve functionality to underutilized furnace minecarts

Provide basic mechanics that can be used by players to create creative complex systems. Some problems are left 
for the player to solve (e.g. carts going too fast need to slow down for corners) 

Default max boosted furnace cart speed of 65 m/s was chosen to be competitive to other transportation options on
effort for travel speed.

- rail transport plus boosted furnace cart
  - 65 m/s (45.96 m/s if travelling diagonally by moving across each horizontal axis)
  - pros
    - lots of storage/seating
    - afk-able
    - faster than elytra
  - cons
    - substantial infrastructure cost
    - optional station infrastructure cost
- elytra
  - 33.5 m/s
  - pros
    - same speed regardless of direction
    - can go anywhere
  - cons
    - slowest for long distances
- normal ice boat road
  - 40 m/s (28.28 m/s if travelling diagonally by moving across each horizontal axis)
  - pros
    - less expensive than boosted furnace cart rail
    - faster than elytra
  - cons
    - little storage/seating
    - not afk-able
    - some infrastructure cost
- blue ice boat road
  - 72.73 m/s (51.43 m/s if travelling diagonally by moving across each horizontal axis)
  - pros
    - fast
  - cons
    - little storage/seating
    - high infrastructure cost
    - not afk-able
- Dolphin's Grace status effect with Depth Strider III and Soul Speed III on a layer of Soul Soil underwater
  - 75 m/s (53.03 m/s if travelling diagonally by moving across each horizontal axis)
  - pros
    - very fast
  - cons
    - requires appropriate enchanted gear
    - no storage/seating
    - high infrastructure cost
    - not afk-able
- Dolphin's Grace status effect with Depth Strider III, Soul Speed III on a layer of Soul Soil, and Speed II status effect underwater
  - 144 m/s (101.82 m/s if travelling diagonally by moving across each horizontal axis)
  - pros
    - fastest
  - cons
    - requires appropriate enchanted gear
    - no storage/seating
    - high infrastructure cost
    - not afk-able

I don't want to make a new best form of transportation that makes the other methods of transportation pointless, but 
rather make the player consider what form of transportation is best for their specific use case.

source for transportation speeds: https://minecraft.wiki/w/Transportation

# Configuration

Each world can be configured using the `world/config/rail-transport-plus.properties` file.

**Changing the config file located at `.minecraft/config/rail-transport-plus.properties` does not affect preexisting
worlds. This file is only used as the initial config file when creating a new world.**

- `maxBoostedSpeed` (default: 60)
  - how fast fueled furnace minecarts will travel when on powered rails in meters/second
- `maxCartsPerFurnaceCart` (default: 3)
  - how many non furnace carts are allowed on a train per furnace minecart
- `maxFurnaceCartsPerTrain` (default: 4)
  - the max number of furnace minecarts per train

## Copyright and License

Copyright © 2021-2023 antD97  
Licensed under the [MIT License](LICENSE)
