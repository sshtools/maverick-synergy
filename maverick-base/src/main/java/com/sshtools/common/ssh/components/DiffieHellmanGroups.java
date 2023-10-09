package com.sshtools.common.ssh.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.sshtools.common.logger.Log;

import com.sshtools.common.util.UnsignedInteger32;

public class DiffieHellmanGroups {

	static final BigInteger TWO = new BigInteger("2");
	
	public static final BigInteger group1 = new BigInteger(
			"FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
					+ "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
					+ "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
					+ "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
					+ "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381"
					+ "FFFFFFFFFFFFFFFF", 16);

	public static final BigInteger group5 = new BigInteger("FFFFFFFF"
			+ "FFFFFFFF" + "C90FDAA2" + "2168C234" + "C4C6628B" + "80DC1CD1"
			+ "29024E08" + "8A67CC74" + "020BBEA6" + "3B139B22" + "514A0879"
			+ "8E3404DD" + "EF9519B3" + "CD3A431B" + "302B0A6D" + "F25F1437"
			+ "4FE1356D" + "6D51C245" + "E485B576" + "625E7EC6" + "F44C42E9"
			+ "A637ED6B" + "0BFF5CB6" + "F406B7ED" + "EE386BFB" + "5A899FA5"
			+ "AE9F2411" + "7C4B1FE6" + "49286651" + "ECE45B3D" + "C2007CB8"
			+ "A163BF05" + "98DA4836" + "1C55D39A" + "69163FA8" + "FD24CF5F"
			+ "83655D23" + "DCA3AD96" + "1C62F356" + "208552BB" + "9ED52907"
			+ "7096966D" + "670C354E" + "4ABC9804" + "F1746C08" + "CA237327"
			+ "FFFFFFFF" + "FFFFFFFF", 16);

	public static final BigInteger group14 = new BigInteger("FFFFFFFF"
			+ "FFFFFFFF" + "C90FDAA2" + "2168C234" + "C4C6628B" + "80DC1CD1"
			+ "29024E08" + "8A67CC74" + "020BBEA6" + "3B139B22" + "514A0879"
			+ "8E3404DD" + "EF9519B3" + "CD3A431B" + "302B0A6D" + "F25F1437"
			+ "4FE1356D" + "6D51C245" + "E485B576" + "625E7EC6" + "F44C42E9"
			+ "A637ED6B" + "0BFF5CB6" + "F406B7ED" + "EE386BFB" + "5A899FA5"
			+ "AE9F2411" + "7C4B1FE6" + "49286651" + "ECE45B3D" + "C2007CB8"
			+ "A163BF05" + "98DA4836" + "1C55D39A" + "69163FA8" + "FD24CF5F"
			+ "83655D23" + "DCA3AD96" + "1C62F356" + "208552BB" + "9ED52907"
			+ "7096966D" + "670C354E" + "4ABC9804" + "F1746C08" + "CA18217C"
			+ "32905E46" + "2E36CE3B" + "E39E772C" + "180E8603" + "9B2783A2"
			+ "EC07A28F" + "B5C55DF0" + "6F4C52C9" + "DE2BCBF6" + "95581718"
			+ "3995497C" + "EA956AE5" + "15D22618" + "98FA0510" + "15728E5A"
			+ "8AACAA68" + "FFFFFFFF" + "FFFFFFFF", 16);

