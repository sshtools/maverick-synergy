
package com.sshtools.synergy.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.events.EventListener;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.util.ByteBufferPool;

/**
 * Each instance of a {@link SshEngine} has a single configuration context.
 */
public class SshEngineContext {

	String product = "SSHD";
	int maximumConnections = -1;
	String tooManyConnectionsText = "Too many connections";

	SshEngine daemon;

	int permanentAcceptThreads = 1;
	int permanentConnectThreads = 1;
	int permanentTransferThreads = 2;
	int maximumChannelsPerThread = 1000;
	int idleServicePeriod = 1;
	int inactivePeriodsPerIdleEvent = 1;
	boolean useDirectByteBuffers = true;
	int bufferPoolArraySize = 65536+4096;
	Map<String, ListeningInterface> interfacesToBind = new ConcurrentHashMap<String, ListeningInterface>(8, 0.9f, 1);

	int ipv6WorkaroundPort = 60022;
	String ipv6WorkaroundBindAddress = "127.0.0.1";

	SelectorProvider selectorProvider = SelectorProvider.provider();
	ByteBufferPool bufferPool = null;

	private Map<String,Object> attributes = new HashMap<String,Object>();
	
	SshEngineContext(SshEngine daemon) {
		this.daemon = daemon;
	}

	/**
	 * Set the product name
	 * 
	 * @param String
	 *            product
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/** Get the product name used to prefix thread names */
	public String getProduct() {
		return product;
	}
	
	/**
	 * Set the maximum number of connections allowed at any one time.
	 * 
	 * @param maximumConnections
	 *            int
	 */
	public void setMaximumConnections(int maximumConnections) {
		this.maximumConnections = maximumConnections;
	}

	/**
	 * Get the maximum number of connections allowed at any one time.
	 * 
	 * @return int
	 */
	public int getMaximumConnections() {
		return maximumConnections;
	}

	/**
	 * Get the text used when disconnecting when the maximum connection threshold has been reached.
	 * @return
	 */
	public String getTooManyConnectionsText() {
		return tooManyConnectionsText;
	}

	/**
	 * Set the text used when disconnecting when the maximum connection threshold has been reached.
	 * @param tooManyConnectionsText
	 */
	public void setTooManyConnectionsText(String tooManyConnectionsText) {
		this.tooManyConnectionsText = tooManyConnectionsText;
	}
	
	/**
	 * Returns a direct buffer pool.
	 * 
	 * @return ByteBufferPool
	 */
	public synchronized ByteBufferPool getBufferPool() {
		if (bufferPool == null)
			bufferPool = new ByteBufferPool(bufferPoolArraySize,
					useDirectByteBuffers);
		return bufferPool;
	}

	/**
	 * Set the SelectorProvider used by the Selector threads.
	 * 
	 * @param selectorProvider
	 *            SelectorProvider
	 */
	public void setSelectorProvider(SelectorProvider selectorProvider) {
		this.selectorProvider = selectorProvider;
	}

	/**
	 * Get the SelectorProvider used to create Selector instances.
	 * 
	 * @return SelectorProvider
	 */
	public SelectorProvider getSelectorProvider() {
		return selectorProvider;
	}

	/**
	 * Get the instance of the SSHD for this context.
	 * 
	 * @return
	 */
	public SshEngine getEngine() {
		return daemon;
	}

	/**
	 * Determine whether the daemon is using direct byte buffers.
	 * 
	 * @return boolean
	 */
	public boolean isUsingDirectBuffers() {
		return useDirectByteBuffers;
	}

	/**
	 * Configure the byte buffer pool to use direct byte buffers.
	 * 
	 * @param useDirectByteBuffers
	 *            boolean
	 */
	public void setUsingDirectBuffers(boolean useDirectByteBuffers) {
		this.useDirectByteBuffers = useDirectByteBuffers;
	}

	/**
	 * Set the size of the byte buffers in the pool. The minimum size is 35000
	 * bytes
	 * 
	 * @param bufferPoolArraySize
	 *            int
	 */
	public void setBufferPoolArraySize(int bufferPoolArraySize) {
		if (bufferPoolArraySize < 35000)
			throw new IllegalArgumentException(
					"The buffer pool must have an array size of at least 35000 bytes (the maximum packet size supported)");
		this.bufferPoolArraySize = bufferPoolArraySize;
	}

