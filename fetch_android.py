
import urllib.request, xml.etree.ElementTree as ET, sys

API = sys.argv[1]
url = "https://dl.google.com/android/repository/repository2-3.xml"
with urllib.request.urlopen(url) as r:
    root = ET.parse(r).getroot()

for pkg in root.findall('.//{*}remotePackage'):
    if pkg.get('path') == f'platforms;android-{API}':
        fname = pkg.find('.//{*}url').text
        print(f"https://dl.google.com/android/repository/{fname}")
        break
    