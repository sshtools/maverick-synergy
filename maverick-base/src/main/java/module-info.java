import java.nio.file.spi.FileSystemProvider;

import com.sshtools.common.files.nio.AbstractFileNIOProvider;
import com.sshtools.common.publickey.SshPrivateKeyProvider;

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
open module com.sshtools.maverick.base {
	/* Optional. Only needed for PuTTYPrivateKeyFile */
	requires static org.bouncycastle.pkix;
	requires static org.bouncycastle.provider;
	requires static org.bouncycastle.util;
	
	requires transitive com.sshtools.common.logger;
	requires com.sshtools.common.util;
	exports com.sshtools.common.auth;
	exports com.sshtools.common.command;
	exports com.sshtools.common.config;
	exports com.sshtools.common.events;
	exports com.sshtools.common.files;
	exports com.sshtools.common.files.direct;
	exports com.sshtools.common.files.nio;
	exports com.sshtools.common.forwarding;
	exports com.sshtools.common.knownhosts;
	exports com.sshtools.common.net;
	exports com.sshtools.common.nio;
	exports com.sshtools.common.permissions;
	exports com.sshtools.common.policy;
	exports com.sshtools.common.publickey;
	exports com.sshtools.common.publickey.authorized;
	exports com.sshtools.common.rsa;
	exports com.sshtools.common.scp;
	exports com.sshtools.common.sftp;
	exports com.sshtools.common.sftp.extensions;
	exports com.sshtools.common.sftp.extensions.filter;
	exports com.sshtools.common.shell;
	exports com.sshtools.common.ssh;
	exports com.sshtools.common.ssh.components;
	exports com.sshtools.common.ssh.components.jce;
	exports com.sshtools.common.ssh.compression;
	exports com.sshtools.common.ssh2;
	exports com.sshtools.common.sshd;
	
	uses SshPrivateKeyProvider; 
	provides FileSystemProvider with AbstractFileNIOProvider;
}