	public static final BigInteger group15 = new BigInteger("FFFFFFFF"
			+ "FFFFFFFF" + "C90FDAA2" + "2168C234" + "C4C6628B" + "80DC1CD1"
			+ "29024E08" + "8A67CC74" + "020BBEA6" + "3B139B22" + "514A0879"
			+ "8E3404DD" + "EF9519B3" + "CD3A431B" + "302B0A6D" + "F25F1437"
			+ "4FE1356D" + "6D51C245" + "E485B576" + "625E7EC6" + "F44C42E9"
			+ "A637ED6B" + "0BFF5CB6" + "F406B7ED" + "EE386BFB" + "5A899FA5"
			+ "AE9F2411" + "7C4B1FE6" + "49286651" + "ECE45B3D" + "C2007CB8"
			+ "A163BF05" + "98DA4836" + "1C55D39A" + "69163FA8" + "FD24CF5F"
			+ "83655D23" + "DCA3AD96" + "1C62F356" + "208552BB" + "9ED52907"
			+ "7096966D" + "670C354E" + "4ABC9804" + "F1746C08" + "CA18217C"
			+ "32905E46" + "2E36CE3B" + "E39E772C" + "180E8603" + "9B2783A2"
			+ "EC07A28F" + "B5C55DF0" + "6F4C52C9" + "DE2BCBF6" + "95581718"
			+ "3995497C" + "EA956AE5" + "15D22618" + "98FA0510" + "15728E5A"
			+ "8AAAC42D" + "AD33170D" + "04507A33" + "A85521AB" + "DF1CBA64"
			+ "ECFB8504" + "58DBEF0A" + "8AEA7157" + "5D060C7D" + "B3970F85"
			+ "A6E1E4C7" + "ABF5AE8C" + "DB0933D7" + "1E8C94E0" + "4A25619D"
			+ "CEE3D226" + "1AD2EE6B" + "F12FFA06" + "D98A0864" + "D8760273"
			+ "3EC86A64" + "521F2B18" + "177B200C" + "BBE11757" + "7A615D6C"
			+ "770988C0" + "BAD946E2" + "08E24FA0" + "74E5AB31" + "43DB5BFC"
			+ "E0FD108E" + "4B82D120" + "A93AD2CA" + "FFFFFFFF" + "FFFFFFFF",
			16);

	public static final BigInteger group16 = new BigInteger("FFFFFFFF"
			+ "FFFFFFFF" + "C90FDAA2" + "2168C234" + "C4C6628B" + "80DC1CD1"
			+ "29024E08" + "8A67CC74" + "020BBEA6" + "3B139B22" + "514A0879"
			+ "8E3404DD" + "EF9519B3" + "CD3A431B" + "302B0A6D" + "F25F1437"
			+ "4FE1356D" + "6D51C245" + "E485B576" + "625E7EC6" + "F44C42E9"
			+ "A637ED6B" + "0BFF5CB6" + "F406B7ED" + "EE386BFB" + "5A899FA5"
			+ "AE9F2411" + "7C4B1FE6" + "49286651" + "ECE45B3D" + "C2007CB8"
			+ "A163BF05" + "98DA4836" + "1C55D39A" + "69163FA8" + "FD24CF5F"
			+ "83655D23" + "DCA3AD96" + "1C62F356" + "208552BB" + "9ED52907"
			+ "7096966D" + "670C354E" + "4ABC9804" + "F1746C08" + "CA18217C"
			+ "32905E46" + "2E36CE3B" + "E39E772C" + "180E8603" + "9B2783A2"
			+ "EC07A28F" + "B5C55DF0" + "6F4C52C9" + "DE2BCBF6" + "95581718"
			+ "3995497C" + "EA956AE5" + "15D22618" + "98FA0510" + "15728E5A"
			+ "8AAAC42D" + "AD33170D" + "04507A33" + "A85521AB" + "DF1CBA64"
			+ "ECFB8504" + "58DBEF0A" + "8AEA7157" + "5D060C7D" + "B3970F85"
			+ "A6E1E4C7" + "ABF5AE8C" + "DB0933D7" + "1E8C94E0" + "4A25619D"
			+ "CEE3D226" + "1AD2EE6B" + "F12FFA06" + "D98A0864" + "D8760273"
			+ "3EC86A64" + "521F2B18" + "177B200C" + "BBE11757" + "7A615D6C"
			+ "770988C0" + "BAD946E2" + "08E24FA0" + "74E5AB31" + "43DB5BFC"
			+ "E0FD108E" + "4B82D120" + "A9210801" + "1A723C12" + "A787E6D7"
			+ "88719A10" + "BDBA5B26" + "99C32718" + "6AF4E23C" + "1A946834"
			+ "B6150BDA" + "2583E9CA" + "2AD44CE8" + "DBBBC2DB" + "04DE8EF9"
			+ "2E8EFC14" + "1FBECAA6" + "287C5947" + "4E6BC05D" + "99B2964F"
			+ "A090C3A2" + "233BA186" + "515BE7ED" + "1F612970" + "CEE2D7AF"
			+ "B81BDD76" + "2170481C" + "D0069127" + "D5B05AA9" + "93B4EA98"
			+ "8D8FDDC1" + "86FFB7DC" + "90A6C08F" + "4DF435C9" + "34063199"
			+ "FFFFFFFF" + "FFFFFFFF",
			16);

