import os
import glob
import json
import requests
import time

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

    # 1. 既存バージョンの削除 (TAG_NAME が一致するものをすべて削除)
    res = requests.get(f"https://api.modrinth.com/v2/project/{actual_project_id}/version", headers=headers)
    if res.status_code == 200:
        versions = res.json()
        for v in versions:
            if v["version_number"] == TAG_NAME:
                print(f"Found existing version '{TAG_NAME}' (ID: {v['id']}). Deleting...")
                requests.delete(f"https://api.modrinth.com/v2/version/{v['id']}", headers=headers)
                time.sleep(1)
        if any(v["version_number"] == TAG_NAME for v in versions):
            print("Waiting for backend synchronization...")
            time.sleep(5)

    # 2. JARファイルの検索
    all_jar_files = glob.glob("dist/**/*.jar", recursive=True)
    all_jar_files = [f for f in all_jar_files if not any(x in os.path.basename(f).lower() for x in ["sources", "dev", "common"])]

    # ローダーごとに分けて投稿
    for loader_type in ["fabric", "neoforge"]:
        loader_jars = [f for f in all_jar_files if loader_type in os.path.basename(f).lower()]
        if not loader_jars:
            continue

        print(f"\n>> Uploading {loader_type.capitalize()} version...")
        
        game_versions = set()
        files_to_upload = []
        file_parts = []
        
        for i, path in enumerate(loader_jars):
            filename = os.path.basename(path)
            mc_ver = os.path.basename(os.path.dirname(path))
            game_versions.add(mc_ver)
            
            part_name = f"file_{i}"
            files_to_upload.append((part_name, (filename, open(path, "rb"), "application/java-archive")))
            file_parts.append(part_name)

        data = {
            "name": f"{RELEASE_NAME} ({loader_type.capitalize()})",
            "version_number": TAG_NAME,
            "changelog": CHANGELOG,
            "dependencies": [],
            "game_versions": sorted(list(game_versions)),
            "loaders": [loader_type],
            "featured": True,
            "project_id": actual_project_id,
            "version_type": "release",
            "client_side": "required",
            "server_side": "unsupported",
            "file_parts": file_parts,
            "primary_file": file_parts[0]
        }

        res = requests.post(
            "https://api.modrinth.com/v2/version",
            headers=headers,
            files=files_to_upload,
            data={"data": json.dumps(data)}
        )

        if res.status_code == 200:
            print(f"Successfully published {loader_type} version!")
        else:
            print(f"Failed to publish {loader_type} (status: {res.status_code})")
            print(f"Response: {res.text}")
        
        time.sleep(2)

if __name__ == "__main__":
    publish()
