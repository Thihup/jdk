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

/*
 * @test
 * @bug 8999999
 * @summary Micro-benchmark for FSRM (Fast Short REP MOVSB) short array copy performance
 * @requires os.arch=="amd64" | os.arch=="x86_64"
 * @run main/othervm -Xbatch -XX:+UseFSRM 
 *                   -XX:CompileCommand=compileonly,compiler.arraycopy.TestFSRMMicroBenchmark::*
 *                   compiler.arraycopy.TestFSRMMicroBenchmark
 * @run main/othervm -Xbatch -XX:-UseFSRM
 *                   -XX:CompileCommand=compileonly,compiler.arraycopy.TestFSRMMicroBenchmark::*
 *                   compiler.arraycopy.TestFSRMMicroBenchmark
 */

public class TestFSRMMicroBenchmark {
    private static final int WARMUP_ITERATIONS = 20000;
    private static final int MEASUREMENT_ITERATIONS = 100000;
    
    // Focus on small array sizes where FSRM provides the most benefit
    private static final int[] SMALL_SIZES = {8, 16, 32, 64, 128, 256};
    
    public static void main(String[] args) {
        System.out.println("FSRM Array Copy Micro-Benchmark");
        System.out.println("================================");
        
        // Test byte arrays (most relevant for FSRM)
        System.out.println("\nByte Arrays:");
        for (int size : SMALL_SIZES) {
            benchmarkByteArray(size);
        }
        
        // Test int arrays
        System.out.println("\nInt Arrays:");
        for (int size : SMALL_SIZES) {
            benchmarkIntArray(size);
        }
        
        System.out.println("\nBenchmark completed successfully");
    }
    
    private static void benchmarkByteArray(int size) {
        byte[] src = new byte[size];
        byte[] dst = new byte[size];
        
        // Initialize
        for (int i = 0; i < size; i++) {
            src[i] = (byte) i;
        }
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            copyByteArray(src, dst, size);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            copyByteArray(src, dst, size);
        }
        long end = System.nanoTime();
        
        double avgNanos = (double) (end - start) / MEASUREMENT_ITERATIONS;
        System.out.printf("  Size %4d bytes: %.2f ns/copy%n", size, avgNanos);
        
        // Verify correctness
        for (int i = 0; i < size; i++) {
            if (dst[i] != (byte) i) {
                throw new RuntimeException("Copy verification failed");
            }
        }
    }
    
    private static void copyByteArray(byte[] src, byte[] dst, int length) {
        System.arraycopy(src, 0, dst, 0, length);
    }
    
    private static void benchmarkIntArray(int size) {
        int[] src = new int[size];
        int[] dst = new int[size];
        
        for (int i = 0; i < size; i++) {
            src[i] = i;
        }
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            copyIntArray(src, dst, size);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            copyIntArray(src, dst, size);
        }
        long end = System.nanoTime();
        
        double avgNanos = (double) (end - start) / MEASUREMENT_ITERATIONS;
        System.out.printf("  Size %4d ints (%5d bytes): %.2f ns/copy%n", 
                         size, size * 4, avgNanos);
        
        // Verify
        for (int i = 0; i < size; i++) {
            if (dst[i] != i) {
                throw new RuntimeException("Copy verification failed");
            }
        }
    }
    
    private static void copyIntArray(int[] src, int[] dst, int length) {
        System.arraycopy(src, 0, dst, 0, length);
    }
}