	public static final BigInteger group17 = new BigInteger("FFFFFFFF"
			+ "FFFFFFFF" + "C90FDAA2" + "2168C234" + "C4C6628B" + "80DC1CD1"
			+ "29024E08" + "8A67CC74" + "020BBEA6" + "3B139B22" + "514A0879"
			+ "8E3404DD" + "EF9519B3" + "CD3A431B" + "302B0A6D" + "F25F1437"
			+ "4FE1356D" + "6D51C245" + "E485B576" + "625E7EC6" + "F44C42E9"
			+ "A637ED6B" + "0BFF5CB6" + "F406B7ED" + "EE386BFB" + "5A899FA5"
			+ "AE9F2411" + "7C4B1FE6" + "49286651" + "ECE45B3D" + "C2007CB8"
			+ "A163BF05" + "98DA4836" + "1C55D39A" + "69163FA8" + "FD24CF5F"
			+ "83655D23" + "DCA3AD96" + "1C62F356" + "208552BB" + "9ED52907"
			+ "7096966D" + "670C354E" + "4ABC9804" + "F1746C08" + "CA18217C"
			+ "32905E46" + "2E36CE3B" + "E39E772C" + "180E8603" + "9B2783A2"
			+ "EC07A28F" + "B5C55DF0" + "6F4C52C9" + "DE2BCBF6" + "95581718"
			+ "3995497C" + "EA956AE5" + "15D22618" + "98FA0510" + "15728E5A"
			+ "8AAAC42D" + "AD33170D" + "04507A33" + "A85521AB" + "DF1CBA64"
			+ "ECFB8504" + "58DBEF0A" + "8AEA7157" + "5D060C7D" + "B3970F85"
			+ "A6E1E4C7" + "ABF5AE8C" + "DB0933D7" + "1E8C94E0" + "4A25619D"
			+ "CEE3D226" + "1AD2EE6B" + "F12FFA06" + "D98A0864" + "D8760273"
			+ "3EC86A64" + "521F2B18" + "177B200C" + "BBE11757" + "7A615D6C"
			+ "770988C0" + "BAD946E2" + "08E24FA0" + "74E5AB31" + "43DB5BFC"
			+ "E0FD108E" + "4B82D120" + "A9210801" + "1A723C12" + "A787E6D7"
			+ "88719A10" + "BDBA5B26" + "99C32718" + "6AF4E23C" + "1A946834"
			+ "B6150BDA" + "2583E9CA" + "2AD44CE8" + "DBBBC2DB" + "04DE8EF9"
			+ "2E8EFC14" + "1FBECAA6" + "287C5947" + "4E6BC05D" + "99B2964F"
			+ "A090C3A2" + "233BA186" + "515BE7ED" + "1F612970" + "CEE2D7AF"
			+ "B81BDD76" + "2170481C" + "D0069127" + "D5B05AA9" + "93B4EA98"
			+ "8D8FDDC1" + "86FFB7DC" + "90A6C08F" + "4DF435C9" + "34028492"
			+ "36C3FAB4" + "D27C7026" + "C1D4DCB2" + "602646DE" + "C9751E76"
			+ "3DBA37BD" + "F8FF9406" + "AD9E530E" + "E5DB382F" + "413001AE"
			+ "B06A53ED" + "9027D831" + "179727B0" + "865A8918" + "DA3EDBEB"
			+ "CF9B14ED" + "44CE6CBA" + "CED4BB1B" + "DB7F1447" + "E6CC254B"
			+ "33205151" + "2BD7AF42" + "6FB8F401" + "378CD2BF" + "5983CA01"
			+ "C64B92EC" + "F032EA15" + "D1721D03" + "F482D7CE" + "6E74FEF6"
			+ "D55E702F" + "46980C82" + "B5A84031" + "900B1C9E" + "59E7C97F"
			+ "BEC7E8F3" + "23A97A7E" + "36CC88BE" + "0F1D45B7" + "FF585AC5"
			+ "4BD407B2" + "2B4154AA" + "CC8F6D7E" + "BF48E1D8" + "14CC5ED2"
			+ "0F8037E0" + "A79715EE" + "F29BE328" + "06A1D58B" + "B7C5DA76"
			+ "F550AA3D" + "8A1FBFF0" + "EB19CCB1" + "A313D55C" + "DA56C9EC"
			+ "2EF29632" + "387FE8D7" + "6E3C0468" + "043E8F66" + "3F4860EE"
			+ "12BF2D5B" + "0B7474D6" + "E694F91E" + "6DCC4024" + "FFFFFFFF"
			+ "FFFFFFFF", 16);

