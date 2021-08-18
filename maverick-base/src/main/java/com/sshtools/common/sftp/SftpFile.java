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

/**
 * Represents an SFTP file object.
 * 
 * @author Lee David Painter
 */
public class SftpFile {
    String filename;
    SftpFileAttributes attrs;
    String absolutePath;

    /**
     * Creates a new SftpFile object.
     * 
     * <p>
     * IMPORTANT: Each SftpFile object should be initialized with the relative
     * path of the file when used in
     * {@link com.sshtools.common.sftp.FileSystem#readDirectory}.
     * For all other uses an absolute path is required.
     * </p>
     * 
     * @param path
     * @param attrs
     */
    public SftpFile(String path, SftpFileAttributes attrs) {
        

        if(!path.equals("/") && path.endsWith("/"))
        	path = path.substring(0, path.length()-1);
        
        this.absolutePath = path;
        
        int i = path.lastIndexOf('/');

        if (i > -1) {
            this.filename = path.substring(i + 1);
        } else {
            this.filename = path;
        }

        this.attrs = attrs;
    }

    public int hashCode() {
        return absolutePath.hashCode();
    }

    /**
     * Get the filename.
     * 
     * @return String
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Get the files attributes.
     * 
     * @return SftpFileAttributes
     */
    public SftpFileAttributes getAttributes() {
        return attrs;
    }

    /**
     * Get the absolute path
     * 
     * @return String
     */
    public String getAbsolutePath() {
        return absolutePath;
    }

}
