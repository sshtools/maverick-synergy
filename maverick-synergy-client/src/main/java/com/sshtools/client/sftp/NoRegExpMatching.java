
package com.sshtools.client.sftp;

import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

/**
 * Implements the RegularExpressionMatching Interface.<br>
 * Performs no regular expression matching so:<br>
 * matchFilesWithPattern() simply returns the files parameter it is passed as an
 * argument<br>
 * matchFileNamesWithPattern() simply returns a 1 element array containing the
 * filename on the first element of the file[] argument passed to it.
 */
public class NoRegExpMatching implements RegularExpressionMatching {

    /**
     * opens and returns the requested filename string
     * 
     * @throws SftpStatusException
     * @throws PermissionDeniedException 
     * @throws IOException 
     */
    public String[] matchFileNamesWithPattern(AbstractFile[] files, String fileNameRegExp) throws SshException, SftpStatusException, IOException, PermissionDeniedException {
        String[] thefile = new String[1];
        for(int i=0;i<files.length;i++) {
        	thefile[i] = files[i].getAbsolutePath();
        }
        return thefile;
    }

    /**
     * returns files
     */
    public SftpFile[] matchFilesWithPattern(SftpFile[] files, String fileNameRegExp) throws SftpStatusException, SshException {
        return files;
    }

}