	public static final BigInteger group18 = new BigInteger("FFFFFFFF"
			+ "FFFFFFFF" + "C90FDAA2" + "2168C234" + "C4C6628B" + "80DC1CD1"
			+ "29024E08" + "8A67CC74" + "020BBEA6" + "3B139B22" + "514A0879"
			+ "8E3404DD" + "EF9519B3" + "CD3A431B" + "302B0A6D" + "F25F1437"
			+ "4FE1356D" + "6D51C245" + "E485B576" + "625E7EC6" + "F44C42E9"
			+ "A637ED6B" + "0BFF5CB6" + "F406B7ED" + "EE386BFB" + "5A899FA5"
			+ "AE9F2411" + "7C4B1FE6" + "49286651" + "ECE45B3D" + "C2007CB8"
			+ "A163BF05" + "98DA4836" + "1C55D39A" + "69163FA8" + "FD24CF5F"
			+ "83655D23" + "DCA3AD96" + "1C62F356" + "208552BB" + "9ED52907"
			+ "7096966D" + "670C354E" + "4ABC9804" + "F1746C08" + "CA18217C"
			+ "32905E46" + "2E36CE3B" + "E39E772C" + "180E8603" + "9B2783A2"
			+ "EC07A28F" + "B5C55DF0" + "6F4C52C9" + "DE2BCBF6" + "95581718"
			+ "3995497C" + "EA956AE5" + "15D22618" + "98FA0510" + "15728E5A"
			+ "8AAAC42D" + "AD33170D" + "04507A33" + "A85521AB" + "DF1CBA64"
			+ "ECFB8504" + "58DBEF0A" + "8AEA7157" + "5D060C7D" + "B3970F85"
			+ "A6E1E4C7" + "ABF5AE8C" + "DB0933D7" + "1E8C94E0" + "4A25619D"
			+ "CEE3D226" + "1AD2EE6B" + "F12FFA06" + "D98A0864" + "D8760273"
			+ "3EC86A64" + "521F2B18" + "177B200C" + "BBE11757" + "7A615D6C"
			+ "770988C0" + "BAD946E2" + "08E24FA0" + "74E5AB31" + "43DB5BFC"
			+ "E0FD108E" + "4B82D120" + "A9210801" + "1A723C12" + "A787E6D7"
			+ "88719A10" + "BDBA5B26" + "99C32718" + "6AF4E23C" + "1A946834"
			+ "B6150BDA" + "2583E9CA" + "2AD44CE8" + "DBBBC2DB" + "04DE8EF9"
			+ "2E8EFC14" + "1FBECAA6" + "287C5947" + "4E6BC05D" + "99B2964F"
			+ "A090C3A2" + "233BA186" + "515BE7ED" + "1F612970" + "CEE2D7AF"
			+ "B81BDD76" + "2170481C" + "D0069127" + "D5B05AA9" + "93B4EA98"
			+ "8D8FDDC1" + "86FFB7DC" + "90A6C08F" + "4DF435C9" + "34028492"
			+ "36C3FAB4" + "D27C7026" + "C1D4DCB2" + "602646DE" + "C9751E76"
			+ "3DBA37BD" + "F8FF9406" + "AD9E530E" + "E5DB382F" + "413001AE"
			+ "B06A53ED" + "9027D831" + "179727B0" + "865A8918" + "DA3EDBEB"
			+ "CF9B14ED" + "44CE6CBA" + "CED4BB1B" + "DB7F1447" + "E6CC254B"
			+ "33205151" + "2BD7AF42" + "6FB8F401" + "378CD2BF" + "5983CA01"
			+ "C64B92EC" + "F032EA15" + "D1721D03" + "F482D7CE" + "6E74FEF6"
			+ "D55E702F" + "46980C82" + "B5A84031" + "900B1C9E" + "59E7C97F"
			+ "BEC7E8F3" + "23A97A7E" + "36CC88BE" + "0F1D45B7" + "FF585AC5"
			+ "4BD407B2" + "2B4154AA" + "CC8F6D7E" + "BF48E1D8" + "14CC5ED2"
			+ "0F8037E0" + "A79715EE" + "F29BE328" + "06A1D58B" + "B7C5DA76"
			+ "F550AA3D" + "8A1FBFF0" + "EB19CCB1" + "A313D55C" + "DA56C9EC"
			+ "2EF29632" + "387FE8D7" + "6E3C0468" + "043E8F66" + "3F4860EE"
			+ "12BF2D5B" + "0B7474D6" + "E694F91E" + "6DBE1159" + "74A3926F"
			+ "12FEE5E4" + "38777CB6" + "A932DF8C" + "D8BEC4D0" + "73B931BA"
			+ "3BC832B6" + "8D9DD300" + "741FA7BF" + "8AFC47ED" + "2576F693"
			+ "6BA42466" + "3AAB639C" + "5AE4F568" + "3423B474" + "2BF1C978"
			+ "238F16CB" + "E39D652D" + "E3FDB8BE" + "FC848AD9" + "22222E04"
			+ "A4037C07" + "13EB57A8" + "1A23F0C7" + "3473FC64" + "6CEA306B"
			+ "4BCBC886" + "2F8385DD" + "FA9D4B7F" + "A2C087E8" + "79683303"
			+ "ED5BDD3A" + "062B3CF5" + "B3A278A6" + "6D2A13F8" + "3F44F82D"
			+ "DF310EE0" + "74AB6A36" + "4597E899" + "A0255DC1" + "64F31CC5"
			+ "0846851D" + "F9AB4819" + "5DED7EA1" + "B1D510BD" + "7EE74D73"
			+ "FAF36BC3" + "1ECFA268" + "359046F4" + "EB879F92" + "4009438B"
			+ "481C6CD7" + "889A002E" + "D5EE382B" + "C9190DA6" + "FC026E47"
			+ "9558E447" + "5677E9AA" + "9E3050E2" + "765694DF" + "C81F56E8"
			+ "80B96E71" + "60C980DD" + "98EDD3DF" + "FFFFFFFF" + "FFFFFFFF",
			16);

