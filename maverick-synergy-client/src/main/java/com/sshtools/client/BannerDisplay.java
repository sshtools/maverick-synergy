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


package com.sshtools.client;


/**
 *
 * <p>Callback interface to display authentication banner messages.</p>
 *
 * <p>In some jurisdictions sending a warning message before
 * authentication may be relevant for getting legal protection.  Many
 * UNIX machines, for example, normally display text from
 * `/etc/issue', or use "tcp wrappers" or similar software to display
 * a banner before issuing a login prompt.</p>
 *
 * <p>Implement this interface to show the authentication banner message.
 * The method should display the message and should not return until the user
 * accepts the message</p>
 * 
 */
public interface BannerDisplay {
  /**
   * Called when a banner message is received.
   * @param message the message to display.
   */
  public void displayBanner(String message);
}