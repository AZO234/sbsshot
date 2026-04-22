import json
import urllib.request
import xml.etree.ElementTree as ET
import re

# User-Agent を設定しないと一部の Maven で 403 になる
USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'

def get_content(url):
    req = urllib.request.Request(url, headers={'User-Agent': USER_AGENT})
    with urllib.request.urlopen(req) as response:
        return response.read()

def get_json(url):
    return json.loads(get_content(url).decode('utf-8'))

def get_maven_versions(metadata_url):
    content = get_content(metadata_url)
    tree = ET.fromstring(content)
    return [v.text for v in tree.findall(".//version")]

def find_latest_match(versions, pattern):
    matches = [v for v in versions if re.search(pattern, v)]
    return matches[-1] if matches else None

def is_stable_mc(ver):
    # 数字とドットのみ（例: 26.1.2）で、26.1から始まるもの
    return bool(re.match(r"^26\.1(\.\d+)?$", ver))

def update():
    print("Fetching Minecraft versions from Fabric Meta...")
    try:
        mc_versions_data = get_json("https://meta.fabricmc.net/v2/versions/game")
        # 安定版のみを抽出
        mc_versions = [v["version"] for v in mc_versions_data if is_stable_mc(v["version"])]
        mc_versions = sorted(list(set(mc_versions)))
    except Exception as e:
        print(f"Error fetching MC versions: {e}")
        return

    if not mc_versions:
        print("No stable Minecraft versions starting with 26.1 found.")
        return

    print(f"Targeting Minecraft versions: {mc_versions}")

    # 共通の最新版を取得
    print("Fetching common dependency versions...")
    try:
        fabric_loader = get_json("https://meta.fabricmc.net/v2/versions/loader")[0]["version"]
        fabric_loom = "1.16"
        
        modmenu_versions = get_maven_versions("https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/maven-metadata.xml")
        latest_modmenu = modmenu_versions[-1]
        
        mdg_versions = get_maven_versions("https://plugins.gradle.org/m2/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/maven-metadata.xml")
        latest_mdg = mdg_versions[-1]

        fapi_versions = get_maven_versions("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml")
        nf_versions = get_maven_versions("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml")
    except Exception as e:
        print(f"Error fetching dependencies: {e}")
        return

    new_config = {}

    for mc in mc_versions:
        print(f"Processing MC {mc}...")
        
        # Fabric API
        fapi = find_latest_match(fapi_versions, re.escape(mc))
        
        # NeoForge
        nf = find_latest_match(nf_versions, f"^{re.escape(mc)}\\.")

        if fapi and nf:
            new_config[mc] = {
                "fabric-loom": fabric_loom,
                "fabric-loader": fabric_loader,
                "fabric-api": fapi,
                "fabric-modmenu": latest_modmenu,
                "neoforge": nf,
                "neoforge-moddev": latest_mdg
            }
            print(f"  -> Found dependencies for {mc}")
        else:
            if not fapi: print(f"  -> [Warn] No Fabric API found for {mc}")
            if not nf: print(f"  -> [Warn] No NeoForge version found for {mc}")

    if new_config:
        with open("versions.json", "w") as f:
            json.dump(new_config, f, indent=2)
        print("\nSuccessfully updated versions.json")
    else:
        print("\nNo versions were updated.")

if __name__ == "__main__":
    update()
