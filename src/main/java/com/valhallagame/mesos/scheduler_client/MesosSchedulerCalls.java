package com.valhallagame.mesos.scheduler_client;

import java.util.List;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.Filters;
import org.apache.mesos.v1.Protos.KillPolicy;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile;

import com.google.protobuf.ByteString;

public interface MesosSchedulerCalls {

	public void teardown();

	public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations);

	public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations, Filters filters);

	public void decline(List<OfferID> offerIds);

	public void decline(List<OfferID> offerIds, Filters filters);

	public void acceptInverseOffers(List<OfferID> offerIds);

	public void acceptInverseOffers(List<OfferID> offerIds, Filters filters);

	public void declineInverseOffers(List<OfferID> offerIds);

	public void declineInverseOffers(List<OfferID> offerIds, Filters filters);

	public void kill(TaskID taskId);

	public void kill(TaskID taskId, AgentID agentId, KillPolicy killPolicy);

	public void kill(TaskID taskId, KillPolicy killPolicy);

	public void kill(TaskID taskId, AgentID agentId);

	public void revive();

	public void shutdown(ExecutorID executorId);

	public void shutdown(ExecutorID executorId, AgentID agentId);

	public void acknowledge(AgentID agentId, TaskID taskId, ByteString uuid);

	public void reconsile(List<Reconcile.Task> tasks);

	public void message(AgentID agentId, ExecutorID executorId, ByteString data);

	public void request(List<org.apache.mesos.v1.Protos.Request> requests);

}
