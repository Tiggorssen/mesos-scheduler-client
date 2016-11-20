package com.valhallagame.mesos.scheduler_client;

import static com.mesosphere.mesos.rx.java.util.UserAgentEntries.literal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.Filters;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.FrameworkInfo;
import org.apache.mesos.v1.Protos.KillPolicy;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.scheduler.Protos.Call;
import org.apache.mesos.v1.scheduler.Protos.Call.Accept;
import org.apache.mesos.v1.scheduler.Protos.Call.AcceptInverseOffers;
import org.apache.mesos.v1.scheduler.Protos.Call.Acknowledge;
import org.apache.mesos.v1.scheduler.Protos.Call.Builder;
import org.apache.mesos.v1.scheduler.Protos.Call.Decline;
import org.apache.mesos.v1.scheduler.Protos.Call.DeclineInverseOffers;
import org.apache.mesos.v1.scheduler.Protos.Call.Kill;
import org.apache.mesos.v1.scheduler.Protos.Call.Message;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile;
import org.apache.mesos.v1.scheduler.Protos.Call.Request;
import org.apache.mesos.v1.scheduler.Protos.Call.Shutdown;
import org.apache.mesos.v1.scheduler.Protos.Call.Subscribe;
import org.apache.mesos.v1.scheduler.Protos.Call.Type;
import org.apache.mesos.v1.scheduler.Protos.Event;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.mesosphere.mesos.rx.java.AwaitableSubscription;
import com.mesosphere.mesos.rx.java.MesosClientBuilder;
import com.mesosphere.mesos.rx.java.SinkOperation;
import com.mesosphere.mesos.rx.java.SinkOperations;
import com.mesosphere.mesos.rx.java.protobuf.ProtobufMesosClientBuilder;
import com.mesosphere.mesos.rx.java.util.UserAgentEntry;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

/**
 * Extend this class to make a simple Mesos scheduler. To correctly close the
 * connection when done you need to call {@link #close()}.
 * 
 * To know more about the messages, please see the documentation on the mesos
 * website.
 * 
 * Here for the general api:
 * http://mesos.apache.org/documentation/latest/scheduler-http-api/
 * 
 * And here for the proto classes:
 * https://github.com/apache/mesos/blob/master/include/mesos/v1/scheduler/scheduler.proto
 * 
 * Please have a look at {@link MesosSchedulerCallbacks} to see a list of
 * responses from Mesos and {@link MesosSchedulerCalls} for a list of calls you
 * can make to mesos
 * 
 */
public abstract class MesosSchedulerClient implements MesosSchedulerCallbacks, MesosSchedulerCalls {

	private static final Logger log = LoggerFactory.getLogger(MesosSchedulerClient.class);

	private SerializedSubject<Optional<SinkOperation<Call>>, Optional<SinkOperation<Call>>> publisher;

	private FrameworkID frameworkId;

	private @NotNull AwaitableSubscription openStream;

	private Thread subscriberThread;

	/**
	 * A subscribe call with some default values.
	 * 
	 * See {{@link #subscribe(URL, String, double, String, Function, String)}}
	 * for documentations
	 */
	public void subscribe(URL mesosMaster, String frameworkName) throws URISyntaxException {
		subscribe(mesosMaster, frameworkName, 3600, "*", literal("MesosJavaClient", "0.1"), null);
	}

	/**
	 * A subscribe call with some default values.
	 * 
	 * See {{@link #subscribe(URL, String, double, String, Function, String)}}
	 * for documentations
	 */
	public void subscribe(URL mesosMaster, String frameworkName, String frameworkId) throws URISyntaxException {
		subscribe(mesosMaster, frameworkName, 3600, "*", literal("MesosJavaClient", "0.1"), frameworkId);
	}

	/**
	 * A subscribe call with some default values.
	 * 
	 * See {{@link #subscribe(URL, String, double, String, Function, String)}}
	 * for documentations
	 */
	public void subscribe(URL mesosMaster, double failoverTimeout, String frameworkName) throws URISyntaxException {
		subscribe(mesosMaster, frameworkName, failoverTimeout, "*", literal("MesosJavaClient", "0.1"), null);
	}

