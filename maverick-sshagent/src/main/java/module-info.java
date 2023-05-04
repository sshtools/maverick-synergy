import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.provider.tcp.TCPAgentProvider;

/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
open module com.sshtools.agent {
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.util;
	requires com.sshtools.common.logger;
	exports com.sshtools.agent;
	exports com.sshtools.agent.client;
	exports com.sshtools.agent.exceptions;
	exports com.sshtools.agent.rfc;
	exports com.sshtools.agent.server;
	exports com.sshtools.agent.openssh;
	uses AgentProvider;
	provides AgentProvider with TCPAgentProvider;
}