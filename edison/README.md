Edison-specific config and helpers for gabeldorsche

*init_gpios.sh*
Sets up the default state for gabeldorsche on boot
* Clears the gpio initial states
* Unblocks bluetooth
* Blocks wifi (to save power)

Push to /etc/init.d/init_gpios.sh

*gabeldorsche.service*
Runs the belt daemon on boot

Push to /etc/systemd/system/gabeldorsche.service

*vibrate.sh*
Helper script to test the vibration motors during hardware development.
