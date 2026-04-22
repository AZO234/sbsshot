import os
import glob
import json
import requests
import time

# 設定
MODRINTH_TOKEN = os.environ.get("MODRINTH_TOKEN")
MODRINTH_PROJECT_ID = os.environ.get("MODRINTH_PROJECT_ID")

TAG_NAME = os.environ.get("TAG_NAME")
RELEASE_NAME = os.environ.get("RELEASE_NAME")
CHANGELOG = os.environ.get("CHANGELOG")

def publish():
    print(f"--- Modrinth Publish: {TAG_NAME} ---")
    headers = {"Authorization": MODRINTH_TOKEN}
    
    # 0. プロジェクト情報の取得 (スラッグから正式なIDを取得)
    res = requests.get(f"https://api.modrinth.com/v2/project/{MODRINTH_PROJECT_ID}", headers=headers)
    if res.status_code == 200:
        project_data = res.json()
        actual_project_id = project_data["id"]
        print(f"Project found: {project_data['title']} (ID: {actual_project_id})")
    else:
        print(f"Failed to fetch project info for '{MODRINTH_PROJECT_ID}' (status: {res.status_code})")
        print("Please check if MODRINTH_PROJECT_ID and MODRINTH_TOKEN are correct.")
        return

    # 1. 既存バージョンの検索
    # プロジェクトの全バージョンを取得
    res = requests.get(f"https://api.modrinth.com/v2/project/{actual_project_id}/version", headers=headers)
    if res.status_code == 200:
        versions = res.json()
        for v in versions:
            if v["version_number"] == TAG_NAME:
                print(f"Found existing version '{TAG_NAME}' (ID: {v['id']}). Deleting...")
                requests.delete(f"https://api.modrinth.com/v2/version/{v['id']}", headers=headers)
                time.sleep(2) # 削除反映待ち
    else:
        print(f"Failed to fetch versions (status: {res.status_code})")

    # (中略: JARファイルの検索部分は変更なし)
    jar_files = glob.glob("dist/**/*.jar", recursive=True)
    if not jar_files:
        print("No JAR files found to upload!")
        return

    print(f"Found {len(jar_files)} JAR files to upload.")

    # バージョン情報 (game_versions, loaders 等を自動収集)
    game_versions = set()
    loaders = set()
    files_to_upload = []
    
    # multipart/form-data の構成
    file_map = {}
    
    for i, path in enumerate(jar_files):
        filename = os.path.basename(path)
        loader = "fabric" if "fabric" in filename.lower() else "neoforge"
        mc_ver = os.path.basename(os.path.dirname(path))
        
        game_versions.add(mc_ver)
        loaders.add(loader)
        
        part_name = f"file_{i}"
        files_to_upload.append((part_name, (filename, open(path, "rb"), "application/java-archive")))
        file_map[part_name] = {
            "hashes": {},
            "file_type": "featured-release",
            "primary": i == 0
        }

    # Modrinth API の要求する JSON データ
    data = {
        "name": f"{RELEASE_NAME} (All versions)",
        "version_number": TAG_NAME,
        "changelog": CHANGELOG,
        "dependencies": [],
        "game_versions": sorted(list(game_versions)),
        "loaders": sorted(list(loaders)),
        "featured": True,
        "project_id": actual_project_id,
        "version_type": "release",
        "file_parts": list(file_map.keys()),
        "primary_file": list(file_map.keys())[0] if file_map else None
    }

    # files_to_upload の構成
    # 各パート名に対応するファイルをタプルで指定
    # 'data' フィールドは json 形式の文字列で送る
    payload = {"data": json.dumps(data)}
    
    # 3. リクエスト送信
    res = requests.post(
        "https://api.modrinth.com/v2/version",
        headers={"Authorization": MODRINTH_TOKEN},
        files=files_to_upload,
        data=payload
    )

    if res.status_code == 200:
        print("Successfully published all versions to Modrinth!")
    else:
        print(f"Failed to publish (status: {res.status_code})")
        print(f"Response: {res.text}")

if __name__ == "__main__":
    publish()
