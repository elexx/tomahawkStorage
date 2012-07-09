TomahawkStorage
===============

This is a [Tomhawk](http://www.tomahawk-player.org/) lan protocol implementation in Java.

Usage
-----
Clone it, build it, change the config (storage.conf and watchedFolders.conf) to your needs, and run the previously build jar. Other Tomahawk players in your network will now see this storage-only player, which only purpose is to serve music.

Why?
----
Their is no GUI on our home-server, but I want to listen to music stored on it using my local Tomahawk client, so I began to develop this headless, serve-only Tomahawk client.

### Why not just a resolver and a daap server?
Because we have more than one storage for music here and a zeroconf solution is much nicer than a solution which requires adaption for each new server/notebook/pc.

TODO
----
A lot! First of all the project should be converted to a maven project. Some tests for the protocol would be nice. A big refactoring of the protocol class. Many many other things. Just feel free to fork the project and help improve it!
