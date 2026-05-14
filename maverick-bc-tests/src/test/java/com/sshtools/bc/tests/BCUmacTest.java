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
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.bc.tests;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.ssh.components.jce.Umac128;
import com.sshtools.common.ssh.components.jce.Umac32;
import com.sshtools.common.ssh.components.jce.Umac64;
import com.sshtools.common.ssh.components.jce.Umac96;
import com.sshtools.common.tests.AbstractHmacTests;

public class BCUmacTest extends AbstractHmacTests {

    @Override
    protected void setUp() {
        JCEProvider.enableBouncyCastle(true);
    }

    @Override
    protected String getTestingJCE() {
        return "BC";
    }

    // ---- Generate/verify round-trip tests ----

    public void testUmac32() throws SshException {
        testHmac(new Umac32());
    }

    public void testUmac64() throws SshException {
        testHmac(new Umac64());
    }

    public void testUmac96() throws SshException {
        testHmac(new Umac96());
    }

    public void testUmac128() throws SshException {
        testHmac(new Umac128());
    }

    // ---- RFC 4418 Appendix test vectors ----
    // K = "abcdefghijklmnop", N = "bcdefghi"

    public void testRfc32Empty() throws Exception {
        testRfcVector(new Umac32(), null, "113145FB");
    }

    public void testRfc32ThreeA() throws Exception {
        testRfcVector(new Umac32(), repeat((byte) 'a', 3), "3B91D102");
    }

    public void testRfc32_1024A() throws Exception {
        testRfcVector(new Umac32(), repeat((byte) 'a', 1 << 10), "599B350B");
    }

    public void testRfc32_32768A() throws Exception {
        testRfcVector(new Umac32(), repeat((byte) 'a', 1 << 15), "58DCF532");
    }

    public void testRfc32_1M_A() throws Exception {
        testRfcVector(new Umac32(), repeat((byte) 'a', 1 << 20), "DB6364D1");
    }

    public void testRfc32AbcOnce() throws Exception {
        testRfcVector(new Umac32(), "abc".getBytes("ASCII"), "ABF3A3A0");
    }

    public void testRfc32Abc500() throws Exception {
        testRfcVector(new Umac32(), repeat("abc".getBytes("ASCII"), 500), "ABEB3C8B");
    }

    public void testRfc64Empty() throws Exception {
        testRfcVector(new Umac64(), null, "6E155FAD26900BE1");
    }

    public void testRfc64ThreeA() throws Exception {
        testRfcVector(new Umac64(), repeat((byte) 'a', 3), "44B5CB542F220104");
    }

    public void testRfc64_1024A() throws Exception {
        testRfcVector(new Umac64(), repeat((byte) 'a', 1 << 10), "26BF2F5D60118BD9");
    }

    public void testRfc64_32768A() throws Exception {
        testRfcVector(new Umac64(), repeat((byte) 'a', 1 << 15), "27F8EF643B0D118D");
    }

    public void testRfc64_1M_A() throws Exception {
        testRfcVector(new Umac64(), repeat((byte) 'a', 1 << 20), "A4477E87E9F55853");
    }

    public void testRfc64AbcOnce() throws Exception {
        testRfcVector(new Umac64(), "abc".getBytes("ASCII"), "D4D7B9F6BD4FBFCF");
    }

    public void testRfc64Abc500() throws Exception {
        testRfcVector(new Umac64(), repeat("abc".getBytes("ASCII"), 500), "D4CF26DDEFD5C01A");
    }

    public void testRfc96Empty() throws Exception {
        testRfcVector(new Umac96(), null, "32FEDB100C79AD58F07FF764");
    }

    public void testRfc96ThreeA() throws Exception {
        testRfcVector(new Umac96(), repeat((byte) 'a', 3), "185E4FE905CBA7BD85E4C2DC");
    }

    public void testRfc96_1024A() throws Exception {
        testRfcVector(new Umac96(), repeat((byte) 'a', 1 << 10), "7A54ABE04AF82D60FB298C3C");
    }

    public void testRfc96_32768A() throws Exception {
        testRfcVector(new Umac96(), repeat((byte) 'a', 1 << 15), "7B136BD911E4B734286EF2BE");
    }

    public void testRfc96_1M_A() throws Exception {
        testRfcVector(new Umac96(), repeat((byte) 'a', 1 << 20), "F8ACFA3AC31CFEEA047F7B11");
    }

    public void testRfc96AbcOnce() throws Exception {
        testRfcVector(new Umac96(), "abc".getBytes("ASCII"), "883C3D4B97A61976FFCF2323");
    }

    public void testRfc96Abc500() throws Exception {
        testRfcVector(new Umac96(), repeat("abc".getBytes("ASCII"), 500), "8824A260C53C66A36C9260A6");
    }
}
