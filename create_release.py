import urllib.request
import json
import sys
import os

# Read token from environment variable to keep it secure
token = os.environ.get("GITHUB_TOKEN")
if not token:
    print("Error: GITHUB_TOKEN environment variable not set.")
    sys.exit(1)

owner = "nxghtCry0"
repo = "marlowwwclient"
tag = "v2.3.1"
name = "Marlow Client v2.3.1"

try:
    with open("CHANGELOG.md", "r", encoding="utf-8") as f:
        body = f.read()
except FileNotFoundError:
    body = "Release version 2.3.1"

data = {
    "tag_name": tag,
    "name": name,
    "body": body,
    "draft": False,
    "prerelease": False
}

req = urllib.request.Request(
    f"https://api.github.com/repos/{owner}/{repo}/releases",
    data=json.dumps(data).encode("utf-8"),
    headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json",
        "Content-Type": "application/json"
    },
    method="POST"
)

try:
    with urllib.request.urlopen(req) as response:
        res_data = json.loads(response.read().decode("utf-8"))
        print(f"Release created with ID: {res_data['id']}")
        upload_url = res_data["upload_url"].split("{")[0]
except Exception as e:
    print(f"Failed to create release: {e}")
    sys.exit(1)

file_path = "build/libs/im-not-cheating-you-are-2.3.1.jar"
file_name = "im-not-cheating-you-are-2.3.1.jar"
upload_uri = f"{upload_url}?name={file_name}"

print(f"Uploading asset to {upload_uri}")

try:
    with open(file_path, "rb") as f:
        asset_data = f.read()
except FileNotFoundError:
    print(f"Error: Build file {file_path} not found.")
    sys.exit(1)

req_upload = urllib.request.Request(
    upload_uri,
    data=asset_data,
    headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json",
        "Content-Type": "application/java-archive"
    },
    method="POST"
)

try:
    with urllib.request.urlopen(req_upload) as response:
        asset_res = json.loads(response.read().decode("utf-8"))
        print(f"Asset uploaded successfully! Download URL: {asset_res['browser_download_url']}")
except Exception as e:
    print(f"Failed to upload asset: {e}")
    sys.exit(1)
