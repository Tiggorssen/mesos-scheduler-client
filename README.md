# Java based Mesos Scheduler client

A wrapper around the mesos java RX client found [here](https://github.com/mesosphere/mesos-rxjava). If you are comfortable with RXJava then I recommend to use that implementation directly.

## Usage

Extend the abstract class MesosSchedulerClient and implement the abstract methods.
To know what calls you can make to the server, look at the MesosSchedulerCalls file.
