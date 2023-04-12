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
module com.sshtools.synergy.s3 {
  requires transitive com.sshtools.maverick.base;
  requires transitive software.amazon.awssdk.awscore;
  requires transitive software.amazon.awssdk.core;
  requires transitive software.amazon.awssdk.services.s3;
  requires transitive software.amazon.awssdk.utils;
  requires transitive software.amazon.awssdk.regions;
  requires transitive software.amazon.awssdk.auth;
  exports com.sshtools.synergy.s3;
}