
This directory contains the following demos :

1) multipleReceives - implements the that create Java threads for implementing the conventional way that a Java application would create a non-blocking receive() on a socket.  The demostrations show that an application can spawn multiple threads and that AgentJ keeps a track of such threads and monitors their lifetime - this demo shows the use of creating multiple socket threads
2) mutlipleThreads - creates multiple threads and sets off timers
3) multipleSocketThreads - creates multiple socket threads and timers to trigger the points at which the application sends the messages, implemented using the java Thread.sleep() method.