	/**
	 * A subscribe call with some default values.
	 * 
	 * See {{@link #subscribe(URL, String, double, String, Function, String)}}
	 * for documentations
	 */
	public void subscribe(URL mesosMaster, double failoverTimeout, String frameworkName, String frameworkId)
			throws URISyntaxException {
		subscribe(mesosMaster, frameworkName, failoverTimeout, "*", literal("MesosJavaClient", "0.1"), frameworkId);
	}

	/**
	 * 
	 * The first call to mesos, needed to setup connection properly and identify
	 * a framework.
	 * 
	 * @param mesosMaster
	 *            The URL on with to connect, for example
	 *            "http://mesos-master.example.com/api/v1/scheduler"
	 * @param frameworkName
	 *            The name of the framework, will be used in mesos http-gui and
	 *            other places for human identification.
	 * @param failoverTimeout
	 *            The time the mesos master waits until it declares the
	 *            framework dead. I.e if the scheduler lost connection to the
	 *            master, it can reconnect again if within the failoverTimeout
	 *            duration (in sec).
	 * @param mesosRole
	 *            See http://mesos.apache.org/documentation/latest/roles/ for
	 *            roles. If you are uncertain use "*" as it is the default role.
	 * @param applicationUserAgentEntry
	 *            Used for generating a user agent for this client. se
	 *            {@link com.mesosphere.mesos.rx.java.util.UserAgentEntries.literal}
	 *            for example on how to create one.
	 * @param frameworkId
	 *            The framework ID is used for identifying the framework, if not
	 *            provided when calling subscribe a new frameworkId will be
	 *            generated by the master and returned in the receivecSuberibe
	 *            call. You need to use this if you want to reconnect to the
	 *            same framework.
	 * @throws URISyntaxException
	 *             if the URL provided was not a syntactically correct URL.
	 */
	public void subscribe(URL mesosMaster, String frameworkName, double failoverTimeout, String mesosRole,
			Function<Class<?>, UserAgentEntry> applicationUserAgentEntry, String frameworkId)
			throws URISyntaxException {

		if (openStream == null || openStream.isUnsubscribed()) {

			// Donno if this can happen, lets stay safe.
			if (subscriberThread != null) {
				subscriberThread.interrupt();
			}

			subscriberThread = new Thread() {
				@Override
				public void run() {
					try {
						connect(mesosMaster, frameworkName, failoverTimeout, mesosRole, applicationUserAgentEntry,
								frameworkId);
					} catch (URISyntaxException e) {
						log.error("Could not connect: ", e);
					}
				}

			};
			subscriberThread.start();
		}
	}

