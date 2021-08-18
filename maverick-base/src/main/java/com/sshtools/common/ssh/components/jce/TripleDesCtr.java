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


package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

import com.sshtools.common.ssh.SecurityLevel;


/**
 * An implementation of the 3DES cipher using a JCE provider. 
 * 
 * @author Lee David Painter
 */
public class TripleDesCtr extends AbstractJCECipher {

  public TripleDesCtr() throws IOException {
    super(JCEAlgorithms.JCE_3DESCTRNOPADDING, "DESede", 24, "3des-ctr", SecurityLevel.WEAK, 0);
  }

}
