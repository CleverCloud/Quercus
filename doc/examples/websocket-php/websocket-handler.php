<?php
/*
 * server-side PHP code for the websocket
 */

// read the next packet from the client 
$value = websocket_read();

// send responses based on the request
// hello -> world
// server -> Resin PHP
//
if ($value == "hello") {
 websocket_write("world");
} 
else if ($value == "server") {
 websocket_write("Resin PHP");
}

