import os
import glob
import json
import requests
import time
from collections import defaultdict

# 設定
MODRINTH_TOKEN = os.environ.get("MODRINTH_TOKEN", "").strip()
MODRINTH_PROJECT_ID = os.environ.get("MODRINTH_PROJECT_ID", "").strip()

TAG_NAME = os.environ.get("TAG_NAME", "").strip()
RELEASE_NAME = os.environ.get("RELEASE_NAME", "").strip()
CHANGELOG = os.environ.get("CHANGELOG", "").strip()

def publish():
    print(f"--- Modrinth Publish: {TAG_NAME} ---")

    if not MODRINTH_TOKEN:
        print("Error: MODRINTH_TOKEN is not set.")
        return
    if not MODRINTH_PROJECT_ID:
        print("Error: MODRINTH_PROJECT_ID is not set.")
        return

    raw_token = MODRINTH_TOKEN.replace("Bearer ", "").strip()
    headers = {"Authorization": raw_token}

    res = requests.get(f"https://api.modrinth.com/v2/project/{MODRINTH_PROJECT_ID}")
    if res.status_code != 200:
        res = requests.get(f"https://api.modrinth.com/v2/project/{MODRINTH_PROJECT_ID}", headers=headers)

    if res.status_code == 200:
        project_data = res.json()
        actual_project_id = project_data["id"]
        print(f"Project found: {project_data['title']} (ID: {actual_project_id})")
    else:
        print(f"Failed to fetch project info (status: {res.status_code})")
        return

    # 1. プロジェクトレベルの環境メタデータを更新
    patch_res = requests.patch(
        f"https://api.modrinth.com/v2/project/{actual_project_id}",
        headers={**headers, "Content-Type": "application/json"},
        data=json.dumps({"client_side": "required", "server_side": "unsupported"})
    )
    if patch_res.status_code == 204:
        print("Project environment metadata updated.")
    else:
        print(f"Warning: Failed to update project metadata (status: {patch_res.status_code})")

    # 2. 既存バージョンの削除 (TAG_NAME が一致するものをすべて削除)
    res = requests.get(f"https://api.modrinth.com/v2/project/{actual_project_id}/version", headers=headers)
    if res.status_code == 200:
        versions = res.json()
        deleted = False
        for v in versions:
            if v["version_number"] == TAG_NAME:
                print(f"Found existing version '{TAG_NAME}' (ID: {v['id']}). Deleting...")
                requests.delete(f"https://api.modrinth.com/v2/version/{v['id']}", headers=headers)
                time.sleep(1)
                deleted = True
        if deleted:
            print("Waiting for backend synchronization...")
            time.sleep(5)

    # 3. JARファイルの検索
    all_jar_files = glob.glob("dist/**/*.jar", recursive=True)
    all_jar_files = [f for f in all_jar_files if not any(x in os.path.basename(f).lower() for x in ["sources", "dev", "common"])]

    # mc_version ディレクトリごとにグループ化
    by_mc_version = defaultdict(list)
    for path in all_jar_files:
        mc_ver = os.path.basename(os.path.dirname(path))
        by_mc_version[mc_ver].append(path)

    # mc_version × loader ごとに1つずつアップロード
    for mc_ver in sorted(by_mc_version.keys()):
        jars = by_mc_version[mc_ver]
        for loader_type in ["fabric", "neoforge"]:
            loader_jars = [f for f in jars if loader_type in os.path.basename(f).lower()]
            if not loader_jars:
                continue

            path = loader_jars[0]
            filename = os.path.basename(path)
            print(f"\n>> Uploading {loader_type.capitalize()} {mc_ver} ({filename})...")

            data = {
                "name": f"{RELEASE_NAME} ({loader_type.capitalize()} {mc_ver})",
                "version_number": TAG_NAME,
                "changelog": CHANGELOG,
                "dependencies": [],
                "game_versions": [mc_ver],
                "loaders": [loader_type],
                "featured": True,
                "project_id": actual_project_id,
                "version_type": "release",
                "client_side": "required",
                "server_side": "unsupported",
                "file_parts": ["file_0"],
                "primary_file": "file_0"
            }

            files_to_upload = [("file_0", (filename, open(path, "rb"), "application/java-archive"))]

            res = requests.post(
                "https://api.modrinth.com/v2/version",
                headers=headers,
                files=files_to_upload,
                data={"data": json.dumps(data)}
            )

            if res.status_code == 200:
                print(f"Successfully published {loader_type} {mc_ver}!")
            else:
                print(f"Failed to publish {loader_type} {mc_ver} (status: {res.status_code})")
                print(f"Response: {res.text}")

            time.sleep(2)

if __name__ == "__main__":
    publish()