	static List<BigInteger> safePrimes = new ArrayList<BigInteger>();
	static List<DHGroup> customPrimes = new ArrayList<DHGroup>();
	
	static {
		safePrimes.add(group1);
		safePrimes.add(group5);
		safePrimes.add(group14);
		safePrimes.add(group15);
		safePrimes.add(group16);
		safePrimes.add(group17);
		safePrimes.add(group18);
	}
	
	public static class DHGroup {
		
		BigInteger g;
		BigInteger p;
		Integer size;
		
		private DHGroup(Integer size, BigInteger g, BigInteger p) {
			this.size = size;
			this.g= g;
			this.p = p;
		}

		public BigInteger getG() {
			return g;
		}

		public BigInteger getP() {
			return p;
		}

		public Integer getSize() {
			return size;
		}
		
	}
	
	public static final int TYPE_SAFE = 2;
	public static final int TESTS_COMPOSITE = 0x01;
	
	public static void loadGroups(URI url) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.toURL().openStream()));
		String line;
		while((line = reader.readLine())!=null) {
			line = line.trim();
			if(line.startsWith("#")) {
				continue;
			}
			String[] parts = line.split("\\s+");
			if(parts.length != 7) {
				continue;
			}
			Integer t = Integer.parseInt(parts[1]);
			if(t != 2) {
				continue;
			}
			t = Integer.parseInt(parts[2]);
			if((t & TESTS_COMPOSITE) != 0 || (t & ~TESTS_COMPOSITE) == 0) {
				continue;
			}
			if(Integer.parseInt(parts[3]) == 0) {
				continue;
			}
			customPrimes.add(new DHGroup(Integer.parseInt(parts[4]) + 1,
					new BigInteger(parts[5], 16),
					new BigInteger(parts[6], 16)));
			
		}
		
		Collections.<DHGroup>sort(customPrimes, new Comparator<DHGroup>() {
			@Override
			public int compare(DHGroup o1, DHGroup o2) {
				return o1.size.compareTo(o2.getSize());
			}
		});
	}
	
	public static boolean verifyParameters(BigInteger shared, BigInteger p) {
		
		if(shared.compareTo(BigInteger.ONE) <= 0 
				|| shared.compareTo(p.subtract(BigInteger.ONE)) >= 0) { 
			Log.error("Invalid DH shared value (1 < y < p-1) {}", shared.toString(16));
			return false;
		}
		return true;
	}
		
	/**
	 * get the biggest safe prime from the list that is <= maximumSize
	 * @param maximumSize
	 * @return BigInteger
	 */
	public static DHGroup getSafePrime(UnsignedInteger32 maximumSize) {

		if(Log.isDebugEnabled()) {
			Log.debug("Looking for diffie hellman group with maximum size of " + maximumSize.intValue() + " bits");
		}
		
		List<DHGroup> selectedGroups = new ArrayList<DHGroup>();
		int largedSize = 0;
		for(DHGroup group : customPrimes) {
			if(group.getSize() > maximumSize.intValue()) {
				break;
			}
			if(largedSize < group.getSize()) {
				largedSize = group.getSize();
				selectedGroups.clear();
			}
			selectedGroups.add(group);
		}
		
		if(!selectedGroups.isEmpty()) {
			return selectedGroups.get(new Random().nextInt(selectedGroups.size()));
		}
		
		BigInteger prime = group1;
		for(Iterator<BigInteger> it = safePrimes.iterator(); it.hasNext(); ) {
			BigInteger p = it.next();
			int len = p.bitLength();
			if(len > maximumSize.intValue()) {
				break;
			}
			prime = p;
		}
		
		if(Log.isDebugEnabled()) {
			Log.debug("Found diffie hellman group with " + prime.bitLength() + " bits");
		}
		return new DHGroup(prime.bitLength(), TWO, prime);
	}

	public static Collection<BigInteger> allDefaultGroups() {
		return Collections.unmodifiableList(safePrimes);
	}
}
