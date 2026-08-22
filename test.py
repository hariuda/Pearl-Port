import re
html = 'data-last-price="25441207.60060408" data-tz-offset=0'
match = re.search(r'data-last-price="([0-9.]+)"', html)
print(match.group(1) if match else "No")