	/**
	 * Add an interface and port to the listening socket list and provide the
	 * protocol context.
	 * 
	 * handler for subsequent connections.
	 * 
	 * @param addressToBind
	 *            String
	 * @param portToBind
	 *            int
	 * @param protocolContext
	 * @throws IOException
	 * @throws IOException
	 */
	public ListeningInterface addListeningInterface(String addressToBind,
			int portToBind, ProtocolContextFactory<?> contextFactory, boolean reuseAddress) throws IOException {
		return addListeningInterface(InetAddress.getByName(addressToBind),
				portToBind, contextFactory, reuseAddress);
	}

	/**
	 * Add an interface and port to the listening socket list and provide the
	 * protocol context.
	 * 
	 * @param addressToBind
	 * @param portToBind
	 * @param context
	 * @throws IOException
	 */
	public ListeningInterface addListeningInterface(InetAddress addressToBind,
			int portToBind, ProtocolContextFactory<?> contextFactory, boolean reuseAddress) throws IOException {
		InetSocketAddress ISA = new InetSocketAddress(addressToBind, portToBind);
		ListeningInterface li = new ListeningInterface(ISA, contextFactory);
		li.setSocketOptionReuseAddress(reuseAddress);
		
		interfacesToBind.put(ISA.toString(), li);

		if (daemon.isStarted() && !daemon.isStarting())
			daemon.startListeningInterface(li);
		return li;

	}

	/**
	 * Remove a listening interface from the daemon at runtime.
	 * 
	 * @param addressBound
	 * @param portBound
	 */
	public void removeListeningInterface(InetAddress addressBound, int portBound) {
		InetSocketAddress ISA = new InetSocketAddress(addressBound, portBound);
		ListeningInterface li = (ListeningInterface) interfacesToBind
				.remove(ISA.toString());
		if (li != null) {
			daemon.removeAcceptor(li);
		}

	}

	/**
	 * Remove a listening interface from the daemon at runtime.
	 * 
	 * @param addressBound
	 * @param portBound
	 * @throws UnknownHostException
	 */
	public void removeListeningInterface(String addressBound, int portBound)
			throws UnknownHostException {
		removeListeningInterface(InetAddress.getByName(addressBound), portBound);
	}

	/**
	 * Remove an interface and port from the listening socket list.
	 * 
	 * @param String
	 *            anInterface
	 * @deprecated use {@link removeListeningInterface(String, int)} instead.
	 * @throws IOException
	 */
	public void removeListeningInterface(String anInterface) throws IOException {
		interfacesToBind.remove(anInterface);
	}

	//
	// /**
	// * Gets the protocol context for an interface.
	// *
	// * @param addressToBind String
	// * @param portToBind int
	// * @return ProtocolContext
	// */
	// public ProtocolContext getProtocolContext(String addressToBind, int
	// portToBind) {
	// return (ProtocolContext)interfacesToBind.get(addressToBind + ":" +
	// portToBind);
	// }

	// /**
	// * Get the listening socket list.
	// * @return
	// */
	// public String[] getListeningInterfaces() {
	// String[] interfaces = new String[interfacesToBind.size()];
	// interfacesToBind.keySet().toArray(interfaces);
	// return interfaces;
	// }

	/**
	 * Get the listening socket list.
	 * 
	 * @return InetSocketAddress[]
	 */
	public ListeningInterface[] getListeningInterfaces() {
		return (ListeningInterface[]) interfacesToBind.values().toArray(
				new ListeningInterface[interfacesToBind.size()]);
	}

	/**
	 * Get the number of permanent accept threads.
	 * 
	 * @return int
	 */
	public int getPermanentAcceptThreads() {
		return permanentAcceptThreads;
	}

	/**
	 * Set the number of permanent accept threads.
	 * <p>
	 * An accept thread services the asynchronous server socket by processing
	 * requests for connections. Once a connection has been accepted it is then
	 * registered with a transfer thread where all IO is handled.
	 * <p>
	 * The server maintains this number of permanent threads but will also
	 * dynamically create additional threads if the permanent threads are
	 * overloaded.
	 * 
	 * @param permanentAcceptThreads
	 */
	public void setPermanentAcceptThreads(int permanentAcceptThreads) {
		if (permanentAcceptThreads < 1)
			throw new IllegalArgumentException(
					"There must be at least one permanent ACCEPT thread");

		this.permanentAcceptThreads = permanentAcceptThreads;
	}

	/**
	 * Get the number of permanent connect threads.
	 * 
	 * @return
	 */
	public int getPermanentConnectThreads() {
		return permanentConnectThreads;
	}

