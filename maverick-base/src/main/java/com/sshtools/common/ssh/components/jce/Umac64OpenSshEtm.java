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
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * UMAC-64 encrypt-then-MAC using the OpenSSH algorithm name (umac-64-etm@openssh.com).
 * Identical UMAC computation to umac-64 (RFC 4418 / draft-miller-secsh-umac), ETM mode.
 */
public class Umac64OpenSshEtm extends AbstractUmac {

    public static class Umac64OpenSshEtmFactory implements SshHmacFactory<Umac64OpenSshEtm> {
        @Override
        public Umac64OpenSshEtm create() throws NoSuchAlgorithmException, IOException {
            return new Umac64OpenSshEtm();
        }

        @Override
        public String[] getKeys() {
            return new String[] { "umac-64-etm@openssh.com" };
        }
    }

    public Umac64OpenSshEtm() {
        super(8, SecurityLevel.STRONG, 1009);
    }

    @Override
    public String getAlgorithm() {
        return "umac-64-etm@openssh.com";
    }

    @Override
    public boolean isETM() {
        return true;
    }
}