	/**
	 * Sets up the connection and is blocking in wait for calls from mesos
	 * master.
	 */
	private void connect(URL mesosMaster, String frameworkName, double failoverTimeout, String mesosRole,
			Function<Class<?>, UserAgentEntry> applicationUserAgentEntry, String frameworkId)
			throws URISyntaxException {

		MesosClientBuilder<Call, Event> clientBuilder = ProtobufMesosClientBuilder.schedulerUsingProtos()
				.mesosUri(mesosMaster.toURI()).applicationUserAgentEntry(applicationUserAgentEntry);

		FrameworkInfo.Builder frameworkBuilder = Protos.FrameworkInfo.newBuilder()
				.setUser(Optional.ofNullable(System.getenv("user")).orElse("root")) // https://issues.apache.org/jira/browse/MESOS-3747
				.setName(frameworkName).setFailoverTimeout(failoverTimeout).setRole(mesosRole);

		if (frameworkId != null && !frameworkId.isEmpty()) {
			frameworkBuilder.setId(Protos.FrameworkID.newBuilder().setValue(frameworkId));
		}

		FrameworkInfo fw = frameworkBuilder.build();

		Call.Builder sub = Call.newBuilder().setType(Type.SUBSCRIBE)
				.setSubscribe(Subscribe.newBuilder().setFrameworkInfo(fw));

		if (frameworkId != null && !frameworkId.isEmpty()) {
			sub.setFrameworkId(Protos.FrameworkID.newBuilder().setValue(frameworkId));
		}

		MesosClientBuilder<Call, Event> subscribe = clientBuilder.subscribe(sub.build());

		subscribe.processStream(unicastEvents -> {

			// In here we handle all data from Mesos and pipe it to the
			// corresponding abstract callback.

			final Observable<Event> events = unicastEvents.share();

			events.filter(event -> event.getType() == Event.Type.ERROR)
					.subscribe(e -> receivedError(e.getError().getMessage()));

			events.filter(event -> event.getType() == Event.Type.FAILURE)
					.subscribe(e -> receivedFailure(e.getFailure()));

			events.filter(event -> event.getType() == Event.Type.HEARTBEAT).subscribe(e -> receivedHeartbeat());

			events.filter(event -> event.getType() == Event.Type.INVERSE_OFFERS)
					.subscribe(e -> receivedInverseOffers(e.getInverseOffers().getInverseOffersList()));

			events.filter(event -> event.getType() == Event.Type.MESSAGE)
					.subscribe(e -> receivedMessage(e.getMessage()));

			events.filter(event -> event.getType() == Event.Type.OFFERS)
					.subscribe(e -> receivedOffers(e.getOffers().getOffersList()));

			events.filter(event -> event.getType() == Event.Type.RESCIND)
					.subscribe(e -> receivedRescind(e.getRescind().getOfferId()));

			events.filter(event -> event.getType() == Event.Type.RESCIND_INVERSE_OFFER)
					.subscribe(e -> receivedRescindInverseOffer(e.getRescindInverseOffer().getInverseOfferId()));

			events.filter(event -> event.getType() == Event.Type.SUBSCRIBED).subscribe(e -> {
				this.frameworkId = e.getSubscribed().getFrameworkId();
				receivedSubscribed(e.getSubscribed());
			});

			events.filter(event -> event.getType() == Event.Type.UPDATE)
					.subscribe(e -> receivedUpdate(e.getUpdate().getStatus()));

			// This is the observable that is responsible for sending calls to
			// mesos master.
			PublishSubject<Optional<SinkOperation<Call>>> p = PublishSubject.create();

			// toSerialised handles the fact that we can add calls on different
			// threads.
			publisher = p.toSerialized();
			return publisher;
		});

		com.mesosphere.mesos.rx.java.MesosClient<Call, Event> client = clientBuilder.build();
		openStream = client.openStream();
		try {
			openStream.await();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void close() throws IOException {
		if (openStream != null) {
			if (!openStream.isUnsubscribed()) {
				openStream.unsubscribe();
			}
		}
	}

	public void sendCall(Call call) {
		if (publisher == null) {
			throw new RuntimeException("No publisher found, please call subscribe before sending anything.");
		}
		publisher.onNext(Optional.of(SinkOperations.create(call)));
	}

	public void sendCall(Call.Builder b, Type t) {
		Call call = b.setType(t).setFrameworkId(frameworkId).build();
		sendCall(call);
	}

	@Override
	public void teardown() {
		sendCall(build(), Type.TEARDOWN);
	}

	@Override
	public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations) {
		Builder accept = build()
				.setAccept(Accept.newBuilder().addAllOfferIds(offerIds).addAllOperations(offerOperations));
		sendCall(accept, Type.ACCEPT);
	}

	@Override
	public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations, Filters filters) {
		Builder accept = build().setAccept(
				Accept.newBuilder().addAllOfferIds(offerIds).addAllOperations(offerOperations).setFilters(filters));
		sendCall(accept, Type.ACCEPT);
	}

	@Override
	public void decline(List<OfferID> offerIds) {
		Builder decline = build().setDecline(Decline.newBuilder().addAllOfferIds(offerIds));
		sendCall(decline, Type.DECLINE);
	}

	@Override
	public void decline(List<OfferID> offerIds, Filters filters) {
		Builder decline = build().setDecline(Decline.newBuilder().addAllOfferIds(offerIds).setFilters(filters));
		sendCall(decline, Type.DECLINE);
	}

