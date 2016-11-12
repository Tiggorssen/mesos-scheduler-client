package com.valhallagame.mesos.scheduler_client;

import java.util.List;

import org.apache.mesos.v1.Protos.InverseOffer;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos;

public interface MesosSchedulerCallbacks {

	public void receivedSubscribed(Protos.Event.Subscribed subscribed);

	public void receivedOffers(List<Offer> offers);

	public void receivedInverseOffers(List<InverseOffer> offers);

	public void receivedRescind(OfferID offerId);

	public void receivedRescindInverseOffer(OfferID offerId);

	public void receivedUpdate(TaskStatus update);

	public void receivedMessage(Protos.Event.Message message);

	public void receivedFailure(Protos.Event.Failure failure);

	public void receivedError(String message);

	public void receivedHeartbeat();
}
