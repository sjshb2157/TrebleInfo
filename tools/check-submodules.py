#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Check that every submodule pointer exists in its upstream repository.

Editing a file inside a submodule and committing it there produces a commit
that only exists locally. The parent repository then records a SHA nobody else
can fetch, and every fresh clone fails with

    fatal: Fetched in submodule path '...', but it did not contain <sha>.

Run from the repository root. Useful as a pre-push hook as well as in CI.
"""

import subprocess
import sys
import tempfile
import urllib.error
import urllib.request

GITILES_HOST = "android.googlesource.com"


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], capture_output=True, text=True, check=True
    ).stdout.strip()


def submodules() -> list[tuple[str, str]]:
    try:
        config = git("config", "-f", ".gitmodules", "--get-regexp", r"^submodule\..*\.path$")
    except subprocess.CalledProcessError:
        return []
    found = []
    for line in config.splitlines():
        key, path = line.split(maxsplit=1)
        name = key[len("submodule.") : -len(".path")]
        url = git("config", "-f", ".gitmodules", "--get", f"submodule.{name}.url")
        found.append((path, url))
    return found


def exists_upstream(url: str, sha: str) -> bool:
    if GITILES_HOST in url:
        try:
            with urllib.request.urlopen(f"{url}/+/{sha}?format=JSON", timeout=30) as response:
                return response.status == 200
        except urllib.error.HTTPError:
            return False
    # Anything else: ask the remote for the object directly.
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["git", "init", "-q", tmp], check=True)
        return subprocess.run(
            ["git", "-C", tmp, "fetch", "-q", "--depth", "1", url, sha],
            capture_output=True,
        ).returncode == 0


def main() -> int:
    failed = False
    for path, url in submodules():
        # The index, so this also works as a pre-commit hook.
        sha = git("ls-files", "-s", path).split()[1]
        if exists_upstream(url, sha):
            print(f"ok       {path} {sha[:12]}")
        else:
            failed = True
            print(f"MISSING  {path} {sha[:12]} is not in {url}")
    if failed:
        print(
            "\nA submodule points at a commit that exists only locally. Reset it with:\n"
            "  git -C <path> fetch origin && git -C <path> checkout <upstream sha>\n"
            "  git add <path>",
            file=sys.stderr,
        )
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
