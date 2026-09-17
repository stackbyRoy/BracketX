import os
from PIL import Image, ImageDraw

source_icon = r"logo/BracketX app icon.png"
if not os.path.exists(source_icon):
    raise FileNotFoundError(f"Source icon not found: {source_icon}")

img = Image.open(source_icon).convert("RGBA")
print(f"Loaded source icon: {img.size}")

# 1. WEB ASSETS
web_public = r"web/public"
os.makedirs(web_public, exist_ok=True)

# Save main logo
img.save(os.path.join(web_public, "logo.png"), "PNG")
print("Saved web/public/logo.png")

# Save favicon.png (64x64)
img.resize((64, 64), Image.Resampling.LANCZOS).save(os.path.join(web_public, "favicon.png"), "PNG")
print("Saved web/public/favicon.png")

# Save PWA icons (192x192, 512x512)
img.resize((192, 192), Image.Resampling.LANCZOS).save(os.path.join(web_public, "icon-192.png"), "PNG")
img.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(web_public, "icon-512.png"), "PNG")
print("Saved web/public PWA icons")

# 2. ANDROID ASSETS
android_res = r"app/src/main/res"

mipmap_sizes = {
    "mipmap-mdpi": (48, 48),
    "mipmap-hdpi": (72, 72),
    "mipmap-xhdpi": (96, 96),
    "mipmap-xxhdpi": (144, 144),
    "mipmap-xxxhdpi": (192, 192),
}

def make_round(im):
    size = im.size
    mask = Image.new('L', size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size[0], size[1]), fill=255)
    output = im.copy()
    output.putalpha(mask)
    return output

for folder, size in mipmap_sizes.items():
    dir_path = os.path.join(android_res, folder)
    os.makedirs(dir_path, exist_ok=True)
    
    # Standard square/squircle icon
    resized = img.resize(size, Image.Resampling.LANCZOS)
    resized.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    
    # Round icon
    round_resized = make_round(resized)
    round_resized.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")
    print(f"Generated Android {folder} ({size[0]}x{size[1]})")

# Adaptive icon foreground (432x432 with icon centered at 288x288 to respect safe zone)
drawable_dir = os.path.join(android_res, "drawable")
os.makedirs(drawable_dir, exist_ok=True)

adaptive_fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
icon_scaled = img.resize((300, 300), Image.Resampling.LANCZOS)
offset = ((432 - 300) // 2, (432 - 300) // 2)
adaptive_fg.paste(icon_scaled, offset, icon_scaled)
adaptive_fg.save(os.path.join(drawable_dir, "ic_launcher_foreground.png"), "PNG")
print("Saved adaptive icon foreground to drawable/ic_launcher_foreground.png")

print("ALL ICONS GENERATED SUCCESSFULLY!")
