# Java based Mesos Scheduler client

A wrapper around the mesos java RX client found [here](https://github.com/mesosphere/mesos-rxjava). If you are comfortable with RXJava then I recommend to use that implementation directly.

## Usage

You can get hold of this code by either cloning this repo or by dependency:

    <dependency>
      <groupId>com.valhalla-game</groupId>
      <artifactId>mesos-scheduler-client</artifactId>
      <version>0.0.1</version>
    </dependency>

Extend the abstract class [MesosSchedulerClient](src/main/java/com/valhallagame/mesos/scheduler_client/MesosSchedulerClient.java) and implement the abstract methods.
To know what calls you can make to the server, look at the [MesosSchedulerCalls](src/main/java/com/valhallagame/mesos/scheduler_client/MesosSchedulerCalls.java) file. To know what data you can get back from mesos, look at the [MesosSchedulerCallbacks](src/main/java/com/valhallagame/mesos/scheduler_client/MesosSchedulerCallbacks.java) file.

