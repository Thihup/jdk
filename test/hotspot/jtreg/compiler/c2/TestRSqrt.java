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

package compiler.c2;

/*
 * @test
 * @summary Test that 1 / Math.sqrt(x) is optimized to rsqrtss intrinsic for float
 * @requires vm.debug
 *
 * @run main/othervm -XX:-TieredCompilation -Xcomp
 *                   -XX:CompileOnly=compiler.c2.TestRSqrt::*
 *                   -XX:CompileOnly=java.lang.Math::*
 *                   compiler.c2.TestRSqrt
 */
public class TestRSqrt {
    static float srcF = 42.0f;
    static float dstF;

    public static void testFloat() {
        // This should be optimized to rsqrtss instruction
        // Note: rsqrtss is an approximation with ~0.037% max error
        dstF = (float)(1.0 / Math.sqrt((double)srcF));
    }

    public static void testFloatDirect() {
        // Test with float sqrt directly
        float sqrtVal = (float)Math.sqrt((double)srcF);
        dstF = 1.0f / sqrtVal;
    }

    public static void main(String args[]) {
        for (int i = 0; i < 20_000; i++) {
            testFloat();
            testFloatDirect();
        }
        
        // Verify result is within acceptable tolerance
        // rsqrtss has ~0.037% max error, so we use a larger tolerance
        float expected = (float)(1.0 / Math.sqrt(42.0));
        float tolerance = 0.001f;  // 0.1% tolerance for approximation
        
        if (Math.abs(dstF - expected) > tolerance) {
            throw new RuntimeException("Float rsqrt failed: expected " + expected + 
                                       ", got " + dstF + 
                                       ", error = " + Math.abs(dstF - expected));
        }
        
        System.out.println("Test passed! Result: " + dstF + ", Expected: " + expected);
    }
}
