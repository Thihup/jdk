/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package compiler.arraycopy;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.whitebox.WhiteBox;
import jdk.test.whitebox.cpuinfo.CPUInfo;

/*
 * @test
 * @bug 8999999
 * @summary Test FSRM (Fast Short REP MOVSB) optimization for array copies
 * @library /test/lib /
 * @requires os.arch=="amd64" | os.arch=="x86_64"
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions 
 *                   -XX:+WhiteBoxAPI compiler.arraycopy.TestFSRMArrayCopy
 */

public class TestFSRMArrayCopy {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final int ITERATIONS = 10000;
    
    // Test arrays of different sizes to cover different copy paths
    private static final int[] SIZES = {4, 8, 16, 32, 64, 128, 256, 512, 1024};
    
    public static void main(String[] args) throws Exception {
        boolean hasFSRM = CPUInfo.hasFeature("fsrm");
        System.out.println("CPU has FSRM support: " + hasFSRM);
        
        if (args.length == 0) {
            // Run with different FSRM settings
            testWithFSRMEnabled(hasFSRM);
            testWithFSRMDisabled();
            System.out.println("\n=== All tests passed ===");
            printVerificationInstructions();
        } else if (args[0].equals("run")) {
            // Actually run the array copy tests
            runTests();
        }
    }
    
    private static void testWithFSRMEnabled(boolean hasFSRM) throws Exception {
        System.out.println("\n=== Testing with UseFSRM enabled ===");
        ProcessBuilder pb = ProcessTools.createTestJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xbootclasspath/a:.",
            "-XX:+UseFSRM",
            "-Xbatch",
            "-XX:CompileCommand=compileonly,compiler.arraycopy.TestFSRMArrayCopy::*",
            "compiler.arraycopy.TestFSRMArrayCopy",
            "run"
        );
        
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        
        if (hasFSRM) {
            output.shouldNotContain("Fast Short REP MOVSB is not available on this CPU");
        } else {
            output.shouldContain("Fast Short REP MOVSB is not available on this CPU");
        }
        
