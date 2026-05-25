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

/*-
 * #%L
 * Base API
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * UMAC-32 encrypt-then-MAC using the OpenSSH algorithm name (umac-32-etm@openssh.com).
 * Identical UMAC computation to umac-32 (RFC 4418 / draft-miller-secsh-umac), ETM mode.
 */
public class Umac32OpenSshEtm extends AbstractUmac {

    public static class Umac32OpenSshEtmFactory implements SshHmacFactory<Umac32OpenSshEtm> {
        @Override
        public Umac32OpenSshEtm create() throws NoSuchAlgorithmException, IOException {
            return new Umac32OpenSshEtm();
        }

        @Override
        public String[] getKeys() {
            return new String[] { "umac-32-etm@openssh.com" };
        }
    }

    public Umac32OpenSshEtm() {
        super(4, SecurityLevel.STRONG, 1008);
    }

    @Override
    public String getAlgorithm() {
        return "umac-32-etm@openssh.com";
    }

    @Override
    public boolean isETM() {
        return true;
    }
}
