
package com.sshtools.client.sftp;

import java.io.File;

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
     */
    public String[] matchFileNamesWithPattern(File[] files, String fileNameRegExp) throws SshException, SftpStatusException {
        String[] thefile = new String[1];
        thefile[0] = files[0].getName();
        return thefile;
    }

    /**
     * returns files
     */
    public SftpFile[] matchFilesWithPattern(SftpFile[] files, String fileNameRegExp) throws SftpStatusException, SshException {
        return files;
    }

}
