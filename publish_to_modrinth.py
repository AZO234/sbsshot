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

    # Modrinth API は Bearer なしのトークンを直接受け取ることが多い
    # ユーザーが Bearer を付けて設定している可能性も考慮
    raw_token = MODRINTH_TOKEN.replace("Bearer ", "").strip()
    headers = {"Authorization": raw_token}
    
    # 0. プロジェクト情報の取得
    # まずはトークンなしでパブリックな情報を試行 (IDが正しいか確認)
    print(f"Checking project info for '{MODRINTH_PROJECT_ID}'...")
    res = requests.get(f"https://api.modrinth.com/v2/project/{MODRINTH_PROJECT_ID}")
    
    if res.status_code != 200:
        # パブリックで見つからない場合はトークン付きで再試行 (プライベートプロジェクトの場合)
        res = requests.get(f"https://api.modrinth.com/v2/project/{MODRINTH_PROJECT_ID}", headers=headers)

    if res.status_code == 200:
        project_data = res.json()
        actual_project_id = project_data["id"]
        print(f"Project found: {project_data['title']} (ID: {actual_project_id})")
    else:
        print(f"Failed to fetch project info (status: {res.status_code})")
        print(f"Response: {res.text}")
        print("Possible reasons:")
        print(f"1. Project ID/Slug '{MODRINTH_PROJECT_ID}' is incorrect.")
        print("2. Project is private/unlisted and the token does not have access.")
        print("3. The token is invalid.")
        return

    # 1. 既存バージョンの検索
    res = requests.get(f"https://api.modrinth.com/v2/project/{actual_project_id}/version", headers=headers)
    if res.status_code == 200:
        versions = res.json()
        for v in versions:
            if v["version_number"] == TAG_NAME:
                print(f"Found existing version '{TAG_NAME}' (ID: {v['id']}). Deleting...")
                del_res = requests.delete(f"https://api.modrinth.com/v2/version/{v['id']}", headers=headers)
                if del_res.status_code != 204:
                    print(f"Warning: Failed to delete version (status: {del_res.status_code})")
                print("Waiting 5 seconds for backend synchronization...")
                time.sleep(5)
    else:
        print(f"Note: Could not fetch existing versions (status: {res.status_code}). Proceeding anyway.")

    # (中略: JARファイルの検索部分は変更なし)
    jar_files = glob.glob("dist/**/*.jar", recursive=True)
    # sources, dev, common などの JAR を除外
    jar_files = [f for f in jar_files if not any(x in os.path.basename(f).lower() for x in ["sources", "dev", "common"])]
    
    if not jar_files:
        print("No JAR files found to upload!")
        return

    print(f"Found {len(jar_files)} JAR files to upload:")
    for f in jar_files:
        print(f"  - {f}")

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
            "primary": i == 0,
            "loaders": [loader],
            "game_versions": [mc_ver]
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
        "client_side": "required",
        "server_side": "unsupported",
        "files": [file_map[name] for name in file_map], # file_parts ではなく files
        "primary_file": list(file_map.keys())[0] if file_map else None
    }

    # data 内の各ファイル情報にパーツ名を紐付ける必要がある場合がある
    # Modrinth API の multipart 仕様に合わせる
    for i, part_name in enumerate(file_map.keys()):
        data["files"][i]["request_file"] = part_name

    # files_to_upload の構成
    # 各パート名に対応するファイルをタプルで指定
    # 'data' フィールドは json 形式の文字列で送る
    payload = {"data": json.dumps(data)}
    
    # 3. リクエスト送信
    res = requests.post(
        "https://api.modrinth.com/v2/version",
        headers=headers,
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
