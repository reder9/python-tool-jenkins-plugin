#!/usr/bin/env python3
"""
Python Versions Catalog Generator for Jenkins Python Tool Plugin.

Fetches the latest official standalone Python releases from
astral-sh/python-build-standalone and indygreg/python-build-standalone,
extracts multi-platform asset URLs (Windows, Linux, macOS),
and updates src/main/resources/io/jenkins/plugins/python/versions.json.
"""

import argparse
import datetime
import json
import os
import re
import sys
import urllib.request

REPO = "astral-sh/python-build-standalone"
API_URL = f"https://api.github.com/repos/{REPO}/releases?per_page=30"

TARGET_MAP = {
    "x86_64-unknown-linux-gnu": "linux-x86_64",
    "aarch64-unknown-linux-gnu": "linux-aarch64",
    "x86_64-apple-darwin": "macos-x86_64",
    "aarch64-apple-darwin": "macos-aarch64",
    "x86_64-pc-windows-msvc": "windows-x86_64",
    "i686-pc-windows-msvc": "windows-x86",
}

# Regex matching: cpython-{version}+{release_tag}-{target}-install_only.tar.gz
ASSET_REGEX = re.compile(
    r"^cpython-(?P<version>\d+\.\d+\.\d+)\+(?P<tag>\d{8})-(?P<target>[\w-]+)-install_only\.tar\.gz$"
)


def fetch_releases(token=None, limit=30):
    url = f"https://api.github.com/repos/{REPO}/releases?per_page={limit}"
    print(f"[INFO] Querying GitHub Releases from {REPO}...")
    headers = {
        "User-Agent": "Jenkins-Python-Plugin-Updater",
        "Accept": "application/vnd.github.v3+json",
    }
    if token:
        headers["Authorization"] = f"token {token}"

    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode("utf-8")
            return json.loads(data)
    except Exception as e:
        print(f"[ERROR] Failed to fetch releases from GitHub: {e}", file=sys.stderr)
        sys.exit(1)


def parse_releases(releases):
    catalog = {}

    for release in releases:
        tag = release.get("tag_name")
        for asset in release.get("assets", []):
            name = asset.get("name", "")
            if "install_only" not in name or "stripped" in name or not name.endswith(".tar.gz"):
                continue

            match = ASSET_REGEX.match(name)
            if not match:
                continue

            version = match.group("version")
            target = match.group("target")

            platform_key = TARGET_MAP.get(target)
            if not platform_key:
                continue

            if version not in catalog:
                catalog[version] = {
                    "releaseTag": tag,
                    "platforms": {},
                }

            if platform_key not in catalog[version]["platforms"]:
                catalog[version]["platforms"][platform_key] = {
                    "url": asset.get("browser_download_url"),
                    "size": asset.get("size", 0),
                    "filename": name,
                }

    full_catalog = {}
    for ver, data in catalog.items():
        platforms = data["platforms"]
        if "linux-x86_64" in platforms and "windows-x86_64" in platforms:
            full_catalog[ver] = data

    return full_catalog


def sort_versions(versions_dict):
    def ver_key(v):
        try:
            return [int(x) for x in v.split(".")]
        except ValueError:
            return [0, 0, 0]

    sorted_keys = sorted(versions_dict.keys(), key=ver_key, reverse=True)
    return {k: versions_dict[k] for k in sorted_keys}


def main():
    parser = argparse.ArgumentParser(description="Update Python standalone versions catalog.")
    parser.add_argument("--catalog", "-c", help="Path to versions.json output file.")
    parser.add_argument("--token", "-t", help="GitHub API personal access token.")
    parser.add_argument("--limit", "-l", type=int, default=30, help="Number of releases to query.")
    args = parser.parse_args()

    if args.catalog:
        target_file = os.path.abspath(args.catalog)
    else:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        # Try local plugin repo structure first
        candidate1 = os.path.join(script_dir, "..", "src", "main", "resources", "io", "jenkins", "plugins", "python", "versions.json")
        candidate2 = os.path.join(script_dir, "..", "Plugins", "python-tool-jenkins-plugin", "src", "main", "resources", "io", "jenkins", "plugins", "python", "versions.json")
        if os.path.exists(os.path.dirname(candidate1)):
            target_file = os.path.abspath(candidate1)
        else:
            target_file = os.path.abspath(candidate2)

    releases = fetch_releases(token=args.token, limit=args.limit)
    catalog = parse_releases(releases)
    sorted_catalog = sort_versions(catalog)

    output_data = {
        "schemaVersion": 1,
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "totalVersions": len(sorted_catalog),
        "versions": sorted_catalog,
    }

    os.makedirs(os.path.dirname(target_file), exist_ok=True)
    with open(target_file, "w", encoding="utf-8") as f:
        json.dump(output_data, f, indent=2)

    print(f"[SUCCESS] Wrote {len(sorted_catalog)} Python versions to:")
    print(f"          {target_file}")
    print("\nAvailable versions summary:")
    for ver in list(sorted_catalog.keys())[:10]:
        plats = list(sorted_catalog[ver]["platforms"].keys())
        print(f"  - Python {ver} (Platforms: {len(plats)})")


if __name__ == "__main__":
    main()
