/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
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
