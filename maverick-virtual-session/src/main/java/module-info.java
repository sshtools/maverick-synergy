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
module com.sshtools.server.vsession {
	requires transitive org.jline;
	requires transitive com.sshtools.maverick.base;
	requires commons.cli;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.synergy.server;
	requires org.apache.commons.lang3;
	requires pty4j;
	exports com.sshtools.server.vsession;
	exports com.sshtools.server.vsession.commands;
	exports com.sshtools.server.vsession.commands.admin;
	exports com.sshtools.server.vsession.commands.fs;
	exports com.sshtools.server.vsession.commands.os;
	exports com.sshtools.server.vsession.commands.sftp;
	exports com.sshtools.server.vsession.jvm;
	exports com.sshtools.vsession.commands.ssh;
}