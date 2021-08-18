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


package com.sshtools.common.sftp;

import java.io.IOException;

/**
 * Thrown when an operation that requires an ordinary file is presented with a directory.
 * 
 * @author Brett Smith
 */
public class FileIsDirectoryException extends IOException {

	private static final long serialVersionUID = 244584508925942734L;
	
	/**
     * Constructs the exception.
     */
    public FileIsDirectoryException() {
        super("File is a directory.");
    }
    
	/**
     * Constructs the exception.
     * 
     * @param msg String
     */
    public FileIsDirectoryException(String msg) {
        super(msg);
    }
}
