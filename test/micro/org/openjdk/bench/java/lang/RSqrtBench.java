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
package org.openjdk.bench.java.lang;

import java.util.concurrent.TimeUnit;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark to compare performance of 1 / Math.sqrt(x) with and without rsqrt intrinsic
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgsAppend = {"-XX:-TieredCompilation"})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class RSqrtBench {

    @Param("0")
    public long seed;

    private double[] doubleValues;
    private float[] floatValues;
    private static final int SIZE = 1024;

    @Setup
    public void setup() {
        Random random = new Random(seed);
        doubleValues = new double[SIZE];
        floatValues = new float[SIZE];
        
        for (int i = 0; i < SIZE; i++) {
            // Generate positive values to avoid NaN from sqrt
            doubleValues[i] = random.nextDouble() * 1000.0 + 1.0;
            floatValues[i] = (float)(random.nextDouble() * 1000.0 + 1.0);
        }
    }

    @Benchmark
    public double rsqrtDouble() {
        double sum = 0.0;
        for (int i = 0; i < SIZE; i++) {
            // This should be optimized to rsqrt intrinsic
            sum += 1.0 / Math.sqrt(doubleValues[i]);
        }
        return sum;
    }

    @Benchmark
    public float rsqrtFloat() {
        float sum = 0.0f;
        for (int i = 0; i < SIZE; i++) {
            // This should be optimized to rsqrtss intrinsic
            sum += (float)(1.0 / Math.sqrt((double)floatValues[i]));
        }
        return sum;
    }

    @Benchmark
    public double sqrtThenDivideDouble() {
        double sum = 0.0;
        for (int i = 0; i < SIZE; i++) {
            // Baseline: separate sqrt and division
            double sqrtVal = Math.sqrt(doubleValues[i]);
            sum += 1.0 / sqrtVal;
        }
        return sum;
    }

    @Benchmark
    public float sqrtThenDivideFloat() {
        float sum = 0.0f;
        for (int i = 0; i < SIZE; i++) {
            // Baseline: separate sqrt and division
            float sqrtVal = (float)Math.sqrt((double)floatValues[i]);
            sum += 1.0f / sqrtVal;
        }
        return sum;
    }

    @Benchmark
    public double sqrtOnlyDouble() {
        double sum = 0.0;
        for (int i = 0; i < SIZE; i++) {
            // Baseline: just sqrt for comparison
            sum += Math.sqrt(doubleValues[i]);
        }
        return sum;
    }

    @Benchmark
    public float sqrtOnlyFloat() {
        float sum = 0.0f;
        for (int i = 0; i < SIZE; i++) {
            // Baseline: just sqrt for comparison
            sum += (float)Math.sqrt((double)floatValues[i]);
        }
        return sum;
    }
}
