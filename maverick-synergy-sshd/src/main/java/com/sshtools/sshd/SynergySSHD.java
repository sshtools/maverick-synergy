package com.sshtools.sshd;

/*-
 * #%L
 * Maverick Synergy SSHD
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.sshtools.common.config.AdaptiveConfiguration;
import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.server.SshServer;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.vsession.ShellCommandFactory;
import com.sshtools.server.vsession.VirtualChannelFactory;
import com.sshtools.server.vsession.VirtualSessionPolicy.VirtualSessionPolicyBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

@Command(
        name = "sshd",
        description = "Maverick Synergy standalone SSH server. Configuration is read from sshd.cfg in the working directory by default.",
        mixinStandardHelpOptions = true,
        version = "Maverick Synergy SSHD 3.2.0-SNAPSHOT",
        subcommands = { SynergyPasswd.class, CommandLine.HelpCommand.class })
public final class SynergySSHD implements Callable<Integer> {

    static final int DEFAULT_PORT = 2222;

    @Option(names = {"-p", "--port"}, paramLabel = "PORT",
            description = "Override the listening port. Takes precedence over Port in the config file and the default of " + DEFAULT_PORT + ".")
    private Integer port;

    @Option(names = {"-f", "--config"}, paramLabel = "FILE",
            description = "Path to the server configuration file. Default: ${DEFAULT-VALUE}",
            defaultValue = "sshd.cfg")
    private File configFile;

    public static void main(String[] args) {
        System.exit(new CommandLine(new SynergySSHD()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        AdaptiveConfiguration config = AdaptiveConfiguration.getConfiguration(configFile.getPath());

        int resolvedPort;
        if (port != null) {
            resolvedPort = port;
        } else {
            int[] configPorts = config.getMultipleIntConfig(AdaptiveConfiguration.PORT);
            resolvedPort = configPorts.length > 0 ? configPorts[0] : DEFAULT_PORT;
        }

        JCEProvider.enableBouncyCastle(true);

        String[] listenAddresses = config.getMultipleConfig(AdaptiveConfiguration.LISTEN_ADDRESS);
        String listenAddress = listenAddresses.length > 0 ? listenAddresses[0] : null;

        SshServer server = buildServer(config, resolvedPort, listenAddress);

        server.setFileFactory(new AdaptiveConfigFileFactory(config));
        server.setChannelFactory(new VirtualChannelFactory(new ShellCommandFactory()));
        server.setDefaultPolicies(VirtualSessionPolicyBuilder.create().build());

        if (!config.getBoolean(AdaptiveConfiguration.ALLOW_FORWARDING, false)) {
            server.getForwardingPolicy().denyForwarding();
        }

        server.addAuthenticator(new AdaptiveConfigPasswordAuthenticator(config));
        server.addAuthenticator(new AdaptiveConfigPublicKeyAuthenticator(config));

        server.start();

        Log.info("SynergySSHD started on port {}", server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "synergy-sshd-shutdown"));

        server.getShutdownFuture().waitForever();
        return 0;
    }

    // -------------------------------------------------------------------------
    // Server construction
    // -------------------------------------------------------------------------

    private static SshServer buildServer(AdaptiveConfiguration config, int port, String listenAddress)
            throws UnknownHostException {
        if (listenAddress != null && !listenAddress.isEmpty()) {
            return new ConfiguredSshServer(config, listenAddress, port);
        }
        return new ConfiguredSshServer(config, port);
    }

    // -------------------------------------------------------------------------
    // Inner server class — applies AdaptiveConfiguration per connection
    // -------------------------------------------------------------------------

    private static final class ConfiguredSshServer extends SshServer {

        private final AdaptiveConfiguration config;
        private File configFolder = new File(".");

        ConfiguredSshServer(AdaptiveConfiguration config, int port) throws UnknownHostException {
            super(port);
            this.config = config;
        }

        ConfiguredSshServer(AdaptiveConfiguration config, String address, int port) throws UnknownHostException {
            super(address, port);
            this.config = config;
        }

        @Override
        public void setConfigFolder(File folder) {
            super.setConfigFolder(folder);
            this.configFolder = folder;
        }

        // Apply per-connection configuration from sshd.cfg.
        @Override
        public void configure(SshServerContext sshContext, SocketChannel sc) throws IOException, SshException {
            super.configure(sshContext, sc);

            // Software version / ident string
            sshContext.setSoftwareVersionComments(
                    config.getProperty(AdaptiveConfiguration.SOFTWARE_VERSION, sshContext.getSoftwareVersionComments()));

            // Authentication policy
            int maxAuth = config.getInt(AdaptiveConfiguration.MAX_AUTHENTICATIONS, 10);
            String banner = config.getProperty(AdaptiveConfiguration.BANNER, null);
            AuthenticationPolicy authPolicy = new AuthenticationPolicy();
            authPolicy.setMaxAuthentications(maxAuth);
            if (banner != null && !banner.isEmpty()) {
                authPolicy.setBannerMessage(banner);
            }
            sshContext.setPolicy(AuthenticationPolicy.class, authPolicy);

            // Idle / keep-alive
            sshContext.setIdleConnectionTimeoutSeconds(
                    config.getInt(AdaptiveConfiguration.IDLE_CONNECTION_TIMEOUT, sshContext.getIdleConnectionTimeoutSeconds()));
            sshContext.setIdleAuthenticationTimeoutSeconds(
                    config.getInt(AdaptiveConfiguration.IDLE_AUTHENTICATION_TIMEOUT, sshContext.getIdleAuthenticationTimeoutSeconds()));
            sshContext.setKeepAliveInterval(
                    config.getInt(AdaptiveConfiguration.KEEP_ALIVE_INTERVAL, sshContext.getKeepAliveInterval()));
            sshContext.setKeepAliveDataMaxLength(
                    config.getInt(AdaptiveConfiguration.KEEP_ALIVE_DATA_MAX_LENGTH, sshContext.getKeepAliveDataMaxLength()));
            sshContext.setSendIgnorePacketOnIdle(
                    config.getBoolean(AdaptiveConfiguration.SEND_IGNORE_ON_IDLE, sshContext.isSendIgnorePacketOnIdle()));

            // Transport / packet
            sshContext.setMaximumPacketLength(
                    config.getInt(AdaptiveConfiguration.MAX_PACKET_LENGTH, sshContext.getMaximumPacketLength()));

            // Rekey thresholds
            sshContext.setKeyExchangeTransferLimit(
                    config.getLong(AdaptiveConfiguration.MAX_NUM_BYTES_BEFORE_REKEY, sshContext.getKeyExchangeTransferLimit()));
            sshContext.setKeyExchangePacketLimit(
                    config.getInt(AdaptiveConfiguration.MAX_NUM_PACKETS_BEFORE_REKEY, (int) sshContext.getKeyExchangePacketLimit()));

            // DH group sizes
            sshContext.setMinDHGroupExchangeKeySize(
                    config.getInt(AdaptiveConfiguration.MIN_DH_GROUP_SIZE, sshContext.getMinDHGroupExchangeKeySize()));
            sshContext.setPreferredDHGroupExchangeKeySize(
                    config.getInt(AdaptiveConfiguration.PREFERRED_DH_GROUP_SIZE, sshContext.getPreferredDHGroupExchangeKeySize()));
            // Note: SshServerContext overrides getMaxDHGroupExchangeKeySize() to return its own maxDHGroupSize field
            // (default 2048). The correct setter for that field is setMaxDHGroupExchangeSize().
            sshContext.setMaxDHGroupExchangeSize(
                    config.getInt(AdaptiveConfiguration.MAX_DH_GROUP_SIZE, sshContext.getMaxDHGroupExchangeKeySize()));

            // Session / channel
            sshContext.setChannelLimit(
                    config.getInt(AdaptiveConfiguration.CHANNEL_LIMIT, sshContext.getChannelLimit()));

            // Signature options
            sshContext.setSHA1SignaturesSupported(
                    config.getBoolean(AdaptiveConfiguration.SHA1_SIGNATURES_SUPPORTED, sshContext.isSHA1SignaturesSupported()));

            // Compression
            sshContext.setCompressionLevel(
                    config.getInt(AdaptiveConfiguration.COMPRESSION_LEVEL, sshContext.getCompressionLevel()));

            // Algorithm preference order (absent = keep context defaults)
            String ciphers = config.getProperty(AdaptiveConfiguration.CIPHERS, null);
            if (ciphers != null) {
                var arr = ciphers.split("\\s*,\\s*");
                sshContext.setPreferredCipherCS(arr);
                sshContext.setPreferredCipherSC(arr);
            }
            String macs = config.getProperty(AdaptiveConfiguration.MACS, null);
            if (macs != null) {
                var arr = macs.split("\\s*,\\s*");
                sshContext.setPreferredMacCS(arr);
                sshContext.setPreferredMacSC(arr);
            }
            String kex = config.getProperty(AdaptiveConfiguration.KEY_EXCHANGE, null);
            if (kex != null) {
                sshContext.setPreferredKeyExchange(kex.split("\\s*,\\s*"));
            }
            String compressions = config.getProperty(AdaptiveConfiguration.COMPRESSION, null);
            if (compressions != null) {
                String first = compressions.split("\\s*,\\s*")[0];
                sshContext.setPreferredCompressionCS(first);
                sshContext.setPreferredCompressionSC(first);
            }

            // Server-specific
            sshContext.setForceServerPreferences(
                    config.getBoolean(AdaptiveConfiguration.FORCE_SERVER_PREFERENCES, sshContext.isForceServerPreferences()));
            sshContext.setEnsureGracefulDisconnect(
                    config.getBoolean(AdaptiveConfiguration.ENSURE_GRACEFUL_DISCONNECT, sshContext.isEnsureGracefulDisconnect()));
        }

        // Load or generate host keys from HostKey directives, or use defaults.
        @Override
        protected synchronized void configureHostKeys(SshServerContext sshContext, SocketChannel sc)
                throws IOException, SshException {

            if (!getHostKeys().isEmpty()) {
                sshContext.addHostKeys(getHostKeys());
                return;
            }

            String[] hostKeyPaths = config.getMultipleConfig(AdaptiveConfiguration.HOST_KEY);

            if (hostKeyPaths.length > 0) {
                // Load/generate each specified key file
                for (String path : hostKeyPaths) {
                    loadConfiguredHostKey(sshContext, new File(path));
                }
            } else {
                // Default set of key types generated under the config folder
                loadOrGenerateDefaultKeys(sshContext);
            }

            if (getHostKeys().isEmpty()) {
                throw new IOException("No host keys could be loaded or generated.");
            }
        }

        private void loadConfiguredHostKey(SshServerContext sshContext, File keyFile) {
            String name = keyFile.getName().toLowerCase();
            String type;
            int bits = 0;
            if (name.contains("rsa")) {
                type = SshKeyPairGenerator.SSH2_RSA;
                bits = 2048;
            } else if (name.contains("ecdsa") && name.contains("521")) {
                type = SshKeyPairGenerator.ECDSA;
                bits = 521;
            } else if (name.contains("ecdsa") && name.contains("384")) {
                type = SshKeyPairGenerator.ECDSA;
                bits = 384;
            } else if (name.contains("ecdsa")) {
                type = SshKeyPairGenerator.ECDSA;
                bits = 256;
            } else if (name.contains("ed448")) {
                type = SshKeyPairGenerator.ED448;
            } else if (name.contains("ed25519")) {
                type = SshKeyPairGenerator.ED25519;
            } else {
                // Default to Ed25519 for unknown file names
                type = SshKeyPairGenerator.ED25519;
            }
            try {
                var pair = sshContext.loadOrGenerateHostKey(keyFile, type, bits);
                addHostKey(pair);
            } catch (Exception e) {
                Log.warn("Could not load or generate host key {}: {}", keyFile, e.getMessage());
            }
        }

        private void loadOrGenerateDefaultKeys(SshServerContext sshContext) {
            record Attempt(String name, File file, String type, int bits) {}
            var attempts = new Attempt[] {
                new Attempt("rsa",     new File(configFolder, "ssh_host_rsa"),       SshKeyPairGenerator.SSH2_RSA, 2048),
                new Attempt("ecdsa256",new File(configFolder, "ssh_host_ecdsa_256"), SshKeyPairGenerator.ECDSA, 256),
                new Attempt("ecdsa384",new File(configFolder, "ssh_host_ecdsa_384"), SshKeyPairGenerator.ECDSA, 384),
                new Attempt("ecdsa521",new File(configFolder, "ssh_host_ecdsa_521"), SshKeyPairGenerator.ECDSA, 521),
                new Attempt("ed25519", new File(configFolder, "ssh_host_ed25519"),   SshKeyPairGenerator.ED25519, 0),
                new Attempt("ed448",   new File(configFolder, "ssh_host_ed448"),     SshKeyPairGenerator.ED448, 0),
            };
            for (var a : attempts) {
                try {
                    var pair = sshContext.loadOrGenerateHostKey(a.file(), a.type(), a.bits());
                    addHostKey(pair);
                    Log.info("Loaded host key: {}", a.name());
                } catch (Exception e) {
                    Log.warn("Host key {} unavailable: {}", a.name(), e.getMessage());
                }
            }
        }
    }

}
