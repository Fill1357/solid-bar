import base64, lzma, os, shutil

version = os.environ.get('VERSION', '1.0.0')

with open('dist/dex/release/classes.dex', 'rb') as f:
    dex_data = f.read()

compressed = lzma.compress(dex_data)
b64_data = base64.b64encode(compressed).decode('ascii')

lines = []
i = 0
while i < len(b64_data):
    lines.append('# ' + b64_data[i:i+76])
    i += 76
dex_text = '\n'.join(lines)

with open('solid_actionbar.py', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('PLACEHOLDER_VERSION', version)
content = content.replace('PLACEHOLDER_DEX', dex_text)

release_dir = os.environ.get('RUNNER_TEMP', '/tmp') + '/release'
os.makedirs(release_dir, exist_ok=True)

output_path = os.path.join(release_dir, 'solid_actionbar.plugin')
with open(output_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Plugin created: " + output_path)
os.system('ls -lh ' + output_path)