        output.shouldContain("Test completed successfully");
    }
    
    private static void testWithFSRMDisabled() throws Exception {
        System.out.println("\n=== Testing with UseFSRM disabled ===");
        ProcessBuilder pb = ProcessTools.createTestJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xbootclasspath/a:.",
            "-XX:-UseFSRM",
            "-Xbatch",
            "-XX:CompileCommand=compileonly,compiler.arraycopy.TestFSRMArrayCopy::*",
            "compiler.arraycopy.TestFSRMArrayCopy",
            "run"
        );
        
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain("Test completed successfully");
    }
    
    private static void runTests() {
        System.out.println("Running array copy tests...");
        
        // Test byte arrays
        testByteArrayCopy();
        
        // Test short arrays
        testShortArrayCopy();
        
        // Test int arrays
        testIntArrayCopy();
        
        // Test long arrays
        testLongArrayCopy();
        
        // Test object arrays
        testObjectArrayCopy();
        
        System.out.println("Test completed successfully");
    }
    
    private static void testByteArrayCopy() {
        for (int size : SIZES) {
            byte[] src = new byte[size];
            byte[] dst = new byte[size];
            
            // Initialize source array
            for (int i = 0; i < size; i++) {
                src[i] = (byte) i;
            }
            
            // Warm up and test
            for (int i = 0; i < ITERATIONS; i++) {
                System.arraycopy(src, 0, dst, 0, size);
            }
            
            // Verify
            for (int i = 0; i < size; i++) {
                if (dst[i] != (byte) i) {
                    throw new RuntimeException("Byte array copy failed at index " + i + 
                                             " for size " + size);
                }
            }
        }
    }
    
    private static void testShortArrayCopy() {
        for (int size : SIZES) {
            short[] src = new short[size];
            short[] dst = new short[size];
            
            for (int i = 0; i < size; i++) {
                src[i] = (short) i;
            }
            
            for (int i = 0; i < ITERATIONS; i++) {
                System.arraycopy(src, 0, dst, 0, size);
            }
            
            for (int i = 0; i < size; i++) {
                if (dst[i] != (short) i) {
                    throw new RuntimeException("Short array copy failed at index " + i);
                }
            }
        }
    }
    
    private static void testIntArrayCopy() {
        for (int size : SIZES) {
            int[] src = new int[size];
            int[] dst = new int[size];
            
            for (int i = 0; i < size; i++) {
                src[i] = i;
            }
            
            for (int i = 0; i < ITERATIONS; i++) {
                System.arraycopy(src, 0, dst, 0, size);
            }
            
            for (int i = 0; i < size; i++) {
                if (dst[i] != i) {
                    throw new RuntimeException("Int array copy failed at index " + i);
                }
            }
        }
    }
    
    private static void testLongArrayCopy() {
        for (int size : SIZES) {
            long[] src = new long[size];
            long[] dst = new long[size];
            
            for (int i = 0; i < size; i++) {
                src[i] = i;
            }
            
            for (int i = 0; i < ITERATIONS; i++) {
                System.arraycopy(src, 0, dst, 0, size);
            }
            
            for (int i = 0; i < size; i++) {
                if (dst[i] != i) {
                    throw new RuntimeException("Long array copy failed at index " + i);
                }
            }
        }
    }
    
    private static void testObjectArrayCopy() {
        for (int size : SIZES) {
            String[] src = new String[size];
            String[] dst = new String[size];
            
            for (int i = 0; i < size; i++) {
                src[i] = "String" + i;
            }
            
            for (int i = 0; i < ITERATIONS; i++) {
                System.arraycopy(src, 0, dst, 0, size);
            }
            
            for (int i = 0; i < size; i++) {
                if (!src[i].equals(dst[i])) {
                    throw new RuntimeException("Object array copy failed at index " + i);
                }
            }
        }
    }
    
    private static void printVerificationInstructions() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("HOW TO VERIFY FSRM INSTRUCTION IS USED");
        System.out.println("=".repeat(70));
        System.out.println();
        System.out.println("To verify that REP MOVSB (FSRM) instruction is being emitted,");
        System.out.println("run the test with PrintAssembly enabled:");
        System.out.println();
        System.out.println("  java -XX:+UnlockDiagnosticVMOptions \\");
        System.out.println("       -XX:+PrintAssembly \\");
        System.out.println("       -XX:+UseFSRM \\");
        System.out.println("       -XX:CompileCommand=print,*arraycopy* \\");
        System.out.println("       -XX:CompileCommand=print,*jbyte_disjoint_arraycopy* \\");
        System.out.println("       compiler.arraycopy.TestFSRMArrayCopy run");
        System.out.println();
        System.out.println("Look for these instruction sequences in the output:");
        System.out.println();
        System.out.println("WITH FSRM (-XX:+UseFSRM on FSRM-capable CPU):");
        System.out.println("  - You should see: 'rep movsb' or 'f3 a4'");
        System.out.println("  - This is the byte-level REP MOVSB instruction");
        System.out.println();
        System.out.println("WITHOUT FSRM (-XX:-UseFSRM or non-FSRM CPU):");
        System.out.println("  - You should see: 'rep movsq' or 'f3 48 a5'");
        System.out.println("  - This is the quadword-level REP MOVSQ instruction");
        System.out.println();
        System.out.println("Alternative verification using CompileCommand=log:");
        System.out.println();
        System.out.println("  java -XX:+UnlockDiagnosticVMOptions \\");
        System.out.println("       -XX:CompileCommand=log,*arraycopy* \\");
        System.out.println("       -XX:+UseFSRM \\");
        System.out.println("       compiler.arraycopy.TestFSRMArrayCopy run 2>&1 | \\");
        System.out.println("       grep -i 'rep\\|movsb\\|movsq'");
        System.out.println();
        System.out.println("CPU Feature Check:");
        System.out.println("  - Run: java -XX:+PrintFlagsFinal -version | grep UseFSRM");
        System.out.println("  - If CPU supports FSRM, UseFSRM will default to 'true'");
        System.out.println("=".repeat(70));
    }
}
