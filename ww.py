import numpy as np
from PIL import Image

r"""img = np.array(Image.open(r"C:\Users\Administrator\Downloads\logow.png").convert("RGBA"))
img[np.all(img == (0,0,0,255), axis=-1)] = (0, 0,0,0)

Image.fromarray(img).resize((256,256),0).convert("P").save(r"C:\Users\Administrator\Downloads\logo.png")"""

Image.open(r"C:\Users\Administrator\Downloads\channels4_profile.jpg").save("w.png")