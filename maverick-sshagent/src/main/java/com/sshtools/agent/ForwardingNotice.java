package com.maverick.agent;

public class ForwardingNotice {
    String remoteHostname;
    String remoteIPAddress;
    int remotePort;

    /**
     * Creates a new ForwardingNotice object.
     *
     * @param remoteHostname
     * @param remoteIPAddress
     * @param remotePort
     */
    public ForwardingNotice(String remoteHostname, String remoteIPAddress,
        int remotePort) {
        this.remoteHostname = remoteHostname;
        this.remoteIPAddress = remoteIPAddress;
        this.remotePort = remotePort;
    }

    /**
     *
     *
     * @return
     */
    public String getRemoteHostname() {
        return remoteHostname;
    }

    /**
     *
     *
     * @return
     */
    public String getRemoteIPAddress() {
        return remoteIPAddress;
    }

    /**
     *
     *
     * @return
     */
    public int getRemotePort() {
        return remotePort;
    }
}
