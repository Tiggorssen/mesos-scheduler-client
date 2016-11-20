package com.valhallagame.mesos.scheduler_client;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.Filters;
import org.apache.mesos.v1.Protos.KillPolicy;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile;

import com.google.protobuf.ByteString;
import com.mesosphere.mesos.rx.java.util.UserAgentEntry;

/**
 * 
 * The calls you can make to the Mesos master
 * 
 * Documentation copied from
 * https://github.com/apache/mesos/blob/master/include/mesos/v1/scheduler/scheduler.proto
 *
 */
public interface MesosSchedulerCalls {

	/**
	 * Subscribes the scheduler with the master to receive events. A scheduler
	 * must send other calls only after it has received the SUBCRIBED event.
	 */
	public void subscribe(URL mesosMaster, String frameworkName, double failoverTimeout, String mesosRole,
			Function<Class<?>, UserAgentEntry> applicationUserAgentEntry, String frameworkId) throws URISyntaxException;

	public void teardown();

	/**
	 * Accepts an offer, performing the specified operations in a sequential
	 * manner.
	 * 
	 * Note that any of the offer’s resources not used in the 'Accept' call
	 * (e.g., to launch a task) are considered unused and might be reoffered to
	 * other frameworks. In other words, the same OfferID cannot be used in more
	 * than one 'Accept' call.
	 */
	public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations);

	/**
	 * Accepts an offer, performing the specified operations in a sequential
	 * manner.
	 * 
	 * Note that any of the offer’s resources not used in the 'Accept' call
	 * (e.g., to launch a task) are considered unused and might be reoffered to
	 * other frameworks. In other words, the same OfferID cannot be used in more
	 * than one 'Accept' call.
	 */
	public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations, Filters filters);

	/**
	 * Declines an offer, signaling the master to potentially reoffer the
	 * resources to a different framework. Note that this is same as sending an
	 * Accept call with no operations. See comments on top of 'Accept' for
	 * semantics.
	 */
	public void decline(List<OfferID> offerIds);

	/**
	 * Declines an offer, signaling the master to potentially reoffer the
	 * resources to a different framework. Note that this is same as sending an
	 * Accept call with no operations. See comments on top of 'Accept' for
	 * semantics.
	 */
	public void decline(List<OfferID> offerIds, Filters filters);

	/**
	 * Accepts an inverse offer. Inverse offers should only be accepted if the
	 * resources in the offer can be safely evacuated before the provided
	 * unavailability.
	 */
	public void acceptInverseOffers(List<OfferID> offerIds);

	/**
	 * Accepts an inverse offer. Inverse offers should only be accepted if the
	 * resources in the offer can be safely evacuated before the provided
	 * unavailability.
	 */
	public void acceptInverseOffers(List<OfferID> offerIds, Filters filters);

	/**
	 * Declines an inverse offer. Inverse offers should be declined if the
	 * resources in the offer might not be safely evacuated before the provided
	 * unavailability.
	 */
	public void declineInverseOffers(List<OfferID> offerIds);

	/**
	 * Declines an inverse offer. Inverse offers should be declined if the
	 * resources in the offer might not be safely evacuated before the provided
	 * unavailability.
	 */
	public void declineInverseOffers(List<OfferID> offerIds, Filters filters);

	/**
	 * Kills a specific task. If the scheduler has a custom executor, the kill
	 * is forwarded to the executor and it is up to the executor to kill the
	 * task and send a TASK_KILLED (or TASK_FAILED) update. Note that Mesos
	 * releases the resources for a task once it receives a terminal update (See
	 * TaskState in v1/mesos.proto) for it. If the task is unknown to the
	 * master, a TASK_LOST update is generated.
	 * 
	 * If a task within a task group is killed before the group is delivered to
	 * the executor, all tasks in the task group are killed. When a task group
	 * has been delivered to the executor, it is up to the executor to decide
	 * how to deal with the kill. Note The default Mesos executor will currently
	 * kill all the tasks in the task group if it gets a kill for any task.
	 */
	public void kill(TaskID taskId);

	/**
	 * Kills a specific task. If the scheduler has a custom executor, the kill
	 * is forwarded to the executor and it is up to the executor to kill the
	 * task and send a TASK_KILLED (or TASK_FAILED) update. Note that Mesos
	 * releases the resources for a task once it receives a terminal update (See
	 * TaskState in v1/mesos.proto) for it. If the task is unknown to the
	 * master, a TASK_LOST update is generated.
	 * 
	 * If a task within a task group is killed before the group is delivered to
	 * the executor, all tasks in the task group are killed. When a task group
	 * has been delivered to the executor, it is up to the executor to decide
	 * how to deal with the kill. Note The default Mesos executor will currently
	 * kill all the tasks in the task group if it gets a kill for any task.
	 */
	public void kill(TaskID taskId, AgentID agentId, KillPolicy killPolicy);

	/**
	 * Kills a specific task. If the scheduler has a custom executor, the kill
	 * is forwarded to the executor and it is up to the executor to kill the
	 * task and send a TASK_KILLED (or TASK_FAILED) update. Note that Mesos
	 * releases the resources for a task once it receives a terminal update (See
	 * TaskState in v1/mesos.proto) for it. If the task is unknown to the
	 * master, a TASK_LOST update is generated.
	 * 
	 * If a task within a task group is killed before the group is delivered to
	 * the executor, all tasks in the task group are killed. When a task group
	 * has been delivered to the executor, it is up to the executor to decide
	 * how to deal with the kill. Note The default Mesos executor will currently
	 * kill all the tasks in the task group if it gets a kill for any task.
	 */
	public void kill(TaskID taskId, KillPolicy killPolicy);

	/**
	 * Kills a specific task. If the scheduler has a custom executor, the kill
	 * is forwarded to the executor and it is up to the executor to kill the
	 * task and send a TASK_KILLED (or TASK_FAILED) update. Note that Mesos
	 * releases the resources for a task once it receives a terminal update (See
	 * TaskState in v1/mesos.proto) for it. If the task is unknown to the
	 * master, a TASK_LOST update is generated.
	 * 
	 * If a task within a task group is killed before the group is delivered to
	 * the executor, all tasks in the task group are killed. When a task group
	 * has been delivered to the executor, it is up to the executor to decide
	 * how to deal with the kill. Note The default Mesos executor will currently
	 * kill all the tasks in the task group if it gets a kill for any task.
	 */
	public void kill(TaskID taskId, AgentID agentId);

	/**
	 * Removes any previous filters set via ACCEPT or DECLINE.
	 */
	public void revive();

	/**
	 * Shuts down a custom executor. When the executor gets a shutdown event, it
	 * is expected to kill all its tasks (and send TASK_KILLED updates) and
	 * terminate. If the executor doesn’t terminate within a certain timeout
	 * (configurable via '--executor_shutdown_grace_period' agent flag), the
	 * agent will forcefully destroy the container (executor and its tasks) and
	 * transition its active tasks to TASK_LOST.
	 */
	public void shutdown(ExecutorID executorId);

	/**
	 * Shuts down a custom executor. When the executor gets a shutdown event, it
	 * is expected to kill all its tasks (and send TASK_KILLED updates) and
	 * terminate. If the executor doesn’t terminate within a certain timeout
	 * (configurable via '--executor_shutdown_grace_period' agent flag), the
	 * agent will forcefully destroy the container (executor and its tasks) and
	 * transition its active tasks to TASK_LOST.
	 */
	public void shutdown(ExecutorID executorId, AgentID agentId);

	/**
	 * Acknowledges the receipt of status update. Schedulers are responsible for
	 * explicitly acknowledging the receipt of status updates that have
	 * 'Update.status().uuid()' field set. Such status updates are retried by
	 * the agent until they are acknowledged by the scheduler.
	 */
	public void acknowledge(AgentID agentId, TaskID taskId, ByteString uuid);

	/**
	 * Allows the scheduler to query the status for non-terminal tasks. This
	 * causes the master to send back the latest task status for each task in
	 * 'tasks', if possible. Tasks that are no longer known will result in a
	 * TASK_LOST update. If 'statuses' is empty, then the master will send the
	 * latest status for each task currently known.
	 */
	public void reconsile(List<Reconcile.Task> tasks);

	/**
	 * Sends arbitrary binary data to the executor. Note that Mesos neither
	 * interprets this data nor makes any guarantees about the delivery of this
	 * message to the executor.
	 */
	public void message(AgentID agentId, ExecutorID executorId, ByteString data);

	/**
	 * Requests a specific set of resources from Mesos's allocator. If the
	 * allocator has support for this, corresponding offers will be sent
	 * asynchronously via the OFFERS event(s).
	 * 
	 * NOTE: The built-in hierarchical allocator doesn't have support for this
	 * call and hence simply ignores it.
	 */
	public void request(List<org.apache.mesos.v1.Protos.Request> requests);

}
