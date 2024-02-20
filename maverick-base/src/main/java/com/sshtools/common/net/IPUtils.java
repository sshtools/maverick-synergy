package com.sshtools.common.net;

import java.util.StringTokenizer;

public abstract class IPUtils {

    private IPUtils() {
    }
    
    /**
     * @param ip
     * @param mask
     * @return int[]
     */
    public static int[] calcNetworkNumber(int ip[], int mask[]) {
        int ret[] = new int[4];
        for (int i = 0; i < 4; i++)
            ret[i] = ip[i] & mask[i];
        return ret;
    }

    /**
     * @param in
     * @param mask
     * @return int[]
     */
    public static int[] calcLastAddress(int in[], int mask) {
        int ret[] = new int[4];
        ret = calcBroadcastAddress(in, mask);
        ret[3] = ret[3] - 1;
        return ret;
    }

    /**
     * @param bit
     * @return int[]
     */
    public static int[] createMaskArray(int bit) {
        int[] mask = new int[4];
        int rem = (bit + 1) / 8;
        int mod = (bit + 1) % 8;
        Integer Int = Integer.valueOf(2);
        Integer modInt = Integer.valueOf(8 - mod);
        double d = Math.pow(Int.doubleValue(), modInt.doubleValue());
        Double dd = Double.valueOf(d);
        int i;
        for (i = 0; i < rem; i++)
            mask[i] = 255;

        if(i < mask.length - 1) {
	        mask[i] = 256 - dd.intValue();
	        for (i++; i < 4; i++)
	            mask[i] = 0;
        }

        return mask;
    }

    /**
     * @param ip
     * @param mask
     * @return int[]
     */
    public static int[] calcFirstAddress(int ip[], int mask[]) {
        int ret[] = new int[4];
        ret = calcNetworkNumber(ip, mask);
        ret[3] = ret[3] + 1;
        return ret;
    }

    /**
     * @param in
     * @param m
     * @return int[]
     */
    public static int[] calcBroadcastAddress(int in[], int m) {
        int ret[] = new int[4];
        Integer totalBits = Integer.valueOf(32);
        Integer bits = Integer.valueOf(totalBits.intValue() - m - 1);
        int mask[] = createMaskArray(m);
        double two = 2D;
        Double hosts = Double.valueOf(Math.pow(two, bits.doubleValue()));
        hosts.intValue();
        int ffOctets = bits.intValue() / 8;
        Integer modBits = Integer.valueOf(bits.intValue() % 8);
        for (int i = 0; i < 4; i++) {
            ret[i] = in[i];
            if (i > 4 - ffOctets - 1)
                ret[i] = 255;
        }

        hosts = Double.valueOf(Math.pow(two, modBits.doubleValue()));
        if (ffOctets > 0)
            ret[4 - ffOctets - 1] = (in[4 - ffOctets - 1] + hosts.intValue()) - 1;
        else
            ret[3] = (mask[3] + hosts.intValue()) - 1;
        return ret;
    }

    /**
     * @param ip
     * @param m
     * @return int
     */
    public static int getNumberOfHosts(int ip[], int m) {
        Integer totalBits = Integer.valueOf(32);
        Integer bits = Integer.valueOf(totalBits.intValue() - m - 1);
        double two = 2D;
        Double hosts = Double.valueOf(Math.pow(two, bits.doubleValue()));
        return hosts.intValue() - 2;
    }

    /**
     * @param addr
     * @return String
     */
    public static String createAddressString(int[] addr) {

        return addr[0] + "." + addr[1] + "." + addr[2] + "." + addr[3];
    }

    /**
     * @param ip
     * @return int[]
     */
    public static int[] nextAddress(int[] ip) {
        if (ip[3] == 255) {
            ip[3] = 0;
            if (ip[2] == 255) {
                ip[2] = 0;
                if (ip[1] == 255) {
                    ip[1] = 0;
                    if (ip[0] == 255)
                        return null;
                    else
                        ip[0]++;
                } else
                    ip[1]++;
            } else
                ip[2]++;
        } else
            ip[3]++;
        return ip;
    }

    /**
     * @param ipAddress
     * @return int[]
     */
    public static int[] getByteAddress(String ipAddress) {
        StringTokenizer tokens = new StringTokenizer(ipAddress, ".");
        int[] ip = new int[4];
        for (int i = 0; i < ip.length; i++) {
            if (!tokens.hasMoreTokens())
                throw new IllegalArgumentException("IP address must consist of xxx.xxx.xxx.xxx");
            try {
                ip[i] = Integer.parseInt(tokens.nextToken().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid IP address " + ipAddress);
            }
        }
        return ip;
    }
}
