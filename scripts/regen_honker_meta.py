import json
import os

def generate_meta():

    #avatars.json
    avatar = {}
    avatarPath = os.path.join("scripts", "assetsNew", "avatars.json")
    with open(avatarPath, "r") as f:
        avatarsJson = json.load(f)
        for avatarId in avatarsJson:
            required = avatarsJson[avatarId]["Promotion"]
            avatar[avatarId] = required.copy()

    #weapons.json
    equipment = {}
    equipmentSkill = {}
    weaponsPath = os.path.join("scripts", "assetsNew", "weapons.json")
    with open(weaponsPath, "r") as f:
        weaponsJson = json.load(f)
        for weaponId in weaponsJson:
            requiredEquipment = weaponsJson[weaponId]["Promotion"]
            requiredEquipmentSkill = weaponsJson[weaponId]["EquipmentSkill"]
            equipment[weaponId] = requiredEquipment.copy()
            equipmentSkill[weaponId] = requiredEquipmentSkill.copy()

    #relics.json
    relic = {
        "setSkill": {}
    }
    relicsPath = os.path.join("scripts", "assetsNew", "relics.json")
    with open(relicsPath, "r") as f:
        relicJson = json.load(f)
        sets = relicJson["Sets"]
        for setId in sets:
            requiredSetSkills = sets[setId]["SetSkills"]
            relic["setSkill"][setId] = requiredSetSkills.copy()

    #tree.json
    tree = {}
    treePath = os.path.join("scripts", "assetsNew", "tree.json")
    with open(treePath, "r") as f:
        treeJson = json.load(f)
        tree = treeJson

    #skills.json
    skills = {}
    skillsPath = os.path.join("scripts", "assetsNew", "skills.json")
    with open(skillsPath, "r") as f:
        skillsJson = json.load(f)
        skills = skillsJson

    data = {
        "avatar": avatar,
        "equipment": equipment,
        "equipmentSkill": equipmentSkill,
        "relic": relic,
        "tree": tree,
        "skills": skills
    }

    output_path = os.path.join("src", "main", "resources", "assets", "honker_meta.json")
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, "w") as f:
        json.dump(data, f, indent=4)

if __name__ == "__main__":
    generate_meta()
