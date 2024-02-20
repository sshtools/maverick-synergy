package com.sshtools.common.net;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CIDRNetwork {

    private int networkBits;
    private InetAddress networkAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    /**
     * Default constructor
     * 
     * @param network
     * @throws IllegalArgumentException
     * @throws UnknownHostException 
     */
    public CIDRNetwork(String network) throws UnknownHostException {
        int index = network.indexOf("/");
        if (index == -1)
            index = network.indexOf("\\");

        if (index == -1) {
        	if(network.indexOf(':') > -1) {
        		network += "/128";
        	} else {
        		network += "/32";
        	}
            index = network.indexOf("/");
        }
        
        try {
            networkBits = Integer.parseInt(network.substring(index + 1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CIDR network setting invalid! " + network);
        }

        this.networkAddress = InetAddress.getByName(network.substring(0, index));
        
        int addressSize = networkAddress.getAddress().length;
        ByteBuffer maskBuffer;
        switch(addressSize) {
        case 4:
        	maskBuffer = ByteBuffer.allocate(addressSize);
        	maskBuffer.putInt(-1);
        	break;
        case 16:
        	maskBuffer = ByteBuffer.allocate(addressSize);
        	maskBuffer.putLong(-1L);
        	maskBuffer.putLong(-1L);
        	break;
        default:
        	throw new IllegalArgumentException(String.format("Invalid IP address format %s", network));
        }
        
        BigInteger subnetMask = (new BigInteger(1, maskBuffer.array())).not().shiftRight(networkBits);
        ByteBuffer b = ByteBuffer.wrap(networkAddress.getAddress());
        BigInteger ip = new BigInteger(1, b.array());
        
        BigInteger start = ip.and(subnetMask);
        BigInteger end = start.add(subnetMask.not());
        
        startAddress = InetAddress.getByAddress(toBytes(start.toByteArray(), addressSize));
        endAddress = InetAddress.getByAddress(toBytes(end.toByteArray(), addressSize));
    }
    
    
    private static  byte[] toBytes(byte[] array, int targetSize) {
        int counter = 0;
        List<Byte> newArr = new ArrayList<>();
        while (counter < targetSize && (array.length - 1 - counter >= 0)) {
            newArr.add(0, array[array.length - 1 - counter]);
            counter++;
        }

        int size = newArr.size();
        for (int i = 0; i < (targetSize - size); i++) {

            newArr.add(0, (byte) 0);
        }

        byte[] ret = new byte[newArr.size()];
        for (int i = 0; i < newArr.size(); i++) {
            ret[i] = newArr.get(i);
        }
        return ret;
    }

    public boolean isValidAddressForNetwork(String ipAddress) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ipAddress);
        BigInteger start = new BigInteger(1, this.startAddress.getAddress());
        BigInteger end = new BigInteger(1, this.endAddress.getAddress());
        BigInteger target = new BigInteger(1, address.getAddress());

        int st = start.compareTo(target);
        int te = target.compareTo(end);

        return (st == -1 || st == 0) && (te == -1 || te == 0);
    }
}
