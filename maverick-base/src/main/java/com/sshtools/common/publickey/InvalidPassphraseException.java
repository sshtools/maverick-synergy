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


package com.sshtools.common.publickey;

/**
 *
 * <p>Thrown by an {@link SshPrivateKeyFile} when it detects that the
 * passphrase supplied was invalid.</p>
 *
 * @author Lee David Painter
 */
public class InvalidPassphraseException extends Exception {

	private static final long serialVersionUID = -1458660635959624570L;

	public InvalidPassphraseException() {
        super("The passphrase supplied was invalid!");
    }
    
    public InvalidPassphraseException(Exception ex) {
    	super(ex.getMessage());
    }
}