	/**
	 * Set the number of permanent connect threads. When existing SSH
	 * connections make outgoing socket connections through port forwarding; the
	 * asynchronous connection process is handled by these threads. Once the
	 * connection has been established the socket is then registered with a
	 * transfer thread where all IO is performed.
	 * 
	 * @param permanentConnectThreads
	 */
	public void setPermanentConnectThreads(int permanentConnectThreads) {
		if (permanentConnectThreads < 1)
			throw new IllegalArgumentException(
					"There must be at least one permanent CONNECT thread");

		this.permanentConnectThreads = permanentConnectThreads;
	}

	/**
	 * Get the number of permanent transfer threads.
	 * 
	 * @return
	 */
	public int getPermanentTransferThreads() {
		return permanentTransferThreads;
	}

	/**
	 * Set the number of permanent transfer threads. Once a socket has either
	 * been accepted or connected, the socket is registered with a transfer
	 * thread. This thread asynchronously performs all the IO for the socket. If
	 * all the permanent threads become fully loaded then additional threads
	 * will be created to handle additional connections and shutdown once they
	 * have no sockets to service.
	 * 
	 * @param permanentAcceptThreads
	 */
	public void setPermanentTransferThreads(int permanentTransferThreads) {
		if (permanentTransferThreads < 1)
			throw new IllegalArgumentException(
					"There must be at least one permanent TRANSFER thread");

		this.permanentTransferThreads = permanentTransferThreads;
	}

	/**
	 * Get the maximum number of channels that can be serviced by a single
	 * selector thread.
	 * 
	 * @return
	 */
	public int getMaximumChannelsPerThread() {
		return maximumChannelsPerThread;
	}

	/**
	 * Set the maximum number of channels that can be serviced by a single
	 * selector thread. Setting this value to 1 effectivley gives you a
	 * thread-per-connection model.
	 * 
	 * @param maximumChannelsPerThread
	 */
	public void setMaximumChannelsPerThread(int maximumChannelsPerThread) {
		if (maximumChannelsPerThread < 1)
			throw new IllegalArgumentException(
					"You must have at least 1 selector per thread");

		this.maximumChannelsPerThread = maximumChannelsPerThread;

	}

	/**
	 * Get the time in seconds for each idle period service run. For example if
	 * this setting is 10 (default) then every 10 seconds an idle service run
	 * will be performed. This will process all the current channels on a thread
	 * and evaluate whether an idle event should be fired on the channel.
	 * 
	 * @return int
	 */
	public int getIdleServiceRunPeriod() {
		return idleServicePeriod;
	}

	/**
	 * 
	 * @param idleServicePeriod
	 *            int
	 */
	public void setIdleServiceRunPeriod(int idleServicePeriod) {
		this.idleServicePeriod = idleServicePeriod;
	}

	/**
	 * To determine whether any channels are idle a service run is performed to
	 * evaluate the state of each channel. Each time a channel is found to be
	 * inactive we flag it. This setting defines how many times it should be
	 * flagged consequentivly before an idle event is fired.
	 * 
	 * @return int
	 */
	public int getInactiveServiceRunsPerIdleEvent() {
		return inactivePeriodsPerIdleEvent;
	}

	/**
	 * To determine whether any channels are idle a service run is performed to
	 * evaluate the state of each channel. Each time a channel is found to be
	 * inactive we flag it. This setting defines how many times it should be
	 * flagged consequentivly before an idle event is fired.
	 * 
	 * @param inactivePeriodsPerIdleEvent
	 *            int
	 */
	public void setInactiveServiceRunsPerIdleEvent(
			int inactivePeriodsPerIdleEvent) {
		this.inactivePeriodsPerIdleEvent = inactivePeriodsPerIdleEvent;
	}

	public static void addEventListener(EventListener listener) {
		EventServiceImplementation.getInstance().addListener(listener);
	}

	public static void removeEventListener(EventListener listener) {
		EventServiceImplementation.getInstance().removeListener(listener);
	}

	public int getIpv6WorkaroundPort() {
		return ipv6WorkaroundPort;
	}

	public void setIpv6WorkaroundPort(int ipv6WorkaroundPort) {
		this.ipv6WorkaroundPort = ipv6WorkaroundPort;
	}

	public String getIpv6WorkaroundBindAddress() {
		return ipv6WorkaroundBindAddress;
	}

	public void setIpv6WorkaroundBindAddress(String ipv6WorkaroundBindAddress) {
		this.ipv6WorkaroundBindAddress = ipv6WorkaroundBindAddress;
	}

    public void setAttribute(String name, Object value) {
    	attributes.put(name, value);
    }
    
    @SuppressWarnings("unchecked")
	public <K> K getAttribute(String name, K defaultValue) {
    	
    	if(!attributes.containsKey(name)) {
    		attributes.put(name, defaultValue);
    	}
    	return (K) attributes.get(name); 
 
    }

}
