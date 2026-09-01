#!/usr/bin/env python3
"""Assert that every native library inside an APK is 16 KB page aligned.

Android 16 requires 16 KB alignment from apps targeting SDK 36 or newer: a 4 KB
aligned library simply fails to dlopen, and Treble Info swallows the resulting
UnsatisfiedLinkError, so the breakage is invisible until someone runs the app on
a 16 KB device. Two things have to hold:

  * every LOAD segment of each .so is linked with p_align >= 16384, and
  * each .so is stored uncompressed at a 16 KB aligned offset in the APK.

SPDX-License-Identifier: GPL-3.0-or-later
"""
import struct
import sys
import zipfile

PAGE = 16 * 1024
PT_LOAD = 1


def load_aligns(data: bytes) -> list[int]:
    if data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    elf64 = data[4] == 2
    if elf64:
        phoff, = struct.unpack_from("<Q", data, 0x20)
        phentsize, phnum = struct.unpack_from("<HH", data, 0x36)
        align_off, fmt = 0x30, "<Q"
    else:
        phoff, = struct.unpack_from("<I", data, 0x1C)
        phentsize, phnum = struct.unpack_from("<HH", data, 0x2A)
        align_off, fmt = 0x1C, "<I"
    aligns = []
    for i in range(phnum):
        off = phoff + i * phentsize
        p_type, = struct.unpack_from("<I", data, off)
        if p_type == PT_LOAD:
            aligns.append(struct.unpack_from(fmt, data, off + align_off)[0])
    return aligns


def check(apk_path: str) -> list[str]:
    failures = []
    raw = open(apk_path, "rb").read()
    with zipfile.ZipFile(apk_path) as apk:
        for info in apk.infolist():
            if not info.filename.endswith(".so"):
                continue
            aligns = load_aligns(apk.read(info.filename))
            name_len, extra_len = struct.unpack_from("<HH", raw, info.header_offset + 26)
            data_offset = info.header_offset + 30 + name_len + extra_len
            problems = []
            if not aligns or min(aligns) < PAGE:
                problems.append("LOAD p_align " + " ".join(hex(a) for a in aligns))
            if info.compress_type != zipfile.ZIP_STORED:
                problems.append("compressed in the APK")
            elif data_offset % PAGE:
                problems.append(f"zip offset {data_offset} is not 16 KB aligned")
            status = "; ".join(problems) if problems else "ok"
            print(f"  {info.filename}: {status}")
            if problems:
                failures.append(f"{apk_path}!{info.filename}: {status}")
    return failures


def main(argv: list[str]) -> int:
    if not argv:
        print("usage: check_elf_alignment.py <apk> [<apk> ...]", file=sys.stderr)
        return 2
    failures = []
    for apk in argv:
        print(apk)
        failures += check(apk)
    for failure in failures:
        print(f"::error::{failure}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