	@Override
	public void acceptInverseOffers(List<OfferID> offerIds) {
		Builder acceptInverseOffers = build()
				.setAcceptInverseOffers(AcceptInverseOffers.newBuilder().addAllInverseOfferIds(offerIds));
		sendCall(acceptInverseOffers, Type.ACCEPT_INVERSE_OFFERS);
	}

	@Override
	public void acceptInverseOffers(List<OfferID> offerIds, Filters filters) {
		Builder acceptInverseOffers = build().setAcceptInverseOffers(
				AcceptInverseOffers.newBuilder().addAllInverseOfferIds(offerIds).setFilters(filters));
		sendCall(acceptInverseOffers, Type.ACCEPT_INVERSE_OFFERS);
	}

	@Override
	public void declineInverseOffers(List<OfferID> offerIds) {
		Builder declineInverseOffers = build()
				.setDeclineInverseOffers(DeclineInverseOffers.newBuilder().addAllInverseOfferIds(offerIds));
		sendCall(declineInverseOffers, Type.DECLINE_INVERSE_OFFERS);
	}

	@Override
	public void declineInverseOffers(List<OfferID> offerIds, Filters filters) {
		Builder declineInverseOffers = build().setDeclineInverseOffers(
				DeclineInverseOffers.newBuilder().addAllInverseOfferIds(offerIds).setFilters(filters));
		sendCall(declineInverseOffers, Type.DECLINE_INVERSE_OFFERS);
	}

	@Override
	public void kill(TaskID taskId) {
		Builder kill = build().setKill(Kill.newBuilder().setTaskId(taskId));
		sendCall(kill, Type.KILL);
	}

	@Override
	public void kill(TaskID taskId, AgentID agentId, KillPolicy killPolicy) {
		Builder kill = build()
				.setKill(Kill.newBuilder().setTaskId(taskId).setAgentId(agentId).setKillPolicy(killPolicy));
		sendCall(kill, Type.KILL);
	}

	@Override
	public void kill(TaskID taskId, KillPolicy killPolicy) {
		Builder kill = build().setKill(Kill.newBuilder().setTaskId(taskId).setKillPolicy(killPolicy));
		sendCall(kill, Type.KILL);
	}

	@Override
	public void kill(TaskID taskId, AgentID agentId) {
		Builder kill = build().setKill(Kill.newBuilder().setTaskId(taskId).setAgentId(agentId));
		sendCall(kill, Type.KILL);
	}

	@Override
	public void revive() {
		Builder revive = build();
		sendCall(revive, Type.REVIVE);
	}

	@Override
	public void shutdown(ExecutorID executorId) {
		Builder shutdown = build().setShutdown(Shutdown.newBuilder().setExecutorId(executorId));
		sendCall(shutdown, Type.SHUTDOWN);
	}

	@Override
	public void shutdown(ExecutorID executorId, AgentID agentId) {
		Builder shutdown = build().setShutdown(Shutdown.newBuilder().setExecutorId(executorId).setAgentId(agentId));
		sendCall(shutdown, Type.SHUTDOWN);
	}

	@Override
	public void acknowledge(AgentID agentId, TaskID taskId, ByteString uuid) {
		Builder acknowledge = build()
				.setAcknowledge(Acknowledge.newBuilder().setAgentId(agentId).setTaskId(taskId).setUuid(uuid));
		sendCall(acknowledge, Type.ACKNOWLEDGE);
	}

	@Override
	public void reconsile(List<Reconcile.Task> tasks) {
		Builder reconsile = build().setReconcile(Reconcile.newBuilder().addAllTasks(tasks));
		sendCall(reconsile, Type.RECONCILE);
	}

	@Override
	public void message(AgentID agentId, ExecutorID executorId, ByteString data) {
		Builder message = build()
				.setMessage(Message.newBuilder().setAgentId(agentId).setExecutorId(executorId).setData(data));
		sendCall(message, Type.MESSAGE);
	}

	@Override
	public void request(List<org.apache.mesos.v1.Protos.Request> requests) {
		Builder request = build().setRequest(Request.newBuilder().addAllRequests(requests));
		sendCall(request, Type.REQUEST);
	}

	private static Builder build() {
		return Call.newBuilder();
	}

}
