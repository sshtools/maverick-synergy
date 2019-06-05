package com.sshtools.client.sftp;

import java.io.File;

import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

/**
 * Interface for treating a filename as a regular expression and returning the
 * list of files that match.
 */
public interface RegularExpressionMatching {

    /**
     * returns each of the SftpFiles that match the pattern fileNameRegExp
     * 
     * @param files
     * @param fileNameRegExp
     * @return SftpFile[]
     * @throws SftpStatusException
     * @throws SshException
     */
    public SftpFile[] matchFilesWithPattern(SftpFile[] files, String fileNameRegExp) throws SftpStatusException, SshException;

    /**
     * returns each of the files that match the pattern fileNameRegExp
     * 
     * @param files
     * @param fileNameRegExp
     * @return String[]
     * @throws SftpStatusException
     * @throws SshException
     */
    public String[] matchFileNamesWithPattern(File[] files, String fileNameRegExp) throws SftpStatusException, SshException;

}