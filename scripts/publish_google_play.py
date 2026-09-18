#!/usr/bin/env python3
import argparse
import os
import sys
from pathlib import Path

import requests
from google.auth.transport.requests import Request
from google.oauth2 import service_account


def fail(response, action):
    print(f"{action} failed: HTTP {response.status_code}", file=sys.stderr)
    print(response.text[:10000], file=sys.stderr)
    raise SystemExit(1)


def request_json(method, url, headers, **kwargs):
    response = requests.request(method, url, headers=headers, timeout=120, **kwargs)
    if not response.ok:
        fail(response, method + " " + url)
    if response.text.strip():
        return response.json()
    return {}


def read_version_name(path):
    for line in path.read_text().splitlines():
        if line.startswith("versionName="):
            return line.split("=", 1)[1].strip()
    raise RuntimeError(f"versionName missing from {path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--aab", required=True)
    parser.add_argument("--package", default="com.beatbridge")
    parser.add_argument("--track", default="production")
    parser.add_argument("--status", default="completed")
    parser.add_argument("--key", default=os.environ.get("GOOGLE_PLAY_JSON_KEY_PATH"))
    parser.add_argument("--version-properties", default="version.properties")
    parser.add_argument(
        "--notes",
        default="Updated app icon and visual hierarchy, plus Bluetooth connection reliability improvements.",
    )
    args = parser.parse_args()

    if not args.key:
        raise SystemExit("Google Play service-account key path is required")

    aab = Path(args.aab)
    if not aab.is_file():
        raise SystemExit(f"AAB not found: {aab}")

    release_name = read_version_name(Path(args.version_properties))
    credentials = service_account.Credentials.from_service_account_file(
        args.key,
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    credentials.refresh(Request())

    auth = {"Authorization": f"Bearer {credentials.token}"}
    json_headers = {**auth, "Content-Type": "application/json"}
    base = (
        "https://androidpublisher.googleapis.com/androidpublisher/v3/"
        f"applications/{args.package}"
    )
    upload_base = (
        "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/"
        f"applications/{args.package}"
    )

    edit = request_json("POST", base + "/edits", json_headers, json={})["id"]
    print(f"Created edit {edit}")

    with aab.open("rb") as handle:
        bundle = request_json(
            "POST",
            f"{upload_base}/edits/{edit}/bundles?uploadType=media",
            {**auth, "Content-Type": "application/octet-stream"},
            data=handle,
        )

    version_code = str(bundle["versionCode"])
    print(f"Uploaded AAB version code {version_code}")

    track = {
        "track": args.track,
        "releases": [
            {
                "name": release_name,
                "versionCodes": [version_code],
                "releaseNotes": [{"language": "en-US", "text": args.notes}],
                "status": args.status,
            }
        ],
    }
    request_json(
        "PUT",
        f"{base}/edits/{edit}/tracks/{args.track}",
        json_headers,
        json=track,
    )
    print(f"Updated {args.track} track to {release_name} ({args.status})")

    validate_url = f"{base}/edits/{edit}:validate"
    validation = requests.post(validate_url, headers=json_headers, json={}, timeout=120)
    if (
        not validation.ok
        and args.status != "draft"
        and validation.status_code == 400
        and "Only releases with status draft may be created on draft app"
        in validation.text
    ):
        track["releases"][0]["status"] = "draft"
        request_json(
            "PUT",
            f"{base}/edits/{edit}/tracks/{args.track}",
            json_headers,
            json=track,
        )
        print("App is still a Play draft; keeping the initial release in draft status")
        validation = requests.post(validate_url, headers=json_headers, json={}, timeout=120)

    if not validation.ok:
        fail(validation, "POST " + validate_url)
    print("Edit validation passed")

    result = request_json("POST", f"{base}/edits/{edit}:commit", json_headers, json={})
    print(f"Committed Google Play edit {result.get('id', edit)}")


if __name__ == "__main__":
    main()
