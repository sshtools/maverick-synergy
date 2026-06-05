package com.sshtools.sshd;

/*-
 * #%L
 * Maverick Synergy SSHD
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import com.sshtools.common.util.BCrypt;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Generates a BCrypt password hash for use in a {@code User} directive in {@code sshd.cfg}.
 *
 * <p>Can be invoked standalone or as the {@code passwd} subcommand of {@link SynergySSHD}:</p>
 * <pre>
 *   java -jar maverick-synergy-sshd.jar passwd [OPTIONS] [PASSWORD]
 *   java -cp maverick-synergy-sshd.jar com.sshtools.sshd.SynergyPasswd [OPTIONS] [PASSWORD]
 * </pre>
 */
@Command(
        name = "passwd",
        description = "Generate a BCrypt password hash for use in a User directive in sshd.cfg.",
        mixinStandardHelpOptions = true)
public final class SynergyPasswd implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", paramLabel = "PASSWORD",
                description = "Password to hash. If not provided you are prompted interactively (no echo).")
    private String password;

    @Option(names = {"-r", "--rounds"}, paramLabel = "N",
            description = "BCrypt work factor (log2 of iteration count, 4-31). Higher values are slower and more secure. Default: ${DEFAULT-VALUE}",
            defaultValue = "12")
    private int rounds;

    @Override
    public Integer call() {
        String pw = password;
        if (pw == null) {
            var console = System.console();
            if (console == null) {
                System.err.println("No console available. Provide PASSWORD as argument or use -h for help.");
                return 1;
            }
            char[] pw1 = console.readPassword("Password: ");
            char[] pw2 = console.readPassword("Confirm password: ");
            if (!Arrays.equals(pw1, pw2)) {
                System.err.println("Passwords do not match.");
                Arrays.fill(pw1, '\0');
                Arrays.fill(pw2, '\0');
                return 1;
            }
            pw = new String(pw1);
            Arrays.fill(pw1, '\0');
            Arrays.fill(pw2, '\0');
        }
        System.out.println(BCrypt.hashpw(pw, BCrypt.gensalt(rounds)));
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new SynergyPasswd()).execute(args));
    }
}
