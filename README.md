# Java based Mesos Scheduler client

This is a simple client for easier implementation of scheduler logic. It is a wrapper around the mesos [RxJava client](https://github.com/mesosphere/mesos-rxjava). But no RxJava is needed for implementation.

This wrapper also remembers the frameworkId and handles acknowledge calls automatically. But you do need to save it if you want to be able to reconnect to the same framework.

## Usage

You can get hold of this code by either cloning this repo or by dependency:

    <dependency>
      <groupId>com.valhalla-game</groupId>
      <artifactId>mesos-scheduler-client</artifactId>
      <version>0.0.2</version>
    </dependency>

Extend the abstract class [MesosSchedulerClient](src/main/java/com/valhallagame/mesos/scheduler_client/MesosSchedulerClient.java) and implement the abstract methods.
To know what calls you can make to the server, look at the [MesosSchedulerCalls](src/main/java/com/valhallagame/mesos/scheduler_client/MesosSchedulerCalls.java) file. To know what data you can get back from mesos, look at the [MesosSchedulerCallbacks](src/main/java/com/valhallagame/mesos/scheduler_client/MesosSchedulerCallbacks.java) file.

#Changelog

*0.0.1* Inital version

*0.0.2* Added auto acknowledge to update calls
