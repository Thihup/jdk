# FSRM (Fast Short REP MOVSB) Testing and Verification Guide

## Overview

This guide explains how to test and verify that the FSRM (Fast Short REP MOVSB) optimization is working correctly in the JVM.

## Test Files

### 1. TestFSRMArrayCopy.java
Comprehensive functional test that:
- Tests array copies with different sizes and types
- Verifies UseFSRM flag behavior
- Checks CPU feature detection
- Validates correctness of array copies

### 2. TestFSRMMicroBenchmark.java
Performance micro-benchmark that:
- Measures array copy performance with and without FSRM
- Focuses on small array sizes where FSRM provides the most benefit
- Reports timing information for comparison

## Running the Tests

### Basic Functional Test
```bash
cd test/hotspot/jtreg
jtreg -vmoption:-Xmx512m compiler/arraycopy/TestFSRMArrayCopy.java
```

### Micro-Benchmark Test
```bash
jtreg compiler/arraycopy/TestFSRMMicroBenchmark.java
```

## Verifying FSRM Instructions are Used

### Method 1: Using PrintAssembly (Most Direct)

**Prerequisites:** 
- Install hsdis (HotSpot Disassembler) plugin for your platform
- On Linux: `hsdis-amd64.so` should be in `$JAVA_HOME/lib/server/`

**Command:**
```bash
java -XX:+UnlockDiagnosticVMOptions \
     -XX:+PrintAssembly \
     -XX:+UseFSRM \
     -XX:CompileCommand=print,*arraycopy* \
     -XX:CompileCommand=print,*jbyte_disjoint_arraycopy* \
     -Xbatch \
     compiler.arraycopy.TestFSRMArrayCopy run \
     2>&1 | tee assembly.log
```

**What to look for in output:**

**WITH FSRM** (`-XX:+UseFSRM` on FSRM-capable CPU):
```
rep movsb          ; or in hex: f3 a4
```

**WITHOUT FSRM** (`-XX:-UseFSRM` or non-FSRM CPU):
```
rep movsq          ; or in hex: f3 48 a5
```

### Method 2: Using CompileCommand=log

```bash
java -XX:+UnlockDiagnosticVMOptions \
     -XX:CompileCommand=log,*arraycopy* \
     -XX:+UseFSRM \
     -Xbatch \
     compiler.arraycopy.TestFSRMArrayCopy run \
     2>&1 | grep -i 'rep\|movsb\|movsq'
```

### Method 3: Check Default Flag Value

Verify FSRM auto-detection:
```bash
java -XX:+PrintFlagsFinal -version | grep UseFSRM
```

**Expected output on FSRM-capable CPU:**
```
bool UseFSRM = true    {product} {default}
```

**Expected output on non-FSRM CPU:**
```
bool UseFSRM = false   {product} {default}
```

### Method 4: Using Compiler Directives File

Create a file `compiler_directives.json`:
```json
[
  {
    "match": ["*arraycopy*", "*jbyte_disjoint_arraycopy*"],
    "PrintAssembly": true,
    "PrintInlining": true
  }
]
```

Run with:
```bash
java -XX:CompilerDirectivesFile=compiler_directives.json \
     -XX:+UnlockDiagnosticVMOptions \
     -XX:+UseFSRM \
     compiler.arraycopy.TestFSRMArrayCopy run
```

## Understanding the Output

### REP MOVSB (FSRM) - Opcode: F3 A4
- Copies RCX bytes from [RSI] to [RDI]
- One byte at a time at the ISA level
- Optimized by FSRM microarchitecture on Ice Lake+
- Best for small copies (few bytes to ~256 bytes)

### REP MOVSQ (Traditional) - Opcode: F3 48 A5  
- Copies RCX quadwords (8 bytes each) from [RSI] to [RDI]
- 8 bytes at a time at the ISA level
- Traditional approach used before FSRM

## Performance Testing

### Compare FSRM vs Non-FSRM Performance

**Run with FSRM:**
```bash
java -Xbatch -XX:+UseFSRM \
     -XX:CompileCommand=compileonly,*TestFSRMMicroBenchmark* \
     compiler.arraycopy.TestFSRMMicroBenchmark
```

**Run without FSRM:**
```bash
java -Xbatch -XX:-UseFSRM \
     -XX:CompileCommand=compileonly,*TestFSRMMicroBenchmark* \
     compiler.arraycopy.TestFSRMMicroBenchmark
```

**Expected Results on FSRM-capable CPU:**
- Small arrays (8-256 bytes): FSRM should be faster or comparable
- Larger arrays (>256 bytes): May vary by CPU microarchitecture

## Troubleshooting

### "Fast Short REP MOVSB is not available on this CPU"
This warning appears when you try to enable UseFSRM on a CPU that doesn't support it.
- FSRM requires Ice Lake (or newer) Intel CPUs
- Check your CPU: `cat /proc/cpuinfo | grep fsrm` (Linux)

### Cannot see assembly output
- Install hsdis disassembler plugin
- Check that hsdis library is in the correct directory
- Verify with: `java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly -version`

### No difference between FSRM and non-FSRM
- Ensure methods are compiled with C2 (use `-Xbatch`)
- Check that AVX3Threshold is not forcing different code paths
- Verify array sizes are in the range where stub uses REP instructions

## CPU Feature Detection

Check if your CPU supports FSRM:

**Linux:**
```bash
cat /proc/cpuinfo | grep -o fsrm
lscpu | grep -i fsrm
```

**Using JVM:**
```bash
java -XX:+UnlockDiagnosticVMOptions \
     -XX:+PrintFlagsFinal \
     -version | grep -E "UseFSRM|CPU Features"
```

## Additional Resources

- Intel Architecture Optimization Reference Manual
- Section on "Fast Short REP MOVSB"
- JEP for FSRM support (if available)
- Related JDK bug/enhancement: 8999999

## Notes

- FSRM is most beneficial for small array copies (common in Java)
- Typical use cases: String operations, small collections, frequent small arraycopy calls
- The optimization is most visible on Ice Lake and newer Intel architectures
- AMD CPUs may have different optimization characteristics
